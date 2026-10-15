package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Devolucao;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemDevolucao;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.StatusDevolucao;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.FinalidadeNotaCredito;
import ao.allon.kubata.faturacao.domain.enums.MeioConhecimentoAdquirente;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.dto.NotaCreditoItemRequest;
import ao.allon.kubata.faturacao.repository.DevolucaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final FaturaService faturaService;
    private final ProdutoService produtoService;
    private final SessionManager sessionManager;

    public DevolucaoService(DevolucaoRepository devolucaoRepository, FaturaService faturaService,
                            ProdutoService produtoService, SessionManager sessionManager) {
        this.devolucaoRepository = devolucaoRepository;
        this.faturaService = faturaService;
        this.produtoService = produtoService;
        this.sessionManager = sessionManager;
    }

    public List<Devolucao> findAll() {
        return devolucaoRepository.findAll();
    }

    public Optional<Devolucao> findById(Long id) {
        return devolucaoRepository.findById(id);
    }

    public List<Devolucao> findByStatus(StatusDevolucao status) {
        return devolucaoRepository.findByStatus(status);
    }

    /**
     * Cria um pedido de devolução ligado às linhas exactas da factura emitida.
     * Quantidades e preço unitário são validados no servidor; não se confia nos
     * dados enviados pela interface nem no preço actual do catálogo.
     */
    @Transactional
    public Devolucao criarSolicitacao(Devolucao devolucao) {
        if (devolucao == null) {
            throw new IllegalArgumentException("A solicitação de devolução é obrigatória.");
        }
        if (devolucao.getId() != null) {
            throw new IllegalArgumentException(
                    "Uma nova solicitação não pode conter um ID já persistido.");
        }
        if (devolucao.getItens() == null || devolucao.getItens().isEmpty()) {
            throw new IllegalArgumentException("A devolução deve conter pelo menos uma linha da factura original.");
        }

        Long origemId = devolucao.getFaturaOrigem() == null ? null : devolucao.getFaturaOrigem().getId();
        Long clienteId = devolucao.getCliente() == null ? null : devolucao.getCliente().getId();
        Fatura original = carregarFacturaElegivel(origemId, clienteId);
        devolucao.setFaturaOrigem(original);
        devolucao.setCliente(original.getCliente());

        Map<Long, ItemFatura> linhasOriginais = indexarLinhasPersistidas(original);
        Set<Long> linhasSolicitadas = new HashSet<>();
        Map<Long, BigDecimal> quantidadesSolicitadas = new HashMap<>();

        for (ItemDevolucao item : devolucao.getItens()) {
            if (item == null || item.getItemFaturaOrigem() == null
                    || item.getItemFaturaOrigem().getId() == null) {
                throw new IllegalArgumentException(
                        "Cada linha devolvida deve referenciar a linha exacta da factura original.");
            }

            Long linhaId = item.getItemFaturaOrigem().getId();
            ItemFatura linhaOriginal = linhasOriginais.get(linhaId);
            if (linhaOriginal == null) {
                throw new IllegalArgumentException(
                        "A linha " + linhaId + " não pertence à factura original seleccionada.");
            }
            if (!linhasSolicitadas.add(linhaId)) {
                throw new IllegalArgumentException(
                        "A mesma linha foi adicionada mais de uma vez. Consolide as quantidades numa única linha.");
            }
            if (linhaOriginal.getProduto() == null || linhaOriginal.getProduto().getId() == null) {
                throw new IllegalArgumentException(
                        "A linha " + linhaId + " não está ligada a um artigo de stock devolvível.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade a devolver deve ser maior que zero.");
            }

            item.setDevolucao(devolucao);
            item.setItemFaturaOrigem(linhaOriginal);
            item.setProduto(linhaOriginal.getProduto());
            item.setValorUnitario(linhaOriginal.getPrecoUnitario());
            item.calculateTotal();
            quantidadesSolicitadas.put(linhaId, BigDecimal.valueOf(item.getQuantidade()));
        }

        // Reservar quantidades já pedidas e ainda abertas evita criar vários
        // pedidos que, em conjunto, excedam a quantidade facturada.
        Map<Long, BigDecimal> reservadas = new HashMap<>();
        List<Devolucao> abertas = devolucaoRepository.findOpenByFaturaOrigem(
                original.getId(),
                List.of(StatusDevolucao.PENDENTE, StatusDevolucao.ANALISE, StatusDevolucao.APROVADA));
        if (abertas != null) {
            for (Devolucao existente : abertas) {
                if (devolucao.getId() != null && Objects.equals(devolucao.getId(), existente.getId())) {
                    continue;
                }
                if (existente.getItens() == null) continue;
                for (ItemDevolucao linha : existente.getItens()) {
                    if (linha.getItemFaturaOrigem() == null
                            || linha.getItemFaturaOrigem().getId() == null
                            || linha.getQuantidade() == null) {
                        throw new IllegalStateException(
                                "A devolução aberta " + existente.getNumero()
                                + " é anterior ao controlo por linha da factura e não tem referência fiscal completa. "
                                + "Reconcilie-a antes de criar novos pedidos para evitar crédito duplicado.");
                    }
                    reservadas.merge(linha.getItemFaturaOrigem().getId(),
                            BigDecimal.valueOf(linha.getQuantidade()), BigDecimal::add);
                }
            }
        }

        for (Map.Entry<Long, BigDecimal> pedido : quantidadesSolicitadas.entrySet()) {
            ItemFatura linha = linhasOriginais.get(pedido.getKey());
            BigDecimal quantidadeFacturada = linha.getQuantidadeDecimal() != null
                    ? linha.getQuantidadeDecimal()
                    : BigDecimal.valueOf(linha.getQuantidade() == null ? 0 : linha.getQuantidade());
            BigDecimal jaReservada = reservadas.getOrDefault(pedido.getKey(), BigDecimal.ZERO);
            if (pedido.getValue().add(jaReservada).compareTo(quantidadeFacturada) > 0) {
                throw new IllegalArgumentException(
                        "A quantidade solicitada excede o saldo facturado da linha " + pedido.getKey()
                        + ". Facturada: " + quantidadeFacturada + "; já reservada: " + jaReservada
                        + "; nova solicitação: " + pedido.getValue() + ".");
            }
        }

        devolucao.setStatus(StatusDevolucao.PENDENTE);
        devolucao.setDataSolicitacao(LocalDateTime.now());
        devolucao.setNumero("DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        devolucao.setUsuarioSolicitante(sessionManager.getCurrentUser());
        return devolucaoRepository.save(devolucao);
    }

    private Fatura carregarFacturaElegivel(Long faturaId, Long clienteId) {
        if (faturaId == null) {
            throw new IllegalArgumentException("Seleccione a factura original da devolução.");
        }
        if (clienteId == null) {
            throw new IllegalArgumentException("Seleccione um cliente válido.");
        }

        Fatura original = faturaService.findByIdWithItens(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura original não encontrada."));
        if (original.getCliente() == null || !Objects.equals(original.getCliente().getId(), clienteId)) {
            throw new IllegalArgumentException("A factura original não pertence ao cliente seleccionado.");
        }
        if (original.getStatus() != StatusFatura.EMITIDA && original.getStatus() != StatusFatura.PAGA) {
            throw new IllegalStateException("Só se aceitam devoluções de facturas emitidas ou pagas.");
        }
        TipoDocumento tipo = original.getTipoDocumento();
        if (tipo != TipoDocumento.FATURA && tipo != TipoDocumento.FATURA_RECIBO
                && tipo != TipoDocumento.VENDA_A_DINHEIRO) {
            throw new IllegalArgumentException("O tipo do documento seleccionado não admite devolução neste fluxo.");
        }
        return original;
    }

    private Map<Long, ItemFatura> indexarLinhasPersistidas(Fatura original) {
        Map<Long, ItemFatura> linhas = new HashMap<>();
        if (original.getItens() == null) return linhas;
        for (ItemFatura item : original.getItens()) {
            if (item.getId() != null) {
                linhas.put(item.getId(), item);
            }
        }
        return linhas;
    }

    @Transactional
    public Devolucao analisarSolicitacao(Long id, boolean aprovar, String parecer) {
        Devolucao devolucao = devolucaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Devolução não encontrada."));

        if (devolucao.getStatus() != StatusDevolucao.PENDENTE) {
            throw new IllegalStateException("A devolução não está pendente de análise.");
        }

        devolucao.setDataAnalise(LocalDateTime.now());
        devolucao.setParecerAnalise(parecer);
        devolucao.setUsuarioAnalista(sessionManager.getCurrentUser());
        devolucao.setStatus(aprovar ? StatusDevolucao.APROVADA : StatusDevolucao.REJEITADA);
        return devolucaoRepository.save(devolucao);
    }

    /**
     * Retoma um processo de devolução usando a evidência já registada.
     * Se não houver prova do conhecimento do adquirente, a emissão da NC
     * falha de forma explícita; não são fabricados dados para satisfazer a regra fiscal.
     */
    @Transactional
    public Devolucao processarDevolucao(Long id) {
        Devolucao devolucao = devolucaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Devolução não encontrada."));
        return processarDevolucao(id,
                devolucao.getFinalidadeNotaCredito(),
                devolucao.getMeioConhecimentoAdquirente(),
                devolucao.getMotivoNotaCredito(),
                devolucao.getComprovativoConhecimentoAdquirente(),
                devolucao.getDataConhecimentoAdquirente());
    }

    @Transactional
    public Devolucao processarDevolucao(Long id,
                                        FinalidadeNotaCredito finalidade,
                                        MeioConhecimentoAdquirente meioConhecimento,
                                        String motivoNotaCredito,
                                        String comprovativoConhecimento,
                                        LocalDateTime dataConhecimento) {
        Devolucao devolucao = devolucaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Devolução não encontrada."));

        if (devolucao.getStatus() != StatusDevolucao.APROVADA) {
            throw new IllegalStateException("A devolução deve estar aprovada para ser processada.");
        }
        if (devolucao.getFaturaOrigem() == null || devolucao.getItens() == null
                || devolucao.getItens().isEmpty()) {
            throw new IllegalStateException(
                    "A devolução aprovada não contém factura de origem e linhas para creditar.");
        }
        if (finalidade == null || meioConhecimento == null
                || motivoNotaCredito == null || motivoNotaCredito.isBlank()
                || comprovativoConhecimento == null || comprovativoConhecimento.isBlank()
                || dataConhecimento == null) {
            throw new IllegalArgumentException(
                    "Para processar a devolução, informe a finalidade da nota, o motivo, o meio, a prova e a data do conhecimento do adquirente.");
        }
        if (dataConhecimento.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("A data de conhecimento do adquirente não pode estar no futuro.");
        }

        List<NotaCreditoItemRequest> linhasParaCreditar = new ArrayList<>();
        for (ItemDevolucao item : devolucao.getItens()) {
            if (item.getItemFaturaOrigem() == null || item.getItemFaturaOrigem().getId() == null
                    || item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalStateException(
                        "A devolução contém uma linha incompleta ou sem referência à factura original.");
            }
            linhasParaCreditar.add(new NotaCreditoItemRequest(
                    item.getItemFaturaOrigem().getId(),
                    BigDecimal.valueOf(item.getQuantidade())));
        }

        devolucao.setFinalidadeNotaCredito(finalidade);
        devolucao.setMeioConhecimentoAdquirente(meioConhecimento);
        devolucao.setMotivoNotaCredito(motivoNotaCredito.trim());
        devolucao.setComprovativoConhecimentoAdquirente(comprovativoConhecimento.trim());
        devolucao.setDataConhecimentoAdquirente(dataConhecimento);

        // A nota de crédito inclui só as quantidades aprovadas. O fluxo fiscal
        // de emissão da NC repõe o stock; não se lança uma segunda entrada aqui.
        Fatura notaCredito = faturaService.emitirNotaCredito(
                devolucao.getFaturaOrigem().getId(),
                motivoNotaCredito.trim(),
                finalidade,
                meioConhecimento,
                comprovativoConhecimento.trim(),
                dataConhecimento,
                linhasParaCreditar);

        devolucao.setNotaCredito(notaCredito);
        devolucao.setDataConclusao(LocalDateTime.now());
        devolucao.setStatus(StatusDevolucao.CONCLUIDA);
        return devolucaoRepository.save(devolucao);
    }

    @Transactional
    public void processarDevolucaoCustomizada(Devolucao devolucao) {
        if (devolucao == null || devolucao.getId() == null) {
            throw new IllegalArgumentException(
                    "Seleccione uma solicitação de devolução já gravada para processar.");
        }
        processarDevolucao(devolucao.getId());
    }
}
