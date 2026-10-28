package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.view.AdminMainView;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.security.PermissionCatalog;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.MaintenanceModeService;
import ao.allon.kubata.core.service.ModuleAccessService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.core.ui.login.KubataLoginView;
import ao.allon.kubata.core.ui.login.LoginViewConfig;
import javafx.geometry.Dimension2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final SessionManager sessionManager;
    private final ApplicationContext applicationContext;
    private final AuthService authService;
    private final PasswordChangeService passwordChangeService;
    private final MaintenanceModeService maintenanceModeService;
    private final ModuleAccessService moduleAccessService;

    private Stage stage;

    public MainController(SessionManager sessionManager,
                          ApplicationContext applicationContext,
                          AuthService authService,
                          PasswordChangeService passwordChangeService,
                          MaintenanceModeService maintenanceModeService,
                          ModuleAccessService moduleAccessService) {
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
        this.authService = authService;
        this.passwordChangeService = passwordChangeService;
        this.maintenanceModeService = maintenanceModeService;
        this.moduleAccessService = moduleAccessService;
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        this.stage = event.getStage();
        ThemeManager.setupCustomTitleBar(stage);
        stage.setTitle("Kubata");
        switchToLogin();
        stage.show();
    }

    public void handleLoginSuccess(User user) {
        if (user == null || user.getRole() == null) {
            showAlert("Acesso negado", "Utilizador inválido.");
            switchToLogin();
            return;
        }

        // O login partilhado já pede ADMINISTRATOR ao AuthService; repetimos a verificação
        // para que o gate do hub não dependa exclusivamente do ecrã de login.
        if (!moduleAccessService.canEnterApplication(user, PermissionCatalog.ADMINISTRATOR)) {
            showAlert("Acesso negado",
                    "Não tem permissão para aceder ao Kubata Administrator. "
                            + "Peça ao administrador para atribuir o perfil correcto.");
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
                authService.logout(user, "127.0.0.1");
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
                authService.logout(user, "127.0.0.1");
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
        KubataLoginView loginView = new KubataLoginView(
                LoginViewConfig.administrator(),
                authService,
                passwordChangeService,
                maintenanceModeService,
                this::handleLoginSuccess);
        Parent root = loginView.createView(stage);

        // Janela de login sem moldura nativa, com controles personalizados
        // e suporte a maximizar/restaurar.
        Dimension2D loginSize = KubataLoginView.recommendedSceneSize();
        Scene scene = new Scene(root, loginSize.getWidth(), loginSize.getHeight());
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setResizable(true);
        stage.setWidth(loginSize.getWidth());
        stage.setHeight(loginSize.getHeight());
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
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);
        a.showAndWait();
    }
}
