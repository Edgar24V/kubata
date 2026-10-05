package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.faturacao.ui.event.LoginSuccessEvent;
import ao.allon.kubata.platform.login.CentralLoginController;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adaptador do login de Faturação para a autenticação central do Kubata.
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
                "FATURACAO",
                "Kubata Faturação",
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
