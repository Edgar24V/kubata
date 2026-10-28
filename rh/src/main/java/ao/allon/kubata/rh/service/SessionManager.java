package ao.allon.kubata.rh.service;

import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.security.PermissionCatalog;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.ModuleAccessService;
import org.springframework.stereotype.Service;

/**
 * Sessão do módulo RH. O utilizador e as permissões são definidos no
 * Kubata Administrator; aqui apenas se verificam.
 */
@Service
public class SessionManager {

    private final AcessoService acessoService;
    private final ModuleAccessService moduleAccessService;
    private final ao.allon.kubata.core.service.AccessGuard accessGuard;

    private User user;

    public SessionManager(AcessoService acessoService,
                          ModuleAccessService moduleAccessService,
                          ao.allon.kubata.core.service.AccessGuard accessGuard) {
        this.acessoService = acessoService;
        this.moduleAccessService = moduleAccessService;
        this.accessGuard = accessGuard;
        // O AccessGuard valida permissões ao nível do serviço; precisa do utilizador deste processo.
        accessGuard.registerUserResolver(() -> user);
    }

    public void login(User user) {
        this.user = user;
    }

    public void logout() {
        this.user = null;
    }

    public User getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user != null
                && (user.getRole() == Role.ADMIN || user.isSuperadmin());
    }

    public boolean podeVer(String recurso) {
        return temAcesso(recurso, PermissaoPerfil.Operacao.VER);
    }

    public boolean temAcesso(String recurso, PermissaoPerfil.Operacao operacao) {
        if (user == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        try {
            // Módulo RH desactivado no Kubata Administrator bloqueia tudo.
            if (!moduleAccessService.isPermissionModuleEnabled(PermissionCatalog.RH)) {
                return false;
            }
            return acessoService.temAcesso(user, PermissionCatalog.RH, recurso, operacao);
        } catch (Exception e) {
            return false;
        }
    }
}
