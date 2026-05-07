package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.enums.TipoMovimento;
import ao.allon.kubata.faturacao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Serviço robusto de FIFO (First-In, First-Out) para gestão de lotes e estoque.
 * Garante consumo por ordem de entrada, rastreabilidade de lotes e integridade de dados.
 */
@Service
public class FifoService {

    private final EstoqueRepository estoqueRepository;
    private final ItemFaturaRepository itemFaturaRepository;
    private final MovimentoStockRepository movimentoStockRepository;
    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;

    public FifoService(EstoqueRepository estoqueRepository,
                       ItemFaturaRepository itemFaturaRepository,
                       MovimentoStockRepository movimentoStockRepository,
                       ProdutoRepository produtoRepository,
                       ArmazemRepository armazemRepository) {
        this.estoqueRepository = estoqueRepository;
        this.itemFaturaRepository = itemFaturaRepository;
        this.movimentoStockRepository = movimentoStockRepository;
        this.produtoRepository = produtoRepository;
        this.armazemRepository = armazemRepository;
    }

    /**
     * Verifica se há estoque suficiente para atender uma demanda via FIFO.
     * @param produto Produto a ser consumido
     * @param quantidade Quantidade desejada
     * @return true se houver estoque suficiente nos lotes mais antigos
     */
    @Transactional(readOnly = true)
    public boolean podeConsumirPorFIFO(Produto produto, int quantidade) {
        if (produto == null || quantidade <= 0) return false;
        List<Estoque> lotes = getLotesOrdenadosPorFIFO(produto);
        int disponivel = lotes.stream()
                .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                .mapToInt(Estoque::getQuantidade)
                .sum();
        return disponivel >= quantidade;
    }

    /**
     * Consome estoque via FIFO, registrando movimento e retornando detalhes dos lotes consumidos.
     * @param produto Produto a ser consumido
     * @param quantidade Quantidade a consumir
     * @param observacao Observação do movimento
     * @return Mapa com breakdown por lote (lote -> quantidade consumida)
     */
    @Transactional
    public Map<String, Integer> consumirPorFIFO(Produto produto, int quantidade, String observacao) {
        if (!podeConsumirPorFIFO(produto, quantidade)) {
            throw new IllegalStateException("Estoque insuficiente para consumir " + quantidade + " do produto " + produto.getNome());
        }

        List<Estoque> lotes = getLotesOrdenadosPorFIFO(produto);
        int restante = quantidade;
        Map<String, Integer> breakdown = new LinkedHashMap<>();

        // Validação adicional: garante que temos saldo total suficiente antes de começar
        int totalDisponivel = lotes.stream()
                .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                .mapToInt(Estoque::getQuantidade)
                .sum();
                
        if (totalDisponivel < quantidade) {
            throw new IllegalStateException("Estoque insuficiente (Saldo real: " + totalDisponivel + ") para consumir " + quantidade + " do produto " + produto.getNome());
        }

        for (Estoque lote : lotes) {
            if (restante <= 0) break;
            if (lote.getQuantidade() == null || lote.getQuantidade() <= 0) continue;

            int usar = Math.min(restante, lote.getQuantidade());
            int saldoAnterior = lote.getQuantidade();
            lote.setQuantidade(saldoAnterior - usar);
            
            // Fix: Ensure dataEntrada is not null to avoid DataIntegrityViolationException
            if (lote.getDataEntrada() == null) {
                lote.setDataEntrada(LocalDate.now());
            }
            
            estoqueRepository.save(lote);

            breakdown.put(lote.getLote() != null ? lote.getLote() : "SEM_LOTE", usar);

            registrarMovimento(produto, lote.getArmazem(), TipoMovimento.SAIDA, usar,
                    saldoAnterior, lote.getQuantidade(),
                    lote.getLote(), observacao != null ? observacao : "Consumo FIFO");

            restante -= usar;
        }

        if (restante > 0) {
            throw new IllegalStateException("Erro inesperado: não foi possível consumir toda a quantidade via FIFO. Restante: " + restante);
        }

        atualizarStockProduto(produto);
        return breakdown;
    }

    /**
     * Aloca lotes via FIFO para uma fatura (sem consumir ainda). Útil para pré-reserva.
     * @param fatura Fatura com itens
     * @return Mapa: produtoId -> breakdown (lote -> quantidade)
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Integer>> alocarLotesParaFatura(Fatura fatura) {
        Map<Long, Map<String, Integer>> alocacao = new HashMap<>();

        for (ItemFatura item : fatura.getItens()) {
            if (item.getProduto() == null || item.getQuantidade() == null) continue;

            Produto produto = item.getProduto();
            int quantidade = item.getQuantidade();

            if (!podeConsumirPorFIFO(produto, quantidade)) {
                throw new IllegalStateException("Estoque insuficiente para alocar via FIFO: " + produto.getNome() + " (qtd: " + quantidade + ")");
            }

            List<Estoque> lotes = getLotesOrdenadosPorFIFO(produto);
            int restante = quantidade;
            Map<String, Integer> breakdown = new LinkedHashMap<>();

            for (Estoque lote : lotes) {
                if (restante <= 0) break;
                if (lote.getQuantidade() == null || lote.getQuantidade() <= 0) continue;

                int usar = Math.min(restante, lote.getQuantidade());
                breakdown.put(lote.getLote() != null ? lote.getLote() : "SEM_LOTE", usar);
                restante -= usar;
            }

            alocacao.put(produto.getId(), breakdown);
        }

        return alocacao;
    }

    /**
     * Processa uma fatura consumindo estoque via FIFO e gravando lotes nos itens.
     * @param fatura Fatura a ser processada
     * @param observacao Observação geral do movimento
     */
    @Transactional
    public void processarFaturaComFIFO(Fatura fatura, String observacao) {
        Map<Long, Map<String, Integer>> alocacao = alocarLotesParaFatura(fatura);

        for (ItemFatura item : fatura.getItens()) {
            if (item.getProduto() == null || item.getQuantidade() == null) continue;

            Produto produto = item.getProduto();
            Map<String, Integer> breakdown = alocacao.get(produto.getId());

            // Grava o lote principal no item (primeiro lote consumido)
            if (breakdown != null && !breakdown.isEmpty()) {
                String primeiroLote = breakdown.keySet().iterator().next();
                item.setLote(primeiroLote);
            }

            // Consome efetivamente o estoque
            consumirPorFIFO(produto, item.getQuantidade(),
                    observacao != null ? observacao : "Venda Fatura #" + fatura.getNumero());
        }
    }

    /**
     * Retorna lotes ordenados por FIFO (data_entrada ASC, lote ASC).
     */
    private List<Estoque> getLotesOrdenadosPorFIFO(Produto produto) {
        List<Estoque> lotes = estoqueRepository.findByProduto(produto);
        lotes.sort((a, b) -> {
            int cmp = 0;
            if (a.getDataEntrada() != null && b.getDataEntrada() != null) {
                cmp = a.getDataEntrada().compareTo(b.getDataEntrada());
            } else if (a.getDataEntrada() == null && b.getDataEntrada() != null) {
                cmp = 1;
            } else if (a.getDataEntrada() != null && b.getDataEntrada() == null) {
                cmp = -1;
            }
            if (cmp == 0) {
                String la = a.getLote() != null ? a.getLote() : "";
                String lb = b.getLote() != null ? b.getLote() : "";
                cmp = la.compareTo(lb);
            }
            return cmp;
        });
        return lotes;
    }

    private void registrarMovimento(Produto produto, Armazem armazem, TipoMovimento tipo,
                                   Integer qtd, Integer anterior, Integer atual,
                                   String lote, String obs) {
        MovimentoStock mov = new MovimentoStock();
        mov.setProduto(produto);
        mov.setArmazem(armazem);
        mov.setTipoMovimento(tipo);
        mov.setQuantidade(qtd);
        mov.setSaldoAnterior(anterior);
        mov.setSaldoAtual(atual);
        mov.setLote(lote);
        mov.setObservacao(obs);
        movimentoStockRepository.save(mov);
    }

    private void atualizarStockProduto(Produto produto) {
        Integer total = estoqueRepository.sumQuantidadeByProduto(produto);
        produto.setStock(total != null ? total : 0);
        // Garante que não haverá erro de null em dataEntrada ao salvar Produto (se Produto tiver campo dataEntrada)
        // Nota: Produto não tem dataEntrada, mas se tiver no futuro, já garante valor padrão
        produtoRepository.save(produto);
    }

    /**
     * Obtém resumo de estoque FIFO para um produto: lote, quantidade, data entrada, validade.
     */
    @Transactional(readOnly = true)
    public List<ResumoLoteFifoDto> resumoPorProduto(Produto produto) {
        List<Estoque> lotes = getLotesOrdenadosPorFIFO(produto);
        List<ResumoLoteFifoDto> resumo = new ArrayList<>();

        for (Estoque e : lotes) {
            if (e.getQuantidade() == null || e.getQuantidade() <= 0) continue;
            resumo.add(new ResumoLoteFifoDto(
                    e.getLote(),
                    e.getQuantidade(),
                    e.getDataEntrada(),
                    e.getValidade(),
                    e.getPrecoCompra(),
                    e.getPrecoVenda(),
                    e.getArmazem() != null ? e.getArmazem().getNome() : null,
                    e.getArmazem() != null ? e.getArmazem().getId() : null
            ));
        }

        return resumo;
    }

    /**
     * DTO para resumo de lote FIFO.
     */
    public record ResumoLoteFifoDto(
            String lote,
            Integer quantidade,
            LocalDate dataEntrada,
            LocalDate validade,
            BigDecimal precoCompra,
            BigDecimal precoVenda,
            String nomeArmazem,
            Long armazemId
    ) {}
}
