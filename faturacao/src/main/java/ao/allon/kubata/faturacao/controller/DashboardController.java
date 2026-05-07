package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DashboardController {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public DashboardController(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    public BigDecimal getVendasMesAtual() {
        // Lógica para buscar vendas do mês atual (exemplo)
        return faturaService.findTotalVendasMesAtual();
    }

    public long getFaturasPendentesCount() {
        return faturaService.countByStatusPendente();
    }

    public int getTotalClientesCount() {
        return clienteService.countTotalClientes();
    }

    public int getTotalProdutosCount() {
        return produtoService.countTotalProdutos();
    }

    public List<Fatura> getFaturasRecentes() {
        // Delega a busca para o FaturaService
        // (Este método precisaria ser criado no FaturaService, por exemplo, para buscar as 10 últimas)
        return faturaService.findAll(); // Simplesmente retornando todos por enquanto
    }

    public List<Produto> getProdutosComStockBaixo() {
        // Delega a busca para o ProdutoService
        return produtoService.buscarProdutosComStockBaixo();
    }

    public List<Object[]> findTopSellingProducts(java.time.LocalDate start, java.time.LocalDate end) {
        return faturaService.findTopSellingProducts(start, end);
    }

    // Outros métodos para buscar dados para os gráficos...
}
