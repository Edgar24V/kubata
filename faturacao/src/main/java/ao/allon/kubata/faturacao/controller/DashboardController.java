package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public BigDecimal getVendasDiaAtual() {
        return faturaService.getVendasDiaAtual();
    }

    public BigDecimal getVendasMesAtual() {
        return faturaService.getVendasMesAtual();
    }

    public long getFaturasHojeCount() {
        return faturaService.countFaturasDoDia();
    }

    public long getFaturasPendentesCount() {
        return faturaService.countByStatusPendente();
    }

    public long getTotalClientesCount() {
        return clienteService.countTotalClientes();
    }

    public long getClientesNovosMesCount() {
        LocalDate hoje = LocalDate.now();
        return clienteService.countClientesCriadosEntre(hoje.withDayOfMonth(1), hoje);
    }

    public long getTotalProdutosCount() {
        return produtoService.countTotalProdutos();
    }

    public long getProdutosBaixoStockCount() {
        return produtoService.buscarProdutosComStockBaixo().size();
    }

    public Map<YearMonth, BigDecimal> getVendasPorMes(int meses) {
        Map<YearMonth, BigDecimal> vendas = new LinkedHashMap<>();
        YearMonth atual = YearMonth.now();
        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = atual.minusMonths(i);
            vendas.put(mes, faturaService.getVendasEmitidasEntre(mes.atDay(1), mes.atEndOfMonth()));
        }
        return vendas;
    }

    /** Linhas {categoria, valor} das vendas emitidas no período, por ordem decrescente. */
    public List<Object[]> getVendasPorCategoria(LocalDate inicio, LocalDate fim) {
        return faturaService.getVendasPorCategoria(inicio, fim);
    }

    /** Linhas {descrição, quantidade, valor} dos artigos mais vendidos no período. */
    public List<Object[]> findTopSellingProducts(LocalDate start, LocalDate end) {
        return faturaService.findTopSellingProducts(start, end);
    }
}
