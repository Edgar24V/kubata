package ao.allon.kubata.admin.extensibilidade;

import java.util.List;

/**
 * Interface Audit
 * Adiciona as permissões da aplicação ao sistema de segurança do sistema.
 */
public interface Audit {
    /**
     * Regista as políticas de segurança e permissões da aplicação.
     */
    void registerSecurityPolicies();
    
    /**
     * Valida se um utilizador tem permissão para uma determinada operação.
     * @param userId ID do utilizador
     * @param operation Operação a validar
     * @return true se permitido, false caso contrário
     */
    boolean hasPermission(String userId, String operation);
    
    /**
     * Retorna a lista de roles ou perfis definidos pela aplicação externa.
     */
    List<String> getApplicationRoles();
}
