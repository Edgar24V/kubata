package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.Produto;

import java.math.BigDecimal;

public final class DiscountPolicy {

    private DiscountPolicy() {}

    public static BigDecimal maxDiscountFor(Produto p) {
        BigDecimal max = new BigDecimal("30");
        if (p != null) {
            if (p.isServico()) max = new BigDecimal("20");
            if (p.getCategoria() != null && p.getCategoria().getNome() != null) {
                String cn = p.getCategoria().getNome().toLowerCase();
                if (cn.contains("bebida") && cn.contains("alco")) max = new BigDecimal("10");
                if (cn.contains("farm") || cn.contains("medic")) max = max.min(new BigDecimal("5"));
            }
            if (p.getImposto() != null && p.getImposto().getPercentual() != null) {
                if (p.getImposto().getPercentual().compareTo(BigDecimal.ZERO) == 0) {
                    max = max.min(new BigDecimal("10"));
                }
            }
        }
        if (max.compareTo(new BigDecimal("50")) > 0) max = new BigDecimal("50");
        if (max.compareTo(BigDecimal.ZERO) < 0) max = BigDecimal.ZERO;
        return max;
    }
}
