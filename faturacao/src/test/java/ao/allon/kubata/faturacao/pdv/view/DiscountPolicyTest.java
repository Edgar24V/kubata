package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class DiscountPolicyTest {

    @Test
    void maxForServicoIsLimited() {
        Produto p = new Produto();
        p.setUnidadeMedida(UnidadeMedida.SERVICO);
        assertEquals(new BigDecimal("20"), DiscountPolicy.maxDiscountFor(p));
    }

    @Test
    void maxForBebidaAlcoolicaIs10() {
        Produto p = new Produto();
        Categoria c = new Categoria();
        c.setNome("Bebidas Alcoólicas");
        p.setCategoria(c);
        assertEquals(new BigDecimal("10"), DiscountPolicy.maxDiscountFor(p));
    }

    @Test
    void maxForMedicamentosIs5() {
        Produto p = new Produto();
        Categoria c = new Categoria();
        c.setNome("Farmácia / Medicamentos");
        p.setCategoria(c);
        assertEquals(new BigDecimal("5"), DiscountPolicy.maxDiscountFor(p));
    }

    @Test
    void maxForIsentoIsAtMost10() {
        Produto p = new Produto();
        Imposto imp = new Imposto();
        imp.setPercentual(BigDecimal.ZERO);
        p.setImposto(imp);
        assertEquals(new BigDecimal("10"), DiscountPolicy.maxDiscountFor(p));
    }
}
