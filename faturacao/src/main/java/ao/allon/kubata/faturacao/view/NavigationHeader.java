package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.header.NotificationModel;
import ao.allon.kubata.faturacao.header.SearchValidator;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import atlantafx.base.theme.Styles;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.PasswordTextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.control.CustomMenuItem;

public final class NavigationHeader extends BorderPane {

    private final SideNav sideNav;
    private final MainView mainView;
    private final SessionManager session;
    private final EmpresaService empresaService;
    private ToggleButton btnHamburger;
    private final CustomTextField searchField = new CustomTextField();
    private final NotificationModel notifications = new NotificationModel();
    private final Button bellButton = new Button("");
    private final Label bellBadge = new Label();
    private final MenuButton userMenu = new MenuButton("");
    private final ContextMenu notifMenu = new ContextMenu();
    private final Label userLabel = new Label();
    private final Label userRoleLabel = new Label();

    public NavigationHeader(SideNav sideNav, MainView mainView, SessionManager session, EmpresaService empresaService) {
        this.sideNav = sideNav;
        this.mainView = mainView;
        this.session = session;
        this.empresaService = empresaService;

        getStyleClass().addAll(Styles.ELEVATED_1, "app-header");
        setPadding(new Insets(8, 16, 8, 16));

        Node leftSection = buildLeftSection();
        setLeft(leftSection);

        Node centerSection = buildCenterSection();
        setCenter(centerSection);

        Node rightSection = buildRightSection();
        setRight(rightSection);

        updateUserInfo();

        notifications.add("Bem-vindo ao Kubata!");
        updateBadge();
    }

    private Node buildLeftSection() {
        FontIcon bars = IconUtils.icon(Feather.MENU, IconUtils.SIZE_MEDIUM);
        FontIcon times = IconUtils.icon(Feather.X, IconUtils.SIZE_MEDIUM);
        times.setOpacity(0);
        StackPane iconStack = new StackPane(bars, times);
        iconStack.setMinSize(18, 18);
        iconStack.setPrefSize(18, 18);

        btnHamburger = new ToggleButton("", iconStack);
        btnHamburger.getStyleClass().addAll(Styles.BUTTON_ICON);
        btnHamburger.setTooltip(new Tooltip("Alternar menu lateral"));
        btnHamburger.setAccessibleText("Alternar menu");
        btnHamburger.setOnAction(e -> {
            boolean hide = btnHamburger.isSelected();
            sideNav.setCollapsed(hide);
            javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                    new javafx.animation.KeyValue(bars.opacityProperty(), hide ? 0 : 1, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(times.opacityProperty(), hide ? 1 : 0, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(iconStack.rotateProperty(), hide ? 90 : 0, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            tl.play();
        });

        Node title = buildCompanyTitle();

        HBox leftBox = new HBox(12, btnHamburger, title);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        return leftBox;
    }

    private Node buildCenterSection() {
        StackPane searchWrapper = buildSearch();
        HBox centerBox = new HBox(searchWrapper);
        centerBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBox, Priority.ALWAYS);
        return centerBox;
    }

    private Node buildRightSection() {
        StackPane bellWrapper = buildNotifications();
        MenuButton recent = buildRecent();
        VBox userInfoBox = buildUserInfoBox();
        MenuButton account = buildAccountMenu();

        HBox rightBox = new HBox(8, bellWrapper, recent, userInfoBox, account);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        return rightBox;
    }

    private Node buildCompanyTitle() {
        String nome = "Kubata";
        String subtitle = "Sistema de Faturação";
        byte[] logo = null;
        try {
            ao.allon.kubata.faturacao.domain.Empresa e = empresaService.getDadosEmpresa();
            if (e != null && e.getNome() != null && !e.getNome().isBlank()) {
                nome = e.getNome();
                if (e.getNif() != null && !e.getNif().isBlank()) {
                    subtitle = "NIF: " + e.getNif();
                }
            }
            if (e != null && e.getLogotipo() != null && e.getLogotipo().length > 0) {
                logo = e.getLogotipo();
            }
        } catch (Exception ignored) {}

        Label lblNome = new Label(nome);
        lblNome.getStyleClass().add(Styles.TITLE_4);

        Label lblSub = new Label(subtitle);
        lblSub.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        VBox textBox = new VBox(2, lblNome, lblSub);
        textBox.setAlignment(Pos.CENTER_LEFT);

        if (logo != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(new java.io.ByteArrayInputStream(logo))
            );
            iv.setPreserveRatio(true);
            iv.setFitHeight(32);
            HBox box = new HBox(10, iv, textBox);
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        } else {
            HBox box = new HBox(10, IconUtils.icon(Feather.BOX, IconUtils.SIZE_MEDIUM), textBox);
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        }
    }

    private VBox buildUserInfoBox() {
        userLabel.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.TEXT_SMALL);
        userRoleLabel.getStyleClass().add(Styles.TEXT_MUTED);

        VBox box = new VBox(1, userLabel, userRoleLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void updateUserInfo() {
        userLabel.setText(session.getCurrentUser());
        ao.allon.kubata.core.domain.User user = session.getUserObject();
        if (user != null && user.getRole() != null) {
            userRoleLabel.setText(user.getRole().name());
        } else {
            userRoleLabel.setText("Convidado");
        }
    }

    private StackPane buildSearch() {
        searchField.setPromptText("Pesquisar produtos, clientes...");
        searchField.setMinWidth(280);
        searchField.setPrefWidth(360);
        searchField.setMaxWidth(480);
        searchField.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));

        StackPane wrapper = new StackPane(searchField);
        wrapper.setPadding(new Insets(0, 8, 0, 8));

        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        searchField.textProperty().addListener((obs, ov, nv) -> {
            debounce.stop();
            debounce.setOnFinished(x -> triggerSearch());
            debounce.playFromStart();
        });

        return wrapper;
    }

    private void triggerSearch() {
        String q = searchField.getText();
        if (!SearchValidator.isValid(q)) {
            searchField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            return;
        }
        searchField.pseudoClassStateChanged(Styles.STATE_DANGER, false);
        try {
            MainController c = (MainController) getScene().getUserData();
            if (c != null) {
                c.onGlobalSearch(q == null ? "" : q.trim());
            }
        } catch (Exception ignored) { }
    }

    private StackPane buildNotifications() {
        bellButton.setGraphic(IconUtils.icon(Feather.BELL, IconUtils.SIZE_MEDIUM));
        bellButton.getStyleClass().addAll(Styles.BUTTON_ICON);
        bellButton.setTooltip(new Tooltip("Notificações"));
        bellButton.setFocusTraversable(false);

        bellBadge.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLD);
        bellBadge.setStyle("-fx-background-radius: 10; -fx-padding: 2 6; -fx-font-size: 10;");
        bellBadge.setText("");
        bellBadge.setVisible(false);

        StackPane wrapper = new StackPane(bellButton, bellBadge);
        StackPane.setAlignment(bellBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(bellBadge, new Insets(-4, -4, 0, 0));

        bellButton.setOnAction(e -> {
            if (notifMenu.isShowing()) {
                notifMenu.hide();
                return;
            }
            rebuildNotifMenu();
            notifMenu.show(bellButton, javafx.geometry.Side.BOTTOM, 0, 4);
        });

        return wrapper;
    }

    private void rebuildNotifMenu() {
        notifMenu.getItems().clear();

        if (notifications.getAll().isEmpty()) {
            MenuItem empty = new MenuItem("Nenhuma notificação");
            empty.setDisable(true);
            notifMenu.getItems().add(empty);
            return;
        }

        for (NotificationModel.Notification n : notifications.getAll()) {
            HBox itemBox = new HBox(8);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            itemBox.setPadding(new Insets(6, 10, 6, 10));

            FontIcon icon = IconUtils.icon(n.isRead() ? Feather.CHECK_CIRCLE : Feather.INFO, IconUtils.SIZE_SMALL);
            Label text = new Label(n.getText());
            text.setWrapText(true);
            text.setMaxWidth(240);

            itemBox.getChildren().addAll(icon, text);
            if (!n.isRead()) {
                itemBox.setStyle("-fx-background-color: -color-bg-subtle;");
            }

            CustomMenuItem menuItem = new CustomMenuItem(itemBox, false);
            menuItem.setOnAction(e -> {
                notifications.markRead(n.getId());
                updateBadge();
            });
            notifMenu.getItems().add(menuItem);
        }

        MenuItem markAll = new MenuItem("Marcar todas como lidas", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        markAll.setOnAction(e -> {
            notifications.markAllRead();
            updateBadge();
        });

        notifMenu.getItems().addAll(new SeparatorMenuItem(), markAll);
    }

    private void updateBadge() {
        int c = notifications.getUnreadCount();
        bellBadge.setVisible(c > 0);
        bellBadge.setText(c > 99 ? "99+" : String.valueOf(c));
    }

    private MenuButton buildRecent() {
        MenuButton recent = new MenuButton("", IconUtils.icon(Feather.CLOCK, IconUtils.SIZE_MEDIUM));
        recent.getStyleClass().add(Styles.BUTTON_ICON);
        recent.setTooltip(new Tooltip("Histórico recente"));

        recent.setOnShowing(e -> {
            recent.getItems().clear();
            MenuItem reopen = new MenuItem("Reabrir última aba", IconUtils.icon(Feather.ROTATE_CCW, IconUtils.SIZE_SMALL));
            reopen.setDisable(!mainView.hasRecentlyClosed());
            reopen.setOnAction(ev -> mainView.reopenLastClosed());
            recent.getItems().add(reopen);
        });

        return recent;
    }

    private MenuButton buildAccountMenu() {
        userMenu.setGraphic(IconUtils.icon(Feather.USER, IconUtils.SIZE_MEDIUM));
        userMenu.getStyleClass().addAll(Styles.BUTTON_ICON);
        userMenu.setTooltip(new Tooltip("Menu do usuário"));

        userMenu.setOnShowing(e -> {
            userMenu.getItems().clear();

            MenuItem profile = new MenuItem("Meu Perfil", IconUtils.icon(Feather.USER, IconUtils.SIZE_SMALL));
            profile.setOnAction(ev -> showProfile());

            // Apenas admin vê Configurações e Dados da Empresa
            boolean isAdmin = session.getUserObject() != null && 
                              session.getUserObject().getRole() == ao.allon.kubata.core.domain.Role.ADMIN;

            if (isAdmin) {
                MenuItem settings = new MenuItem("Configurações", IconUtils.icon(Feather.SETTINGS, IconUtils.SIZE_SMALL));
                settings.setOnAction(ev -> showSettings());

                MenuItem empresa = new MenuItem("Dados da Empresa", IconUtils.icon(Feather.HOME, IconUtils.SIZE_SMALL));
                empresa.setOnAction(ev -> showEmpresaConfig());

                userMenu.getItems().addAll(profile, settings, empresa);
            } else {
                userMenu.getItems().add(profile);
            }

            SeparatorMenuItem sep1 = new SeparatorMenuItem();

            MenuItem switchUser = new MenuItem("Trocar Usuário...", IconUtils.icon(Feather.USERS, IconUtils.SIZE_SMALL));
            switchUser.setOnAction(ev -> showSwitchUserDialog());

            MenuItem logout = new MenuItem("Encerrar Sessão", IconUtils.icon(Feather.LOG_OUT, IconUtils.SIZE_SMALL));
            logout.getStyleClass().add(Styles.DANGER);
            logout.setOnAction(ev -> performLogoutWithConfirmation());

            userMenu.getItems().addAll(sep1, switchUser, logout);
        });

        return userMenu;
    }

    private void showProfile() {
        try {
            MainController c = (MainController) getScene().getUserData();
            if (c != null) {
                c.showPerfilUsuario();
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível abrir o perfil.", ex);
        }
    }

    private void showSettings() {
        try {
            MainController c = (MainController) getScene().getUserData();
            if (c != null) {
                c.showConfiguracoes();
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível abrir as configurações.", ex);
        }
    }

    private void showEmpresaConfig() {
        try {
            MainController c = (MainController) getScene().getUserData();
            if (c != null) {
                c.showConfiguracoes();
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível abrir configurações da empresa.", ex);
        }
    }

    private void showSwitchUserDialog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Trocar Usuário");
        confirm.setHeaderText("Deseja trocar de usuário?");
        confirm.setContentText("A sessão atual será encerrada e você retornará à tela de login.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                performLogoutToLogin();
            }
        });
    }

    private void performLogoutWithConfirmation() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Encerrar Sessão");
        confirm.setHeaderText("Deseja encerrar a sessão?");
        confirm.setContentText("Você será desconectado e retornará à tela de login.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                performLogoutToLogin();
            }
        });
    }

    private void performLogoutToLogin() {
        try {
            MainController c = (MainController) getScene().getUserData();
            if (c != null) {
                c.performLogoutAndShowLogin();
            } else {
                session.logout();
                AlertUtils.showInfoAlert("Logout", "Sessão encerrada.");
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Erro ao encerrar sessão: " + ex.getMessage(), ex);
        }
    }

    public void addNotification(String text) {
        notifications.add(text);
        updateBadge();
    }
}
