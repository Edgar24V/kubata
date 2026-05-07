package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.repository.EstoqueRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.faturacao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RelatorioEstoqueLoteService {

    private final EstoqueRepository estoqueRepository;
    private final ItemFaturaRepository itemFaturaRepository;

    public static class MargemLoteDto {
        private final String lote;
        private final BigDecimal precoCompra;
        private final BigDecimal precoVenda;
        private final Integer quantidadeEntrada;
        private final Integer quantidadeAtual;
        private final Integer quantidadeVendida;
        private final BigDecimal receita;
        private final BigDecimal custo;
        private final BigDecimal margem;
        private final LocalDate dataEntrada;
        private final LocalDate validade;

        public MargemLoteDto(String lote,
                             BigDecimal precoCompra,
                             BigDecimal precoVenda,
                             Integer quantidadeEntrada,
                             Integer quantidadeAtual,
                             Integer quantidadeVendida,
                             BigDecimal receita,
                             BigDecimal custo,
                             BigDecimal margem,
                             LocalDate dataEntrada,
                             LocalDate validade) {
            this.lote = lote;
            this.precoCompra = precoCompra;
            this.precoVenda = precoVenda;
            this.quantidadeEntrada = quantidadeEntrada;
            this.quantidadeAtual = quantidadeAtual;
            this.quantidadeVendida = quantidadeVendida;
            this.receita = receita;
            this.custo = custo;
            this.margem = margem;
            this.dataEntrada = dataEntrada;
            this.validade = validade;
        }

        public String getLote() { return lote; }
        public BigDecimal getPrecoCompra() { return precoCompra; }
        public BigDecimal getPrecoVenda() { return precoVenda; }
        public Integer getQuantidadeEntrada() { return quantidadeEntrada; }
        public Integer getQuantidadeAtual() { return quantidadeAtual; }
        public Integer getQuantidadeVendida() { return quantidadeVendida; }
        public BigDecimal getReceita() { return receita; }
        public BigDecimal getCusto() { return custo; }
        public BigDecimal getMargem() { return margem; }
        public LocalDate getDataEntrada() { return dataEntrada; }
        public LocalDate getValidade() { return validade; }
    }

    public static class ValorizacaoEstoqueDto {
        private final Long produtoId;
        private final String codigo;
        private final String nome;
        private final Integer stockTotal;
        private final BigDecimal custoMedio;
        private final BigDecimal ultimoCusto;
        private final BigDecimal valorTotalCustoMedio;
        private final BigDecimal valorTotalUltimoCusto;
        private final BigDecimal precoVendaAtual;
        private final BigDecimal valorTotalVenda;

        public ValorizacaoEstoqueDto(Long produtoId, String codigo, String nome, Integer stockTotal,
                                     BigDecimal custoMedio, BigDecimal ultimoCusto,
                                     BigDecimal valorTotalCustoMedio, BigDecimal valorTotalUltimoCusto,
                                     BigDecimal precoVendaAtual, BigDecimal valorTotalVenda) {
            this.produtoId = produtoId;
            this.codigo = codigo;
            this.nome = nome;
            this.stockTotal = stockTotal;
            this.custoMedio = custoMedio;
            this.ultimoCusto = ultimoCusto;
            this.valorTotalCustoMedio = valorTotalCustoMedio;
            this.valorTotalUltimoCusto = valorTotalUltimoCusto;
            this.precoVendaAtual = precoVendaAtual;
            this.valorTotalVenda = valorTotalVenda;
        }

        public Long getProdutoId() { return produtoId; }
        public String getCodigo() { return codigo; }
        public String getNome() { return nome; }
        public Integer getStockTotal() { return stockTotal; }
        public BigDecimal getCustoMedio() { return custoMedio; }
        public BigDecimal getUltimoCusto() { return ultimoCusto; }
        public BigDecimal getValorTotalCustoMedio() { return valorTotalCustoMedio; }
        public BigDecimal getValorTotalUltimoCusto() { return valorTotalUltimoCusto; }
        public BigDecimal getPrecoVendaAtual() { return precoVendaAtual; }
        public BigDecimal getValorTotalVenda() { return valorTotalVenda; }
    }

    private final ProdutoRepository produtoRepository;

    public RelatorioEstoqueLoteService(EstoqueRepository estoqueRepository,
                                       ItemFaturaRepository itemFaturaRepository,
                                       ProdutoRepository produtoRepository) {
        this.estoqueRepository = estoqueRepository;
        this.itemFaturaRepository = itemFaturaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public List<ValorizacaoEstoqueDto> gerarRelatorioValorizacao() {
        List<Produto> produtos = produtoRepository.findAll();
        List<ValorizacaoEstoqueDto> relatorio = new ArrayList<>();

        for (Produto p : produtos) {
            int stock = p.getStock() != null ? p.getStock() : 0;
            if (stock <= 0) continue;

            BigDecimal custoMedio = p.getPrecoCompra() != null ? p.getPrecoCompra() : BigDecimal.ZERO;
            BigDecimal precoVenda = p.getPrecoUnitario() != null ? p.getPrecoUnitario() : BigDecimal.ZERO;
            
            // Obter último custo do lote mais recente
            BigDecimal ultimoCusto = custoMedio;
            List<Estoque> lotes = estoqueRepository.findByProduto(p);
            if (!lotes.isEmpty()) {
                lotes.sort((a, b) -> {
                    if (a.getDataEntrada() == null) return 1;
                    if (b.getDataEntrada() == null) return -1;
                    return b.getDataEntrada().compareTo(a.getDataEntrada());
                });
                if (lotes.get(0).getPrecoCompra() != null) {
                    ultimoCusto = lotes.get(0).getPrecoCompra();
                }
            }

            BigDecimal totalCustoMedio = custoMedio.multiply(BigDecimal.valueOf(stock));
            BigDecimal totalUltimoCusto = ultimoCusto.multiply(BigDecimal.valueOf(stock));
            BigDecimal totalVenda = precoVenda.multiply(BigDecimal.valueOf(stock));

            relatorio.add(new ValorizacaoEstoqueDto(
                p.getId(), p.getCodigoBarra(), p.getNome(), stock,
                custoMedio, ultimoCusto, totalCustoMedio, totalUltimoCusto,
                precoVenda, totalVenda
            ));
        }
        return relatorio;
    }

    @Transactional(readOnly = true)
    public List<MargemLoteDto> calcularMargemPorLote(Produto produto, LocalDate inicio, LocalDate fim) {
        if (produto == null) return Collections.emptyList();

        List<Estoque> estoques = estoqueRepository.findByProduto(produto);
        Map<String, Estoque> porLote = new LinkedHashMap<>();
        for (Estoque e : estoques) {
            String lote = e.getLote() != null ? e.getLote() : "";
            porLote.putIfAbsent(lote, e);
        }

        List<ItemFatura> itens = itemFaturaRepository.findAll();
        LocalDateTime ini = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime fimDt = fim != null ? fim.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Map<String, Integer> qtdVendidaPorLote = new HashMap<>();
        Map<String, BigDecimal> receitaPorLote = new HashMap<>();

        for (ItemFatura it : itens) {
            if (it.getProduto() == null || !Objects.equals(it.getProduto().getId(), produto.getId())) continue;
            if (it.getFatura() == null || it.getFatura().getDataEmissao() == null) continue;
            LocalDateTime dataEmissao = it.getFatura().getDataEmissao().atStartOfDay();
            if (ini != null && dataEmissao.isBefore(ini)) continue;
            if (fimDt != null && dataEmissao.isAfter(fimDt)) continue;

            String lote = it.getLote() != null ? it.getLote() : "";
            int qtd = it.getQuantidade() != null ? it.getQuantidade() : 0;
            BigDecimal total = it.getTotal() != null ? it.getTotal() : BigDecimal.ZERO;

            qtdVendidaPorLote.merge(lote, qtd, Integer::sum);
            receitaPorLote.merge(lote, total, BigDecimal::add);
        }

        List<MargemLoteDto> result = new ArrayList<>();
        for (Map.Entry<String, Estoque> entry : porLote.entrySet()) {
            String lote = entry.getKey();
            Estoque e = entry.getValue();

            Integer qtdAtual = e.getQuantidade() != null ? e.getQuantidade() : 0;
            Integer qtdVendida = qtdVendidaPorLote.getOrDefault(lote, 0);
            Integer qtdEntrada = qtdAtual + qtdVendida;

            BigDecimal precoCompra = e.getPrecoCompra() != null ? e.getPrecoCompra() : BigDecimal.ZERO;
            BigDecimal precoVenda = e.getPrecoVenda() != null ? e.getPrecoVenda() : produto.getPrecoUnitario();
            BigDecimal receita = receitaPorLote.getOrDefault(lote, BigDecimal.ZERO);
            BigDecimal custo = precoCompra.multiply(BigDecimal.valueOf(qtdVendida)).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal margem = receita.subtract(custo);

            result.add(new MargemLoteDto(
                    lote,
                    precoCompra,
                    precoVenda,
                    qtdEntrada,
                    qtdAtual,
                    qtdVendida,
                    receita,
                    custo,
                    margem,
                    e.getDataEntrada(),
                    e.getValidade()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<String> validarIntegridadeEstoque(Produto produto) {
        if (produto == null) return Collections.singletonList("Produto nulo");
        List<String> alertas = new ArrayList<>();
        Integer totalLotes = estoqueRepository.sumQuantidadeByProduto(produto);
        int stockProduto = produto.getStock() != null ? produto.getStock() : 0;
        int total = totalLotes != null ? totalLotes : 0;
        if (total != stockProduto) {
            alertas.add("Divergência de stock para produto " + produto.getNome()
                    + ": Produto.stock=" + stockProduto + ", Soma lotes=" + total);
        }
        return alertas;
    }
}

