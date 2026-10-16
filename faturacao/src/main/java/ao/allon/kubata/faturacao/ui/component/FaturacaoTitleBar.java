package ao.allon.kubata.faturacao.ui.component;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.view.MainView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra de título verde do Kubata Faturação, alinhada ao padrão do Administrator.
 * Inclui pesquisa de comandos, notificações, histórico, sessão e controlos de janela.
 */
public final class FaturacaoTitleBar extends HBox {

    private final Stage stage;
    private final MainController controller;
    private final SessionManager session;
    private final MainView owner;
    private final TextField searchField = new TextField();
    private final StackPane searchContainer = new StackPane();
    private final Label userLabel = new Label();
    private final Label userAvatar = new Label("U");
    private final Label notificationBadge = new Label();
    private final Button notificationButton = new Button();
    private final ContextMenu searchResults = new ContextMenu();
    private final ContextMenu notificationMenu = new ContextMenu();
    private final List<String> notifications = new ArrayList<>();
    private Consumer<String> searchCallback;
    private double dragOffsetX;
    private double dragOffsetY;

    public FaturacaoTitleBar(Stage stage, String appTitle, MainController controller,
                             SessionManager session, MainView owner) {
        this.stage = stage;
        this.controller = controller;
        this.session = session;
        this.owner = owner;

        getStyleClass().add("custom-title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        setMinHeight(40);
        setPrefHeight(40);
        setMaxHeight(40);

        FontIcon appIcon = IconUtils.icon(Feather.FILE_TEXT, 20);
        appIcon.setIconColor(Color.WHITE);
        appIcon.getStyleClass().add("title-bar-app-icon");
        StackPane iconContainer = new StackPane(appIcon);
        iconContainer.setPadding(new Insets(0, 6, 0, 12));

        Label appName = new Label(appTitle == null ? "Kubata Faturação" : appTitle);
        appName.getStyleClass().add("title-bar-app-name");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.SOMETIMES);
        leftSpacer.setMinWidth(12);

        searchField.setPromptText("Pesquisar páginas e comandos...");
        searchField.getStyleClass().add("title-bar-search-box");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (searchCallback != null) {
                    String query = searchField.getText() == null ? "" : searchField.getText().trim();
                    if (!query.isBlank()) searchCallback.accept(query);
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                searchResults.hide();
                searchField.clear();
            }
        });

        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setMinWidth(200);
        searchContainer.setPrefWidth(380);
        searchContainer.setMaxWidth(560);
        HBox.setHgrow(searchContainer, Priority.ALWAYS);
        searchContainer.getChildren().add(searchField);
        FontIcon searchIcon = IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL);
        searchIcon.setIconColor(Color.web("#FFFFFF"));
        searchIcon.setOpacity(0.72);
        searchIcon.setMouseTransparent(true);
        searchIcon.setTranslateX(10);
        searchContainer.getChildren().add(searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);
        rightSpacer.setMinWidth(12);

        Button recentButton = createIconButton(Feather.CLOCK, "Histórico recente");
        recentButton.setOnAction(e -> showRecentMenu(recentButton));

        notificationButton.setGraphic(IconUtils.icon(Feather.BELL, IconUtils.SIZE_MEDIUM));
        notificationButton.getStyleClass().add("title-bar-control-btn");
        notificationButton.setTooltip(new Tooltip("Notificações"));
        notificationButton.setFocusTraversable(false);
        notificationButton.setOnAction(e -> showNotificationMenu());
        notificationBadge.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 9; " +
                "-fx-text-fill: #217346; -fx-font-size: 9px; -fx-font-weight: bold; " +
                "-fx-padding: 1 4 1 4;"
        );
        notificationBadge.setVisible(false);
        StackPane notificationStack = new StackPane(notificationButton, notificationBadge);
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(notificationBadge, new Insets(1, 3, 0, 0));

        userAvatar.getStyleClass().add("title-bar-user-avatar");
        userLabel.getStyleClass().add("title-bar-user-label");
        HBox userInfo = new HBox(7, userLabel, userAvatar);
        userInfo.setAlignment(Pos.CENTER);
        userInfo.setPadding(new Insets(0, 4, 0, 4));

        MenuButton profileMenu = new MenuButton();
        profileMenu.setGraphic(userInfo);
        profileMenu.getStyleClass().add("title-bar-user-menu");
        profileMenu.setTooltip(new Tooltip("Perfil e sessão"));
        profileMenu.setPopupSide(Side.BOTTOM);
        MenuItem profileItem = new MenuItem("Meu Perfil", IconUtils.icon(Feather.USER, IconUtils.SIZE_SMALL));
        profileItem.setOnAction(e -> controller.showPerfilUsuario());
        MenuItem configItem = new MenuItem("Configurações", IconUtils.icon(Feather.SETTINGS, IconUtils.SIZE_SMALL));
        configItem.setOnAction(e -> controller.showConfiguracoes());
        MenuItem changeUser = new MenuItem("Trocar utilizador...", IconUtils.icon(Feather.USERS, IconUtils.SIZE_SMALL));
        changeUser.setOnAction(e -> owner.confirmLogout());
        MenuItem logoutItem = new MenuItem("Encerrar sessão", IconUtils.icon(Feather.LOG_OUT, IconUtils.SIZE_SMALL));
        logoutItem.getStyleClass().add("danger");
        logoutItem.setOnAction(e -> owner.confirmLogout());
        profileMenu.getItems().add(profileItem);
        if (isAdmin()) profileMenu.getItems().add(configItem);
        profileMenu.getItems().addAll(new SeparatorMenuItem(), changeUser, logoutItem);

        Button minimize = createWindowButton("—", "Minimizar", false);
        Button maximize = createWindowButton(stage != null && stage.isMaximized() ? "❐" : "□", "Maximizar / Restaurar", false);
        Button close = createWindowButton("×", "Fechar", true);
        minimize.setOnAction(e -> { if (this.stage != null) this.stage.setIconified(true); });
        maximize.setOnAction(e -> { if (this.stage != null) this.stage.setMaximized(!this.stage.isMaximized()); });
        close.setOnAction(e -> owner.confirmApplicationClose());
        if (stage != null) {
            stage.maximizedProperty().addListener((obs, oldValue, maximized) -> {
                maximize.setText(maximized ? "❐" : "□");
                maximize.setTooltip(new Tooltip(maximized ? "Restaurar" : "Maximizar"));
            });
        }

        getChildren().addAll(iconContainer, appName, leftSpacer, searchContainer, rightSpacer,
                recentButton, notificationStack, profileMenu, minimize, maximize, close);
        setUserName(currentUserName());
        notifications.add("Kubata Faturação iniciado.");
        updateNotificationBadge();
        setupDragging();
    }

    public void setOnSearch(Consumer<String> callback) {
        this.searchCallback = callback;
    }

    public void setSearchPrompt(String prompt) {
        searchField.setPromptText(prompt);
    }

    public void clearSearch() {
        searchField.clear();
        searchResults.hide();
    }

    public void setUserName(String name) {
        String safeName = name == null || name.isBlank() ? "Utilizador" : name.trim();
        userLabel.setText(safeName);
        userLabel.setVisible(!"Utilizador".equals(safeName));
        userLabel.setManaged(userLabel.isVisible());
        String[] parts = safeName.split("\\s+");
        String initials = parts.length > 1
                ? parts[0].substring(0, 1) + parts[1].substring(0, 1)
                : safeName.substring(0, 1);
        userAvatar.setText(initials.toUpperCase(java.util.Locale.ROOT));
    }

    public void addNotification(String message) {
        if (message == null || message.isBlank()) return;
        notifications.add(0, message.trim());
        while (notifications.size() > 30) notifications.remove(notifications.size() - 1);
        updateNotificationBadge();
    }

    public void showSearchResults(List<MenuItem> items) {
        if (searchResults.isShowing()) searchResults.hide();
        searchResults.getItems().setAll(items == null ? List.of() : items);
        if (!searchResults.getItems().isEmpty() && searchContainer.getScene() != null) {
            searchResults.show(searchContainer, Side.BOTTOM, 0, 4);
        }
    }

    private void showRecentMenu(Node ownerNode) {
        ContextMenu menu = new ContextMenu();
        MenuItem reopen = new MenuItem("Reabrir última aba");
        reopen.setDisable(!owner.hasRecentlyClosed());
        reopen.setOnAction(e -> owner.reopenLastClosed());
        menu.getItems().add(reopen);
        menu.show(ownerNode, Side.BOTTOM, 0, 4);
    }

    private void showNotificationMenu() {
        notificationMenu.getItems().clear();
        if (notifications.isEmpty()) {
            MenuItem empty = new MenuItem("Nenhuma notificação");
            empty.setDisable(true);
            notificationMenu.getItems().add(empty);
        } else {
            int limit = Math.min(12, notifications.size());
            for (int i = 0; i < limit; i++) {
                MenuItem item = new MenuItem(notifications.get(i));
                item.setOnAction(e -> {
                    notifications.remove(item.getText());
                    updateNotificationBadge();
                });
                notificationMenu.getItems().add(item);
            }
            notificationMenu.getItems().add(new SeparatorMenuItem());
            MenuItem markAll = new MenuItem("Marcar todas como lidas");
            markAll.setOnAction(e -> {
                notifications.clear();
                updateNotificationBadge();
                notificationMenu.hide();
            });
            notificationMenu.getItems().add(markAll);
        }
        notificationMenu.show(notificationButton, Side.BOTTOM, 0, 4);
    }

    private void updateNotificationBadge() {
        int count = notifications.size();
        notificationBadge.setText(count > 99 ? "99+" : Integer.toString(count));
        notificationBadge.setVisible(count > 0);
    }

    private Button createIconButton(Feather icon, String tooltip) {
        Button button = new Button();
        button.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_MEDIUM));
        button.getStyleClass().add("title-bar-control-btn");
        button.setTooltip(new Tooltip(tooltip));
        button.setFocusTraversable(false);
        return button;
    }

    private Button createWindowButton(String text, String tooltip, boolean close) {
        Button button = new Button(text);
        button.getStyleClass().add(close ? "title-bar-close-btn" : "title-bar-control-btn");
        button.setTooltip(new Tooltip(tooltip));
        button.setAccessibleText(tooltip);
        button.setFocusTraversable(false);
        return button;
    }

    private String currentUserName() {
        User user = session == null ? null : session.getUserObject();
        return user == null ? null : user.getNome();
    }

    private boolean isAdmin() {
        User user = session == null ? null : session.getUserObject();
        return user != null && user.getRole() == Role.ADMIN;
    }

    private void setupDragging() {
        if (stage == null) return;
        setOnMousePressed(e -> {
            if (isInteractiveTarget(e)) {
                dragOffsetX = Double.NaN;
                dragOffsetY = Double.NaN;
                return;
            }
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        setOnMouseDragged(e -> {
            if (!stage.isMaximized() && !Double.isNaN(dragOffsetX) && !Double.isNaN(dragOffsetY)) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isInteractiveTarget(e) && e.getX() < getWidth() - 138) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private boolean isInteractiveTarget(MouseEvent event) {
        Node node = event.getTarget() instanceof Node n ? n : null;
        while (node != null && node != this) {
            if (node instanceof ButtonBase || node instanceof TextInputControl) return true;
            node = node.getParent();
        }
        return false;
    }
}
