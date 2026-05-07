package ao.allon.kubata.faturacao.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {

    private Money() {
    }

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Locale LOCALE_AO = new Locale("pt", "AO");

    public static BigDecimal scale(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public static NumberFormat getCurrencyFormat() {
        return NumberFormat.getCurrencyInstance(LOCALE_AO);
    }

    public static String format(BigDecimal v) {
        if (v == null) return getCurrencyFormat().format(BigDecimal.ZERO);
        return getCurrencyFormat().format(v);
    }

    /**
     * Formata no padrão ISO AOA (ex: 1.250,00 AOA)
     */
    public static String formatAOA(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_AO);
        nf.setMinimumFractionDigits(SCALE);
        nf.setMaximumFractionDigits(SCALE);
        return nf.format(v) + " AOA";
    }
}
