package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.view.AdminMainView;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.ui.modal.ModalManager;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final LoginController loginController;
    private final SessionManager sessionManager;
    private final ApplicationContext applicationContext;

    private Stage stage;

    public MainController(LoginController loginController, SessionManager sessionManager, ApplicationContext applicationContext) {
        this.loginController = loginController;
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        this.stage = event.getStage();
        ThemeManager.setupCustomTitleBar(stage);
        stage.setTitle("Kubata");
        switchToLogin();
        stage.show();
    }

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        User user = event.getUser();
        if (user == null || user.getRole() == null) {
            showAlert("Acesso negado", "Utilizador inválido.");
            switchToLogin();
            return;
        }

        if (user.getRole() != Role.ADMIN) {
            showAlert("Acesso negado", "Apenas administradores podem aceder ao Kubata.");
            switchToLogin();
            return;
        }

        sessionManager.login(user);
        switchToMain();
    }

    /**
     * Termina a sessão no serviço de autenticação e regressa ao login.
     * Se o registo de logout falhar, mantém a sessão local activa para evitar
     * mostrar o login enquanto a sessão anterior ainda consta como válida.
     */
    public void logoutToLogin() {
        User user = sessionManager.getUser();
        if (user != null) {
            try {
                applicationContext.getBean(AuthService.class).logout(user, "127.0.0.1");
            } catch (Exception ex) {
                showAlert(
                        "Não foi possível encerrar a sessão",
                        "O encerramento não foi confirmado pela base de dados. "
                                + "A sessão permanece activa.\n\n"
                                + (ex.getMessage() == null ? "Erro desconhecido." : ex.getMessage())
                );
                return;
            }
            sessionManager.logout();
        }
        switchToLogin();
    }

    /**
     * Fecha a aplicação apenas depois de confirmar o fim da sessão persistida.
     * Se a gravação do logout falhar, a janela permanece aberta.
     */
    public void closeApplication() {
        User user = sessionManager.getUser();
        if (user != null) {
            try {
                applicationContext.getBean(AuthService.class).logout(user, "127.0.0.1");
            } catch (Exception ex) {
                showAlert(
                        "Não foi possível encerrar a sessão",
                        "O Kubata Administrator continuará aberto porque o encerramento "
                                + "da sessão não foi confirmado.\n\n"
                                + (ex.getMessage() == null ? "Erro desconhecido." : ex.getMessage())
                );
                return;
            }
            sessionManager.logout();
        }

        if (stage != null) {
            stage.close();
        }
    }

    private void switchToLogin() {
        Parent root = loginController.createView(stage);

        // Janela de login sem moldura nativa, com controles personalizados
        // e suporte a maximizar/restaurar.
        Scene scene = new Scene(root, 1120, 720);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setMinWidth(920);
        stage.setMinHeight(560);
        stage.setResizable(true);
        stage.setWidth(1120);
        stage.setHeight(720);
        stage.centerOnScreen();
    }

    private void switchToMain() {
        AdminMainView view = applicationContext.getBean(AdminMainView.class);
        view.init(stage);
        Parent root = view;

        javafx.geometry.Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double w = Math.max(1100, vb.getWidth() * 0.92);
        double h = Math.max(720, vb.getHeight() * 0.92);

        Scene scene = new Scene(root, w, h);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    private void showAlert(String title, String content) {
        ModalManager manager = ModalManager.getActiveInstance();
        if (manager != null && manager.hasRoot()) {
            manager.warning(title, content);
        } else {
            org.slf4j.LoggerFactory.getLogger(MainController.class)
                    .warn("{}: {}", title, content);
        }
    }
}
