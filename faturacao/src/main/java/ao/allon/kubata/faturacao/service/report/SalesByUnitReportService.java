package ao.allon.kubata.faturacao.service.report;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.util.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SalesByUnitReportService {

    public record Row(Long produtoId, String produto, String categoria, String unidade,
                      BigDecimal quantidade, BigDecimal receita, BigDecimal custo, BigDecimal margem) {
    }

    private final FaturaRepository faturaRepository;

    public SalesByUnitReportService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    public List<Row> gerar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) return List.of();
        List<Fatura> faturas = faturaRepository.findByDataEmissaoBetweenWithItens(inicio, fim);
        Map<String, Agg> map = new LinkedHashMap<>();

        for (Fatura f : faturas) {
            if (f == null) continue;
            if (f.getStatus() != StatusFatura.EMITIDA) continue;
            if (f.getItens() == null) continue;

            for (ItemFatura it : f.getItens()) {
                if (it == null) continue;
                Produto p = it.getProduto();
                String produto = p != null ? p.getNome() : it.getDescricao();
                Long produtoId = p != null ? p.getId() : null;
                String categoria = (p != null && p.getCategoria() != null) ? p.getCategoria().getNome() : "";
                UnidadeMedida u = p != null ? p.getUnidadeMedida() : null;
                String unidade = unidadeLabel(u);

                BigDecimal q = quantidadeBase(it, u);
                BigDecimal receita = it.getSubtotal() != null ? it.getSubtotal() : BigDecimal.ZERO;
                BigDecimal custoUnit = p != null && p.getPrecoCompra() != null ? p.getPrecoCompra() : BigDecimal.ZERO;
                BigDecimal custo = Money.scale(custoUnit.multiply(q));
                BigDecimal margem = Money.scale(receita.subtract(custo));

                String key = Objects.toString(produtoId, "null") + "|" + unidade + "|" + produto;
                Agg agg = map.computeIfAbsent(key, k -> new Agg(produtoId, produto, categoria, unidade));
                agg.quantidade = agg.quantidade.add(q);
                agg.receita = agg.receita.add(receita);
                agg.custo = agg.custo.add(custo);
                agg.margem = agg.margem.add(margem);
            }
        }

        List<Row> out = new ArrayList<>();
        for (Agg a : map.values()) {
            out.add(new Row(a.produtoId, a.produto, a.categoria, a.unidade,
                    a.quantidade, Money.scale(a.receita), Money.scale(a.custo), Money.scale(a.margem)));
        }
        out.sort(Comparator.comparing(Row::receita, Comparator.nullsFirst(Comparator.reverseOrder())));
        return out;
    }

    private static BigDecimal quantidadeBase(ItemFatura it, UnidadeMedida u) {
        if (it == null) return BigDecimal.ZERO;
        if (it.getQuantidadeDecimal() != null) return it.getQuantidadeDecimal();
        Integer qInt = it.getQuantidade();
        if (qInt == null) return BigDecimal.ZERO;
        if (u == null) return BigDecimal.valueOf(qInt);
        if (u == UnidadeMedida.KILOGRAMA || u == UnidadeMedida.LITRO || u == UnidadeMedida.METRO) {
            return BigDecimal.valueOf(qInt).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
        }
        if (u == UnidadeMedida.HORA || u == UnidadeMedida.SERVICO) {
            return BigDecimal.valueOf(qInt).divide(new BigDecimal("60"), 3, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(qInt);
    }

    private static String unidadeLabel(UnidadeMedida u) {
        if (u == null) return "un";
        return switch (u) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        };
    }

    private static final class Agg {
        final Long produtoId;
        final String produto;
        final String categoria;
        final String unidade;
        BigDecimal quantidade = BigDecimal.ZERO;
        BigDecimal receita = BigDecimal.ZERO;
        BigDecimal custo = BigDecimal.ZERO;
        BigDecimal margem = BigDecimal.ZERO;

        Agg(Long produtoId, String produto, String categoria, String unidade) {
            this.produtoId = produtoId;
            this.produto = produto;
            this.categoria = categoria;
            this.unidade = unidade;
        }
    }
}
