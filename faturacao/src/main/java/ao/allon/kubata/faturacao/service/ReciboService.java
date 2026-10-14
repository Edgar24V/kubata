package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReciboService {

    private static final Logger log = LoggerFactory.getLogger(ReciboService.class);

    private final ReciboRepository reciboRepository;
    private final FaturaRepository faturaRepository;
    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;

    @Value("${faturacao.contabilidade.obrigatoria:true}")
    private boolean contabilidadeObrigatoria = true;

    public ReciboService(ReciboRepository reciboRepository, FaturaRepository faturaRepository,
                         ContabilidadeService contabilidadeService, PlanoContaRepository planoContaRepository) {
        this.reciboRepository = reciboRepository;
        this.faturaRepository = faturaRepository;
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
    }

    public List<Recibo> findAll() {
        return reciboRepository.findAll();
    }

    public List<Recibo> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return reciboRepository.findByDataRecebimentoBetween(inicio, fim);
    }

    public List<Recibo> findByFatura(Long faturaId) {
        return reciboRepository.findByFaturaId(faturaId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRecebidoFatura(Long faturaId) {
        BigDecimal total = reciboRepository.sumValorByFaturaId(faturaId);
        return dinheiro(total);
    }

    /**
     * Regista um recebimento real e actualiza a liquidação da factura.
     * O recebimento e o respectivo lançamento contabilístico pertencem à mesma
     * transacção: em produção, uma falha contabilística faz rollback do recibo.
     */
    @Transactional
    public Recibo registrar(Long faturaId, BigDecimal valor, String referencia, String observacoes) {
        return registrar(faturaId, valor, referencia, observacoes, MetodoPagamento.DINHEIRO);
    }

    @Transactional
    public Recibo registrar(Long faturaId, BigDecimal valor, String referencia,
                            String observacoes, MetodoPagamento metodoPagamento) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor recebido deve ser maior que zero.");
        }
        if (metodoPagamento == null) {
            throw new IllegalArgumentException("O método de pagamento é obrigatório.");
        }

        BigDecimal valorRecebido = dinheiro(valor);
        if (valorRecebido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor recebido deve ser de pelo menos 0,01.");
        }

        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura não encontrada."));

        if (fatura.getStatus() != StatusFatura.EMITIDA) {
            throw new IllegalStateException(
                    "Só é possível receber pagamentos de facturas emitidas e ainda não liquidadas.");
        }
        TipoDocumento tipo = fatura.getTipoDocumento();
        if (tipo != TipoDocumento.FATURA && tipo != TipoDocumento.NOTA_DEBITO) {
            throw new IllegalStateException(
                    "Este tipo de documento não aceita recebimentos parciais por este fluxo.");
        }

        BigDecimal totalDocumento = dinheiro(fatura.getTotal());
        if (totalDocumento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O documento não tem um total válido para liquidação.");
        }

        BigDecimal recebidoAnteriormente = totalRecebidoFatura(faturaId);
        // Reconcilia dados legados antes de avançar. A coluna é actualizada em
        // cada pagamento e, combinada com @Version em Fatura, detecta concorrência.
        BigDecimal recebidoPersistido = dinheiro(fatura.getValorRecebido());
        if (recebidoPersistido.compareTo(recebidoAnteriormente) != 0) {
            fatura.setValorRecebido(recebidoAnteriormente);
        }
        BigDecimal pendente = totalDocumento.subtract(recebidoAnteriormente).setScale(2, RoundingMode.HALF_UP);
        if (pendente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("A factura já se encontra totalmente liquidada.");
        }
        if (valorRecebido.compareTo(pendente) > 0) {
            throw new IllegalArgumentException(
                    "O valor recebido (" + valorRecebido.toPlainString()
                    + ") excede o saldo pendente (" + pendente.toPlainString() + ").");
        }

        Recibo recibo = new Recibo();
        recibo.setNumero(gerarNumeroRecibo());
        recibo.setFatura(fatura);
        recibo.setDataRecebimento(LocalDate.now());
        recibo.setValor(valorRecebido);
        recibo.setMetodoPagamento(metodoPagamento);
        recibo.setReferencia(referencia);
        recibo.setObservacoes(observacoes);

        Recibo salvo = reciboRepository.save(recibo);

        // Não engolir falhas contabilísticas em produção: a transacção tem de
        // falhar em conjunto, evitando recibos persistidos sem o movimento financeiro.
        lancarNaContabilidade(salvo);

        BigDecimal liquidado = recebidoAnteriormente.add(valorRecebido).setScale(2, RoundingMode.HALF_UP);
        fatura.setValorRecebido(liquidado); // força actualização versionada até nos pagamentos parciais
        fatura.setStatus(liquidado.compareTo(totalDocumento) >= 0
                ? StatusFatura.PAGA : StatusFatura.EMITIDA);
        faturaRepository.save(fatura);
        return salvo;
    }

    private void lancarNaContabilidade(Recibo recibo) {
        PlanoConta contaCaixa = planoContaRepository.findByCodigo("45.1").orElse(null);
        PlanoConta contaCliente = planoContaRepository.findByCodigo("31.1").orElse(null);

        if (contaCaixa == null || contaCliente == null) {
            String message = "Contas contabilísticas 45.1 (caixa) e 31.1 (clientes) são necessárias para registar o recibo.";
            if (contabilidadeObrigatoria) {
                throw new IllegalStateException(message);
            }
            log.warn("{} O recibo {} não gerou lançamento porque a contabilidade é opcional neste perfil.",
                    message, recibo.getNumero());
            return;
        }

        try {
            contabilidadeService.lancar(
                    recibo.getDataRecebimento(),
                    contaCaixa,
                    contaCliente,
                    recibo.getValor(),
                    "Recebimento - Recibo " + recibo.getNumero(),
                    recibo.getNumero(),
                    TipoDocumento.RECIBO.name(),
                    null
            );
        } catch (RuntimeException ex) {
            if (contabilidadeObrigatoria) {
                throw new IllegalStateException(
                        "Não foi possível lançar contabilisticamente o recibo " + recibo.getNumero() + ".", ex);
            }
            log.error("Falha contabilística no recibo {} (permitida apenas pelo perfil não produtivo).",
                    recibo.getNumero(), ex);
        }
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : valor.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * A numeração por contagem mantém compatibilidade com os dados existentes.
     * Em instalações com emissão concorrente, a sequência deve ser migrada para
     * uma série documental transaccional antes de tratar este recibo como fiscal.
     */
    private String gerarNumeroRecibo() {
        int year = LocalDate.now().getYear();
        long count = reciboRepository.count();
        return String.format("RC%d%06d", year, count + 1);
    }
}
