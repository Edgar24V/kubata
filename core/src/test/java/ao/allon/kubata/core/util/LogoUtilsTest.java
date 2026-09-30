package ao.allon.kubata.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogoUtilsTest {

    @Test
    void deveDetectarPng() {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        assertEquals("image/png", LogoUtils.detectMimeType(png));
        assertNull(LogoUtils.validate(png));
    }

    @Test
    void deveDetectarJpeg() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        assertEquals("image/jpeg", LogoUtils.detectMimeType(jpeg));
        assertNull(LogoUtils.validate(jpeg));
    }

    @Test
    void deveRejeitarFormatoDesconhecido() {
        byte[] invalid = "arquivo".getBytes();
        assertNull(LogoUtils.detectMimeType(invalid));
        assertNotNull(LogoUtils.validate(invalid));
    }

    @Test
    void deveRejeitarFicheiroMaiorQue2Mb() {
        byte[] bytes = new byte[LogoUtils.MAX_SIZE_BYTES + 1];
        assertEquals("O logótipo não pode exceder 2 MB.", LogoUtils.validate(bytes));
    }

    @Test
    void deveAceitarAssinaturaWebp() {
        byte[] webp = new byte[]{
                'R','I','F','F',0,0,0,0,'W','E','B','P'
        };
        assertEquals("image/webp", LogoUtils.detectMimeType(webp));
        assertNull(LogoUtils.validate(webp));
    }
}
