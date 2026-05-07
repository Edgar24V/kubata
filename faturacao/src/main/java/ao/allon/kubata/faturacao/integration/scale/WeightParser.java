package ao.allon.kubata.faturacao.integration.scale;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeightParser {

    private static final Pattern DEC_PATTERN = Pattern.compile("(?i)(?:^|\\s|[:=])\\s*(-?\\d{1,6}(?:[\\.,]\\d{1,4})?)\\s*(kg)?");

    private WeightParser() {}

    public static BigDecimal parse(String s) {
        if (s == null) return null;
        Matcher m = DEC_PATTERN.matcher(s);
        while (m.find()) {
            String num = m.group(1);
            if (num == null || num.isBlank()) continue;
            num = num.replace(",", ".");
            try {
                BigDecimal v = new BigDecimal(num);
                if (v.compareTo(BigDecimal.ZERO) <= 0) continue;
                return v;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
