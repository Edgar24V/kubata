package ao.allon.kubata.faturacao.util;

import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Qty {

    private Qty() {
    }

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static int scaleFor(Produto p) {
        if (p != null && p.getCasasDecimaisQuantidade() != null) {
            return clamp(p.getCasasDecimaisQuantidade(), 0, 6);
        }
        UnidadeMedida u = p != null ? p.getUnidadeMedida() : null;
        if (u == null) return 3;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> 3;
            default -> 0;
        };
    }

    public static BigDecimal scale(Produto p, BigDecimal q) {
        if (q == null) return null;
        return q.setScale(scaleFor(p), ROUNDING);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public static BigDecimal qtyOrInt(BigDecimal quantidadeDecimal, Integer quantidadeInt) {
        if (quantidadeDecimal != null) return quantidadeDecimal;
        return BigDecimal.valueOf(quantidadeInt != null ? quantidadeInt : 0);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
