package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.MovimentoStock;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.TipoMovimento;
import ao.allon.kubata.faturacao.repository.MovimentoStockRepository;
import ao.allon.kubata.faturacao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final MovimentoStockRepository movimentoStockRepository;
    private final EstoqueService estoqueService;

    public ProdutoService(ProdutoRepository produtoRepository, 
                          MovimentoStockRepository movimentoStockRepository,
                          EstoqueService estoqueService) {
        this.produtoRepository = produtoRepository;
        this.movimentoStockRepository = movimentoStockRepository;
        this.estoqueService = estoqueService;
    }

    public List<Produto> findAll() {
        return produtoRepository.findAll();
    }
    
    public Optional<Produto> findByCodigoBarra(String codigo) {
        return produtoRepository.findByCodigoBarra(codigo);
    }
    
        public Optional<Produto> findByCodigoBarraPrefix(String prefix) {
            List<Produto> list = produtoRepository.findByCodigoBarraStartingWith(prefix);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }

    public List<Produto> findPopulares(int limitDias, int limit) {
        java.time.LocalDateTime inicio = java.time.LocalDate.now().minusDays(limitDias).atStartOfDay();
        List<Object[]> rows = movimentoStockRepository.findTopVendidosDesde(inicio);
        java.util.List<Long> ids = rows.stream().map(r -> (Long) r[0]).toList();
        java.util.Map<Long, Produto> map = new java.util.HashMap<>();
        produtoRepository.findAllById(ids).forEach(p -> map.put(p.getId(), p));
        java.util.List<Produto> ordered = new java.util.ArrayList<>();
        for (Object[] r : rows) {
            Long id = (Long) r[0];
            Produto p = map.get(id);
            if (p != null) ordered.add(p);
            if (ordered.size() >= limit) break;
        }
        return ordered;
    }
    public Optional<Produto> findById(Long id) {
        if (id == null) return Optional.empty();
        return produtoRepository.findById(id);
    }

    @Transactional
    public Produto save(Produto produto) {
        if (produto.getId() == null && produtoRepository.existsByCodigoBarra(produto.getCodigoBarra())) {
            throw new IllegalArgumentException("Já existe um produto com este código de barras.");
        }
        return produtoRepository.save(produto);
    }

    @Transactional
    public void delete(Long id) {
        produtoRepository.deleteById(id);
    }

    @Transactional
    public void incrementarEstoque(Long produtoId, int quantidade) {
        registarDevolucao(produtoId, quantidade, "Estorno/Cancelamento de Venda");
    }

    @Transactional
    public void decrementarEstoque(Long produtoId, int quantidade) {
        registarSaida(produtoId, quantidade, "Venda/Saída de Stock");
    }

    @Transactional
    public void registarEntrada(Long produtoId, int quantidade, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva.");

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        int saldoAnterior = produto.getStock();
        int saldoAtual = saldoAnterior + quantidade;

        produto.setStock(saldoAtual);
        produtoRepository.save(produto);

        registrarMovimento(produto, TipoMovimento.ENTRADA, quantidade, saldoAnterior, saldoAtual, observacao);
    }

    @Transactional
    public void registarDevolucao(Long produtoId, int quantidade, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva.");

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        int saldoAnterior = produto.getStock();
        int saldoAtual = saldoAnterior + quantidade;

        produto.setStock(saldoAtual);
        produtoRepository.save(produto);

        registrarMovimento(produto, ao.allon.kubata.faturacao.enums.TipoMovimento.DEVOLUCAO, quantidade, saldoAnterior, saldoAtual, observacao);
    }
    
    @Transactional
    public void registarSaida(Long produtoId, int quantidade, String observacao) {
        if (produtoId == null) throw new IllegalArgumentException("ID do produto não pode ser nulo.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva.");

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        // Se o produto tiver uma ficha técnica, registamos a saída dos componentes
        if (produto.hasFichaTecnica()) {
            for (ao.allon.kubata.faturacao.domain.FichaTecnicaItem item : produto.getFichaTecnica().getItens()) {
                int qtdComponente = item.getQuantidade().multiply(java.math.BigDecimal.valueOf(quantidade)).intValue();
                registarSaida(item.getProduto().getId(), qtdComponente, "Composição de " + produto.getNome() + " (" + observacao + ")");
            }
            // Geralmente, o produto final não tem stock próprio se for produzido na hora
            // Mas registamos um movimento de saída simbólico ou apenas ignoramos o stock do pai
            if (produto.getUnidadeMedida() != ao.allon.kubata.faturacao.enums.UnidadeMedida.SERVICO) {
                registrarMovimento(produto, TipoMovimento.SAIDA, quantidade, produto.getStock(), produto.getStock(), observacao + " (Produção)");
            }
            return;
        }

        if (produto.getStock() < quantidade) {
            throw new IllegalArgumentException("Stock insuficiente para " + produto.getNome() + ". Disponível: " + produto.getStock());
        }

        int saldoAnterior = produto.getStock();
        int saldoAtual = saldoAnterior - quantidade;

        produto.setStock(saldoAtual);
        produtoRepository.save(produto);

        registrarMovimento(produto, TipoMovimento.SAIDA, quantidade, saldoAnterior, saldoAtual, observacao);
    }
    
    @Transactional
    public void ajustarStock(Long produtoId, int novoSaldo, String observacao) {
        if (novoSaldo < 0) throw new IllegalArgumentException("O saldo não pode ser negativo.");

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        int saldoAnterior = produto.getStock();
        int diferenca = novoSaldo - saldoAnterior;
        
        if (diferenca == 0) return;

        produto.setStock(novoSaldo);
        produtoRepository.save(produto);

        registrarMovimento(produto, TipoMovimento.AJUSTE, Math.abs(diferenca), saldoAnterior, novoSaldo, observacao);
    }

    private void registrarMovimento(Produto produto, TipoMovimento tipo, int quantidade, int saldoAnterior, int saldoAtual, String observacao) {
        MovimentoStock movimento = new MovimentoStock();
        movimento.setProduto(produto);
        movimento.setTipoMovimento(tipo);
        movimento.setQuantidade(quantidade);
        movimento.setSaldoAnterior(saldoAnterior);
        movimento.setSaldoAtual(saldoAtual);
        movimento.setObservacao(observacao);
        movimentoStockRepository.save(movimento);
    }
    
    public List<MovimentoStock> buscarHistorico(Long produtoId) {
        return movimentoStockRepository.findByProdutoIdOrderByDataMovimentoDesc(produtoId);
    }
    
    public List<Produto> buscarProdutosComStockBaixo() {
        return produtoRepository.findProdutosComStockBaixo();
    }

    public int countTotalProdutos() {
        return (int) produtoRepository.count();
    }
}
