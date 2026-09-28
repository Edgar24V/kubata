package ao.allon.kubata.admin.ui.modal;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Service;

/**
 * Gestor central dos modais do Kubata Administrator.
 *
 * <p>Todos os modais utilizam a mesma estrutura visual e funcional:
 * cabeçalho, conteúdo responsivo, rodapé de acções, estados semânticos,
 * atalhos de teclado e suporte a conteúdo rolável.</p>
 *
 * <p>A API existente é preservada para não obrigar as vistas actuais a serem
 * alteradas. Novas funcionalidades podem ser usadas através de {@link ModalConfig}.</p>
 */
@Service
public class ModalManager {

    private static final String DEFAULT_DIALOG_CLASS = "card-container";
    private static final String TITLE_CLASS = "label-title";
    private static final Insets DEFAULT_MARGIN = new Insets(20);
    private static final Insets DEFAULT_PADDING = new Insets(20);

    private final JMetroModalPane modalPane = new JMetroModalPane();
    private final JMetroModalPane modalPaneTop = new JMetroModalPane();
    private final JMetroModalPane modalPaneTopmost = new JMetroModalPane();

    private boolean persistent = false;
    private InternalModalBox currentLoadingModal;

    public enum ModalType {
        DEFAULT, TOP, TOPMOST
    }

    /** Semântica visual do modal. */
    public enum ModalTone {
        DEFAULT, INFO, SUCCESS, WARNING, DANGER
    }

    /**
     * Configuração fluente e retrocompatível do modal.
     */
    public static class ModalConfig {
        private String title = "";
        private String subtitle = "";
        private boolean scrollable = false;
        private boolean resizable = true;
        private boolean closeOnOverlayClick = true;
        private boolean closeOnEscape = true;
        private int width = (int) Region.USE_PREF_SIZE;
        private int height = (int) Region.USE_PREF_SIZE;
        private int minWidth = -1;
        private int minHeight = -1;
        private int maxWidth = -1;
        private int maxHeight = -1;
        private boolean showConfirmButtons = false;
        private String confirmText = "Confirmar";
        private String cancelText = "Cancelar";
        private String confirmStyleClass = "button-primary";
        private String cancelStyleClass = "button-outlined";
        private Feather headerIcon = null;
        private ModalTone tone = ModalTone.DEFAULT;
        private boolean showFooterDivider = true;
        private String footerHint = "";
        private Runnable onConfirm = () -> {};
        private Runnable onCancel = () -> {};
        private ModalType modalType = ModalType.DEFAULT;

        public ModalConfig title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public ModalConfig subtitle(String subtitle) {
            this.subtitle = subtitle == null ? "" : subtitle;
            return this;
        }

        public ModalConfig scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return this;
        }

        public ModalConfig resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        public ModalConfig closeOnOverlayClick(boolean enabled) {
            this.closeOnOverlayClick = enabled;
            return this;
        }

        public ModalConfig closeOnEscape(boolean enabled) {
            this.closeOnEscape = enabled;
            return this;
        }

        public ModalConfig autoSize() {
            this.width = (int) Region.USE_PREF_SIZE;
            this.height = (int) Region.USE_PREF_SIZE;
            return this;
        }

        public ModalConfig size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ModalConfig minSize(int width, int height) {
            this.minWidth = width;
            this.minHeight = height;
            return this;
        }

        public ModalConfig maxSize(int width, int height) {
            this.maxWidth = width;
            this.maxHeight = height;
            return this;
        }

        public ModalConfig withConfirmButtons(String confirmText, String cancelText) {
            this.showConfirmButtons = true;
            this.confirmText = confirmText == null ? "Confirmar" : confirmText;
            this.cancelText = cancelText == null ? "Cancelar" : cancelText;
            return this;
        }

        public ModalConfig withConfirmButtons(String confirmText) {
            this.showConfirmButtons = true;
            this.confirmText = confirmText == null ? "Confirmar" : confirmText;
            return this;
        }

        public ModalConfig confirmStyle(String styleClass) {
            this.confirmStyleClass = styleClass == null ? "button-primary" : styleClass;
            return this;
        }

        public ModalConfig cancelStyle(String styleClass) {
            this.cancelStyleClass = styleClass == null ? "button-outlined" : styleClass;
            return this;
        }

        public ModalConfig icon(Feather icon) {
            this.headerIcon = icon;
            return this;
        }

        public ModalConfig tone(ModalTone tone) {
            this.tone = tone == null ? ModalTone.DEFAULT : tone;
            return this;
        }

        public ModalConfig footerHint(String hint) {
            this.footerHint = hint == null ? "" : hint;
            return this;
        }

        public ModalConfig footerDivider(boolean visible) {
            this.showFooterDivider = visible;
            return this;
        }

        public ModalConfig onConfirm(Runnable onConfirm) {
            this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
            return this;
        }

        public ModalConfig onCancel(Runnable onCancel) {
            this.onCancel = onCancel == null ? () -> {} : onCancel;
            return this;
        }

        public ModalType getModalType() {
            return modalType;
        }

        public ModalConfig modalType(ModalType type) {
            this.modalType = type == null ? ModalType.DEFAULT : type;
            return this;
        }
    }

    public ModalManager() {
        setupModalPanes();
    }

    private void setupModalPanes() {
        modalPane.setId("modalPane");
        modalPaneTop.setId("modalPaneTop");
        modalPaneTopmost.setId("modalPaneTopmost");
    }

    /**
     * Liga os overlays ao StackPane raiz da aplicação.
     */
    public void setRoot(StackPane stackPane) {
        if (stackPane == null) {
            return;
        }

        if (!stackPane.getChildren().contains(modalPane)) {
            stackPane.getChildren().add(modalPane);
        }
        if (!stackPane.getChildren().contains(modalPaneTop)) {
            stackPane.getChildren().add(modalPaneTop);
        }
        if (!stackPane.getChildren().contains(modalPaneTopmost)) {
            stackPane.getChildren().add(modalPaneTopmost);
        }
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
        modalPane.setPersistent(persistent);
        modalPaneTop.setPersistent(persistent);
        modalPaneTopmost.setPersistent(persistent);
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    public void showLoadingModal(Task<?> task, String title, String message) {
        showLoadingModal(task, title, message, ModalType.DEFAULT);
    }

    public void showLoadingModal(Task<?> task,
                                 String title,
                                 String message,
                                 ModalType modalType) {
        JMetroModalPane targetPane = getModalPane(modalType);
        targetPane.setPersistent(true);

        VBox loadingContent = createLoadingContent(title, message, task);
        InternalModalBox loadingModal = new InternalModalBox(targetPane, true, true);
        loadingModal.setPrefSize(410, 230);
        loadingModal.addContent(loadingContent);

        currentLoadingModal = loadingModal;
        targetPane.show(loadingModal);
        animateIn(loadingModal);

        if (task != null) {
            setupTaskHandlers(task, targetPane);
        }
    }

    public void showLoadingModal(String title, String message) {
        showLoadingModal(title, message, ModalType.DEFAULT);
    }

    public void showLoadingModal(String title, String message, ModalType modalType) {
        showLoadingModal(null, title, message, modalType);
    }

    private VBox createLoadingContent(String title, String message, Task<?> task) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 6, 6, 6));
        content.setAlignment(Pos.CENTER);

        Label icon = new Label("", IconUtils.icon(Feather.LOADER, 26));

        Label titleLabel = new Label(
                title == null || title.isBlank() ? "A processar..." : title
        );
        titleLabel.getStyleClass().add(TITLE_CLASS);

        Label messageLabel = new Label(
                message == null || message.isBlank() ? "Aguarde enquanto a operação é executada." : message
        );
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(340);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.getStyleClass().add("text-muted");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(320);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        if (task != null) {
            progressBar.progressProperty().bind(task.progressProperty());
        } else {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }

        VBox.setVgrow(progressBar, Priority.NEVER);

        content.getChildren().addAll(icon, titleLabel, messageLabel, progressBar);

        if (task != null) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER);

            Button cancel = new Button(
                    "Cancelar",
                    IconUtils.icon(Feather.X, 12)
            );
            cancel.getStyleClass().add("button-outlined");
            cancel.setOnAction(e -> {
                if (!task.isDone()) {
                    task.cancel();
                }
            });

            actions.getChildren().add(cancel);
            content.getChildren().add(actions);
        }

        return content;
    }

    private void setupTaskHandlers(Task<?> task, JMetroModalPane targetPane) {
        task.setOnSucceeded(event -> closeLoading(targetPane));
        task.setOnFailed(event -> {
            closeLoading(targetPane);
            Throwable exception = task.getException();
            Platform.runLater(() ->
                    showErrorModal(
                            "Erro no processamento",
                            "A operação não pôde ser concluída.",
                            exception
                    )
            );
        });
        task.setOnCancelled(event -> closeLoading(targetPane));
    }

    private void closeLoading(JMetroModalPane targetPane) {
        Platform.runLater(() -> {
            targetPane.hide();
            if (currentLoadingModal != null && currentLoadingModal.getParent() == null) {
                currentLoadingModal = null;
            } else {
                currentLoadingModal = null;
            }
        });
    }

    public void hideLoadingModal() {
        modalPane.hide();
        modalPaneTop.hide();
        modalPaneTopmost.hide();
        currentLoadingModal = null;
        resetModalPersistence();
    }

    // ---------------------------------------------------------------------
    // Main API
    // ---------------------------------------------------------------------

    public void showModal(Node content, ModalConfig config) {
        if (content == null) {
            throw new IllegalArgumentException("O conteúdo do modal não pode ser nulo.");
        }

        ModalConfig safeConfig = config == null ? new ModalConfig() : config;
        JMetroModalPane targetPane = getModalPane(safeConfig.modalType);
        targetPane.setPersistent(persistent);

        if (safeConfig.scrollable) {
            showScrollableModal(targetPane, content, safeConfig);
        } else {
            showStandardModal(targetPane, content, safeConfig);
        }
    }

    private JMetroModalPane getModalPane(ModalType type) {
        return switch (type) {
            case TOP -> modalPaneTop;
            case TOPMOST -> modalPaneTopmost;
            default -> modalPane;
        };
    }

    private void showStandardModal(JMetroModalPane targetPane,
                                   Node content,
                                   ModalConfig config) {
        VBox dialogContent = new VBox();
        dialogContent.setFillWidth(true);

        if (!config.title.isBlank() || !config.subtitle.isBlank() || config.headerIcon != null) {
            dialogContent.getChildren().add(
                    createHeader(config, targetPane)
            );
        }

        VBox contentWrapper = new VBox();
        contentWrapper.setPadding(DEFAULT_PADDING);
        contentWrapper.setFillWidth(true);
        prepareContentForDialog(content);
        contentWrapper.getChildren().add(content);

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(config.scrollable
                ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("kubata-modal-scroll");
        scrollPane.setMinHeight(0);
        scrollPane.setMinWidth(0);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        dialogContent.getChildren().add(scrollPane);

        if (config.showConfirmButtons) {
            if (config.showFooterDivider) {
                Separator separator = new Separator();
                separator.getStyleClass().add("kubata-modal-separator");
                dialogContent.getChildren().add(separator);
            }

            HBox buttons = createConfirmationButtons(config, targetPane);

            VBox footer = new VBox(8);
            footer.getStyleClass().add("kubata-modal-footer");
            footer.setPadding(new Insets(12, 20, 16, 20));

            if (!config.footerHint.isBlank()) {
                Label hint = new Label(config.footerHint);
                hint.getStyleClass().add("text-muted");
                hint.setWrapText(true);
                footer.getChildren().add(hint);
            }

            footer.getChildren().add(buttons);
            dialogContent.getChildren().add(footer);
        }

        InternalModalBox dialog = new InternalModalBox(
                targetPane,
                config.closeOnEscape,
                true
        );
        dialog.setOnMouseClicked(event -> event.consume());

        StackPane.setMargin(dialog, DEFAULT_MARGIN);
        configureInternalDialogSize(dialog, config);
        dialog.addContent(dialogContent);

        targetPane.configureOverlayDismiss(config.closeOnOverlayClick);
        targetPane.show(dialog);
        animateIn(dialog);
    }

    private void showScrollableModal(JMetroModalPane targetPane,
                                     Node content,
                                     ModalConfig config) {
        showStandardModal(targetPane, content, config);
    }

    private HBox createHeader(ModalConfig config, JMetroModalPane targetPane) {
        HBox header = new HBox(10);
        header.getStyleClass().add("kubata-modal-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));

        Node icon = createHeaderIcon(config);
        if (icon != null) {
            header.getChildren().add(icon);
        }

        VBox titles = new VBox(2);
        titles.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titles, Priority.ALWAYS);

        if (!config.title.isBlank()) {
            Label titleLabel = createTitleLabel(config.title);
            titleLabel.getStyleClass().add("kubata-modal-title");
            titles.getChildren().add(titleLabel);
        }

        if (!config.subtitle.isBlank()) {
            Label subtitle = new Label(config.subtitle);
            subtitle.getStyleClass().add("kubata-modal-subtitle");
            subtitle.setWrapText(true);
            titles.getChildren().add(subtitle);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button(
                "",
                IconUtils.icon(Feather.X, 15)
        );
        closeButton.setAccessibleText("Fechar");
        closeButton.getStyleClass().addAll("button-icon", "flat", "kubata-modal-close");
        closeButton.setOnAction(e -> {
            targetPane.hide();
            resetModalPersistence();
        });

        header.getChildren().addAll(titles, spacer, closeButton);
        return header;
    }

    private Node createHeaderIcon(ModalConfig config) {
        Feather icon = config.headerIcon;
        if (icon == null) {
            icon = switch (config.tone) {
                case INFO -> Feather.INFO;
                case SUCCESS -> Feather.CHECK_CIRCLE;
                case WARNING -> Feather.ALERT_TRIANGLE;
                case DANGER -> Feather.ALERT_OCTAGON;
                default -> Feather.LAYERS;
            };
        }

        String toneClass = switch (config.tone) {
            case INFO -> "kubata-modal-icon-info";
            case SUCCESS -> "kubata-modal-icon-success";
            case WARNING -> "kubata-modal-icon-warning";
            case DANGER -> "kubata-modal-icon-danger";
            default -> "kubata-modal-icon-default";
        };

        Label wrapper = new Label("", IconUtils.icon(icon, 15));
        wrapper.getStyleClass().addAll("kubata-modal-icon", toneClass);
        return wrapper;
    }

    private void prepareContentForDialog(Node content) {
        if (content instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
            HBox.setHgrow(region, Priority.ALWAYS);
        }
    }

    private void configureInternalDialogSize(InternalModalBox dialog,
                                             ModalConfig config) {
        if (config.width > 0 && config.height > 0) {
            dialog.setPrefSize(config.width, config.height);
        }

        if (config.minWidth > 0) {
            dialog.setMinWidth(config.minWidth);
        }
        if (config.minHeight > 0) {
            dialog.setMinHeight(config.minHeight);
        }

        if (config.maxWidth > 0) {
            dialog.setMaxWidth(config.maxWidth);
        } else if (!config.resizable) {
            dialog.setMaxWidth(config.width > 0 ? config.width : Region.USE_PREF_SIZE);
        } else {
            dialog.setMaxWidth(Region.USE_PREF_SIZE);
        }

        if (config.maxHeight > 0) {
            dialog.setMaxHeight(config.maxHeight);
        } else if (!config.resizable) {
            dialog.setMaxHeight(config.height > 0 ? config.height : Region.USE_PREF_SIZE);
        } else {
            dialog.setMaxHeight(Region.USE_PREF_SIZE);
        }
    }

    private Label createTitleLabel(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(TITLE_CLASS);
        return titleLabel;
    }

    private HBox createConfirmationButtons(ModalConfig config,
                                            JMetroModalPane targetPane) {
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button(
                config.cancelText,
                IconUtils.icon(Feather.X, 12)
        );
        cancelButton.getStyleClass().add(config.cancelStyleClass);
        cancelButton.setOnAction(event -> {
            config.onCancel.run();
            targetPane.hide();
            resetModalPersistence();
        });

        Button confirmButton = new Button(
                config.confirmText,
                IconUtils.icon(
                        config.tone == ModalTone.DANGER
                                ? Feather.TRASH_2
                                : Feather.CHECK,
                        12
                )
        );
        confirmButton.getStyleClass().add(config.confirmStyleClass);
        confirmButton.setDefaultButton(true);
        confirmButton.setOnAction(event -> {
            targetPane.hide();
            resetModalPersistence();
            try {
                config.onConfirm.run();
            } catch (Exception e) {
                Platform.runLater(() ->
                        alert(
                                "Erro inesperado",
                                e.getMessage(),
                                "error",
                                e
                        )
                );
            }
        });

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        return buttonBox;
    }

    private void resetModalPersistence() {
        modalPane.setPersistent(persistent);
        modalPaneTop.setPersistent(persistent);
        modalPaneTopmost.setPersistent(persistent);
    }

    // ---------------------------------------------------------------------
    // Convenience API
    // ---------------------------------------------------------------------

    public void showModalSimple(Node content, String title) {
        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .icon(Feather.LAYERS)
        );
    }

    public void showScrollable(Node content, String title) {
        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .scrollable(true)
                        .icon(Feather.FILE_TEXT)
        );
    }

    public void showConfirmModal(Node content,
                                 String title,
                                 Runnable onConfirm,
                                 Runnable onCancel) {
        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .icon(Feather.HELP_CIRCLE)
                        .withConfirmButtons("Confirmar", "Cancelar")
                        .onConfirm(onConfirm)
                        .onCancel(onCancel)
        );
    }

    public void showConfirmModal(Node content,
                                 String title,
                                 Runnable onConfirm,
                                 Runnable onCancel,
                                 ModalConfig baseConfig) {
        ModalConfig config = baseConfig == null ? new ModalConfig() : baseConfig;
        showModal(
                content,
                config
                        .title(title)
                        .withConfirmButtons("Confirmar", "Cancelar")
                        .onConfirm(onConfirm)
                        .onCancel(onCancel)
        );
    }

    public void showConfirm(String title,
                            String message,
                            Runnable onConfirm) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(4, 0, 4, 0));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("kubata-modal-message");

        content.getChildren().add(messageLabel);

        showConfirmModal(content, title, onConfirm, null);
    }

    public void info(String title, String message) {
        showMessageModal(title, message, ModalTone.INFO, Feather.INFO);
    }

    public void success(String title, String message) {
        showMessageModal(title, message, ModalTone.SUCCESS, Feather.CHECK_CIRCLE);
    }

    public void warning(String title, String message) {
        showMessageModal(title, message, ModalTone.WARNING, Feather.ALERT_TRIANGLE);
    }

    public void danger(String title, String message) {
        showMessageModal(title, message, ModalTone.DANGER, Feather.ALERT_OCTAGON);
    }

    private void showMessageModal(String title,
                                  String message,
                                  ModalTone tone,
                                  Feather icon) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 4, 0));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(620);
        messageLabel.getStyleClass().add("kubata-modal-message");

        content.getChildren().add(messageLabel);

        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .icon(icon)
                        .tone(tone)
                        .withConfirmButtons("OK", "")
                        .confirmStyle("button-primary")
                        .cancelStyle("button-outlined")
        );
    }

    public void showErrorModal(String title,
                               String message,
                               Throwable exception) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 4, 0));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("kubata-modal-message");
        content.getChildren().add(messageLabel);

        if (exception != null) {
            TitledPane details = new TitledPane();
            details.setText("Ver detalhes técnicos");
            details.setExpanded(false);

            TextArea stackTrace = new TextArea(getStackTrace(exception));
            stackTrace.setEditable(false);
            stackTrace.setWrapText(false);
            stackTrace.setPrefRowCount(8);
            stackTrace.setPrefColumnCount(70);
            stackTrace.getStyleClass().add("kubata-modal-stacktrace");

            Button copy = new Button(
                    "Copiar detalhes",
                    IconUtils.icon(Feather.COPY, 12)
            );
            copy.getStyleClass().add("button-outlined");
            copy.setOnAction(e -> {
                javafx.scene.input.ClipboardContent clipboard =
                        new javafx.scene.input.ClipboardContent();
                clipboard.putString(stackTrace.getText());
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboard);
            });

            VBox detailsBox = new VBox(8, stackTrace, copy);
            details.setContent(detailsBox);
            content.getChildren().add(details);
        }

        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .icon(Feather.X_CIRCLE)
                        .tone(ModalTone.DANGER)
                        .withConfirmButtons("OK", "Fechar")
                        .confirmStyle("button-danger")
        );
    }

    public void showErrorModal(String title, String message) {
        showErrorModal(title, message, null);
    }

    public void hideModal() {
        modalPane.hide();
        modalPaneTop.hide();
        modalPaneTopmost.hide();
        currentLoadingModal = null;
        resetModalPersistence();
    }

    public void alert(String title,
                      String message,
                      String type,
                      Throwable exception) {
        ModalTone tone = switch (type == null ? "" : type.toLowerCase()) {
            case "success", "ok" -> ModalTone.SUCCESS;
            case "info" -> ModalTone.INFO;
            case "warning", "warn" -> ModalTone.WARNING;
            case "error", "danger", "critical" -> ModalTone.DANGER;
            default -> ModalTone.DEFAULT;
        };

        Feather icon = switch (tone) {
            case SUCCESS -> Feather.CHECK_CIRCLE;
            case INFO -> Feather.INFO;
            case WARNING -> Feather.ALERT_TRIANGLE;
            case DANGER -> Feather.X_CIRCLE;
            default -> Feather.LAYERS;
        };

        if (exception != null) {
            showErrorModal(title, message, exception);
            return;
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(4, 0, 4, 0));

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(640);
        messageLabel.getStyleClass().add("kubata-modal-message");
        content.getChildren().add(messageLabel);

        showModal(
                content,
                new ModalConfig()
                        .title(title)
                        .icon(icon)
                        .tone(tone)
                        .withConfirmButtons("OK", "Fechar")
                        .confirmStyle(tone == ModalTone.DANGER
                                ? "button-danger"
                                : "button-primary")
        );
    }

    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private void animateIn(Node node) {
        node.setOpacity(0);
        node.setScaleX(0.98);
        node.setScaleY(0.98);

        FadeTransition fade = new FadeTransition(Duration.millis(150), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1);
        scale.setToY(1);

        fade.play();
        scale.play();
    }

    /**
     * Overlay central responsável por bloquear a aplicação subjacente.
     */
    private static class JMetroModalPane extends StackPane {
        private final BooleanProperty display = new SimpleBooleanProperty(false);
        private boolean persistent = false;
        private boolean overlayDismissEnabled = true;

        JMetroModalPane() {
            setVisible(false);
            setManaged(false);
            setAlignment(Pos.CENTER);
            getStyleClass().add("kubata-modal-overlay");

            setOnMouseClicked(e -> {
                if (overlayDismissEnabled && !persistent && e.getTarget() == this) {
                    hide();
                }
            });
        }

        void setPersistent(boolean persistent) {
            this.persistent = persistent;
        }

        void configureOverlayDismiss(boolean enabled) {
            this.overlayDismissEnabled = enabled;
        }

        void show(Node node) {
            getChildren().clear();
            getChildren().add(node);
            setVisible(true);
            setManaged(true);
            display.set(true);
            toFront();
        }

        void hide() {
            setVisible(false);
            setManaged(false);
            getChildren().clear();
            display.set(false);
        }

        BooleanProperty displayProperty() {
            return display;
        }
    }

    /**
     * Contentor do diálogo com suporte a ESC e focus inicial.
     */
    private static class InternalModalBox extends StackPane {
        private final JMetroModalPane parent;
        private final VBox container = new VBox();

        InternalModalBox(JMetroModalPane parent,
                         boolean closeOnEscape,
                         boolean focusOnShow) {
            this.parent = parent;
            getChildren().add(container);
            getStyleClass().addAll(DEFAULT_DIALOG_CLASS, "kubata-modal-dialog");

            if (closeOnEscape) {
                addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        parent.hide();
                        event.consume();
                    }
                });
            }

            if (focusOnShow) {
                parent.displayProperty().addListener((obs, oldValue, visible) -> {
                    if (visible) {
                        Platform.runLater(this::requestFocus);
                    }
                });
            }

            setFocusTraversable(true);
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
        }

        void addContent(Node content) {
            container.getChildren().add(content);
            VBox.setVgrow(content, Priority.ALWAYS);
        }
    }
}
