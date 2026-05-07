package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.*;
import ao.allon.kubata.core.repository.UserAccessPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SecurityService {

    private final UserAccessPermissionRepository permissionRepository;

    public SecurityService(UserAccessPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * Verifica se um utilizador tem permissão para uma determinada operação num recurso/módulo.
     * 
     * @param user O utilizador a verificar
     * @param modulo O módulo (ex: FATURACAO, CLIENTES)
     * @param operacao A operação (ex: VER, CRIAR, EDITAR, REMOVER)
     * @return true se tiver permissão ou for ADMIN
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String modulo, String operacao) {
        if (user == null) return false;
        
        // Superadmin ou ADMIN têm acesso total por definição
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) {
            return true;
        }

        // 1. Verifica permissões legadas (diretas no usuário)
        List<UserAccessPermission> directPerms = permissionRepository.findByUser(user);
        boolean hasDirect = directPerms.stream()
                .anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo) && 
                               p.getOpcao().equalsIgnoreCase(operacao));
        if (hasDirect) return true;

        // 2. Verifica permissões via Perfis (RBAC)
        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                .filter(PerfilAcesso::getActivo)
                .flatMap(p -> p.getPermissoes().stream())
                .anyMatch(pm -> 
                    pm.getModulo().equalsIgnoreCase(modulo) &&
                    (pm.getRecurso().equalsIgnoreCase(modulo) || pm.getRecurso().equalsIgnoreCase("TODOS")) && // Se o recurso for o próprio módulo ou "TODOS"
                    pm.getOperacao().name().equalsIgnoreCase(operacao) &&
                    Boolean.TRUE.equals(pm.getPermitido())
                );
        }

        return false;
    }

    /**
     * Versão sobrecarregada para aceitar o enum Operacao.
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String modulo, String recurso, PermissaoPerfil.Operacao operacao) {
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        // Verifica nos perfis
        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                .filter(PerfilAcesso::getActivo)
                .flatMap(p -> p.getPermissoes().stream())
                               .anyMatch(pm ->
                    pm.getModulo().equalsIgnoreCase(modulo) &&
                    (pm.getRecurso().equalsIgnoreCase(recurso) || pm.getRecurso().equalsIgnoreCase("TODOS")) &&
                    pm.getOperacao() == operacao &&
                    Boolean.TRUE.equals(pm.getPermitido())
                );
        }
        
        return false;
    }

    /**
     * Obtém o valor de restrição (context filter) para um determinado recurso.
     * Ex: getRestrictionValue(user, "FATURACAO", "FATURAS", "SERIE")
     */
    @Transactional(readOnly = true)
    public String getRestrictionValue(User user, String modulo, String recurso) {
        if (user == null || user.isSuperadmin() || user.getRole() == Role.ADMIN) return null;

        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                .filter(PerfilAcesso::getActivo)
                .flatMap(p -> p.getPermissoes().stream())
                .filter(pm -> pm.getModulo().equalsIgnoreCase(modulo) && 
                              pm.getRecurso().equalsIgnoreCase(recurso) &&
                              pm.getValorRestricao() != null)
                .map(PermissaoPerfil::getValorRestricao)
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    /**
     * Verifica se o utilizador tem acesso a pelo menos uma operação no módulo.
     */
    @Transactional(readOnly = true)
    public boolean hasModuleAccess(User user, String modulo) {
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        // Verifica permissões legadas
        List<UserAccessPermission> perms = permissionRepository.findByUser(user);
        if (perms.stream().anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo))) return true;

        // Verifica nos perfis
        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                .filter(PerfilAcesso::getActivo)
                .flatMap(p -> p.getPermissoes().stream())
                .anyMatch(pm -> pm.getModulo().equalsIgnoreCase(modulo) && Boolean.TRUE.equals(pm.getPermitido()));
        }

        return false;
    }
}
