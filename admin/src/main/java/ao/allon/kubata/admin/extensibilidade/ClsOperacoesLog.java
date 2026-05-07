package ao.allon.kubata.admin.extensibilidade;

import java.util.List;

/**
 * Classe que disponibiliza as entidades da aplicação cujas operações serão registadas no log.
 */
public interface ClsOperacoesLog {
    /**
     * Retorna a lista de entidades cujas operações serão alvo de logging.
     */
    List<String> getEntidadesLog();
    
    /**
     * Regista uma operação de log no sistema.
     * @param entidade Nome da entidade
     * @param operacao Tipo de operação (Criar, Modificar, Apagar, etc)
     * @param detalhes Detalhes do que foi alterado
     */
    void registarLog(String entidade, String operacao, String detalhes);
}
