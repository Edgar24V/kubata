package ao.allon.kubata.admin.extensibilidade;

import java.util.Map;

/**
 * Componente opcional para mapeamento de utilizadores e logins associados.
 */
public interface ClsLoginsAssociados {
    /**
     * Retorna o mapeamento de logins do sistema e logins externos da aplicação.
     * @return Map de userId do sistema -> userId externo
     */
    Map<String, String> getMapeamentoLogins();
    
    /**
     * Associa um login externo a um login do sistema.
     * @param userId ID do utilizador no sistema (Kubata)
     * @param loginExterno ID ou Login do utilizador na aplicação externa
     */
    void associarLogin(String userId, String loginExterno);
    
    /**
     * Remove a associação de login para um utilizador do sistema.
     */
    void removerAssociacao(String userId);
}
