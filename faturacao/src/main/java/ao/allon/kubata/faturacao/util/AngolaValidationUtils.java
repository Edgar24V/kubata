package ao.allon.kubata.faturacao.util;

public class AngolaValidationUtils {

    private static final String CONSUMIDOR_FINAL_NIF = "999999999";

    public static boolean isValidNif(String nif) {
        return validateNifMessage(nif) == null;
    }

    public static String validateNifMessage(String nif) {
        if (nif == null || nif.trim().isEmpty()) {
            return "NIF é obrigatório.";
        }
        String digits = normalizeNif(nif);
        if (digits == null || digits.isEmpty()) {
            return "NIF deve conter apenas dígitos.";
        }
        if (CONSUMIDOR_FINAL_NIF.equals(digits)) {
            return null;
        }
        if (digits.length() != 10) {
            return "NIF deve ter exatamente 10 dígitos.";
        }
        if (!digits.matches("\\d{10}")) {
            return "NIF deve conter apenas dígitos.";
        }
        if (!hasValidPrefix(digits)) {
            return "Prefixo de NIF inválido para contribuintes angolanos.";
        }
        if (!hasValidCheckDigit(digits)) {
            return "Dígito de controlo do NIF é inválido.";
        }
        return null;
    }

    public static String normalizeNif(String nif) {
        if (nif == null) {
            return null;
        }
        String digits = nif.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String formatNif(String nif) {
        String digits = normalizeNif(nif);
        if (digits == null || digits.length() != 10) {
            return nif;
        }
        return digits.substring(0, 3) + " " +
               digits.substring(3, 6) + " " +
               digits.substring(6);
    }

    private static boolean hasValidPrefix(String digits) {
        char first = digits.charAt(0);
        if (first == '1' || first == '2') {
            return true;
        }
        if (first == '5' || first == '6') {
            return true;
        }
        if (first == '7' || first == '8' || first == '9') {
            return true;
        }
        return false;
    }

    private static boolean hasValidCheckDigit(String digits) {
        int[] weights = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int d = digits.charAt(i) - '0';
            sum += d * weights[i];
        }
        int mod11 = sum % 11;
        int check = 11 - mod11;
        if (check >= 10) {
            check = 0;
        }
        int last = digits.charAt(9) - '0';
        return last == check;
    }

    public static boolean isValidTelefone(String telefone) {
        if (telefone == null || telefone.trim().isEmpty()) return false;
        String clean = telefone.replaceAll("\\D", "");
        return clean.length() == 9 && clean.startsWith("9");
    }
}
