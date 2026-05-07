package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.EmpresaService;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.function.Supplier;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.ScrollPane;

public class MainView extends BorderPane {
    
    private final StackPane overlay = new StackPane();
    private final TabPane pageTabs = new TabPane();
    private final Map<String, Tab> openTabs = new HashMap<>();
    private final Deque<TabInfo> recentlyClosed = new ArrayDeque<>();
    private MainController controllerRef;
    private SideNav sideRef;
    private NavigationHeader headerRef;
    private final EmpresaService empresaService;
    private Label footerClock;
    private Timeline footerTimeline;
    private Label footerCaixa;
    private Timeline caixaTimeline;
    
    public MainView(MainController controller, SessionManager sessionManager, EmpresaService empresaService) {
        this.empresaService = empresaService;
        SideNav side = new SideNav(controller, sessionManager);
        side.getStyleClass().add("app-sidenav");
        this.sideRef = side;
        NavigationHeader header = new NavigationHeader(side, this, sessionManager, empresaService);
        header.getStyleClass().add("app-header");
        this.headerRef = header;
        setLeft(side);
        setTop(header);
        super.setCenter(overlay);
        overlay.getStyleClass().add("app-center");
        overlay.getChildren().add(pageTabs);
        setBottom(buildStatusBar(sessionManager, empresaService));

        widthProperty().addListener((obs, oldW, newW) -> applyResponsiveClasses(side, newW.doubleValue()));
        sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                n.widthProperty().addListener((o2, ov, nv) -> applyResponsiveClasses(side, nv.doubleValue()));
                // External CSS removed
                n.setUserData(controllerRef);
            }
        });
        applyResponsiveClasses(side, getWidth());

        controllerSet(controller);
        controller.setNotificationSink(header::addNotification);
        configureTabs();
        validateLayout();
    }
    
    public void rebuildNavigation(SessionManager sessionManager) {
        SideNav side = new SideNav(controllerRef, sessionManager);
        side.getStyleClass().add("app-sidenav");
        setLeft(side);
        this.sideRef = side;
        NavigationHeader header = new NavigationHeader(side, this, sessionManager, empresaService);
        header.getStyleClass().add("app-header");
        setTop(header);
        this.headerRef = header;
        applyResponsiveClasses(side, getWidth());
    }
    
    public void setCenterContent(Node node) {
        openOrFocusTab("single", "Conteúdo", () -> node);
    }

    public void setCenterContent(Node node, boolean scrollable) {
        openOrFocusTab("single", "Conteúdo", () -> node, scrollable);
    }

    public void replaceCurrentContent(Supplier<? extends Node> supplier) {
        Tab current = pageTabs.getSelectionModel().getSelectedItem();
        if (current == null) return;
        Node next = wrapScrollable(supplier.get());
        Node prev = current.getContent();
        current.setContent(next);
        if (next != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(140), unwrapIfWrapped(next));
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
        if (prev != null) prev.setManaged(false);
    }

    public void openOrFocusTab(String id, String title, Supplier<? extends Node> contentSupplier) {
        openOrFocusTab(id, title, contentSupplier, true);
    }

    public void openOrFocusTab(String id, String title, Supplier<? extends Node> contentSupplier, boolean scrollable) {
        if (openTabs.containsKey(id)) {
            pageTabs.getSelectionModel().select(openTabs.get(id));
            return;
        }
        Node raw = contentSupplier.get();
        Node content = scrollable ? wrapScrollable(raw) : raw;
        Tab tab = new Tab(title, content);
        tab.setClosable(true);
        tab.setOnClosed(e -> {
            openTabs.remove(id);
            recentlyClosed.push(new TabInfo(id, title, contentSupplier));
        });
        pageTabs.getTabs().add(tab);
        openTabs.put(id, tab);
        pageTabs.getSelectionModel().select(tab);
    }

    private Node wrapScrollable(Node node) {
        if (node == null) return null;
        if (node instanceof ScrollPane) return node;

        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private Node unwrapIfWrapped(Node node) {
        if (node instanceof ScrollPane sp) {
            Node c = sp.getContent();
            return c != null ? c : node;
        }
        return node;
    }

    public boolean hasRecentlyClosed() {
        return !recentlyClosed.isEmpty();
    }

    public void reopenLastClosed() {
        if (recentlyClosed.isEmpty()) return;
        TabInfo info = recentlyClosed.pop();
        openOrFocusTab(info.id, info.title, info.supplier);
    }

    public void clearRecentlyClosed() {
        recentlyClosed.clear();
    }

    private void configureTabs() {
        pageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        pageTabs.getStyleClass().add(atlantafx.base.theme.Styles.TABS_FLOATING);
        pageTabs.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null && nv.getContent() != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(140), nv.getContent());
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();
            }
        });
    }
    
    private HBox buildStatusBar(SessionManager sessionManager, EmpresaService empresaService) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 10, 4, 10));
        bar.getStyleClass().add("app-footer");
        
        ao.allon.kubata.faturacao.domain.Empresa emp = null;
        try { emp = empresaService.getDadosEmpresa(); } catch (Exception ignored) {}
        String empresaNome = emp != null && emp.getNome() != null ? emp.getNome() : "Empresa";
        String empresaNif = emp != null && emp.getNif() != null ? emp.getNif() : "";
        String usr = sessionManager.getCurrentUser();
        String role = (sessionManager.getUserObject() != null && sessionManager.getUserObject().getRole() != null)
                ? sessionManager.getUserObject().getRole().name()
                : "";
        Label left = new Label("Pronto  •  Utilizador: " + usr + (role.isBlank() ? "" : " (" + role + ")") + "  •  Empresa: " + empresaNome + (empresaNif.isBlank() ? "" : "  •  NIF " + empresaNif));
        left.getStyleClass().add(Styles.TEXT_MUTED);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        footerClock = new Label();
        footerClock.getStyleClass().add(Styles.TEXT_MUTED);
        updateClock();
        startClock();
        footerCaixa = new Label();
        footerCaixa.getStyleClass().add(Styles.TEXT_MUTED);
        updateCaixaStatus();
        startCaixaMonitor();
        
        bar.getChildren().addAll(left, spacer, footerCaixa, new Label("  •  "), footerClock);
        return bar;
    }

    private void updateClock() {
        String dt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .format(java.time.LocalDateTime.now());
        if (footerClock != null) footerClock.setText(dt);
    }

    private void startClock() {
        if (footerTimeline != null) {
            footerTimeline.stop();
        }
        footerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        footerTimeline.setCycleCount(Timeline.INDEFINITE);
        footerTimeline.play();
    }

    private void updateCaixaStatus() {
        try {
            Object o = getScene() != null ? getScene().getUserData() : null;
            if (o instanceof ao.allon.kubata.faturacao.controller.MainController c) {
                String s = c.getCaixaStatusTexto();
                if (footerCaixa != null) footerCaixa.setText("Caixa: " + s);
            }
        } catch (Exception ignored) {}
    }

    private void startCaixaMonitor() {
        if (caixaTimeline != null) caixaTimeline.stop();
        caixaTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateCaixaStatus()));
        caixaTimeline.setCycleCount(Timeline.INDEFINITE);
        caixaTimeline.play();
    }

    private void applyResponsiveClasses(SideNav side, double width) {
        if (width < 1100) {
            side.setCollapsed(true);
        } else {
            side.setCollapsed(false);
        }
    }

    private void controllerSet(MainController controller) {
        controller.setMainView(this);
        this.controllerRef = controller;
    }

    private void validateLayout() {
        if (getTop() == null || getLeft() == null || getCenter() == null || getBottom() == null) {
            throw new IllegalStateException("Layout incompleto: TOP/LEFT/CENTER/BOTTOM devem estar definidos.");
        }
        overlay.setPadding(Insets.EMPTY);
        pageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    public StackPane getRootPane() {
        return overlay;
    }

    private record TabInfo(String id, String title, Supplier<? extends Node> supplier) {}
}
