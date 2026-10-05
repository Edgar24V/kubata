package ao.allon.kubata.rh.controller;

import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.platform.login.CentralLoginController;
import ao.allon.kubata.rh.ui.event.LoginSuccessEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Login central da plataforma para o módulo RH.
 */
@Component
public class LoginController {

    private final CentralLoginController delegate;

    public LoginController(
            AuthService authService,
            ApplicationEventPublisher eventPublisher,
            PasswordChangeService passwordChangeService) {

        this.delegate = new CentralLoginController(
                authService,
                passwordChangeService,
                "RH",
                "Kubata Recursos Humanos",
                user -> eventPublisher.publishEvent(
                        new LoginSuccessEvent(this, user)
                )
        );
    }

    public StackPane createView(Stage stage) {
        return delegate.createView(stage);
    }

    public void shutdown() {
        delegate.shutdown();
    }
}
