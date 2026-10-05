package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.repository.ArmazemRepository;
import ao.allon.kubata.inventario.repository.EstoqueRepository;
import ao.allon.kubata.inventario.repository.ProdutoRepository;
import ao.allon.kubata.inventario.repository.ReservaStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventarioDashboardService {
    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;
    private final EstoqueRepository estoqueRepository;
    private final ReservaStockRepository reservaRepository;

    public InventarioDashboardService(ProdutoRepository produtoRepository,
                                      ArmazemRepository armazemRepository,
                                      EstoqueRepository estoqueRepository,
                                      ReservaStockRepository reservaRepository) {
        this.produtoRepository = produtoRepository;
        this.armazemRepository = armazemRepository;
        this.estoqueRepository = estoqueRepository;
        this.reservaRepository = reservaRepository;
    }

    public DashboardResumo resumo() {
        List<Produto> produtos = produtoRepository.findAll();
        List<Estoque> stocks = estoqueRepository.findAll();

        long quantidadeProdutos = produtos.stream().filter(p -> Boolean.TRUE.equals(p.getActive())).count();
        long quantidadeArmazens = armazemRepository.findAll().stream().filter(a -> Boolean.TRUE.equals(a.getActive())).count();
        BigDecimal valorStock = stocks.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .map(s -> BigDecimal.valueOf(s.getQuantidade() == null ? 0 : s.getQuantidade())
                        .multiply(s.getPrecoCompra() == null ? BigDecimal.ZERO : s.getPrecoCompra()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long itensStockBaixo = produtos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStock() != null && p.getStockMinimo() != null && p.getStock() <= p.getStockMinimo())
                .count();

        BigDecimal reservado = stocks.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .map(s -> reservaRepository.sumAtivas(s.getProduto().getId(), s.getArmazem().getId()))
                .map(x -> x == null ? BigDecimal.ZERO : x)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardResumo(quantidadeProdutos, quantidadeArmazens,
                stocks.stream().filter(s -> Boolean.TRUE.equals(s.getActive()))
                        .mapToInt(s -> s.getQuantidade() == null ? 0 : s.getQuantidade()).sum(),
                reservado, valorStock, itensStockBaixo);
    }

    public record DashboardResumo(long produtos, long armazens, int unidades,
                                  BigDecimal reservado, BigDecimal valorStock, long stockBaixo) {}
}
