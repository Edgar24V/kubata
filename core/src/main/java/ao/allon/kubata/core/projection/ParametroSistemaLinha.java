package ao.allon.kubata.core.projection;

import java.time.LocalDateTime;

/**
 * Linha da lista de parâmetros de sistema; a empresa associada não é projectada porque a
 * associação é lazy e a lista é servida fora de transacção.
 */
public record ParametroSistemaLinha(Long id,
                                    String chave,
                                    String valor,
                                    String tipoValor,
                                    String descricao,
                                    String grupo,
                                    Boolean editavel,
                                    LocalDateTime atualizadoEm,
                                    String atualizadoPor) {
}
