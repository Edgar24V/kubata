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
    private String currentUser;
    private User userObject;
    private Usuario usuario;
    private Set<String> permissions = new HashSet<>();
    @Autowired
    private AcessoService acessoService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public SessionManager() {
        // Default permissions for development
        currentUser = "Admin";
        permissions.add("INVENTARIO_VIEW");
        permissions.add("INVENTARIO_CREATE");
        permissions.add("INVENTARIO_EDIT");
        permissions.add("INVENTARIO_DELETE");
        permissions.add("INVENTARIO_STOCK"); // Stock operations
        permissions.add("INVENTARIO_REPORT");
        permissions.add("INVENTARIO_ADMIN");
        permissions.add("FATURACAO_VIEW");
        permissions.add("FATURACAO_CREATE");
        permissions.add("CLIENTE_VIEW");
        permissions.add("RECIBO_VIEW");
        permissions.add("RECIBO_CREATE");
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
        this.currentUser = "Convidado";
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

    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("ADMIN");
    }

    public boolean hasAccess(String modulo, String opcao) {
        if (userObject != null && userObject.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) return true;
        if (userObject == null) return permissions.contains("ADMIN");
        try {
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
