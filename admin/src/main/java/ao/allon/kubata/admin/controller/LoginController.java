package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.MaintenanceModeService;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.platform.login.CentralLoginController;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adaptador do login do Administrator para o login central da plataforma.
 *
 * <p>A autenticação não é diferente das restantes aplicações: o Core valida
 * identidade, MFA, políticas e sessão. O contexto ADMINISTRATOR apenas exige
 * uma conta administrativa.</p>
 */
@Component
public class LoginController {

    private final CentralLoginController delegate;

    public LoginController(
            AuthService authService,
            ApplicationEventPublisher eventPublisher,
            MaintenanceModeService maintenanceModeService,
            PasswordChangeService passwordChangeService,
            ModalManager modalManager) {

        this.delegate = new CentralLoginController(
                authService,
                passwordChangeService,
                "ADMINISTRATOR",
                "Kubata Administrator",
                user -> eventPublisher.publishEvent(
                        new LoginSuccessEvent(this, user)
                )
        );
    }

    public Parent createView(Stage stage) {
        return delegate.createView(stage);
    }

    public void shutdown() {
        delegate.shutdown();
    }
}
