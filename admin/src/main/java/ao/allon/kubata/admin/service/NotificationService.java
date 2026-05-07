package ao.allon.kubata.admin.service;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.admin.ui.util.IconUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de notificações moderno para o Kubata Administrator.
 * Fornece toasts elegantes no rodapé da aplicação.
 */
@Service
public class NotificationService {

    private StackPane rootContainer;
    private VBox notificationBox;

    /**
     * Define o container raiz onde as notificações serão exibidas.
     */
    public void setRoot(StackPane root) {
        if (root == null) return;
        this.rootContainer = root;
        
        if (notificationBox == null) {
            notificationBox = new VBox(10);
            notificationBox.setPickOnBounds(false);
            notificationBox.setAlignment(Pos.BOTTOM_RIGHT);
            notificationBox.setPadding(new Insets(0, 20, 40, 0)); // Acima da status bar
            notificationBox.setMouseTransparent(false);
        }
        
        // Garante que a notificationBox esteja no container atual e no topo
        if (!rootContainer.getChildren().contains(notificationBox)) {
            rootContainer.getChildren().add(notificationBox);
        }
        notificationBox.toFront();
    }

    public void showSuccess(String title, String summary) {
        Platform.runLater(() -> createNotification(title, summary, "notification-success", Feather.CHECK_CIRCLE));
    }

    public void showError(String title, String summary) {
        Platform.runLater(() -> createNotification(title, summary, "notification-error", Feather.ALERT_CIRCLE));
    }

    public void showInfo(String title, String summary) {
        Platform.runLater(() -> createNotification(title, summary, "notification-info", Feather.INFO));
    }

    public void showWarning(String title, String summary) {
        Platform.runLater(() -> createNotification(title, summary, "notification-warning", Feather.ALERT_TRIANGLE));
    }

    private void createNotification(String titleStr, String summaryStr, String styleClass, Feather icon) {
        if (rootContainer == null || notificationBox == null) return;

        VBox toast = new VBox(5);
        toast.getStyleClass().addAll("notification-toast", styleClass);
        toast.setMinWidth(300);
        toast.setMaxWidth(400);
        toast.setPadding(new Insets(12, 15, 12, 15));
        
        // Estilo base via código para garantir visibilidade inicial, mas preferir CSS
        toast.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 4); " +
                      "-fx-border-color: #e0e0e0; -fx-border-radius: 8px; -fx-border-width: 1px;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label();
        iconLabel.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_SMALL));
        
        Label title = new Label(titleStr);
        title.getStyleClass().add("notification-title");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(IconUtils.icon(Feather.X, 12));
        closeBtn.getStyleClass().add("button-icon-small");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        
        header.getChildren().addAll(iconLabel, title, spacer, closeBtn);

        Label summary = new Label(summaryStr);
        summary.setWrapText(true);
        summary.getStyleClass().add("notification-summary");
        summary.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Label time = new Label("Agora • " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        time.getStyleClass().add("notification-time");
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");

        toast.getChildren().addAll(header, summary, time);

        // Animação de entrada
        toast.setOpacity(0);
        toast.setTranslateY(20);
        
        notificationBox.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toast);
        fadeIn.setToValue(1);
        
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), toast);
        slideIn.setFromY(20);
        slideIn.setToY(0);

        ParallelTransition show = new ParallelTransition(fadeIn, slideIn);
        show.play();

        // Auto-close após 5 segundos
        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(5), e -> closeNotification(toast)));
        autoClose.play();

        closeBtn.setOnAction(e -> {
            autoClose.stop();
            closeNotification(toast);
        });
        
        // Efeito de hover
        toast.setOnMouseEntered(e -> toast.setStyle(toast.getStyle() + "-fx-border-color: #2ecc71;"));
        toast.setOnMouseExited(e -> toast.setStyle(toast.getStyle() + "-fx-border-color: #e0e0e0;"));
    }

    private void closeNotification(VBox toast) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setToValue(0);
        
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toast);
        slideOut.setToY(20);

        ParallelTransition hide = new ParallelTransition(fadeOut, slideOut);
        hide.setOnFinished(e -> notificationBox.getChildren().remove(toast));
        hide.play();
    }
}
