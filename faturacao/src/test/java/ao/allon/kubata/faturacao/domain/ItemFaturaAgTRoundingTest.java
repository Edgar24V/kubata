package ao.allon.kubata.faturacao.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemFaturaAgTRoundingTest {

    @Test
    void roundsVatUpToTheNextCentAsRequiredByAgt() {
        ItemFatura item = new ItemFatura();
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("165.39"));
        item.setPercentualIva(new BigDecimal("14.00"));
        item.setDesconto(BigDecimal.ZERO);

        item.calculateTotals();

        assertEquals(0, new BigDecimal("165.39").compareTo(item.getSubtotal()));
        assertEquals(0, new BigDecimal("23.16").compareTo(item.getValorIva()));
        assertEquals(0, new BigDecimal("188.55").compareTo(item.getTotal()));
    }

    @Test
    void exactCentAmountsAreNotIncreased() {
        ItemFatura item = new ItemFatura();
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setPercentualIva(new BigDecimal("14.00"));
        item.setDesconto(BigDecimal.ZERO);

        item.calculateTotals();

        assertEquals(0, new BigDecimal("14.00").compareTo(item.getValorIva()));
    }
}
