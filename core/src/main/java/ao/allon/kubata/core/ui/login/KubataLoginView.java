package ao.allon.kubata.core.ui.login;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.MfaRequiredException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.MaintenanceModeService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.core.ui.theme.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Ecrã de autenticação partilhado por todos os módulos Kubata.
 *
 * <p>Toda a apresentação vive em CSS (classes {@code login-*}); este ficheiro
 * constrói o grafo de nós, o comportamento e os textos. O cartão acompanha o
 * tamanho da janela e colapsa a coluna de branding abaixo do ponto de rutura.
 */
public class KubataLoginView {

    private static final Logger log = LoggerFactory.getLogger(KubataLoginView.class);

    private static final String PREF_EMAIL = "rememberedEmail";

    /** Largura abaixo da qual a coluna de branding é dispensada. */
    private static final double COMPACT_WIDTH = 760;

    /** Altura abaixo da qual os textos de apoio são dispensados. */
    private static final double SHORT_HEIGHT = 520;

    private static final DateTimeFormatter SINCE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private final LoginViewConfig config;
    private final LoginViewConfig.Copy copy;
    private final AuthService authService;
    private final MaintenanceModeService maintenanceModeService;
    private final PasswordChangeService passwordChangeService;
    private final Consumer<User> onSuccess;
    private final Preferences preferences;

    private final ExecutorService executor =
            Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "kubata-login");
                thread.setDaemon(true);
                return thread;
            });

    private StackPane rootPane;
    private Node overlayPane;
    private Region backdrop;

    private VBox card;
    private VBox brandingPane;
    private VBox formPane;
    private VBox securityCard;
    private Label brandingDescription;
    private Label buildLabel;
    private Label windowSubtitle;
    private Label formSubtitle;
    private Label footerNote;
    private HBox statusChip;

    private HBox maintenanceBanner;
    private Label maintenanceText;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    private TextField mfaCodeField;
    private VBox mfaBox;
    private Label mfaHintLabel;

    private Label messageLabel;
    private Label emailStateLabel;

    private Button loginButton;
    private ProgressIndicator loading;
    private ToggleButton showPasswordBtn;
    private CheckBox rememberEmail;
    private Hyperlink forgotPasswordLink;
    private Button helpButton;
    private Hyperlink recoveryModeLink;
    private boolean recoveryMode;

    private Button maximizeButton;
    private Rectangle windowClip;

    private double normalStageX;
    private double normalStageY;
    private double normalStageWidth;
    private double normalStageHeight;
    private boolean windowMaximized;

    private double xOffset;
    private double yOffset;

    private boolean compactLayout;
    private boolean shortLayout;

    public KubataLoginView(LoginViewConfig config,
                           AuthService authService,
                           PasswordChangeService passwordChangeService,
                           MaintenanceModeService maintenanceModeService,
                           Consumer<User> onSuccess) {
        this.config = config;
        this.copy = config.copy();
        this.authService = authService;
        this.passwordChangeService = passwordChangeService;
        this.maintenanceModeService = maintenanceModeService;
        this.onSuccess = onSuccess;
        this.preferences = Preferences.userRoot().node(config.preferenceNode());
    }

    /**
     * Dimensão recomendada para a janela de autenticação, relativa ao ecrã,
     * para nunca ocupar a totalidade do monitor.
     */
    public static javafx.geometry.Dimension2D recommendedSceneSize() {
        javafx.geometry.Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double width = clamp(vb.getWidth() * 0.60, 600, 840);
        double height = clamp(vb.getHeight() * 0.66, 420, 560);
        return new javafx.geometry.Dimension2D(width, height);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public Parent createView(Stage stage) {
        StackPane root = new StackPane();
        this.rootPane = root;
        root.getStyleClass().add("login-container");

        backdrop = new Region();
        backdrop.getStyleClass().add("login-backdrop");

        card = createLoginShell(stage);
        bindCardToScene(root);

        root.getChildren().addAll(backdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);

        installRoundedWindow(root);
        installWindowDragging(root, stage);
        installSubmitShortcut(root);

        refreshMaintenanceNotice();
        animateEntrance(card);

        Platform.runLater(this::requestInitialFocus);

        return root;
    }

    /**
     * O cartão acompanha a janela: o preferred deriva do tamanho da cena e o
     * maximum segue o preferred, para que a janela deixe de o congelar no
     * arranque (min e max fixos era o defeito do desenho anterior).
     */
    private void bindCardToScene(Region root) {
        card.setMinSize(480, 336);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        card.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> clamp(root.getWidth() - 48, 520, 980),
                root.widthProperty()));
        card.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> clamp(root.getHeight() - 48, 336, 680),
                root.heightProperty()));

        root.widthProperty().addListener((obs, oldValue, width) ->
                applyBreakpoints(width.doubleValue(), root.getHeight()));
        root.heightProperty().addListener((obs, oldValue, height) ->
                applyBreakpoints(root.getWidth(), height.doubleValue()));

        // A cena ainda não tem dimensões definidas no momento da construção.
        Platform.runLater(() -> applyBreakpoints(root.getWidth(), root.getHeight()));
    }

    private void applyBreakpoints(double sceneWidth, double sceneHeight) {
        boolean compact = sceneWidth > 0 && sceneWidth < COMPACT_WIDTH;
        boolean shortWindow = sceneHeight > 0 && sceneHeight < SHORT_HEIGHT;

        if (compact == compactLayout && shortWindow == shortLayout) {
            return;
        }

        compactLayout = compact;
        shortLayout = shortWindow;

        if (!rootPane.getStyleClass().contains("login-compact") && compact) {
            rootPane.getStyleClass().add("login-compact");
        } else if (!compact) {
            rootPane.getStyleClass().remove("login-compact");
        }

        if (!rootPane.getStyleClass().contains("login-short") && shortWindow) {
            rootPane.getStyleClass().add("login-short");
        } else if (!shortWindow) {
            rootPane.getStyleClass().remove("login-short");
        }

        setCollapsible(brandingPane, !compact);
        setCollapsible(statusChip, !shortWindow);
        setCollapsible(footerNote, !shortWindow);
        setCollapsible(formSubtitle, !shortWindow);
        setCollapsible(windowSubtitle, !shortWindow);
        setCollapsible(buildLabel, !shortWindow);
        setCollapsible(brandingDescription, !shortWindow);
        setCollapsible(securityCard, !compact && !shortWindow);
    }

    private void setCollapsible(Node node, boolean shown) {
        if (node == null) {
            return;
        }
        node.setVisible(shown);
        if (node instanceof Region region) {
            region.setManaged(shown);
        }
    }

    private void requestInitialFocus() {
        if (emailField.getText() == null || emailField.getText().isBlank()) {
            emailField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }

    private void installSubmitShortcut(Region root) {
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER || overlayPane != null) {
                return;
            }
            // Controlos com ativação própria (botões, links) já tratam do ENTER.
            if (event.getTarget() instanceof Button
                    || event.getTarget() instanceof Hyperlink
                    || event.getTarget() instanceof ToggleButton
                    || event.getTarget() instanceof CheckBox) {
                return;
            }
            if (!loginButton.isDisabled()) {
                handleLogin();
            }
        });
    }

    private void installRoundedWindow(StackPane root) {
        windowClip = new Rectangle();
        windowClip.widthProperty().bind(root.widthProperty());
        windowClip.heightProperty().bind(root.heightProperty());
        windowClip.setArcWidth(28);
        windowClip.setArcHeight(28);
        root.setClip(windowClip);
    }

    private VBox createLoginShell(Stage stage) {
        VBox shell = new VBox();
        shell.getStyleClass().add("login-card");

        HBox header = createLoginHeader(stage);

        VBox branding = createBrandingPane();
        this.brandingPane = branding;

        VBox form = createFormPane();
        this.formPane = form;

        HBox body = new HBox();
        body.getStyleClass().add("login-body");
        HBox.setHgrow(form, Priority.ALWAYS);
        body.setMinHeight(0);
        body.getChildren().addAll(branding, form);

        shell.getChildren().addAll(header, body);
        return shell;
    }

    private HBox createLoginHeader(Stage stage) {
        HBox header = new HBox(10);
        header.getStyleClass().add("login-window-header");
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(config.brandingIcon());
        icon.getStyleClass().add("login-window-icon");

        Label title = new Label(config.windowTitle());
        title.getStyleClass().add("login-window-title");

        windowSubtitle = new Label(copy.windowSubtitle());
        windowSubtitle.getStyleClass().add("login-window-subtitle");

        VBox titleBox = new VBox(1, title, windowSubtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        spacer.getStyleClass().add("spacer");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = createWindowBar(stage);

        header.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && isHeaderTarget(event.getTarget())) {
                toggleWindowMaximize(stage);
                event.consume();
            }
        });

        header.getChildren().addAll(icon, titleBox, spacer, controls);
        return header;
    }

    private boolean isHeaderTarget(Object target) {
        return target == null
                || target instanceof HBox
                || target instanceof VBox
                || target instanceof Label
                || target instanceof FontIcon;
    }

    private HBox createWindowBar(Stage stage) {
        HBox bar = new HBox(2);
        bar.getStyleClass().add("login-window-controls");
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPickOnBounds(false);

        Button minimizeButton = createWindowControl(
                Feather.MINUS, "Minimizar", "kubata-modal-window-control",
                () -> stage.setIconified(true));

        maximizeButton = createWindowControl(
                Feather.MAXIMIZE_2, "Maximizar", "kubata-modal-window-control",
                () -> toggleWindowMaximize(stage));

        Button closeButton = createWindowControl(
                Feather.X, "Fechar", "kubata-modal-window-control-danger",
                () -> {
                    hideOverlay();
                    stage.close();
                });

        bar.getChildren().addAll(minimizeButton, maximizeButton, closeButton);
        return bar;
    }

    private Button createWindowControl(Feather icon,
                                       String accessibleText,
                                       String styleClass,
                                       Runnable action) {
        FontIcon glyph = new FontIcon(icon);
        glyph.getStyleClass().add("login-window-control-icon");

        Button button = new Button("", glyph);
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll("button-icon", "flat", styleClass);
        button.setCursor(Cursor.HAND);
        button.setOnAction(event -> {
            action.run();
            event.consume();
        });
        return button;
    }

    private void toggleWindowMaximize(Stage stage) {
        if (windowMaximized) {
            restoreLoginWindow(stage);
        } else {
            maximizeLoginWindow(stage);
        }
    }

    private void maximizeLoginWindow(Stage stage) {
        normalStageX = stage.getX();
        normalStageY = stage.getY();
        normalStageWidth = stage.getWidth();
        normalStageHeight = stage.getHeight();

        javafx.geometry.Rectangle2D bounds = Screen.getScreensForRectangle(
                stage.getX(),
                stage.getY(),
                Math.max(stage.getWidth(), 1),
                Math.max(stage.getHeight(), 1)
        ).stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        windowMaximized = true;
        updateWindowMaximizeButton();

        if (windowClip != null) {
            windowClip.setArcWidth(0);
            windowClip.setArcHeight(0);
        }
    }

    private void restoreLoginWindow(Stage stage) {
        stage.setX(normalStageX);
        stage.setY(normalStageY);
        stage.setWidth(normalStageWidth);
        stage.setHeight(normalStageHeight);

        windowMaximized = false;
        updateWindowMaximizeButton();

        if (windowClip != null) {
            windowClip.setArcWidth(28);
            windowClip.setArcHeight(28);
        }
    }

    private void updateWindowMaximizeButton() {
        if (maximizeButton == null) {
            return;
        }

        String label = windowMaximized ? "Restaurar tamanho" : "Maximizar";
        FontIcon glyph = new FontIcon(
                windowMaximized ? Feather.MINIMIZE_2 : Feather.MAXIMIZE_2);
        glyph.getStyleClass().add("login-window-control-icon");

        maximizeButton.setGraphic(glyph);
        maximizeButton.setAccessibleText(label);
        maximizeButton.setTooltip(new javafx.scene.control.Tooltip(label));
    }

    private void installWindowDragging(StackPane root, Stage stage) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (windowMaximized) {
                return;
            }
            if (event.getTarget() instanceof Control
                    && !(event.getTarget() instanceof Label)) {
                return;
            }
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private VBox createBrandingPane() {
        FontIcon emblemIcon = new FontIcon(config.brandingIcon());
        emblemIcon.getStyleClass().add("login-branding-emblem-icon");

        StackPane emblem = new StackPane(emblemIcon);
        emblem.getStyleClass().add("login-branding-emblem");

        Label wordmark = new Label(copy.wordmark());
        wordmark.getStyleClass().add("login-branding-wordmark");

        Label subtitle = new Label(config.productSubtitle());
        subtitle.getStyleClass().add("login-branding-subtitle");

        brandingDescription = new Label(config.description());
        brandingDescription.getStyleClass().add("login-branding-description");
        brandingDescription.setWrapText(true);
        brandingDescription.setMaxWidth(270);

        securityCard = new VBox(8);
        securityCard.getStyleClass().add("login-security-card");
        securityCard.setMaxWidth(290);

        FontIcon lockIcon = new FontIcon(Feather.LOCK);
        lockIcon.getStyleClass().add("login-security-card-icon");

        Label securityTitle = new Label(copy.securityCardTitle());
        securityTitle.getStyleClass().add("login-security-card-title");

        HBox securityHeader = new HBox(8, lockIcon, securityTitle);
        securityHeader.setAlignment(Pos.CENTER_LEFT);

        Label securityText = new Label(copy.securityCardText());
        securityText.getStyleClass().add("login-security-card-text");
        securityText.setWrapText(true);

        securityCard.getChildren().addAll(securityHeader, securityText);

        this.buildLabel = new Label(config.versionLabel());
        this.buildLabel.getStyleClass().add("login-build-label");

        VBox panel = new VBox(18,
                emblem, wordmark, subtitle, brandingDescription, securityCard, this.buildLabel);
        panel.getStyleClass().add("login-branding-pane");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMinWidth(200);
        panel.setMaxWidth(Region.USE_PREF_SIZE);
        return panel;
    }

    private VBox createFormPane() {
        Label title = new Label(copy.formTitle());
        title.getStyleClass().add("login-form-title");

        FontIcon helpGlyph = new FontIcon(Feather.HELP_CIRCLE);
        helpGlyph.getStyleClass().add("login-help-icon");

        helpButton = new Button(copy.helpLabel(), helpGlyph);
        helpButton.getStyleClass().add("button-outlined");
        helpButton.setCursor(Cursor.HAND);
        helpButton.setAccessibleText(copy.helpLabel());
        helpButton.setOnAction(event -> showSecurityHelp());

        Region spacer = new Region();
        spacer.getStyleClass().add("spacer");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topLine = new HBox(title, spacer, helpButton);
        topLine.getStyleClass().add("login-form-topline");
        topLine.setAlignment(Pos.CENTER_LEFT);

        formSubtitle = new Label(config.formSubtitle());
        formSubtitle.getStyleClass().add("login-form-subtitle");
        formSubtitle.setWrapText(true);

        buildMaintenanceBanner();

        VBox form = createForm();

        footerNote = new Label(copy.footerNote());
        footerNote.getStyleClass().add("login-footer-note");
        footerNote.setWrapText(true);

        VBox panel = new VBox(14, topLine, formSubtitle, maintenanceBanner, form, footerNote);
        panel.getStyleClass().add("login-form-pane");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMinWidth(280);
        return panel;
    }

    private void buildMaintenanceBanner() {
        FontIcon icon = new FontIcon(Feather.ALERT_TRIANGLE);
        icon.getStyleClass().add("login-maintenance-icon");

        maintenanceText = new Label();
        maintenanceText.getStyleClass().add("login-maintenance-text");
        maintenanceText.setWrapText(true);
        maintenanceText.setMaxWidth(420);

        maintenanceBanner = new HBox(9, icon, maintenanceText);
        maintenanceBanner.getStyleClass().add("login-maintenance-banner");
        maintenanceBanner.setAlignment(Pos.CENTER_LEFT);
        maintenanceBanner.setVisible(false);
        maintenanceBanner.setManaged(false);
    }

    private VBox createForm() {
        emailStateLabel = new Label();
        emailStateLabel.getStyleClass().add("login-state-label");

        Label emailLabel = createFieldLabel(copy.emailLabel(), "Email");
        HBox emailHeader = new HBox(6, emailLabel, emailStateLabel);
        emailHeader.setAlignment(Pos.CENTER_LEFT);

        emailField = new TextField();
        emailField.getStyleClass().add("modern-text-field");
        emailField.setPromptText(copy.emailPrompt());
        emailField.setAccessibleText(copy.emailLabel());
        emailLabel.setLabelFor(emailField);

        String rememberedEmail = preferences.get(PREF_EMAIL, "");
        if (!rememberedEmail.isBlank()) {
            emailField.setText(rememberedEmail);
            emailStateLabel.setText(copy.emailSavedHint());
        }
        emailField.textProperty().addListener(
                (obs, oldValue, value) -> updateEmailState(value));

        VBox emailBox = new VBox(6, emailHeader, emailField);

        VBox passwordBox = createPasswordBox();

        buildMfaBox();

        rememberEmail = new CheckBox(copy.rememberEmailLabel());
        rememberEmail.getStyleClass().add("login-checkbox");
        rememberEmail.setSelected(!rememberedEmail.isBlank());

        forgotPasswordLink = new Hyperlink(copy.recoverAccessLabel());
        forgotPasswordLink.getStyleClass().add("login-link");
        forgotPasswordLink.setOnAction(event -> showRecoveryInfo());

        Region actionsSpacer = new Region();
        actionsSpacer.getStyleClass().add("spacer");
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);

        HBox actions = new HBox(rememberEmail, actionsSpacer, forgotPasswordLink);
        actions.getStyleClass().add("login-form-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        messageLabel = new Label();
        messageLabel.getStyleClass().addAll("login-message", "is-error");
        messageLabel.setWrapText(true);
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        FontIcon loginGlyph = new FontIcon(Feather.LOG_IN);
        loginGlyph.getStyleClass().add("login-submit-icon");

        loginButton = new Button(config.loginButtonText(), loginGlyph);
        loginButton.getStyleClass().add("button-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setCursor(Cursor.HAND);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> handleLogin());

        loading = new ProgressIndicator();
        loading.setMaxSize(26, 26);
        loading.setVisible(false);
        loading.setManaged(false);

        StackPane loginStack = new StackPane(loginButton, loading);
        loginStack.getStyleClass().add("login-submit-stack");

        statusChip = createStatusChip();

        VBox form = new VBox(11,
                emailBox,
                passwordBox,
                mfaBox,
                actions,
                messageLabel,
                loginStack,
                statusChip);
        form.getStyleClass().add("login-form");
        return form;
    }

    private void buildMfaBox() {
        Label mfaLabel = createFieldLabel(copy.mfaLabel(), "MFA");

        mfaHintLabel = new Label(copy.mfaHintTotp());
        mfaHintLabel.getStyleClass().add("login-hint");

        Region spacer = new Region();
        spacer.getStyleClass().add("spacer");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recoveryModeLink = new Hyperlink(copy.recoveryModeLinkLabel());
        recoveryModeLink.getStyleClass().addAll("login-link", "login-link--minor");
        recoveryModeLink.setOnAction(event -> setRecoveryMode(!recoveryMode));

        HBox mfaHeader = new HBox(6, mfaLabel, mfaHintLabel, spacer, recoveryModeLink);
        mfaHeader.setAlignment(Pos.CENTER_LEFT);

        mfaCodeField = new TextField();
        mfaCodeField.getStyleClass().add("modern-text-field");
        mfaCodeField.setAccessibleText(copy.mfaLabel());
        mfaLabel.setLabelFor(mfaCodeField);

        setRecoveryMode(false);

        mfaBox = new VBox(6, mfaHeader, mfaCodeField);
        mfaBox.getStyleClass().add("login-mfa-box");
        // Só aparece quando o serviço confirma que a conta tem segundo fator.
        mfaBox.setVisible(false);
        mfaBox.setManaged(false);
    }

    private HBox createStatusChip() {
        FontIcon icon = new FontIcon(Feather.SHIELD);
        icon.getStyleClass().add("login-status-chip-icon");

        Label text = new Label(copy.statusChip());
        text.getStyleClass().add("login-status-chip-text");
        text.setWrapText(true);

        HBox chip = new HBox(9, icon, text);
        chip.getStyleClass().add("login-status-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private Label createFieldLabel(String text, String accessibleName) {
        Label label = new Label(text);
        label.getStyleClass().add("login-field-label");
        label.setAccessibleText(accessibleName);
        return label;
    }

    private VBox createPasswordBox() {
        Label label = createFieldLabel(copy.passwordLabel(), "Palavra-passe");

        showPasswordBtn = new ToggleButton(copy.showPasswordLabel());
        showPasswordBtn.getStyleClass().addAll("button-icon-small", "login-show-password");
        showPasswordBtn.setCursor(Cursor.HAND);
        showPasswordBtn.setTooltip(
                new javafx.scene.control.Tooltip("Mostrar ou ocultar a palavra-passe"));
        showPasswordBtn.setAccessibleText(copy.showPasswordLabel());
        updateShowPasswordGlyph(false);

        Region spacer = new Region();
        spacer.getStyleClass().add("spacer");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(label, spacer, showPasswordBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        passwordField = new PasswordField();
        passwordField.getStyleClass().add("modern-text-field");
        passwordField.setPromptText(copy.passwordPrompt());
        passwordField.setAccessibleText(copy.passwordLabel());
        label.setLabelFor(passwordField);

        passwordVisibleField = new TextField();
        passwordVisibleField.getStyleClass().add("modern-text-field");
        passwordVisibleField.setPromptText(copy.passwordPrompt());
        passwordVisibleField.setAccessibleText(copy.passwordLabel());
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordBtn.selectedProperty().addListener(
                (obs, wasSelected, selected) -> {
                    showPasswordBtn.setText(
                            selected ? copy.hidePasswordLabel() : copy.showPasswordLabel());
                    showPasswordBtn.setAccessibleText(showPasswordBtn.getText());
                    updateShowPasswordGlyph(selected);

                    passwordField.setVisible(!selected);
                    passwordField.setManaged(!selected);
                    passwordVisibleField.setVisible(selected);
                    passwordVisibleField.setManaged(selected);

                    if (selected) {
                        passwordVisibleField.requestFocus();
                    } else {
                        passwordField.requestFocus();
                    }
                });

        return new VBox(6, header, passwordField, passwordVisibleField);
    }

    private void updateShowPasswordGlyph(boolean visible) {
        FontIcon glyph = new FontIcon(visible ? Feather.EYE_OFF : Feather.EYE);
        glyph.getStyleClass().add("login-show-password-icon");
        showPasswordBtn.setGraphic(glyph);
    }

    private void setRecoveryMode(boolean enabled) {
        recoveryMode = enabled;
        mfaCodeField.clear();

        if (recoveryMode) {
            mfaCodeField.setPromptText(copy.mfaPromptRecovery());
            mfaHintLabel.setText(copy.mfaHintRecovery());
            recoveryModeLink.setText(copy.totpModeLinkLabel());
            mfaCodeField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText()
                            .toUpperCase()
                            .matches("[A-HJ-NP-Z2-9-]{0,14}")
                            ? change
                            : null));
        } else {
            mfaCodeField.setPromptText(copy.mfaPromptTotp());
            mfaHintLabel.setText(copy.mfaHintTotp());
            recoveryModeLink.setText(copy.recoveryModeLinkLabel());
            mfaCodeField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d{0,6}")
                            ? change
                            : null));
        }
    }

    private void updateEmailState(String email) {
        emailField.getStyleClass().removeAll("is-valid", "is-invalid");

        if (email == null || email.isBlank()) {
            emailStateLabel.setText("");
            return;
        }

        if (EMAIL_PATTERN.matcher(email.trim()).matches()) {
            emailStateLabel.setText("");
            emailField.getStyleClass().add("is-valid");
        } else {
            emailStateLabel.setText("");
            emailField.getStyleClass().add("is-invalid");
        }
    }

    /** Revela o passo do segundo fator depois de o serviço o exigir. */
    private void revealMfaStep() {
        if (mfaBox.isManaged() && mfaBox.isVisible()) {
            mfaCodeField.requestFocus();
            mfaCodeField.selectAll();
            return;
        }

        setCollapsible(mfaBox, true);
        animateReveal(mfaBox);
        mfaCodeField.requestFocus();
    }

    private void animateReveal(Node node) {
        if (ThemeManager.isReducedMotion()) {
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String mfaRaw = mfaCodeField.getText() == null ? "" : mfaCodeField.getText().trim();

        if (email.isBlank()) {
            showMessage("Introduza o email da conta.", true);
            emailField.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showMessage("Introduza um endereço de email válido.", true);
            emailField.requestFocus();
            return;
        }

        if (password.isBlank()) {
            showMessage("Introduza a palavra-passe.", true);
            passwordField.requestFocus();
            return;
        }

        Integer mfaCode = null;
        String recoveryCode = null;

        if (!mfaRaw.isBlank()) {
            if (recoveryMode) {
                String normalizedRecovery = mfaRaw.replace("-", "").trim().toUpperCase();

                if (!(normalizedRecovery.length() == 12 || normalizedRecovery.length() == 14)) {
                    showMessage(
                            "O código de recuperação deve ter 12 caracteres, "
                                    + "com ou sem hífens.",
                            true);
                    mfaCodeField.requestFocus();
                    return;
                }
                recoveryCode = mfaRaw;
            } else {
                if (mfaRaw.length() != 6) {
                    showMessage("O código MFA deve ter 6 dígitos.", true);
                    mfaCodeField.requestFocus();
                    return;
                }
                mfaCode = Integer.valueOf(mfaRaw);
            }
        }

        setLoading(true);
        showMessage("", false);

        final Integer finalMfaCode = mfaCode;
        final String finalRecoveryCode = recoveryCode;
        final String sourceIp = resolveSourceIp();

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return authService.authenticate(
                        email, password, finalMfaCode, finalRecoveryCode,
                        sourceIp, config.applicationCode());
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);

            User user = task.getValue();
            persistRememberedEmail(email);
            hideOverlay();
            playExitAnimation(() -> Platform.runLater(() -> onSuccess.accept(user)));
        });

        task.setOnFailed(event -> {
            setLoading(false);

            Throwable error = task.getException();

            if (error instanceof PasswordChangeRequiredException required) {
                showMandatoryPasswordChange(
                        required.getUser(), password, required.isExpired());
                return;
            }

            if (error instanceof MfaRequiredException) {
                revealMfaStep();
                showMessage(
                        recoveryMode
                                ? "Introduza o código de recuperação de 12 caracteres."
                                : "Introduza o código MFA de 6 dígitos.",
                        false);
                return;
            }

            // Falhas de negócio já trazem uma mensagem própria para o utilizador.
            if (error instanceof AuthenticationException authError
                    && authError.getMessage() != null
                    && !authError.getMessage().isBlank()) {
                log.warn("Falha de autenticação para {}: {}", email, authError.getMessage());
                showMessage(authError.getMessage(), true);
                shakeNode(loginButton);
                refreshMaintenanceNotice();
                return;
            }

            // Erros técnicos devem ser registados com stack trace para facilitar o diagnóstico.
            log.error("Erro técnico ao iniciar sessão para {}", email, error);
            showMessage(
                    "Erro técnico ao iniciar sessão"
                            + (error == null
                            ? "."
                            : " (" + error.getClass().getSimpleName() + "). "
                            + "Consulte o registo da aplicação para mais detalhes."),
                    true);
            shakeNode(loginButton);
        });

        executor.submit(task);
    }

    /**
     * Consulta a manutenção fora do thread JavaFX: um Estado de manutenção não
     * pode atrasar o desenho do ecrã nem bloquear o botão (ADMIN/superadmin
     * continuam a entrar, e é o serviço quem decide).
     */
    private void refreshMaintenanceNotice() {
        if (maintenanceModeService == null || maintenanceBanner == null) {
            return;
        }

        executor.submit(() -> {
            String notice = "";
            try {
                if (maintenanceModeService.isEnabled()) {
                    StringBuilder text = new StringBuilder(copy.maintenanceNotice());

                    String reason = maintenanceModeService.getReason();
                    if (reason != null && !reason.isBlank()) {
                        text.append(' ').append(reason.trim());
                    }

                    LocalDateTime since = maintenanceModeService.getSince();
                    if (since != null) {
                        text.append("  ·  ").append(SINCE_FORMAT.format(since));
                    }

                    notice = text.toString();
                }
            } catch (RuntimeException ex) {
                log.debug("Estado de manutenção indisponível para o login.", ex);
            }

            final String maintenanceMessage = notice;
            Platform.runLater(() -> {
                maintenanceText.setText(maintenanceMessage);
                boolean active = !maintenanceMessage.isBlank();
                if (active && !maintenanceBanner.isManaged()) {
                    animateReveal(maintenanceBanner);
                }
                setCollapsible(maintenanceBanner, active);
            });
        });
    }

    private void showMandatoryPasswordChange(User user,
                                             String currentPassword,
                                             boolean expired) {
        PasswordField newPassword = new PasswordField();
        newPassword.getStyleClass().addAll("modern-text-field", "login-dialog-field");
        newPassword.setPromptText("Nova palavra-passe (mín. 8 caracteres)");
        newPassword.setAccessibleText("Nova palavra-passe");

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.getStyleClass().addAll("modern-text-field", "login-dialog-field");
        confirmPassword.setPromptText("Confirmar nova palavra-passe");
        confirmPassword.setAccessibleText("Confirmar nova palavra-passe");

        Label policy = new Label(
                "A nova palavra-passe precisa de pelo menos 8 caracteres, "
                        + "com maiúsculas, minúsculas e números.");
        policy.getStyleClass().add("login-policy-text");
        policy.setWrapText(true);

        Label status = new Label();
        status.getStyleClass().add("login-field-error");
        status.setWrapText(true);
        status.setManaged(false);
        status.setVisible(false);

        Button cancel = new Button("Cancelar", new FontIcon(Feather.X));
        cancel.getStyleClass().add("button-outlined");

        Button confirm = new Button("Alterar e continuar", new FontIcon(Feather.CHECK));
        confirm.getStyleClass().add("button-primary");
        confirm.setDefaultButton(true);

        HBox actions = new HBox(8, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label explanation = new Label(
                expired
                        ? "A sua palavra-passe expirou. "
                        + "Por segurança, deve definir agora uma nova palavra-passe."
                        : "A credencial temporária foi validada. "
                        + "Por segurança, deve definir agora uma nova palavra-passe.");
        explanation.getStyleClass().add("login-modal-body");
        explanation.setWrapText(true);

        Label account = new Label("Conta: " + (user == null ? "-" : user.getEmail()));
        account.getStyleClass().add("login-modal-body");

        Label newLabel = new Label("Nova palavra-passe");
        newLabel.getStyleClass().add("login-field-label");
        newLabel.setLabelFor(newPassword);

        Label confirmLabel = new Label("Confirmar nova palavra-passe");
        confirmLabel.getStyleClass().add("login-field-label");
        confirmLabel.setLabelFor(confirmPassword);

        VBox content = new VBox(14,
                explanation, account, new Separator(),
                newLabel, newPassword,
                confirmLabel, confirmPassword,
                policy, status, actions);
        content.getStyleClass().add("login-dialog");

        cancel.setOnAction(event -> {
            hideOverlay();
            showMessage("A alteração da palavra-passe é obrigatória para entrar.", true);
        });

        confirm.setOnAction(event -> {
            String newValue = newPassword.getText() == null ? "" : newPassword.getText();
            String confirmation = confirmPassword.getText() == null
                    ? ""
                    : confirmPassword.getText();

            Optional<String> validation = validatePassword(newValue, confirmation);

            if (validation.isPresent()) {
                status.setText(validation.get());
                status.setManaged(true);
                status.setVisible(true);
                return;
            }

            setMandatoryPasswordLoading(confirm, cancel, true);

            executor.submit(() -> {
                try {
                    User changed = passwordChangeService.changeOwnPassword(
                            user.getId(), currentPassword, newValue);

                    Platform.runLater(() -> {
                        setMandatoryPasswordLoading(confirm, cancel, false);
                        hideOverlay();
                        playExitAnimation(() ->
                                Platform.runLater(() -> onSuccess.accept(changed)));
                    });
                } catch (Exception ex) {
                    log.warn("Falha ao concluir alteração obrigatória de palavra-passe para {}",
                            user.getEmail(), ex);

                    Platform.runLater(() -> {
                        setMandatoryPasswordLoading(confirm, cancel, false);
                        status.setText("Não foi possível alterar a palavra-passe. "
                                + "Verifique os dados e tente novamente.");
                        status.setManaged(true);
                        status.setVisible(true);
                    });
                }
            });
        });

        showOverlay(expired ? "Palavra-passe expirada" : "Primeiro acesso", content, false);
        Platform.runLater(newPassword::requestFocus);
    }

    private void setMandatoryPasswordLoading(Button confirm,
                                             Button cancel,
                                             boolean loadingState) {
        confirm.setDisable(loadingState);
        cancel.setDisable(loadingState);
        confirm.setText(loadingState ? "A guardar..." : "Alterar e continuar");
    }

    private Optional<String> validatePassword(String password, String confirmation) {
        if (password.isBlank()) {
            return Optional.of("Introduza a nova palavra-passe.");
        }

        if (password.length() < 8) {
            return Optional.of("A nova palavra-passe deve ter pelo menos 8 caracteres.");
        }

        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);

        if (!upper || !lower || !digit) {
            return Optional.of("Use pelo menos uma maiúscula, uma minúscula e um número.");
        }

        if (!password.equals(confirmation)) {
            return Optional.of("A confirmação da palavra-passe não coincide.");
        }

        return Optional.empty();
    }

    private void showRecoveryInfo() {
        FontIcon icon = new FontIcon(Feather.LIFE_BUOY);
        icon.getStyleClass().add("login-modal-icon");

        Label title = new Label("Recuperação de acesso");
        title.getStyleClass().add("login-modal-title");

        Label body = new Label(
                "A recuperação automática por email não está activa "
                        + "neste ambiente. Contacte o administrador no "
                        + "Kubata Administrator para redefinir o acesso.");
        body.getStyleClass().add("login-modal-body");
        body.setWrapText(true);

        VBox textBox = new VBox(3, title, body);

        HBox header = new HBox(10, icon, textBox);
        header.setAlignment(Pos.CENTER_LEFT);

        Label warningLabel = new Label(
                "Por segurança, o Kubata não apresenta senhas, "
                        + "tokens ou códigos de recuperação nesta tela.");
        warningLabel.getStyleClass().add("login-modal-warning-text");
        warningLabel.setWrapText(true);

        VBox warning = new VBox(6, warningLabel);
        warning.getStyleClass().add("login-modal-warning");

        VBox content = new VBox(12, header, warning);
        content.getStyleClass().add("login-dialog");

        showOverlay("Recuperar acesso", content, true);
    }

    private void showSecurityHelp() {
        VBox content = new VBox(12,
                securityInfo(Feather.KEY, "Palavra-passe",
                        "Nunca partilhe a sua credencial. "
                                + "Senhas provisórias precisam de ser trocadas "
                                + "antes do acesso normal."),
                securityInfo(Feather.SHIELD, "MFA",
                        "Quando a conta usa MFA, o campo do código aparece "
                                + "depois de a palavra-passe ser aceite."),
                securityInfo(Feather.LOCK, "Bloqueio",
                        "Tentativas falhadas podem activar o bloqueio "
                                + "temporário definido pelo administrador."),
                securityInfo(Feather.ACTIVITY, "Auditoria",
                        "Operações administrativas relevantes são "
                                + "registadas para rastreabilidade."));
        content.getStyleClass().add("login-dialog");

        showOverlay("Segurança do acesso", content, true);
    }

    private VBox securityInfo(Feather iconType, String title, String description) {
        FontIcon itemIcon = new FontIcon(iconType);
        itemIcon.getStyleClass().add("login-info-icon");

        Label itemTitle = new Label(title);
        itemTitle.getStyleClass().add("login-info-title");

        Label itemDescription = new Label(description);
        itemDescription.getStyleClass().add("login-info-text");
        itemDescription.setWrapText(true);

        VBox texts = new VBox(3, itemTitle, itemDescription);

        HBox row = new HBox(10, itemIcon, texts);
        row.getStyleClass().add("login-info-row");
        row.setAlignment(Pos.TOP_LEFT);

        return new VBox(row);
    }

    private void persistRememberedEmail(String email) {
        if (rememberEmail.isSelected()) {
            preferences.put(PREF_EMAIL, email);
        } else {
            preferences.remove(PREF_EMAIL);
        }
    }

    private String resolveSourceIp() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return hostAddress == null || hostAddress.isBlank() ? "127.0.0.1" : hostAddress;
        } catch (Exception ex) {
            log.debug("Não foi possível resolver o IP local.", ex);
            return "127.0.0.1";
        }
    }

    private void setLoading(boolean on) {
        loginButton.setDisable(on);
        loading.setVisible(on);
        loading.setManaged(on);

        emailField.setDisable(on);
        passwordField.setDisable(on);
        passwordVisibleField.setDisable(on);
        mfaCodeField.setDisable(on);
        showPasswordBtn.setDisable(on);
        rememberEmail.setDisable(on);
        forgotPasswordLink.setDisable(on);
        recoveryModeLink.setDisable(on);
        helpButton.setDisable(on);

        loginButton.setOpacity(on ? 0 : 1);
    }

    private void showMessage(String message, boolean error) {
        if (message == null || message.isBlank()) {
            messageLabel.setText("");
            messageLabel.setAccessibleText("");
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
            return;
        }

        messageLabel.setText(message);
        messageLabel.setAccessibleText(message);
        messageLabel.getStyleClass().removeAll("is-error", "is-info");
        messageLabel.getStyleClass().add(error ? "is-error" : "is-info");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void animateEntrance(Node node) {
        if (ThemeManager.isReducedMotion()) {
            node.setOpacity(1);
            return;
        }

        node.setOpacity(0);
        node.setTranslateY(18);
        node.setScaleX(0.985);
        node.setScaleY(0.985);

        FadeTransition fade = new FadeTransition(Duration.millis(420), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(420), node);
        translate.setFromY(18);
        translate.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(420), node);
        scale.setFromX(0.985);
        scale.setFromY(0.985);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(fade, translate, scale).play();
    }

    private void playExitAnimation(Runnable onFinished) {
        Pane exitTarget = rootPane;

        if (ThemeManager.isReducedMotion() || exitTarget == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(220), exitTarget);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fade.play();
    }

    private void shakeNode(Node node) {
        if (ThemeManager.isReducedMotion()) {
            return;
        }

        TranslateTransition shake = new TranslateTransition(Duration.millis(45), node);
        shake.setFromX(-6);
        shake.setToX(6);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> node.setTranslateX(0));
        shake.play();
    }

    private void showOverlay(String title, Node content, boolean closable) {
        hideOverlay();
        if (rootPane == null) {
            return;
        }

        StackPane dim = new StackPane();
        dim.getStyleClass().add("login-overlay-dim");

        Label heading = new Label(title);
        heading.getStyleClass().add("login-overlay-title");

        VBox cardContent = new VBox(14, heading, content);
        cardContent.getStyleClass().add("login-overlay-card");
        cardContent.setMaxWidth(560);

        if (closable) {
            Button close = new Button("Fechar");
            close.getStyleClass().add("button-primary");
            close.setOnAction(event -> hideOverlay());

            HBox actions = new HBox(close);
            actions.setAlignment(Pos.CENTER_RIGHT);
            cardContent.getChildren().add(actions);

            dim.setOnMouseClicked(event -> {
                if (event.getTarget() == dim) {
                    hideOverlay();
                }
            });
        }

        StackPane.setAlignment(cardContent, Pos.CENTER);
        dim.getChildren().add(cardContent);
        overlayPane = dim;
        rootPane.getChildren().add(dim);
    }

    private void hideOverlay() {
        if (rootPane != null && overlayPane != null) {
            rootPane.getChildren().remove(overlayPane);
            overlayPane = null;
        }
    }
}
