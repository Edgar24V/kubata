package ao.allon.kubata.admin.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.util.List;
import java.util.function.Consumer;

/**
 * Barra de título personalizada ao estilo Excel 365 Verde.
 */
public class CustomTitleBar extends HBox {

    private final Stage stage;
    private final TextField searchField;
    private final StackPane searchContainer;
    private final Label userLabel;
    private final Label userAvatar;
    private final MenuButton profileMenu;
    private final ContextMenu searchResultsPopup = new ContextMenu();

    // Para arrastar a janela
    private double dragOffsetX;
    private double dragOffsetY;

    private Consumer<String> onSearchCallback;
    private Runnable onUserInfoCallback;
    private Runnable onLogoutCallback;
    private Runnable onWindowCloseCallback;

    public CustomTitleBar(Stage stage, String appTitle) {
        this.stage = stage;

        getStyleClass().add("custom-title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(new Insets(0, 0, 0, 0));

        // ── Ícone da aplicação ──────────────────────────────
        FontIcon appIcon = new FontIcon(MaterialDesignF.FILE_DOCUMENT_OUTLINE);
        appIcon.setIconSize(20);
        appIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        appIcon.getStyleClass().add("title-bar-app-icon");
        
        StackPane iconContainer = new StackPane(appIcon);
        iconContainer.setPadding(new Insets(0, 6, 0, 12));

        // ── Nome da aplicação ───────────────────────────────
        Label nameLabel = new Label(appTitle);
        nameLabel.getStyleClass().add("title-bar-app-name");

        // ── Espaço flexível esquerdo ────────────────────────
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.SOMETIMES);
        leftSpacer.setMinWidth(24);

        // ── Campo de Pesquisa ───────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Pesquisar páginas e comandos...");
        searchField.getStyleClass().add("title-bar-search-box");
        searchField.setMaxWidth(Double.MAX_VALUE);

        // Ícone de pesquisa (sobreposição visual)
        FontIcon searchIcon = new FontIcon(MaterialDesignM.MAGNIFY);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));
        searchIcon.setMouseTransparent(true);

        // Container do campo de pesquisa com ícone
        searchContainer = new StackPane();
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setMinWidth(190);
        searchContainer.setPrefWidth(390);
        searchContainer.setMaxWidth(560);
        HBox.setHgrow(searchContainer, Priority.ALWAYS);
        searchContainer.getChildren().addAll(searchField);

        // Posicionamento do ícone dentro do campo
        searchIcon.setTranslateX(10);
        searchContainer.getChildren().add(searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);

        // Acção ao pressionar Enter
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                triggerSearch(searchField.getText().trim());
            } else if (e.getCode() == KeyCode.ESCAPE) {
                searchResultsPopup.hide();
                searchField.clear();
            }
        });

        // ── Espaço flexível direito ─────────────────────────
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);
        rightSpacer.setMinWidth(24);

        // ── Info do utilizador ──────────────────────────────
        userAvatar = new Label("U");
        userAvatar.getStyleClass().add("title-bar-user-avatar");
        userAvatar.setTooltip(new Tooltip("Perfil do utilizador"));

        userLabel = new Label("");
        userLabel.getStyleClass().add("title-bar-user-label");
        userLabel.setVisible(false);
        userLabel.setManaged(false);

        HBox userBox = new HBox(7, userLabel, userAvatar);
        userBox.setAlignment(Pos.CENTER);
        userBox.setPadding(new Insets(0, 2, 0, 2));

        profileMenu = new MenuButton();
        profileMenu.setGraphic(userBox);
        profileMenu.getStyleClass().add("title-bar-user-menu");
        profileMenu.setTooltip(new Tooltip("Perfil e sessão"));
        profileMenu.setPopupSide(Side.BOTTOM);
        profileMenu.setFocusTraversable(true);

        MenuItem sessionInfoItem = new MenuItem("Informações da sessão");
        sessionInfoItem.setOnAction(e -> {
            if (onUserInfoCallback != null) {
                onUserInfoCallback.run();
            }
        });

        MenuItem logoutItem = new MenuItem("Encerrar sessão");
        logoutItem.setOnAction(e -> {
            if (onLogoutCallback != null) {
                onLogoutCallback.run();
            }
        });

        profileMenu.getItems().addAll(
                sessionInfoItem,
                new SeparatorMenuItem(),
                logoutItem
        );

        // ── Botões de controlo da janela ────────────────────
        Button minimizeBtn = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_MINIMIZE), "Minimizar", false);
        Button maximizeBtn = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_MAXIMIZE), "Maximizar / Restaurar", false);
        Button closeBtn    = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_CLOSE), "Fechar", true);

        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        maximizeBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        stage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
            FontIcon icon = new FontIcon(
                    isMaximized ? MaterialDesignW.WINDOW_RESTORE : MaterialDesignW.WINDOW_MAXIMIZE
            );
            icon.setIconColor(javafx.scene.paint.Color.WHITE);
            maximizeBtn.setGraphic(icon);
            maximizeBtn.setTooltip(new Tooltip(isMaximized ? "Restaurar" : "Maximizar"));
        });
        if (stage.isMaximized()) {
            FontIcon restoreIcon = new FontIcon(MaterialDesignW.WINDOW_RESTORE);
            restoreIcon.setIconColor(javafx.scene.paint.Color.WHITE);
            maximizeBtn.setGraphic(restoreIcon);
            maximizeBtn.setTooltip(new Tooltip("Restaurar"));
        }
        closeBtn.setOnAction(e -> {
            if (onWindowCloseCallback != null) {
                onWindowCloseCallback.run();
            } else {
                stage.close();
            }
        });

        // ── Montagem ────────────────────────────────────────
        getChildren().addAll(
            iconContainer,
            nameLabel,
            leftSpacer,
            searchContainer,
            rightSpacer,
            profileMenu,
            minimizeBtn,
            maximizeBtn,
            closeBtn
        );

        // ── Arrastar a janela ───────────────────────────────
        setupDragging();

        // ── Duplo-clique para maximizar ─────────────────────
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && !isInteractiveTarget(e.getTarget())
                    && !isOnControlButton(e.getX())) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    // ── API pública ─────────────────────────────────────────

    public void setOnSearch(Consumer<String> callback) {
        this.onSearchCallback = callback;
    }

    public void setOnUserInfo(Runnable callback) {
        this.onUserInfoCallback = callback;
    }

    public void setOnLogout(Runnable callback) {
        this.onLogoutCallback = callback;
    }

    public void setOnWindowClose(Runnable callback) {
        this.onWindowCloseCallback = callback;
    }

    /**
     * Mostra opções de pesquisa ligadas a páginas ou comandos reais.
     * Os itens são criados pelo AdminMainView e cada um contém a sua acção.
     */
    public void showSearchResults(List<MenuItem> items) {
        if (searchResultsPopup.isShowing()) {
            searchResultsPopup.hide();
        }
        searchResultsPopup.getItems().setAll(items == null ? List.of() : items);
        if (searchResultsPopup.getItems().isEmpty()) {
            return;
        }
        if (searchContainer.getScene() != null && searchContainer.isVisible()) {
            searchResultsPopup.show(searchContainer, Side.BOTTOM, 0, 4);
        }
    }

    public void setUserName(String name) {
        String safeName = name == null || name.isBlank() ? "Utilizador" : name.trim();
        userLabel.setText(safeName);
        userLabel.setVisible(!"Utilizador".equals(safeName));
        userLabel.setManaged(userLabel.isVisible());

        String[] parts = safeName.split("\\s+");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                : safeName.length() > 0 ? String.valueOf(safeName.charAt(0)) : "U";
        userAvatar.setText(initials.toUpperCase(java.util.Locale.ROOT));
    }

    public void clearSearch() {
        searchField.clear();
        searchResultsPopup.hide();
    }

    public void setSearchPrompt(String prompt) {
        searchField.setPromptText(prompt);
    }

    // ── Privado ─────────────────────────────────────────────

    private void triggerSearch(String query) {
        if (onSearchCallback != null && !query.isEmpty()) {
            onSearchCallback.accept(query);
        }
    }

    private Button createWindowButton(FontIcon icon, String tooltip, boolean isClose) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add("button-icon");
        btn.getStyleClass().add(isClose ? "title-bar-close-btn" : "title-bar-control-btn");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setAccessibleText(tooltip);
        btn.setFocusTraversable(false);
        return btn;
    }

    private void setupDragging() {
        setOnMousePressed(e -> {
            if (isInteractiveTarget(e.getTarget())) {
                dragOffsetX = Double.NaN;
                dragOffsetY = Double.NaN;
                return;
            }
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        setOnMouseDragged(e -> {
            if (!stage.isMaximized()
                    && !Double.isNaN(dragOffsetX)
                    && !Double.isNaN(dragOffsetY)) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
    }

    private boolean isInteractiveTarget(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null && node != this) {
            if (node instanceof ButtonBase || node instanceof TextInputControl) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private boolean isOnControlButton(double x) {
        double w = getWidth();
        return x > w - 138;
    }
}
