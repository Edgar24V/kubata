package ao.allon.kubata.faturacao.util;

import java.math.BigDecimal;

public final class BarcodeUtils {
    private BarcodeUtils() {}
    
    public static class PluResult {
        public final String baseCode;
        public final BigDecimal weightKg;
        public PluResult(String baseCode, BigDecimal weightKg) {
            this.baseCode = baseCode;
            this.weightKg = weightKg;
        }
    }
    
    public static boolean isWeightEAN13(String code) {
        return code != null && code.length() == 13 && code.charAt(0) == '2' && code.matches("\\d{13}");
    }
    
    public static PluResult parseWeightEAN13(String code) {
        if (!isWeightEAN13(code)) return null;
        String base = code.substring(1, 6);
        String gramsStr = code.substring(6, 11);
        int grams = Integer.parseInt(gramsStr);
        BigDecimal kg = new BigDecimal(grams).divide(new BigDecimal(1000));
        return new PluResult(base, kg);
    }
}
