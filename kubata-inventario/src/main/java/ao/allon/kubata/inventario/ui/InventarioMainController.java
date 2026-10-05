package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.inventario.ui.event.LoginSuccessEvent;
import ao.allon.kubata.inventario.ui.modal.ModalManager;
import ao.allon.kubata.inventario.ui.views.*;
import com.pixelduke.control.Ribbon;
import com.pixelduke.control.ribbon.RibbonGroup;
import com.pixelduke.control.ribbon.RibbonTab;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InventarioMainController extends StackPane {
    private final ApplicationContext context;
    private final LoginController loginController;
    private final AuthService authService;
    private final ModalManager modalManager;

    private Stage stage;
    private User currentUser;
    private TabPane tabs;
    private Label status;

    public InventarioMainController(ApplicationContext context,
                                    LoginController loginController,
                                    AuthService authService,
                                    ModalManager modalManager) {
        this.context = context;
        this.loginController = loginController;
        this.authService = authService;
        this.modalManager = modalManager;
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        stage = event.getStage();
        showLogin();
    }

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        currentUser = event.getUser();
        showMain();
    }

    private void showLogin() {
        StackPane login = loginController.createView(stage);
        Scene scene = new Scene(login, 1120, 720);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Kubata • Gestão de Inventário");
        stage.setMinWidth(920);
        stage.setMinHeight(600);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
        stage.setOnCloseRequest(e -> {
            loginController.shutdown();
            stage.close();
        });
    }

    private void showMain() {
        buildMain();
        Scene scene = new Scene(this, 1280, 820);
        scene.getStylesheets().add(getClass()
                .getResource("/ao/allon/kubata/inventario/ui/inventario.css")
                .toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Kubata • Gestão de Inventário");
        stage.setMinWidth(1080);
        stage.setMinHeight(680);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> {
            e.consume();
            logoutAndExit();
        });
        stage.show();
    }

    private void buildMain() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("inventario-shell");

        HBox titlebar = new HBox(12);
        titlebar.getStyleClass().add("module-titlebar");
        titlebar.setAlignment(Pos.CENTER_LEFT);
        titlebar.setPadding(new Insets(9, 16, 9, 16));

        FontIcon icon = new FontIcon(Feather.ARCHIVE);
        icon.getStyleClass().add("title-icon");
        Label title = new Label("Kubata Inventário");
        title.getStyleClass().add("shell-title");
        Label user = new Label(currentUser == null ? "" : currentUser.getNome());
        user.getStyleClass().add("shell-user");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button sair = new Button("Encerrar sessão", new FontIcon(Feather.LOG_OUT));
        sair.setOnAction(e -> logoutAndExit());
        titlebar.getChildren().addAll(icon, title, user, spacer, sair);

        root.setTop(new VBox(titlebar, createRibbon()));

        tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getStyleClass().add("office365-tabs");
        root.setCenter(tabs);

        HBox bottom = new HBox();
        bottom.getStyleClass().add("status-bar");
        bottom.setPadding(new Insets(6, 14, 6, 14));
        status = new Label("Sessão ativa • Inventário");
        Label version = new Label("Kubata Inventário 1.0.0");
        Region push = new Region();
        HBox.setHgrow(push, Priority.ALWAYS);
        bottom.getChildren().addAll(status, push, version);
        root.setBottom(bottom);

        getChildren().setAll(root);
        open("inicio", "Visão Geral", InventarioDashboardView.class, false);
    }

    private Ribbon createRibbon() {
        Ribbon ribbon = new Ribbon();

        RibbonTab artigos = new RibbonTab("Artigos");
        RibbonGroup cadastro = new RibbonGroup();
        cadastro.setTitle("Cadastro");
        cadastro.getNodes().add(button("Artigos", Feather.BOX,
                () -> open("produtos", "Artigos", ProdutosView.class, true)));
        cadastro.getNodes().add(button("Categorias", Feather.TAG,
                () -> open("categorias", "Categorias", CategoriasView.class, true)));
        artigos.getRibbonGroups().add(cadastro);

        RibbonTab stockTab = new RibbonTab("Stocks");
        RibbonGroup stock = new RibbonGroup();
        stock.setTitle("Gestão de Stock");
        stock.getNodes().add(button("Existências", Feather.DATABASE,
                () -> open("estoque", "Gestão de Stock", EstoqueView.class, true)));
        stock.getNodes().add(button("Armazéns", Feather.HOME,
                () -> open("armazens", "Armazéns", ArmazensView.class, true)));
        stockTab.getRibbonGroups().add(stock);

        RibbonTab sistema = new RibbonTab("Sistema");
        RibbonGroup actions = new RibbonGroup();
        actions.setTitle("Sessão");
        actions.getNodes().add(button("Atualizar", Feather.REFRESH_CW, this::refreshCurrent));
        actions.getNodes().add(button("Sair", Feather.LOG_OUT, this::logoutAndExit));
        sistema.getRibbonGroups().add(actions);

        ribbon.getTabs().addAll(artigos, stockTab, sistema);
        return ribbon;
    }

    private Button button(String text, Feather icon, Runnable action) {
        Button b = new Button(text);
        b.setContentDisplay(ContentDisplay.TOP);
        b.setGraphic(new FontIcon(icon));
        b.getStyleClass().add("ribbon-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void open(String id, String title, Class<? extends Node> type, boolean closable) {
        Tab existing = tabs.getTabs().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst().orElse(null);
        if (existing != null) {
            tabs.getSelectionModel().select(existing);
            return;
        }

        try {
            Node view = context.getBean(type);
            Tab tab = new Tab(title, view);
            tab.setId(id);
            tab.setClosable(closable);
            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);
        } catch (Exception ex) {
            modalManager.error(this, "Inventário",
                    "Não foi possível abrir " + title + ": " + ex.getMessage());
        }
    }

    private void refreshCurrent() {
        Tab selected = tabs == null ? null : tabs.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Node content = selected.getContent();
        if (content instanceof ProdutosView v) v.refreshData();
        else if (content instanceof CategoriasView v) v.refreshData();
        else if (content instanceof ArmazensView v) v.refreshData();
        else if (content instanceof EstoqueView v) v.refreshData();
    }

    private void logoutAndExit() {
        try {
            if (currentUser != null) {
                authService.logout(currentUser, currentUser.getSessionId(), "127.0.0.1");
            }
        } catch (Exception ignored) {
        } finally {
            currentUser = null;
            loginController.shutdown();
            stage.setOnCloseRequest(null);
            stage.close();
        }
    }
}
