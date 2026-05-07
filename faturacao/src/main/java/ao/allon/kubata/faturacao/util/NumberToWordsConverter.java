package ao.allon.kubata.faturacao.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário para converter números em extenso (Português/Kwanzas).
 */
public class NumberToWordsConverter {

    private static final String[] TRILLION = {"", "trilhão", "trilhões"};
    private static final String[] BILLION = {"", "bilhão", "bilhões"};
    private static final String[] MILLION = {"", "milhão", "milhões"};
    private static final String[] THOUSAND = {"", "mil", "mil"};
    private static final String[] HUNDRED = {"", "cem", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos"};
    private static final String[] TEN = {"", "dez", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa"};
    private static final String[] UNIT = {"", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove"};
    private static final String[] TEENS = {"dez", "onze", "doze", "treze", "quatorze", "quinze", "dezasseis", "dezassete", "dezoito", "dezanove"};

    public static String convertToExtenso(BigDecimal value) {
        if (value == null) return "";
        if (value.compareTo(BigDecimal.ZERO) == 0) return "Zero Kwanzas";

        long integerPart = value.setScale(0, RoundingMode.FLOOR).longValue();
        int decimalPart = value.subtract(new BigDecimal(integerPart)).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValue();

        StringBuilder sb = new StringBuilder();

        if (integerPart > 0) {
            sb.append(integerToWords(integerPart));
            sb.append(integerPart == 1 ? " Kwanza" : " Kwanzas");
        }

        if (decimalPart > 0) {
            if (integerPart > 0) sb.append(" e ");
            sb.append(integerToWords(decimalPart));
            sb.append(decimalPart == 1 ? " Cêntimo" : " Cêntimos");
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) return "Zero Kwanzas";
        
        // Capitalize first letter
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    private static String integerToWords(long n) {
        if (n == 0) return "";
        if (n < 10) return UNIT[(int) n];
        if (n < 20) return TEENS[(int) (n - 10)];
        if (n < 100) {
            long tens = n / 10;
            long units = n % 10;
            return TEN[(int) tens] + (units > 0 ? " e " + UNIT[(int) units] : "");
        }
        if (n < 1000) {
            if (n == 100) return "cem";
            long hundreds = n / 100;
            long rest = n % 100;
            return HUNDRED[(int) hundreds + 1] + (rest > 0 ? " e " + integerToWords(rest) : "");
        }
        
        // Handling larger numbers (thousands, millions, etc.)
        String[] units = {"", "mil", "milhão", "bilhão", "trilhão"};
        String[] unitsPlural = {"", "mil", "milhões", "bilhões", "trilhões"};
        
        List<String> parts = new ArrayList<>();
        int unitIndex = 0;
        
        while (n > 0) {
            long chunk = n % 1000;
            if (chunk > 0) {
                String chunkWords = integerToWords(chunk);
                String unitLabel = chunk == 1 ? units[unitIndex] : unitsPlural[unitIndex];
                parts.add(chunkWords + (unitLabel.isEmpty() ? "" : " " + unitLabel));
            }
            n /= 1000;
            unitIndex++;
        }
        
        Collections.reverse(parts);
        return String.join(" e ", parts).replace("mil e mil", "mil"); // Simplistic join
    }
}
