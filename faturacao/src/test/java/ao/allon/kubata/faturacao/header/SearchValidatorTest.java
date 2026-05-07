package ao.allon.kubata.faturacao.header;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchValidatorTest {
    @Test
    void validQueries() {
        assertTrue(SearchValidator.isValid(""));
        assertTrue(SearchValidator.isValid("ab"));
        assertTrue(SearchValidator.isValid("produto 123"));
        assertTrue(SearchValidator.isValid("email@dominio.com"));
    }

    @Test
    void invalidQueries() {
        assertFalse(SearchValidator.isValid("a"));
        assertFalse(SearchValidator.isValid(null));
        assertFalse(SearchValidator.isValid("##"));
    }
}
