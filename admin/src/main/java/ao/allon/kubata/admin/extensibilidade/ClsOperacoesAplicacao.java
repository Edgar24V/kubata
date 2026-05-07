package ao.allon.kubata.admin.extensibilidade;

import java.util.List;

/**
 * Classe que define e disponibiliza as permissões específicas da aplicação.
 */
public interface ClsOperacoesAplicacao {
    /**
     * Retorna a lista de operações disponíveis na aplicação para gestão de permissões.
     */
    List<String> getOperacoesDisponiveis();
    
    /**
     * Retorna a descrição de uma operação específica.
     */
    String getDescricaoOperacao(String operacao);
}
