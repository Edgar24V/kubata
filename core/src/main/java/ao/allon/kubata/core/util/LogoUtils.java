package ao.allon.kubata.core.util;

import java.util.Locale;

/**
 * Regras comuns para logótipos empresariais.
 *
 * <p>Valida tamanho e assinatura binária básica, evitando confiar apenas na
 * extensão do ficheiro ou no MIME informado pelo sistema operativo.</p>
 */
public final class LogoUtils {

    public static final int MAX_SIZE_BYTES = 2 * 1024 * 1024;

    private LogoUtils() {
    }

    public static String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        if (isPng(bytes)) {
            return "image/png";
        }
        if (isJpeg(bytes)) {
            return "image/jpeg";
        }
        if (isWebp(bytes)) {
            return "image/webp";
        }
        return null;
    }

    public static String validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        if (bytes.length > MAX_SIZE_BYTES) {
            return "O logótipo não pode exceder 2 MB.";
        }

        if (detectMimeType(bytes) == null) {
            return "Formato de logótipo não suportado. Utilize PNG, JPG/JPEG ou WebP.";
        }

        return null;
    }

    public static boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        return switch (mimeType.trim().toLowerCase(Locale.ROOT)) {
            case "image/png", "image/jpeg", "image/webp" -> true;
            default -> false;
        };
    }

    private static boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89
                && (b[1] & 0xFF) == 0x50
                && (b[2] & 0xFF) == 0x4E
                && (b[3] & 0xFF) == 0x47
                && (b[4] & 0xFF) == 0x0D
                && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A
                && (b[7] & 0xFF) == 0x0A;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P';
    }
}
