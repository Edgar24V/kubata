package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.component.CustomTitleBar;
import ao.allon.kubata.admin.ui.fxribbon.Ribbon;
import ao.allon.kubata.admin.ui.fxribbon.RibbonProgrammaticService;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class AdminMainView extends StackPane {

    private final ApplicationContext applicationContext;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final NotificationService notificationService;
    private final RibbonProgrammaticService ribbonService;

    private final BorderPane layout = new BorderPane();
    private final Ribbon ribbon = new Ribbon();
    private final TabPane tabs = new TabPane();
    private CustomTitleBar titleBar;

    public AdminMainView(ApplicationContext applicationContext,
                        SessionManager sessionManager,
                        ModalManager modalManager,
                        NotificationService notificationService,
                        RibbonProgrammaticService ribbonService) {
        this.applicationContext = applicationContext;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.notificationService = notificationService;
        this.ribbonService = ribbonService;
    }

    public void init(Stage stage) {
        this.titleBar = new CustomTitleBar(stage, "Kubata Administrator");
        if (sessionManager.getUser() != null) {
            titleBar.setUserName(sessionManager.getUser().getNome());
        }
        buildUI();
        buildRibbon();
    }

    // ── Construção da UI ────────────────────────────────────────────────────────

    private void buildUI() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getStyleClass().add("office365-tabs");

        // Sincronizar seleção do Ribbon com a tab ativa
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                ribbon.highlightButton(newTab.getId());
            }
        });

        VBox topArea = new VBox(0, titleBar, ribbon);
        layout.setTop(topArea);
        layout.setCenter(tabs);
        layout.setBottom(buildStatusBar());

        getChildren().setAll(layout);
        modalManager.setRoot(this);
        notificationService.setRoot(this);

        // Tab inicial sempre aberta e não-fechável
        openOrFocusTab("console", "Consola", () -> applicationContext.getBean(ConsoleView.class), false);
    }

    private Node buildStatusBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("status-bar");

        Label userLbl = new Label();
        userLbl.getStyleClass().add("status-bar-label");
        userLbl.setText(sessionManager.getUser() != null
                ? "Utilizador: " + sessionManager.getUser().getNome()
                : "Utilizador: (não autenticado)");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label version = new Label("Kubata Administrator v1.0",
                IconUtils.icon(Feather.INFO, IconUtils.SIZE_SMALL));
        version.getStyleClass().add("status-bar-label");

        bar.getChildren().addAll(userLbl, spacer, version);
        return bar;
    }

    private void buildRibbon() {
        ribbonService.buildRibbon(
                ribbon,
                sessionManager.getUser(),
                this::openOrFocusTab,
                beanClass -> applicationContext.getBean(beanClass)
        );
    }

    // ── Tab helpers ─────────────────────────────────────────────────────────────

    public void openOrFocusTab(String id, String title,
                                Supplier<? extends Node> contentSupplier,
                                boolean closable) {
        Tab existing = tabs.getTabs().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst().orElse(null);

        if (existing != null) {
            tabs.getSelectionModel().select(existing);
            return;
        }

        Tab t = new Tab(title);
        t.setId(id);
        t.setClosable(closable);
        t.setContent(contentSupplier.get());
        tabs.getTabs().add(t);
        tabs.getSelectionModel().select(t);
    }
}
