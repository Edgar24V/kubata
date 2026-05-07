package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.view.AdminMainView;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
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

    private void switchToLogin() {
        Parent root = loginController.createView(stage);
        // Dimensões sincronizadas com o LoginController (960x680)
        Scene scene = new Scene(root, 960, 680);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setResizable(false);
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
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);
        a.showAndWait();
    }
}
