package ao.allon.kubata.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NifUtilsTest {

    @Test
    void deveNormalizarNifComSeparadores() {
        assertEquals("5000000000", NifUtils.normalize("500-000-0000"));
        assertEquals("ABC123456789", NifUtils.normalize(" abc.123 456-789 "));
    }

    @Test
    void deveValidarNifDePessoaColectiva() {
        assertNull(NifUtils.validate("5000000000", NifUtils.TipoContribuinte.PESSOA_COLECTIVA));
        assertNull(NifUtils.validate("500 000 0000", NifUtils.TipoContribuinte.PESSOA_COLECTIVA));
        assertNotNull(NifUtils.validate("50000000", NifUtils.TipoContribuinte.PESSOA_COLECTIVA));
        assertNotNull(NifUtils.validate("500000000A", NifUtils.TipoContribuinte.PESSOA_COLECTIVA));
    }

    @Test
    void devePermitirNifAlfanumericoParaPessoaSingular() {
        assertNull(NifUtils.validate("004797863LA048", NifUtils.TipoContribuinte.PESSOA_SINGULAR));
        assertNull(NifUtils.validate("0047-9786-3LA048", NifUtils.TipoContribuinte.PESSOA_SINGULAR));
        assertNotNull(NifUtils.validate("ABC", NifUtils.TipoContribuinte.PESSOA_SINGULAR));
    }

    @Test
    void deveValidarNifNaoResidenteComoNumerico() {
        assertNull(NifUtils.validate("123456789", NifUtils.TipoContribuinte.NAO_RESIDENTE));
        assertNotNull(NifUtils.validate("12345678A", NifUtils.TipoContribuinte.NAO_RESIDENTE));
    }
}
