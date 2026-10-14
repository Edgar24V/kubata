package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaturaFiscalIntegrityTest {

    @Test
    void rascunhoPodeSerDescartado() {
        Fatura fatura = new Fatura();
        fatura.setStatus(StatusFatura.RASCUNHO);

        assertDoesNotThrow(fatura::validarEliminacaoFiscal);
    }

    @Test
    void documentoForaDeRascunhoNaoPodeSerEliminado() {
        Fatura fatura = new Fatura();
        for (StatusFatura estado : new StatusFatura[] {
                StatusFatura.EMITIDA,
                StatusFatura.PAGA,
                StatusFatura.CANCELADA,
                StatusFatura.PROCESSADA,
                StatusFatura.NULA
        }) {
            fatura.setStatus(estado);
            assertThrows(IllegalStateException.class, fatura::validarEliminacaoFiscal,
                "Deve preservar o histórico quando o documento está em " + estado);
        }
    }
}
