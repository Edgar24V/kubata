package ao.allon.kubata.core.projection;

/**
 * Linha da lista de fornecedores servida pela consulta paginada do hub.
 */
public record FornecedorLinha(Long id,
                              String nome,
                              String nif,
                              String telefone,
                              String email,
                              String cidade,
                              String pessoaContacto,
                              String termosPagamento,
                              Integer avaliacao) {
}
