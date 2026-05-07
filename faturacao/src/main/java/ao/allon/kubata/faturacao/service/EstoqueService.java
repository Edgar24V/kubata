package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.domain.MovimentoStock;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.TipoMovimento;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import ao.allon.kubata.faturacao.repository.ArmazemRepository;
import ao.allon.kubata.faturacao.repository.EstoqueRepository;
import ao.allon.kubata.faturacao.repository.MovimentoStockRepository;
import ao.allon.kubata.faturacao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentoStockRepository movimentoStockRepository;
    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;
    private final FifoService fifoService;

    public EstoqueService(EstoqueRepository estoqueRepository, MovimentoStockRepository movimentoStockRepository, ProdutoRepository produtoRepository, ArmazemRepository armazemRepository, FifoService fifoService) {
        this.estoqueRepository = estoqueRepository;
        this.movimentoStockRepository = movimentoStockRepository;
        this.produtoRepository = produtoRepository;
        this.armazemRepository = armazemRepository;
        this.fifoService = fifoService;
    }

    /**
     * Entrada de Stock Profissional com Conversão de Unidades e Atualização de Custos.
     */
    @Transactional
    public void adicionarStockComConversao(Produto produto, Armazem armazem, BigDecimal quantidadeEntrada, UnidadeMedida unidadeEntrada, 
                                          BigDecimal precoCustoEntrada, BigDecimal novoPrecoVenda,
                                          String lote, LocalDate validade, String observacao, Fornecedor fornecedor) {
        if (quantidadeEntrada.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");

        // Cálculo da quantidade final na Unidade Base
        BigDecimal quantidadeBase = quantidadeEntrada;
        BigDecimal custoBase = precoCustoEntrada;

        if (unidadeEntrada != null && unidadeEntrada == produto.getUnidadeCompra()) {
            quantidadeBase = quantidadeEntrada.multiply(produto.getFatorConversao());
            // Se comprou em caixa, o custo unitário base é CustoCaixa / Fator
            if (precoCustoEntrada != null && produto.getFatorConversao().compareTo(BigDecimal.ZERO) > 0) {
                custoBase = precoCustoEntrada.divide(produto.getFatorConversao(), 2, RoundingMode.HALF_UP);
            }
        }
        
        int qtdFinalInt = quantidadeBase.setScale(0, RoundingMode.HALF_UP).intValue();
        
        // Atualização de Preços Profissional
        atualizarPrecosProduto(produto, qtdFinalInt, custoBase, novoPrecoVenda);

        adicionarStock(produto, armazem, qtdFinalInt, lote, validade, observacao, fornecedor);
    }

    private void atualizarPrecosProduto(Produto produto, int quantidadeNova, BigDecimal custoNovo, BigDecimal novoPrecoVenda) {
        if (custoNovo == null) return;

        // 1. Cálculo do Preço Médio Ponderado (PMP)
        int stockAtual = produto.getStock() != null ? produto.getStock() : 0;
        BigDecimal custoAtual = produto.getPrecoCompra() != null ? produto.getPrecoCompra() : BigDecimal.ZERO;

        if (stockAtual <= 0) {
            produto.setPrecoCompra(custoNovo);
        } else {
            BigDecimal valorAtual = custoAtual.multiply(BigDecimal.valueOf(stockAtual));
            BigDecimal valorNovo = custoNovo.multiply(BigDecimal.valueOf(quantidadeNova));
            BigDecimal valorTotal = valorAtual.add(valorNovo);
            int stockTotal = stockAtual + quantidadeNova;
            
            BigDecimal pmp = valorTotal.divide(BigDecimal.valueOf(stockTotal), 2, RoundingMode.HALF_UP);
            produto.setPrecoCompra(pmp);
        }

        // 2. Atualização do Preço de Venda (se fornecido)
        if (novoPrecoVenda != null && novoPrecoVenda.compareTo(BigDecimal.ZERO) > 0) {
            produto.setPrecoUnitario(novoPrecoVenda);
        }

        produtoRepository.save(produto);
    }

    @Transactional
    public void adicionarStock(Produto produto, Armazem armazem, Integer quantidade, String lote, LocalDate validade, String observacao, Fornecedor fornecedor) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");

        Estoque estoque = estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, lote)
                .orElseGet(() -> {
                    Estoque e = new Estoque();
                    e.setProduto(produto);
                    e.setArmazem(armazem);
                    e.setLote(lote);
                    e.setValidade(validade);
                    e.setDataEntrada(LocalDate.now());
                    e.setPrecoCompra(produto.getPrecoCompra());
                    e.setPrecoVenda(produto.getPrecoUnitario());
                    return e;
                });

        int saldoAnterior = estoque.getQuantidade();
        estoque.setQuantidade(saldoAnterior + quantidade);
        estoqueRepository.save(estoque);

        registrarMovimento(produto, armazem, TipoMovimento.ENTRADA, quantidade, saldoAnterior, estoque.getQuantidade(), lote, observacao, fornecedor);
        atualizarStockProduto(produto);
    }

    @Transactional
    public void removerStock(Produto produto, Armazem armazem, Integer quantidade, String lote, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");

        Estoque estoque = estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, lote)
                .orElseThrow(() -> new IllegalArgumentException("Estoque não encontrado para este produto/armazém/lote"));

        if (estoque.getQuantidade() < quantidade) {
            throw new IllegalArgumentException("Saldo insuficiente. Disponível: " + estoque.getQuantidade());
        }

        int saldoAnterior = estoque.getQuantidade();
        estoque.setQuantidade(saldoAnterior - quantidade);
        estoqueRepository.save(estoque);

        registrarMovimento(produto, armazem, TipoMovimento.SAIDA, quantidade, saldoAnterior, estoque.getQuantidade(), lote, observacao, null);
        atualizarStockProduto(produto);
    }

    @Transactional
    public void transferirStock(Produto produto, Armazem origem, Armazem destino, Integer quantidade, String lote, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
        if (origem.getId().equals(destino.getId())) throw new IllegalArgumentException("Armazém de origem e destino devem ser diferentes");

        // 1. Remover da origem
        Estoque estoqueOrigem = estoqueRepository.findByProdutoAndArmazemAndLote(produto, origem, lote)
                .orElseThrow(() -> new IllegalArgumentException("Estoque não encontrado na origem"));

        if (estoqueOrigem.getQuantidade() < quantidade) {
            throw new IllegalArgumentException("Saldo insuficiente na origem. Disponível: " + estoqueOrigem.getQuantidade());
        }

        int saldoAnteriorOrigem = estoqueOrigem.getQuantidade();
        estoqueOrigem.setQuantidade(saldoAnteriorOrigem - quantidade);
        estoqueRepository.save(estoqueOrigem);

        registrarMovimento(produto, origem, TipoMovimento.TRANSFERENCIA, quantidade, saldoAnteriorOrigem, estoqueOrigem.getQuantidade(), lote, "Saída p/ " + destino.getNome() + ": " + observacao, null);

        // 2. Adicionar no destino
        Estoque estoqueDestino = estoqueRepository.findByProdutoAndArmazemAndLote(produto, destino, lote)
                .orElseGet(() -> {
                    Estoque e = new Estoque();
                    e.setProduto(produto);
                    e.setArmazem(destino);
                    e.setLote(lote);
                    e.setValidade(estoqueOrigem.getValidade()); // Mantém a validade original
                    return e;
                });

        int saldoAnteriorDestino = estoqueDestino.getQuantidade();
        estoqueDestino.setQuantidade(saldoAnteriorDestino + quantidade);
        estoqueRepository.save(estoqueDestino);

        registrarMovimento(produto, destino, TipoMovimento.TRANSFERENCIA, quantidade, saldoAnteriorDestino, estoqueDestino.getQuantidade(), lote, "Entrada de " + origem.getNome() + ": " + observacao, null);
        
        // Atualiza o total geral do produto (embora numa transferência interna o total global não mude, é bom garantir consistência)
        atualizarStockProduto(produto);
    }
    
    @Transactional
    public void ajustarStock(Produto produto, Armazem armazem, Integer quantidadeReal, String lote, String observacao) {
        if (quantidadeReal < 0) throw new IllegalArgumentException("Quantidade real não pode ser negativa");

        Estoque estoque = estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, lote)
                .orElseGet(() -> {
                    Estoque e = new Estoque();
                    e.setProduto(produto);
                    e.setArmazem(armazem);
                    e.setLote(lote);
                    return e;
                });
        
        int saldoAnterior = estoque.getQuantidade();
        int diferenca = quantidadeReal - saldoAnterior;
        
        if (diferenca == 0) return; // Sem alteração

        estoque.setQuantidade(quantidadeReal);
        estoqueRepository.save(estoque);

        registrarMovimento(produto, armazem, TipoMovimento.AJUSTE, Math.abs(diferenca), saldoAnterior, quantidadeReal, lote, observacao, null);
        atualizarStockProduto(produto);
    }

    private void registrarMovimento(Produto produto, Armazem armazem, TipoMovimento tipo, Integer qtd, Integer anterior, Integer atual, String lote, String obs, Fornecedor fornecedor) {
        MovimentoStock mov = new MovimentoStock();
        mov.setProduto(produto);
        mov.setArmazem(armazem);
        mov.setTipoMovimento(tipo);
        mov.setQuantidade(qtd);
        mov.setSaldoAnterior(anterior);
        mov.setSaldoAtual(atual);
        mov.setLote(lote);
        mov.setObservacao(obs);
        mov.setFornecedor(fornecedor);
        movimentoStockRepository.save(mov);
    }

    private void atualizarStockProduto(Produto produto) {
        Integer total = estoqueRepository.sumQuantidadeByProduto(produto);
        produto.setStock(total != null ? total : 0);
        produtoRepository.save(produto);
    }

    public List<Estoque> listarPorProduto(Produto produto) {
        return estoqueRepository.findByProduto(produto);
    }

    public List<MovimentoStock> listarMovimentos() {
        return movimentoStockRepository.findAllByOrderByDataMovimentoDesc();
    }
    
    public List<Estoque> listarPorArmazem(Armazem armazem) {
        return estoqueRepository.findByArmazem(armazem);
    }

    public List<Estoque> listarTodosEstoque() {
        return estoqueRepository.findAll();
    }
    
    public boolean hasValidStock(Produto produto) {
        return estoqueRepository.existsValidoByProduto(produto, java.time.LocalDate.now());
    }

    @Transactional(readOnly = true)
    public boolean canConsumeByFIFO(Produto produto, int quantidade) {
        return fifoService.podeConsumirPorFIFO(produto, quantidade);
    }

    @Transactional
    public void consumirPorFIFO(Produto produto, int quantidade, String observacao) {
        fifoService.consumirPorFIFO(produto, quantidade, observacao);
    }

    @Transactional
    public java.util.LinkedHashMap<String, Integer> consumirPorFIFOWithBreakdown(Produto produto, int quantidade, String observacao) {
        java.util.Map<String, Integer> breakdown = fifoService.consumirPorFIFO(produto, quantidade, observacao);
        java.util.LinkedHashMap<String, Integer> ordered = new java.util.LinkedHashMap<>();
        ordered.putAll(breakdown);
        return ordered;
    }

    // LEGADO: compatibilidade temporária (depreciado)
    @Transactional(readOnly = true)
    @Deprecated
    public boolean canConsumeByFEFO(Produto produto, int quantidade) {
        return canConsumeByFIFO(produto, quantidade);
    }

    @Transactional
    @Deprecated
    public void consumirPorFEFO(Produto produto, int quantidade, String observacao) {
        consumirPorFIFO(produto, quantidade, observacao);
    }

    @Transactional
    @Deprecated
    public java.util.LinkedHashMap<String, Integer> consumirPorFEFOWithBreakdown(Produto produto, int quantidade, String observacao) {
        return consumirPorFIFOWithBreakdown(produto, quantidade, observacao);
    }


    public Armazem getArmazemPrincipal() {
        return armazemRepository.findByIsPrincipalTrue()
                .orElseThrow(() -> new IllegalStateException("Armazém principal não configurado"));
    }
    
    @Transactional(readOnly = true)
    public java.math.BigDecimal getPrecoVendaAtivo(Produto produto) {
        if (produto == null) return null;
        Armazem armazem = getArmazemPrincipal();
        List<Estoque> lotes = estoqueRepository.findByProduto(produto).stream()
                .filter(e -> armazem.equals(e.getArmazem()))
                .toList();
        Estoque primeiro = lotes.stream()
                .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                .sorted((a, b) -> {
                    java.time.LocalDate da = a.getDataEntrada();
                    java.time.LocalDate db = b.getDataEntrada();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return da.compareTo(db);
                })
                .findFirst()
                .orElse(null);
        if (primeiro != null && primeiro.getPrecoVenda() != null) {
            return primeiro.getPrecoVenda();
        }
        return produto.getPrecoUnitario();
    }
    
    public List<Armazem> listarArmazens() {
        return armazemRepository.findAll();
    }
    
    public List<MovimentoStock> listarMovimentosPeriodo(Long produtoId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        if (produtoId != null) {
            return movimentoStockRepository.findByProdutoIdAndDataMovimentoBetween(produtoId, inicio, fim);
        }
        return movimentoStockRepository.findByDataMovimentoBetween(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<FifoService.ResumoLoteFifoDto> resumoFifoPorProduto(Produto produto) {
        return fifoService.resumoPorProduto(produto);
    }
}
