package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.view.AdminMainView;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
    private final ModalManager modalManager;

    private Stage stage;
    private AdminMainView activeMainView;

    public MainController(LoginController loginController,
                          SessionManager sessionManager,
                          ApplicationContext applicationContext,
                          AuthService authService,
                          ModalManager modalManager) {
        this.loginController = loginController;
        this.sessionManager = sessionManager;
        this.applicationContext = applicationContext;
        this.authService = authService;
        this.modalManager = modalManager;
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
        activeMainView = view;
        view.init(stage);

        // "Encerrar Sessão" volta ao login; o X da janela encerra o processo.
        view.setOnLogout(this::confirmLogout);
        view.setOnWindowClose(this::requestApplicationClose);

        stage.setOnCloseRequest(event -> {
            event.consume();
            requestApplicationClose();
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

    private void requestApplicationClose() {
        if (activeMainView != null && activeMainView.hasUnsavedChanges()) {
            showUnsavedChangesWarning();
            return;
        }

        performLogoutAndExit();
    }

    private void showUnsavedChangesWarning() {
        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(4));

        Label title = new Label("Existem alterações ou operações por concluir.");
        title.getStyleClass().add("label-title");

        Label message = new Label(
                "Há conteúdo aberto que pode ainda não ter sido guardado. "
                        + "Fechar agora pode descartar essas alterações."
        );
        message.setWrapText(true);
        message.getStyleClass().add("text-muted");

        Label action = new Label(
                "Escolha «Fechar sem guardar» apenas depois de confirmar que não precisa destes dados."
        );
        action.setWrapText(true);
        action.getStyleClass().add("kubata-modal-warning-text");

        content.getChildren().addAll(title, message, action);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Alterações por guardar")
                        .subtitle("Confirmar encerramento do Kubata Administrator")
                        .icon(org.kordamp.ikonli.feather.Feather.ALERT_TRIANGLE)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(560, 310)
                        .minSize(500, 280)
                        .maximizable(false)
                        .minimizable(false)
                        .withConfirmButtons("Fechar sem guardar", "Cancelar")
                        .confirmStyle("button-danger")
                        .cancelStyle("button-outlined")
                        .closeOnOverlayClick(false)
                        .onConfirm(() -> {
                            modalManager.hideModal();
                            performLogoutAndExit();
                        })
        );
    }

    private void performLogoutAndExit() {
        User user = sessionManager.getUser();
        Long exactSessionId = sessionManager.getSessionId();

        try {
            if (user != null) {
                authService.logout(user, exactSessionId, "127.0.0.1");
            }
        } catch (Exception ignored) {
            // O encerramento não fica bloqueado por uma falha de persistência.
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
