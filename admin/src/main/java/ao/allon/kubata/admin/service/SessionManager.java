package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AcessoService;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    private final AcessoService acessoService;

    private User user;

    public SessionManager(AcessoService acessoService) {
        this.acessoService = acessoService;
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

    public boolean hasAccess(String modulo, String opcao) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) {
            return true;
        }
        try {
            return acessoService.temAcesso(user, modulo, opcao);
        } catch (Exception e) {
            return false;
        }
    }
}
