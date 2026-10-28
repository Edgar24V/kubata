package ao.allon.kubata.vendas.view;

import ao.allon.kubata.vendas.enums.TipoCliente;

import java.math.BigDecimal;

/**
 * Linha da lista de clientes.
 */
public record ClienteLinha(Long id,
                           String nome,
                           String nif,
                           TipoCliente tipo,
                           String telefone,
                           String email,
                           String cidade,
                           BigDecimal limiteCredito) {
}
