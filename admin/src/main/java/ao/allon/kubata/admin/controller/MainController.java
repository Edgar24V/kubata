package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.view.AdminMainView;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
    private final AuthService authService;

    private Stage stage;

    public MainController(LoginController loginController,
                          SessionManager sessionManager,
                          ApplicationContext applicationContext,
                          AuthService authService) {
        this.loginController = loginController;
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
        this.authService = authService;
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

    private void switchToLogin() {
        Parent root = loginController.createView(stage);

        // Janela de login sem moldura nativa, com controles personalizados
        // e suporte a maximizar/restaurar.
        Scene scene = new Scene(root, 1120, 720);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setOnCloseRequest(null);
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
        stage.setOnCloseRequest(event -> {
            event.consume();
            performLogoutAndExit();
        });
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

    private void performLogoutAndExit() {
        User user = sessionManager.getUser();
        try {
            if (user != null) {
                authService.logout(user, "127.0.0.1");
            }
        } catch (Exception ignored) {
            // O encerramento da aplicação não deve ficar bloqueado por falha de persistência.
        } finally {
            sessionManager.logout();
            stage.setOnCloseRequest(null);
            stage.close();
        }
    }

    private void confirmLogout() {
        User user = sessionManager.getUser();

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Encerrar Sessão");
        confirmation.setHeaderText("Pretende encerrar a sessão actual?");
        confirmation.setContentText(
                user == null || user.getNome() == null
                        ? "Será devolvido ao ecrã de login."
                        : "A sessão de \"" + user.getNome()
                                + "\" será encerrada e voltará ao ecrã de login."
        );

        confirmation.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                performLogoutAndShowLogin();
            }
        });
    }

    /**
     * Encerra a sessão do administrador autenticado e regressa ao login.
     * O contexto local é sempre limpo, mesmo que o registo da saída falhe.
     */
    public void performLogoutAndShowLogin() {
        User user = sessionManager.getUser();

        try {
            if (user != null) {
                authService.logout(user, "127.0.0.1");
            }
        } catch (Exception ignored) {
            // O encerramento local não deve ficar bloqueado por uma falha de persistência.
        } finally {
            sessionManager.logout();
            switchToLogin();
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);
        a.showAndWait();
    }
}
