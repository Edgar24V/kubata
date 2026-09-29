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
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    /**
     * Pilha de modais activos. Cada modal recebe o seu próprio overlay,
     * permitindo que um modal filho seja aberto sem destruir o modal pai.
     */
    private final Deque<ModalFrame> modalStack = new ArrayDeque<>();

    /**
     * Overlays actualmente criados. São removidos da raiz quando o modal
     * correspondente é fechado, evitando acumulação de nós na Scene.
     */
    private final List<JMetroModalPane> modalPanes = new ArrayList<>();
    private final FlowPane minimizedDock = new FlowPane(Orientation.HORIZONTAL, 8, 8);

    private StackPane attachedRoot;
    private boolean persistent = false;
    private InternalModalBox currentLoadingModal;
    private JMetroModalPane currentLoadingPane;

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
        private boolean showWindowControls = true;
        private boolean minimizable = true;
        private boolean maximizable = true;

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

        public ModalConfig singleButton(String text) {
            this.showConfirmButtons = true;
            this.confirmText = text == null ? "OK" : text;
            this.cancelText = "";
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

        public ModalConfig windowControls(boolean enabled) {
            this.showWindowControls = enabled;
            return this;
        }

        public ModalConfig minimizable(boolean enabled) {
            this.minimizable = enabled;
            return this;
        }

        public ModalConfig maximizable(boolean enabled) {
            this.maximizable = enabled;
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
        // Os overlays são criados sob demanda para suportar nesting ilimitado.
    }

    /**
     * Liga os overlays ao StackPane raiz da aplicação.
     */
    public void setRoot(StackPane stackPane) {
        if (stackPane == null) {
            return;
        }

        if (attachedRoot != null && attachedRoot != stackPane) {
            attachedRoot.getChildren().removeAll(modalPanes);
        }

        attachedRoot = stackPane;

        if (!stackPane.getChildren().contains(minimizedDock)) {
            StackPane.setAlignment(minimizedDock, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(minimizedDock, new Insets(0, 18, 18, 18));
            minimizedDock.getStyleClass().add("kubata-modal-minimized-dock");
            stackPane.getChildren().add(minimizedDock);
        }

        for (JMetroModalPane pane : modalPanes) {
            if (!stackPane.getChildren().contains(pane)) {
                stackPane.getChildren().add(pane);
            }
        }

        updateMinimizedDock();
        bringTopModalToFront();
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
        for (JMetroModalPane pane : modalPanes) {
            pane.setPersistent(persistent);
        }
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
        JMetroModalPane targetPane = createModalPane(modalType);
        targetPane.setPersistent(true);

        VBox loadingContent = createLoadingContent(title, message, task);
        targetPane.setCloseAction(() -> closeModalPane(targetPane));
        InternalModalBox loadingModal = new InternalModalBox(targetPane, true, true);
        loadingModal.setPrefSize(410, 230);
        loadingModal.addContent(loadingContent);

        currentLoadingModal = loadingModal;
        currentLoadingPane = targetPane;
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

        Thread worker = new Thread(task, "kubata-modal-task");
        worker.setDaemon(true);
        worker.start();
    }

    private void closeLoading(JMetroModalPane targetPane) {
        Platform.runLater(() -> {
            closeModalPane(targetPane);
            currentLoadingModal = null;
            currentLoadingPane = null;
            resetModalPersistence();
        });
    }

    public void hideLoadingModal() {
        if (currentLoadingPane != null) {
            closeModalPane(currentLoadingPane);
        }
        currentLoadingModal = null;
        currentLoadingPane = null;
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
        JMetroModalPane targetPane = createModalPane(safeConfig.modalType);
        targetPane.setPersistent(persistent);

        if (safeConfig.scrollable) {
            showScrollableModal(targetPane, content, safeConfig);
        } else {
            showStandardModal(targetPane, content, safeConfig);
        }
    }

    /**
     * Cria sempre uma nova camada para o modal actual.
     * O modal anterior permanece visível por baixo, formando uma verdadeira
     * hierarquia pai → filho.
     */
    private JMetroModalPane createModalPane(ModalType type) {
        JMetroModalPane pane = new JMetroModalPane();

        String layer = type == null
                ? "default"
                : type.name().toLowerCase();

        pane.setId("modalPane-" + (modalPanes.size() + 1));
        pane.getStyleClass().add("kubata-modal-layer-" + layer);
        pane.setPersistent(persistent);

        modalPanes.add(pane);
        modalStack.push(new ModalFrame(
                pane,
                type == null ? ModalType.DEFAULT : type
        ));

        if (attachedRoot != null && !attachedRoot.getChildren().contains(pane)) {
            attachedRoot.getChildren().add(pane);
        }

        return pane;
    }

    /**
     * Fecha apenas a camada indicada e revela automaticamente o modal pai.
     */
    private void closeModalPane(JMetroModalPane pane) {
        if (pane == null) {
            return;
        }

        ModalFrame frame = modalStack.stream()
                .filter(item -> item.pane() == pane)
                .findFirst()
                .orElse(null);

        pane.hide();

        if (frame != null) {
            modalStack.remove(frame);
        }

        modalPanes.remove(pane);

        if (frame != null) {
            removeMinimizedFrame(frame);
        }

        if (attachedRoot != null) {
            attachedRoot.getChildren().remove(pane);
        }

        updateMinimizedDock();
        bringTopModalToFront();
        resetModalPersistence();
    }

    private void bringTopModalToFront() {
        ModalFrame top = modalStack.stream()
                .filter(frame -> !frame.minimized)
                .findFirst()
                .orElse(null);

        if (top != null) {
            top.pane.setMouseTransparent(false);
            top.pane.setVisible(true);
            top.pane.setManaged(true);
            top.pane.toFront();
        }
    }

    /**
     * Permite inspeccionar a profundidade actual para integrações e testes.
     */
    public int getModalDepth() {
        return modalStack.size();
    }

    public boolean hasOpenModal() {
        return !modalStack.isEmpty();
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

        targetPane.setCloseAction(() -> closeModalPane(targetPane));

        InternalModalBox dialog = new InternalModalBox(
                targetPane,
                config.closeOnEscape,
                true
        );
        dialog.setOnMouseClicked(event -> event.consume());

        StackPane.setMargin(dialog, DEFAULT_MARGIN);
        configureInternalDialogSize(dialog, config);
        dialog.addContent(dialogContent);

        ModalFrame frame = findFrame(targetPane);
        if (frame != null) {
            frame.dialog = dialog;
            frame.config = config;
            frame.title = config.title == null || config.title.isBlank()
                    ? "Kubata"
                    : config.title;
            frame.normalWidth = dialog.getPrefWidth();
            frame.normalHeight = dialog.getPrefHeight();
        }

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

        header.getChildren().addAll(titles, spacer);

        if (config.showWindowControls) {
            if (config.minimizable) {
                Button minimizeButton = createWindowControl(
                        Feather.MINUS,
                        "Minimizar",
                        "kubata-modal-window-control",
                        () -> minimizeModal(targetPane)
                );
                header.getChildren().add(minimizeButton);
            }

            if (config.maximizable) {
                Button maximizeButton = createWindowControl(
                        Feather.MAXIMIZE_2,
                        "Maximizar",
                        "kubata-modal-window-control",
                        () -> toggleMaximizeModal(targetPane)
                );
                header.getChildren().add(maximizeButton);

                ModalFrame frame = findFrame(targetPane);
                if (frame != null) {
                    frame.maximizeButton = maximizeButton;
                }
            }

            Button closeButton = createWindowControl(
                    Feather.X,
                    "Fechar",
                    "kubata-modal-window-control-danger",
                    () -> closeModalPane(targetPane)
            );
            header.getChildren().add(closeButton);
        }

        header.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && config.showWindowControls
                    && config.maximizable) {
                toggleMaximizeModal(targetPane);
                event.consume();
            }
        });

        return header;
    }

    private Button createWindowControl(Feather icon,
                                          String accessibleText,
                                          String styleClass,
                                          Runnable action) {
        Button button = new Button("", IconUtils.icon(icon, 13));
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll("button-icon", "flat", styleClass);
        button.setOnAction(event -> {
            action.run();
            event.consume();
        });
        return button;
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

    private ModalFrame findFrame(JMetroModalPane pane) {
        return modalStack.stream()
                .filter(frame -> frame.pane == pane)
                .findFirst()
                .orElse(null);
    }

    private void minimizeModal(JMetroModalPane pane) {
        ModalFrame frame = findFrame(pane);
        if (frame == null || frame.minimized) {
            return;
        }

        frame.minimized = true;
        pane.setVisible(false);
        pane.setManaged(false);
        pane.setMouseTransparent(true);

        createMinimizedDockItem(frame);
        updateMinimizedDock();
        bringTopModalToFront();
    }

    private void restoreModal(JMetroModalPane pane) {
        ModalFrame frame = findFrame(pane);
        if (frame == null) {
            return;
        }

        frame.minimized = false;
        pane.setMouseTransparent(false);
        pane.setVisible(true);
        pane.setManaged(true);

        removeMinimizedFrame(frame);
        updateMinimizedDock();
        pane.toFront();

        Platform.runLater(() -> {
            if (frame.dialog != null) {
                frame.dialog.requestFocus();
            }
        });
    }

    private void toggleMaximizeModal(JMetroModalPane pane) {
        ModalFrame frame = findFrame(pane);
        if (frame == null || frame.dialog == null) {
            return;
        }

        if (frame.maximized) {
            restoreMaximizedSize(frame);
        } else {
            maximizeModal(frame);
        }
    }

    private void maximizeModal(ModalFrame frame) {
        if (attachedRoot == null) {
            return;
        }

        double width = Math.max(520, attachedRoot.getLayoutBounds().getWidth() - 48);
        double height = Math.max(320, attachedRoot.getLayoutBounds().getHeight() - 48);

        if (frame.dialog.getWidth() > 0) {
            frame.normalWidth = frame.dialog.getWidth();
        } else if (frame.config != null && frame.config.width > 0) {
            frame.normalWidth = frame.config.width;
        }

        if (frame.dialog.getHeight() > 0) {
            frame.normalHeight = frame.dialog.getHeight();
        } else if (frame.config != null && frame.config.height > 0) {
            frame.normalHeight = frame.config.height;
        }

        frame.dialog.getStyleClass().add("kubata-modal-dialog-maximized");
        frame.dialog.setMinWidth(420);
        frame.dialog.setMinHeight(260);
        frame.dialog.setMaxWidth(width);
        frame.dialog.setMaxHeight(height);
        frame.dialog.setPrefWidth(width);
        frame.dialog.setPrefHeight(height);

        frame.maximized = true;
        updateMaximizeButton(frame);
    }

    private void restoreMaximizedSize(ModalFrame frame) {
        if (frame.dialog == null) {
            return;
        }

        frame.dialog.getStyleClass().remove("kubata-modal-dialog-maximized");

        if (frame.config != null) {
            configureInternalDialogSize(frame.dialog, frame.config);
        }

        if (frame.normalWidth > 0) {
            frame.dialog.setPrefWidth(frame.normalWidth);
        }

        if (frame.normalHeight > 0) {
            frame.dialog.setPrefHeight(frame.normalHeight);
        }

        frame.maximized = false;
        updateMaximizeButton(frame);
    }

    private void updateMaximizeButton(ModalFrame frame) {
        if (frame.maximizeButton == null) {
            return;
        }

        frame.maximizeButton.setGraphic(
                IconUtils.icon(
                        frame.maximized ? Feather.MINIMIZE_2 : Feather.MAXIMIZE_2,
                        13
                )
        );
        frame.maximizeButton.setAccessibleText(
                frame.maximized ? "Restaurar tamanho" : "Maximizar"
        );
    }

    private void createMinimizedDockItem(ModalFrame frame) {
        if (frame.dockItem != null) {
            return;
        }

        HBox item = new HBox(4);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("kubata-modal-minimized-item");

        Button restore = new Button(
                frame.title,
                IconUtils.icon(frame.maximized ? Feather.MINIMIZE_2 : Feather.MAXIMIZE_2, 11)
        );
        restore.setTooltip(new Tooltip("Restaurar "" + frame.title + """));
        restore.setAccessibleText("Restaurar " + frame.title);
        restore.getStyleClass().addAll("button-icon", "flat", "kubata-modal-minimized-restore");
        restore.setOnAction(event -> restoreModal(frame.pane));

        Button close = new Button(
                "",
                IconUtils.icon(Feather.X, 11)
        );
        close.setTooltip(new Tooltip("Fechar "" + frame.title + """));
        close.setAccessibleText("Fechar " + frame.title);
        close.getStyleClass().addAll("button-icon", "flat", "kubata-modal-minimized-close");
        close.setOnAction(event -> closeModalPane(frame.pane));

        HBox.setHgrow(restore, Priority.ALWAYS);
        item.getChildren().addAll(restore, close);

        frame.dockItem = item;
        minimizedDock.getChildren().add(item);
    }

    private void removeMinimizedFrame(ModalFrame frame) {
        if (frame != null && frame.dockItem != null) {
            minimizedDock.getChildren().remove(frame.dockItem);
            frame.dockItem = null;
        }
    }

    private void updateMinimizedDock() {
        minimizedDock.setManaged(!minimizedDock.getChildren().isEmpty());
        minimizedDock.setVisible(!minimizedDock.getChildren().isEmpty());
    }

    private HBox createConfirmationButtons(ModalConfig config,
                                            JMetroModalPane targetPane) {
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

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
            closeModalPane(targetPane);
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

        if (config.cancelText != null && !config.cancelText.isBlank()) {
            Button cancelButton = new Button(
                    config.cancelText,
                    IconUtils.icon(Feather.X, 12)
            );
            cancelButton.getStyleClass().add(config.cancelStyleClass);
            cancelButton.setOnAction(event -> {
                config.onCancel.run();
                closeModalPane(targetPane);
            });
            buttonBox.getChildren().add(cancelButton);
        }

        buttonBox.getChildren().add(confirmButton);
        return buttonBox;
    }

    private void resetModalPersistence() {
        for (JMetroModalPane pane : modalPanes) {
            pane.setPersistent(persistent);
        }
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
                        .singleButton("OK")
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

    /**
     * Fecha somente o modal actualmente no topo.
     * Os modais pai permanecem intactos.
     */
    public void hideModal() {
        ModalFrame top = modalStack.stream()
                .filter(frame -> !frame.minimized)
                .findFirst()
                .orElse(modalStack.peek());

        if (top != null) {
            closeModalPane(top.pane);
        }

        if (currentLoadingPane == null || !modalStack.stream()
                .anyMatch(frame -> frame.pane == currentLoadingPane)) {
            currentLoadingModal = null;
            currentLoadingPane = null;
        }

        resetModalPersistence();
    }

    /**
     * Fecha toda a hierarquia de modais. Útil para logout, troca de contexto
     * ou encerramento da aplicação.
     */
    public void hideAllModals() {
        for (ModalFrame frame : new ArrayList<>(modalStack)) {
            frame.pane.hide();
            if (attachedRoot != null) {
                attachedRoot.getChildren().remove(frame.pane);
            }
        }

        modalStack.clear();
        modalPanes.clear();
        currentLoadingModal = null;
        currentLoadingPane = null;
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
                        .singleButton("OK")
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
                    if (closeAction != null) {
                        closeAction.run();
                    } else {
                        hide();
                    }
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

        private void closeFromKeyboard() {
            // Fecho delegado ao ModalManager através do callback configurado.
            if (closeAction != null) {
                closeAction.run();
            }
        }

        private Runnable closeAction;

        void setCloseAction(Runnable closeAction) {
            this.closeAction = closeAction;
        }

        BooleanProperty displayProperty() {
            return display;
        }
    }

    private static class ModalFrame {
        private final JMetroModalPane pane;
        private final ModalType type;

        private InternalModalBox dialog;
        private ModalConfig config;
        private String title = "Kubata";
        private double normalWidth = -1;
        private double normalHeight = -1;
        private boolean minimized;
        private boolean maximized;
        private Button maximizeButton;
        private HBox dockItem;

        private ModalFrame(JMetroModalPane pane, ModalType type) {
            this.pane = pane;
            this.type = type;
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
                        parent.closeFromKeyboard();
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
