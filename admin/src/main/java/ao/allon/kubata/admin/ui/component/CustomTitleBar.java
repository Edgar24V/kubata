package ao.allon.kubata.admin.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.util.function.Consumer;

/**
 * Barra de título personalizada ao estilo Excel 365 Verde.
 */
public class CustomTitleBar extends HBox {

    private final Stage stage;
    private final TextField searchField;
    private final Label userLabel;
    private final Label userAvatar;

    // Para arrastar a janela
    private double dragOffsetX;
    private double dragOffsetY;

    private Consumer<String> onSearchCallback;

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
        searchField.setPromptText("Pesquisar na aplicação...");
        searchField.getStyleClass().add("title-bar-search-box");

        // Ícone de pesquisa (sobreposição visual)
        FontIcon searchIcon = new FontIcon(MaterialDesignM.MAGNIFY);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));
        searchIcon.setMouseTransparent(true);

        // Container do campo de pesquisa com ícone
        StackPane searchContainer = new StackPane();
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.getChildren().addAll(searchField);

        // Posicionamento do ícone dentro do campo
        searchIcon.setTranslateX(10);
        searchContainer.getChildren().add(searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);

        // Acção ao pressionar Enter
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                triggerSearch(searchField.getText().trim());
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

        HBox userBox = new HBox(6, userLabel, userAvatar);
        userBox.setAlignment(Pos.CENTER);
        userBox.setPadding(new Insets(0, 8, 0, 8));

        // ── Botões de controlo da janela ────────────────────
        Button minimizeBtn = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_MINIMIZE), "Minimizar", false);
        Button maximizeBtn = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_MAXIMIZE), "Maximizar / Restaurar", false);
        Button closeBtn    = createWindowButton(new FontIcon(MaterialDesignW.WINDOW_CLOSE), "Fechar", true);

        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        maximizeBtn.setOnAction(e -> {
            stage.setMaximized(!stage.isMaximized());
            maximizeBtn.setGraphic(stage.isMaximized() 
                ? new FontIcon(MaterialDesignW.WINDOW_RESTORE) 
                : new FontIcon(MaterialDesignW.WINDOW_MAXIMIZE));
        });
        closeBtn.setOnAction(e -> stage.close());

        // ── Montagem ────────────────────────────────────────
        getChildren().addAll(
            iconContainer,
            nameLabel,
            leftSpacer,
            searchContainer,
            rightSpacer,
            userBox,
            minimizeBtn,
            maximizeBtn,
            closeBtn
        );

        // ── Arrastar a janela ───────────────────────────────
        setupDragging();

        // ── Duplo-clique para maximizar ─────────────────────
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isOnControlButton(e.getX())) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    // ── API pública ─────────────────────────────────────────

    public void setOnSearch(Consumer<String> callback) {
        this.onSearchCallback = callback;
    }

    public void setUserName(String name) {
        userLabel.setText(name);
        userLabel.setVisible(true);
        userLabel.setManaged(true);

        String[] parts = name.trim().split("\\s+");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
            : name.length() > 0 ? String.valueOf(name.charAt(0)) : "U";
        userAvatar.setText(initials.toUpperCase());
    }

    public void clearSearch() {
        searchField.clear();
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
        btn.setFocusTraversable(false);
        return btn;
    }

    private void setupDragging() {
        setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });

        setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
    }

    private boolean isOnControlButton(double x) {
        double w = getWidth();
        return x > w - 138;
    }
}
