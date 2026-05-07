package ao.allon.kubata.faturacao.ui.event;

import ao.allon.kubata.core.domain.User;
import org.springframework.context.ApplicationEvent;

public class PermissionsChangedEvent extends ApplicationEvent {
    private final User user;
    public PermissionsChangedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
    public User getUser() {
        return user;
    }
}

