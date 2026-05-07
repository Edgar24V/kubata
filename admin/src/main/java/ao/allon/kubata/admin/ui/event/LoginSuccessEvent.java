package ao.allon.kubata.admin.ui.event;

import ao.allon.kubata.core.domain.User;
import org.springframework.context.ApplicationEvent;

public class LoginSuccessEvent extends ApplicationEvent {

    private final User user;

    public LoginSuccessEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
