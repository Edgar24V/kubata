package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.*;
import ao.allon.kubata.core.repository.UserAccessPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SecurityService {

    private final UserAccessPermissionRepository permissionRepository;
    private final UserSecurityProfileService userSecurityProfileService;

    public SecurityService(UserAccessPermissionRepository permissionRepository,
                           UserSecurityProfileService userSecurityProfileService) {
        this.permissionRepository = permissionRepository;
        this.userSecurityProfileService = userSecurityProfileService;
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
        if (user == null || modulo == null || modulo.isBlank()) return false;

        // O perfil individual funciona como uma restrição adicional ao RBAC.
        if (!userSecurityProfileService.isModuleAllowed(user, modulo)) {
            return false;
        }

        // Superadmin ou ADMIN têm acesso total no RBAC, mas continuam sujeitos
        // às restrições explícitas do perfil de segurança individual.
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
        if (user == null || modulo == null || modulo.isBlank()
                || recurso == null || recurso.isBlank() || operacao == null) {
            return false;
        }

        // A política individual de segurança é uma restrição adicional e não
        // pode ser ultrapassada por permissões directas ou por um perfil RBAC.
        if (!userSecurityProfileService.isModuleAllowed(user, modulo)) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        // Compatibilidade com concessões directas legadas. Esse formato antigo
        // guarda módulo/recurso, mas não separa operações; preservamos o seu
        // contrato histórico enquanto os perfis RBAC usam operação exacta.
        boolean hasDirect = permissionRepository.findByUser(user).stream()
                .anyMatch(permission ->
                        permission.getModulo() != null
                                && permission.getModulo().equalsIgnoreCase(modulo)
                                && permission.getOpcao() != null
                                && permission.getOpcao().equalsIgnoreCase(recurso));
        if (hasDirect) return true;

        // RBAC moderno: módulo, recurso e operação têm de coincidir.
        if (user.getPerfis() != null) {
            return user.getPerfis().stream()
                    .filter(perfil -> Boolean.TRUE.equals(perfil.getActivo()))
                    .filter(perfil -> perfil.getPermissoes() != null)
                    .flatMap(perfil -> perfil.getPermissoes().stream())
                    .anyMatch(permission ->
                            permission.getModulo() != null
                                    && permission.getModulo().equalsIgnoreCase(modulo)
                                    && permission.getRecurso() != null
                                    && (permission.getRecurso().equalsIgnoreCase(recurso)
                                        || permission.getRecurso().equalsIgnoreCase("TODOS"))
                                    && permission.getOperacao() == operacao
                                    && Boolean.TRUE.equals(permission.getPermitido())
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
        if (user == null || modulo == null || modulo.isBlank()) return false;
        if (!userSecurityProfileService.isModuleAllowed(user, modulo)) return false;
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
