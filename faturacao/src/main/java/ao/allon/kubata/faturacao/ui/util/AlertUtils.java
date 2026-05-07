package ao.allon.kubata.faturacao.ui.util;

import ao.allon.kubata.faturacao.ui.modal.ModalService;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class AlertUtils {

    private static final AtomicReference<ModalService> MODAL_SERVICE = new AtomicReference<>();

    private AlertUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(ModalService modalService) {
        MODAL_SERVICE.set(modalService);
    }

    private static ModalService getModalServiceOrThrow() {
        ModalService ms = MODAL_SERVICE.get();
        if (ms == null) {
            throw new IllegalStateException("AlertUtils não foi inicializado. Chame AlertUtils.initialize(modalService) na inicialização da UI.");
        }
        return ms;
    }

    private static Node buildMessageContent(FontIcon icon, String header, String message, Node extra) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(5, 0, 0, 0));

        HBox top = new HBox(10);
        top.setAlignment(Pos.TOP_LEFT);
        if (icon != null) {
            icon.setIconSize(22);
            top.getChildren().add(icon);
        }

        VBox texts = new VBox(6);
        if (header != null && !header.isBlank()) {
            Label lblHeader = new Label(header);
            lblHeader.getStyleClass().addAll(Styles.TITLE_4);
            lblHeader.setWrapText(true);
            texts.getChildren().add(lblHeader);
        }

        if (message != null && !message.isBlank()) {
            Label lblMsg = new Label(message);
            lblMsg.getStyleClass().addAll(Styles.TEXT_MUTED);
            lblMsg.setWrapText(true);
            texts.getChildren().add(lblMsg);
        }

        top.getChildren().add(texts);
        root.getChildren().add(top);

        if (extra != null) {
            root.getChildren().add(extra);
        }
        return root;
    }

    private static FontIcon icon(Feather feather, String styleClass) {
        FontIcon ic = new FontIcon(feather);
        if (styleClass != null && !styleClass.isBlank()) {
            ic.getStyleClass().add(styleClass);
        }
        return ic;
    }

    public static void showSuccess(String title, String message) {
        Platform.runLater(() -> getModalServiceOrThrow().create()
                .title(title)
                .content(buildMessageContent(icon(Feather.CHECK_CIRCLE, Styles.SUCCESS), null, message, null))
                .autoSize()
                .withCloseButton()
                .buildAndShow());
    }

    public static void showError(String title, String message) {
        Platform.runLater(() -> getModalServiceOrThrow().create()
                .title(title)
                .content(buildMessageContent(icon(Feather.X_CIRCLE, Styles.DANGER), "Erro Encontrado", message, null))
                .autoSize()
                .withCloseButton()
                .buildAndShow());
    }
    
    public static void showWarning(String title, String message) {
        Platform.runLater(() -> getModalServiceOrThrow().create()
                .title(title)
                .content(buildMessageContent(icon(Feather.ALERT_TRIANGLE, Styles.WARNING), "Atenção", message, null))
                .autoSize()
                .withCloseButton()
                .buildAndShow());
    }

    public static boolean showConfirmation(String title, String message) {
        AtomicReference<Boolean> ref = new AtomicReference<>(false);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> getModalServiceOrThrow().create()
                .title(title)
                .content(buildMessageContent(icon(Feather.HELP_CIRCLE, Styles.ACCENT), "Confirmação", message, null))
                .autoSize()
                .withCustomButton("Sim", () -> {
                    ref.set(true);
                    latch.countDown();
                }, Styles.SUCCESS)
                .withCancelButton("Não", latch::countDown)
                .buildAndShow());

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Boolean.TRUE.equals(ref.get());
    }

    public static void showInfoAlert(String title, String message) {
        showSuccess(title, message);
    }

    public static void showErrorAlert(String title, String message) {
        showError(title, message);
    }

    public static void showSuccessNotification(String message) {
        showSuccess("Sucesso", message);
    }

    public static boolean showConfirmationAlert(String title, String message) {
        return showConfirmation(title, message);
    }

    public static void showWarningAlert(String title, String message) {
        showWarning(title, message);
    }

    public static void showExceptionAlert(String title, String header, Exception ex) {
        showException(title, header, ex);
    }

    public static void showExceptionAlert(String title, String header, Throwable ex) {
        if (ex instanceof Exception) {
            showException(title, header, (Exception) ex);
        } else {
            showException(title, header, new Exception(ex));
        }
    }

    public static void showException(String title, String header, Exception ex) {
        Platform.runLater(() -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea ta = new TextArea(exceptionText);
            ta.setEditable(false);
            ta.setWrapText(false);

            ScrollPane sp = new ScrollPane(ta);
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setPrefViewportHeight(260);
            sp.setPrefViewportWidth(720);

            Button btnCopy = new Button("Copiar Erro", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
            btnCopy.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
            btnCopy.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(exceptionText);
                clipboard.setContent(content);
                btnCopy.setText("Copiado!");
            });

            VBox extra = new VBox(10, new Label("Detalhes do erro:"), sp, btnCopy);
            extra.setPadding(new Insets(10, 0, 0, 0));

            getModalServiceOrThrow().create()
                    .title(title)
                    .content(buildMessageContent(icon(Feather.X_CIRCLE, Styles.DANGER), header, "Ocorreu um erro inesperado.", extra))
                    .dynamicSize()
                    .withCloseButton()
                    .buildAndShow();
        });
    }

    /**
     * Mostra alerta de erro com opção de copiar a mensagem.
     * @param title Título do alerta
     * @param message Mensagem de erro
     */
    public static void showErrorWithCopy(String title, String message) {
        Platform.runLater(() -> {
            Button btnCopy = new Button("Copiar Mensagem", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
            btnCopy.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
            btnCopy.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(message);
                clipboard.setContent(content);
                btnCopy.setText("Copiado!");
            });

            VBox extra = new VBox(10, btnCopy);
            extra.setPadding(new Insets(10, 0, 0, 0));

            getModalServiceOrThrow().create()
                    .title(title)
                    .content(buildMessageContent(icon(Feather.X_CIRCLE, Styles.DANGER), "Erro", message, extra))
                    .autoSize()
                    .withCloseButton()
                    .buildAndShow();
        });
    }
}
