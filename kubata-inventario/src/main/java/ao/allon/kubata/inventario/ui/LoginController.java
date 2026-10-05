package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.platform.login.CentralLoginController;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LoginController {
    private final CentralLoginController delegate;

    public LoginController(AuthService authService,
                           ApplicationEventPublisher eventPublisher,
                           PasswordChangeService passwordChangeService) {
        this.delegate = new CentralLoginController(
                authService,
                passwordChangeService,
                "INVENTARIO",
                "Kubata Gestão de Inventário",
                user -> eventPublisher.publishEvent(new ao.allon.kubata.inventario.ui.event.LoginSuccessEvent(this, user))
        );
    }

    public StackPane createView(Stage stage) {
        return delegate.createView(stage);
    }

    public void shutdown() {
        delegate.shutdown();
    }
}
