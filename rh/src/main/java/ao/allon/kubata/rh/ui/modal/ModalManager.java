package ao.allon.kubata.rh.ui.modal;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.springframework.stereotype.Service;

/**
 * Classe para gerenciar modais utilizando apenas JMetro e componentes nativos JavaFX.
 * O AtlantaFX foi removido deste módulo.
 */
@Service
public class ModalManager {

    // Constantes para configurações padrão - Otimizado para JMetro
    private static final String DEFAULT_DIALOG_STYLE =
            "-fx-background-color: white;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-border-radius: 8px;" +
                    "-fx-border-color: #dee2e6;" +
                    "-fx-border-width: 1px;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20.0, 0.2, 0, 5);";

    private static final String TITLE_STYLE = "-fx-font-size: 14pt; -fx-font-weight: bold; -fx-text-fill: #212529;";
    private static final Insets DEFAULT_MARGIN = new Insets(20);
    private static final Insets DEFAULT_PADDING = new Insets(20);

    // Modal panes customizados (Substitutos do ModalPane do AtlantaFX)
    private final JMetroModalPane modalPane = new JMetroModalPane();
    private final JMetroModalPane modalPaneTop = new JMetroModalPane();
    private final JMetroModalPane modalPaneTopmost = new JMetroModalPane();

    // Controle de persistência global
    private boolean persistent = false;

    // Modal de loading atual
    private Node currentLoadingModal;

    /**
     * Enum para definir tipos de modal
     */
    public enum ModalType {
        DEFAULT, TOP, TOPMOST
    }

    /**
     * Configurações para criação de modais
     */
    public static class ModalConfig {
        private String title = "";
        private boolean scrollable = false;
        private boolean resizable = true;
        private int width = (int) Region.USE_PREF_SIZE;
        private int height = (int) Region.USE_PREF_SIZE;
        private int minWidth = -1;
        private int minHeight = -1;
        private int maxWidth = -1;
        private int maxHeight = -1;
        private boolean showConfirmButtons = false;
        private String confirmText = "Confirmar";
        private String cancelText = "Cancelar";
        private Runnable onConfirm = () -> {};
        private Runnable onCancel = () -> {};
        private ModalType modalType = ModalType.DEFAULT;

        // Builder pattern methods
        public ModalConfig title(String title) { this.title = title; return this; }
        public ModalConfig scrollable(boolean scrollable) { this.scrollable = scrollable; return this; }
        public ModalConfig resizable(boolean resizable) { this.resizable = resizable; return this; }
        public ModalConfig autoSize() {
            this.width = (int) Region.USE_PREF_SIZE;
            this.height = (int) Region.USE_PREF_SIZE;
            return this;
        }
        public ModalConfig size(int width, int height) { this.width = width; this.height = height; return this; }
        public ModalConfig minSize(int width, int height) { this.minWidth = width; this.minHeight = height; return this; }
        public ModalConfig maxSize(int width, int height) { this.maxWidth = width; this.maxHeight = height; return this; }
        public ModalConfig withConfirmButtons(String confirmText, String cancelText) {
            this.showConfirmButtons = true;
            this.confirmText = confirmText;
            this.cancelText = cancelText;
            return this;
        }
        public ModalConfig withConfirmButtons(String confirmText) {
            this.showConfirmButtons = true;
            this.confirmText = confirmText;
            return this;
        }
        public ModalConfig onConfirm(Runnable onConfirm) { this.onConfirm = onConfirm; return this; }
        public ModalConfig onCancel(Runnable onCancel) { this.onCancel = onCancel; return this; }
        public ModalType getModalType() { return modalType; }
        public ModalConfig modalType(ModalType type) { this.modalType = type; return this; }
    }

    public ModalManager() {
        setupModalPanes();
    }

    private void setupModalPanes() {
        modalPane.setId("modalPane");
        modalPaneTop.setId("modalPaneTop");
        modalPaneTopmost.setId("modalPaneTopmost");
    }

    public void setRoot(StackPane stackPane) {
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
    }

    // ============ MÉTODOS DE CARREGAMENTO (TASK) ============

    public void showLoadingModal(Task<?> task, String title, String message) {
        showLoadingModal(task, title, message, ModalType.DEFAULT);
    }

    public void showLoadingModal(Task<?> task, String title, String message, ModalType modalType) {
        JMetroModalPane targetPane = getModalPane(modalType);
        targetPane.setPersistent(true);

        VBox loadingContent = createLoadingContent(title, message, task);
        InternalModalBox loadingModal = new InternalModalBox(targetPane);
        loadingModal.setPrefSize(350, 180);
        loadingModal.addContent(loadingContent);

        currentLoadingModal = loadingModal;
        targetPane.show(loadingModal);

        setupTaskHandlers(task, targetPane);
    }

    public void showLoadingModal(String title, String message) {
        showLoadingModal(title, message, ModalType.DEFAULT);
    }

    public void showLoadingModal(String title, String message, ModalType modalType) {
        JMetroModalPane targetPane = getModalPane(modalType);
        targetPane.setPersistent(true);

        VBox loadingContent = createLoadingContent(title, message, null);
        InternalModalBox loadingModal = new InternalModalBox(targetPane);
        loadingModal.setPrefSize(350, 180);
        loadingModal.addContent(loadingContent);

        currentLoadingModal = loadingModal;
        targetPane.show(loadingModal);
    }

    private VBox createLoadingContent(String title, String message, Task<?> task) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setAlignment(Pos.CENTER);

        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setStyle(TITLE_STYLE);
            content.getChildren().add(titleLabel);
        }

        Label messageLabel = new Label(message != null ? message : "A carregar...");
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);
        content.getChildren().add(messageLabel);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(280);

        if (task != null) {
            progressBar.progressProperty().bind(task.progressProperty());
        } else {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }

        content.getChildren().add(progressBar);

        return content;
    }

    private void setupTaskHandlers(Task<?> task, JMetroModalPane targetPane) {
        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                targetPane.hide();
                currentLoadingModal = null;
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                targetPane.hide();
                currentLoadingModal = null;
                Throwable exception = task.getException();
                showErrorModal("Erro", "Ocorreu um erro no processamento: " + exception.getMessage());
            });
        });

        task.setOnCancelled(event -> {
            Platform.runLater(() -> {
                targetPane.hide();
                currentLoadingModal = null;
            });
        });

        new Thread(task).start();
    }

    public void showErrorModal(String title, String message) {
        VBox errorContent = new VBox(15);
        errorContent.setPadding(new Insets(25));
        errorContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setStyle(TITLE_STYLE + " -fx-text-fill: #dc3545;"); // Danger color

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        Button okButton = new Button("OK");
        okButton.setPrefWidth(100);
        okButton.setOnAction(e -> hideModal());

        errorContent.getChildren().addAll(titleLabel, messageLabel, okButton);

        showModal(errorContent, new ModalConfig()
                .modalType(ModalType.DEFAULT));
    }

    public void hideLoadingModal() {
        if (currentLoadingModal != null) {
            modalPane.hide();
            modalPaneTop.hide();
            modalPaneTopmost.hide();
            currentLoadingModal = null;
        }
    }

    // ============ MÉTODOS PRINCIPAIS DE MODAL ============

    public void showModal(Node content, ModalConfig config) {
        JMetroModalPane targetPane = getModalPane(config.modalType);
        targetPane.setPersistent(persistent);

        if (config.scrollable) {
            showScrollableModal(targetPane, content, config);
        } else {
            showStandardModal(targetPane, content, config);
        }
    }

    private JMetroModalPane getModalPane(ModalType type) {
        return switch (type) {
            case TOP -> modalPaneTop;
            case TOPMOST -> modalPaneTopmost;
            default -> modalPane;
        };
    }

    private void showStandardModal(JMetroModalPane targetPane, Node content, ModalConfig config) {
        VBox dialogContent = new VBox(20);
        dialogContent.setPadding(DEFAULT_PADDING);

        if (!config.title.isEmpty()) {
            HBox header = createHeader(config.title, targetPane);
            dialogContent.getChildren().add(header);
        }

        prepareContentForDialog(content);
        dialogContent.getChildren().add(content);

        if (config.showConfirmButtons) {
            HBox buttons = createConfirmationButtons(config.confirmText, config.cancelText,
                    config.onConfirm, config.onCancel);
            dialogContent.getChildren().add(buttons);
        }

        InternalModalBox dialog = new InternalModalBox(targetPane);
        StackPane.setMargin(dialog, DEFAULT_MARGIN);
        configureInternalDialogSize(dialog, config);
        dialog.addContent(dialogContent);
        targetPane.show(dialog);
    }

    private void showScrollableModal(JMetroModalPane targetPane, Node content, ModalConfig config) {
        VBox dialogContent = new VBox();

        if (!config.title.isEmpty()) {
            HBox header = createHeader(config.title, targetPane);
            dialogContent.getChildren().add(header);
        }

        VBox contentWrapper = new VBox(20);
        contentWrapper.setPadding(DEFAULT_PADDING);

        prepareContentForDialog(content);
        contentWrapper.getChildren().add(content);

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        dialogContent.getChildren().add(scrollPane);

        if (config.showConfirmButtons) {
            HBox buttons = createConfirmationButtons(config.confirmText, config.cancelText,
                    config.onConfirm, config.onCancel);
            dialogContent.getChildren().add(buttons);
        }

        InternalModalBox dialog = new InternalModalBox(targetPane);
        StackPane.setMargin(dialog, DEFAULT_MARGIN);
        configureInternalDialogSize(dialog, config);
        dialog.addContent(dialogContent);
        targetPane.show(dialog);
    }

    private HBox createHeader(String title, JMetroModalPane targetPane) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        header.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 0 0 1px 0;");

        Label titleLabel = createTitleLabel(title);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeButton = new Button("✕");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-weight: bold;");
        closeButton.setOnAction(e -> targetPane.hide());

        header.getChildren().addAll(titleLabel, spacer, closeButton);
        return header;
    }

    private void prepareContentForDialog(Node content) {
        if (content instanceof Region region) {
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
            HBox.setHgrow(region, Priority.ALWAYS);
        }
    }

    private void configureInternalDialogSize(InternalModalBox dialog, ModalConfig config) {
        if (config.width > 0 && config.height > 0) {
            dialog.setPrefSize(config.width, config.height);
        }

        if (config.minWidth > 0 && config.minHeight > 0) {
            dialog.setMinSize(config.minWidth, config.minHeight);
        }

        if (!config.resizable) {
            dialog.setMaxSize(config.width > 0 ? config.width : Region.USE_PREF_SIZE,
                              config.height > 0 ? config.height : Region.USE_PREF_SIZE);
        } else if (config.maxWidth > 0 && config.maxHeight > 0) {
            dialog.setMaxSize(config.maxWidth, config.maxHeight);
        } else {
            dialog.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
    }

    private Label createTitleLabel(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(TITLE_STYLE);
        return titleLabel;
    }

    private HBox createConfirmationButtons(String confirmText, String cancelText,
                                           Runnable onConfirm, Runnable onCancel) {
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);
        buttonBox.setSpacing(15);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button cancelButton = new Button(cancelText);
        cancelButton.setOnAction(event -> {
            if (onCancel != null) onCancel.run();
            hideModal();
        });

        Button confirmButton = new Button(confirmText);
        confirmButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        confirmButton.setOnAction(event -> {
            if (onConfirm != null) onConfirm.run();
            hideModal();
        });

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        return buttonBox;
    }

    public void showModalSimple(Node content, String title) {
        ModalConfig config = new ModalConfig().title(title);
        showModal(content, config);
    }

    public void showScrollable(Node content, String title) {
        ModalConfig config = new ModalConfig().title(title).scrollable(true);
        showModal(content, config);
    }

    public void showConfirmModal(Node content, String title, Runnable onConfirm, Runnable onCancel) {
        ModalConfig config = new ModalConfig()
                .title(title)
                .withConfirmButtons("Confirmar", "Cancelar")
                .onConfirm(onConfirm)
                .onCancel(onCancel);
        showModal(content, config);
    }

    public void info(String title, String message) {
        VBox infoContent = new VBox(15);
        infoContent.setPadding(new Insets(25));
        infoContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setStyle(TITLE_STYLE + " -fx-text-fill: #17a2b8;"); // Info color

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        Button okButton = new Button("OK");
        okButton.setPrefWidth(100);
        okButton.setOnAction(e -> hideModal());

        infoContent.getChildren().addAll(titleLabel, messageLabel, okButton);

        showModal(infoContent, new ModalConfig()
                .modalType(ModalType.DEFAULT));
    }

    public void hideModal() {
        modalPane.hide();
        modalPaneTop.hide();
        modalPaneTopmost.hide();
        currentLoadingModal = null;
    }

    public void alert(String title, String message, String type, Throwable exception) {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(10));
        contentBox.setAlignment(Pos.CENTER_LEFT);
        
        Label lblMessage = new Label(message);
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(500);
        
        if ("error".equalsIgnoreCase(type)) {
            lblMessage.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
        } else if ("warning".equalsIgnoreCase(type)) {
            lblMessage.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffc107;");
        }
        
        contentBox.getChildren().add(lblMessage);

        if (exception != null) {
            VBox errorDetails = new VBox(5);
            errorDetails.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 4;");
            
            Label lblDetails = new Label("Detalhes do Erro:");
            lblDetails.setStyle("-fx-font-size: 0.9em;");
            
            javafx.scene.control.TextArea txtError = new javafx.scene.control.TextArea(getStackTrace(exception));
            txtError.setEditable(false);
            txtError.setWrapText(true);
            txtError.setPrefRowCount(6);
            txtError.setStyle("-fx-font-family: 'Consolas', 'Monospace'; -fx-font-size: 0.9em;");
            VBox.setVgrow(txtError, Priority.ALWAYS);

            errorDetails.getChildren().addAll(lblDetails, txtError);
            contentBox.getChildren().add(errorDetails);
        }
        
        showModal(contentBox, new ModalConfig().title(title).resizable(true).withConfirmButtons("OK"));
    }

    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * ModalPane customizado para JMetro.
     * Atua como um overlay sobre a aplicação.
     */
    private static class JMetroModalPane extends StackPane {
        private final BooleanProperty display = new SimpleBooleanProperty(false);
        private boolean persistent = false;

        public JMetroModalPane() {
            setVisible(false);
            setManaged(false);
            setAlignment(Pos.CENTER);
            setStyle("-fx-background-color: rgba(0,0,0,0.4);"); // Overlay semi-transparente
            
            // Fecha ao clicar no fundo (se não for persistente)
            setOnMouseClicked(e -> {
                if (!persistent && e.getTarget() == this) {
                    hide();
                }
            });
        }

        public void setPersistent(boolean persistent) {
            this.persistent = persistent;
        }

        public void show(Node node) {
            getChildren().clear();
            getChildren().add(node);
            setVisible(true);
            setManaged(true);
            display.set(true);
            toFront();
        }

        public void hide() {
            setVisible(false);
            setManaged(false);
            getChildren().clear();
            display.set(false);
        }

        public BooleanProperty displayProperty() {
            return display;
        }
    }

    /**
     * Modal personalizado interno para JMetro.
     */
    private static class InternalModalBox extends StackPane {
        private final JMetroModalPane parent;
        private final VBox container = new VBox();

        public InternalModalBox(JMetroModalPane parent) {
            this.parent = parent;
            getChildren().add(container);
            setStyle(DEFAULT_DIALOG_STYLE);
            
            addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    parent.hide();
                }
            });

            parent.displayProperty().addListener((obs, old, val) -> {
                if (val) {
                    Platform.runLater(this::requestFocus);
                }
            });
            
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
        }

        public void addContent(Node content) {
            container.getChildren().add(content);
            VBox.setVgrow(content, Priority.ALWAYS);
        }
    }
}
