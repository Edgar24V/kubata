package ao.allon.kubata.inventario.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha do stock por armazém; os nomes do produto e do armazém vêm por junção explícita
 * porque as associações da entidade são lazy e a lista é servida fora de transacção.
 */
public record EstoqueLinha(Long id,
                           String produto,
                           String armazem,
                           String lote,
                           LocalDate validade,
                           Integer quantidade,
                           BigDecimal precoVenda) {
}
