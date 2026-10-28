package ao.allon.kubata.inventario.view;

import ao.allon.kubata.inventario.enums.Provincia;

/**
 * Linha da lista de armazéns.
 */
public record ArmazemLinha(Long id,
                           String nome,
                           Provincia provincia,
                           String municipio,
                           String responsavel,
                           String telefone,
                           Boolean principal) {
}
