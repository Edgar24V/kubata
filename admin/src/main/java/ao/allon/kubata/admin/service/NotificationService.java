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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de notificações moderno para o Kubata Administrator.
 * Fornece toasts elegantes no rodapé da aplicação.
 */
@Service
public class NotificationService {

    private StackPane rootContainer;
    private VBox notificationBox;
    private final Map<String, Long> recentNotifications = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 2000; // 2 segundos para evitar duplicados

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

        // Anti-duplicação (Debounce)
        String key = titleStr + "|" + summaryStr;
        long now = System.currentTimeMillis();
        if (recentNotifications.containsKey(key)) {
            if (now - recentNotifications.get(key) < DEBOUNCE_MS) {
                return; // Ignora se for muito recente (duplicado)
            }
        }
        recentNotifications.put(key, now);

        VBox toast = new VBox(5);
        toast.getStyleClass().addAll("notification-toast", styleClass);
        toast.setMinWidth(320);
        toast.setMaxWidth(400);
        toast.setPadding(new Insets(15));
        
        // Cores baseadas no estilo
        final String finalAccentColor;
        if (styleClass.contains("error")) finalAccentColor = "#e74c3c";
        else if (styleClass.contains("warning")) finalAccentColor = "#f39c12";
        else if (styleClass.contains("info")) finalAccentColor = "#3498db";
        else finalAccentColor = "#2ecc71"; // Success

        toast.setStyle("-fx-background-color: white; " +
                      "-fx-background-radius: 12px; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5); " +
                      "-fx-border-color: " + finalAccentColor + "; " +
                      "-fx-border-width: 0 0 0 5; " + // Barra lateral colorida
                      "-fx-cursor: hand;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(30, 30);
        iconCircle.setStyle("-fx-background-color: " + finalAccentColor + "22; -fx-background-radius: 50;");
        iconCircle.getChildren().add(IconUtils.icon(icon, 16));
        
        Label title = new Label(titleStr);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(IconUtils.icon(Feather.X, 12));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-padding: 0;");
        
        header.getChildren().addAll(iconCircle, title, spacer, closeBtn);

        Label summary = new Label(summaryStr);
        summary.setWrapText(true);
        summary.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 0 42;");

        toast.getChildren().addAll(header, summary);

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
        
        // Efeito de hover e clique para fechar
        toast.setOnMouseEntered(e -> toast.setStyle(toast.getStyle() + "-fx-border-color: " + finalAccentColor + "; -fx-background-color: #fafafa;"));
        toast.setOnMouseExited(e -> toast.setStyle(toast.getStyle().replace("-fx-background-color: #fafafa;", "")));
        toast.setOnMouseClicked(e -> {
            autoClose.stop();
            closeNotification(toast);
        });
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
