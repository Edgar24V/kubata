package ao.allon.kubata.faturacao.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AngolaValidationUtilsTest {

    @Test
    void validNifPessoaSingular() {
        assertTrue(AngolaValidationUtils.isValidNif("1234567890"));
        assertTrue(AngolaValidationUtils.isValidNif("123 456 7890"));
    }

    @Test
    void validNifPessoaColetiva() {
        assertTrue(AngolaValidationUtils.isValidNif("5123456783"));
    }

    @Test
    void validNifEntidadeEspecial() {
        assertTrue(AngolaValidationUtils.isValidNif("7123456785"));
    }

    @Test
    void invalidNifPorTamanho() {
        assertFalse(AngolaValidationUtils.isValidNif("123456789"));
        assertFalse(AngolaValidationUtils.isValidNif("12345678901"));
    }

    @Test
    void invalidNifPorPrefixo() {
        assertFalse(AngolaValidationUtils.isValidNif("0123456789"));
    }

    @Test
    void invalidNifPorDigitoControlo() {
        assertFalse(AngolaValidationUtils.isValidNif("1234567891"));
    }

    @Test
    void normalizeAndFormat() {
        String normalized = AngolaValidationUtils.normalizeNif("123 456 7890");
        assertEquals("1234567890", normalized);
        String formatted = AngolaValidationUtils.formatNif("1234567890");
        assertEquals("123 456 7890", formatted);
    }
}

