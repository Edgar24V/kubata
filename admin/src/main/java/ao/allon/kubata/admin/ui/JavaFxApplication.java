package ao.allon.kubata.admin.ui;

import ao.allon.kubata.admin.KubataAdminApplication;
import ao.allon.kubata.admin.service.PendingRestoreApplier;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.util.GlobalExceptionHandler;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        GlobalExceptionHandler.setup(); // Configura o handler de exceções global
        ao.allon.kubata.core.config.KubataDatabasePaths.ensureDirectory();
        PendingRestoreApplier.applyIfPresent();
        applicationContext = new SpringApplicationBuilder(KubataAdminApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        applicationContext.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            releaseUserSession();
            applicationContext.close();
        }
        Platform.exit();
    }

    /**
     * Fecha a sessão do utilizador na BD ao sair, para que uma janela fechada
     * não deixe uma sessão activa a consumir o limite de sessões simultâneas.
     */
    private void releaseUserSession() {
        try {
            SessionManager sessionManager = applicationContext.getBean(SessionManager.class);
            User user = sessionManager.getUser();
            if (user != null) {
                applicationContext.getBean(AuthService.class).logout(user, "127.0.0.1");
                sessionManager.logout();
            }
        } catch (Exception ex) {
            // A saída da aplicação nunca deve falhar devido ao registo da sessão.
            LoggerFactory.getLogger(JavaFxApplication.class)
                    .warn("Não foi possível terminar a sessão ao sair: {}", ex.getMessage());
        }
    }
}
