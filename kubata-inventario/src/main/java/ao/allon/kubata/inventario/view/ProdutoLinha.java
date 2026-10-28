package ao.allon.kubata.inventario.view;

import ao.allon.kubata.inventario.enums.UnidadeMedida;

import java.math.BigDecimal;

/**
 * Linha da lista de produtos servida directamente pela consulta paginada.
 */
public record ProdutoLinha(Long id,
                           String codigoBarra,
                           String nome,
                           String categoria,
                           UnidadeMedida unidadeMedida,
                           BigDecimal precoUnitario,
                           Integer stock,
                           Integer stockMinimo,
                           BigDecimal percentualIva) {
}
