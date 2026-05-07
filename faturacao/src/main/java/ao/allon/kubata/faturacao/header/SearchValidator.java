package ao.allon.kubata.faturacao.header;

public final class SearchValidator {
    private SearchValidator() {}

    public static boolean isValid(String q) {
        if (q == null) return false;
        String s = q.trim();
        if (s.isEmpty()) return true;
        if (s.length() < 2) return false;
        return s.matches("[\\p{L}0-9 .,@_-]+");
    }
}
