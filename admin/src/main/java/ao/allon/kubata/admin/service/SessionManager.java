package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AcessoService;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

    private final AcessoService acessoService;

    private User user;
    private Long sessionId;

    public SessionManager(AcessoService acessoService) {
        this.acessoService = acessoService;
    }

    public void login(User user) {
        this.user = user;
        this.sessionId = user == null ? null : user.getSessionId();
    }

    public void logout() {
        if (this.user != null) {
            this.user.setSessionId(null);
        }
        this.user = null;
        this.sessionId = null;
    }

    public Long getSessionId() {
        return sessionId;
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
