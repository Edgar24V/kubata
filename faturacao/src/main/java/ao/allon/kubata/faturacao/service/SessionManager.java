package ao.allon.kubata.faturacao.service;

import org.springframework.stereotype.Service;
import ao.allon.kubata.core.domain.User;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import ao.allon.kubata.core.service.AcessoService;
import org.springframework.context.ApplicationEventPublisher;
import ao.allon.kubata.faturacao.ui.event.PermissionsChangedEvent;
import ao.allon.kubata.faturacao.domain.Usuario;

@Service
public class SessionManager {
    /** Sem sessão iniciada nada é atribuído a "Admin": o histórico usa o mesmo default do logout. */
    private static final String SEM_SESSAO = "Convidado";

    private String currentUser = SEM_SESSAO;
    private User userObject;
    private Usuario usuario;
    private Set<String> permissions = new HashSet<>();
    @Autowired
    private AcessoService acessoService;
    @Autowired
    private ao.allon.kubata.core.service.ModuleAccessService moduleAccessService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ao.allon.kubata.core.service.AccessGuard accessGuard;

    @jakarta.annotation.PostConstruct
    void exporUtilizadorAoGuard() {
        // O AccessGuard valida permissões ao nível do serviço; precisa do utilizador deste processo.
        accessGuard.registerUserResolver(() -> userObject);
    }

    public void login(String user, Set<String> perms) {
        this.currentUser = user;
        this.permissions = new HashSet<>(perms);
    }

    public void login(User user) {
        this.userObject = user;
        this.currentUser = user.getNome();
    }

    public void login(Usuario usuario) {
        this.usuario = usuario;
        this.currentUser = usuario != null && usuario.getNome() != null ? usuario.getNome() : this.currentUser;
    }

    public void logout() {
        this.currentUser = SEM_SESSAO;
        this.userObject = null;
        this.usuario = null;
        this.permissions.clear();
    }

    public User getUserObject() {
        return userObject;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Long getCurrentUserId() {
        if (userObject != null && userObject.getId() != null) {
            return userObject.getId();
        }
        return usuario != null ? usuario.getId() : null;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("ADMIN");
    }

    public boolean hasAccess(String modulo, String opcao) {
        if (userObject != null && userObject.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) return true;
        if (userObject == null) return permissions.contains("ADMIN");
        try {
            // Um módulo desactivado no Kubata Administrator bloqueia o acesso aqui também.
            if (!moduleAccessService.isPermissionModuleEnabled(
                    ao.allon.kubata.core.security.PermissionCatalog.canonicalSecurityModule(modulo))) {
                return false;
            }
            return acessoService.temAcesso(userObject, modulo, opcao);
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void notifyPermissionsChanged() {
        if (userObject != null && eventPublisher != null) {
            eventPublisher.publishEvent(new PermissionsChangedEvent(this, userObject));
        }
    }
}
