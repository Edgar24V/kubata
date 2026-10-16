package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.component.FaturacaoTitleBar;
import ao.allon.kubata.faturacao.ui.fxribbon.Ribbon;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonButton;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonGroup;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonTab;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shell principal do Faturação alinhado com o Kubata Administrator:
 * barra de título, Ribbon de comandos, separadores centrais e barra de estado.
 * Os ecrãs e serviços de negócio continuam a ser os do módulo Faturação.
 */
public class MainView extends BorderPane {

    private final StackPane overlay = new StackPane();
    private final TabPane pageTabs = new TabPane();
    private final Map<String, Tab> openTabs = new HashMap<>();
    private final Deque<TabInfo> recentlyClosed = new ArrayDeque<>();
    private final MainController controllerRef;
    private final SessionManager sessionManager;
    private final EmpresaService empresaService;

    private final VBox topArea = new VBox(0);
    private FaturacaoTitleBar titleBar;
    private Ribbon ribbon;

    private Label sessionUserLabel;
    private Label activePageLabel;
    private Label openTabsLabel;
    private Label companyLabel;
    private Label footerClock;
    private Label footerCaixa;
    private Timeline footerTimeline;
    private Timeline caixaTimeline;

    public MainView(MainController controller, SessionManager sessionManager,
                    EmpresaService empresaService, Stage stage) {
        this.controllerRef = controller;
        this.sessionManager = sessionManager;
        this.empresaService = empresaService;

        getStyleClass().addAll("kubata-admin-shell", "main-window");
        setPadding(Insets.EMPTY);

        pageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        pageTabs.getStyleClass().addAll("office365-tabs", "kubata-main-tabpane");
        pageTabs.setMinWidth(0);
        pageTabs.setMinHeight(0);
        pageTabs.setMaxWidth(Double.MAX_VALUE);
        pageTabs.setMaxHeight(Double.MAX_VALUE);

        pageTabs.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth == null ? 0 : newWidth.doubleValue();
            pageTabs.getStyleClass().removeAll("kubata-tabs-compact", "kubata-tabs-small");
            if (width > 0 && width < 760) {
                pageTabs.getStyleClass().add("kubata-tabs-small");
            } else if (width > 0 && width < 1050) {
                pageTabs.getStyleClass().add("kubata-tabs-compact");
            }
        });

        overlay.getStyleClass().add("content-area");
        overlay.setPadding(Insets.EMPTY);
        overlay.getChildren().add(pageTabs);

        titleBar = new FaturacaoTitleBar(
                stage, "Kubata Faturação", controller, sessionManager, this
        );
        titleBar.setSearchPrompt("Pesquisar páginas, módulos e comandos...");
        titleBar.setOnSearch(this::performGlobalSearch);

        ribbon = new Ribbon();
        new FaturacaoRibbonBuilder(controller, sessionManager).build(ribbon);
        topArea.getStyleClass().add("kubata-main-top-area");
        topArea.getChildren().setAll(titleBar, ribbon);

        setTop(topArea);
        setCenter(overlay);
        setBottom(buildStatusBar());

        controller.setMainView(this);
        controller.setNotificationSink(titleBar::addNotification);
        configureTabs();

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setUserData(controllerRef);
            }
        });
        validateLayout();
    }

    /**
     * Compatibilidade para chamadas antigas do módulo; a aplicação principal
     * deve usar o construtor que recebe o Stage para mostrar a barra de título.
     */
    public MainView(MainController controller, SessionManager sessionManager,
                    EmpresaService empresaService) {
        this(controller, sessionManager, empresaService, null);
    }

    public void rebuildNavigation(SessionManager currentSession) {
        ribbon = new Ribbon();
        new FaturacaoRibbonBuilder(controllerRef, currentSession).build(ribbon);
        topArea.getChildren().set(1, ribbon);
        if (titleBar != null && currentSession.getUserObject() != null) {
            titleBar.setUserName(currentSession.getUserObject().getNome());
        }
        updateStatusBar();
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
        Node previous = current.getContent();
        current.setContent(next);
        if (next != null) {
            Node animationTarget = unwrapIfWrapped(next);
            FadeTransition transition = new FadeTransition(Duration.millis(140), animationTarget);
            transition.setFromValue(0.0);
            transition.setToValue(1.0);
            transition.play();
        }
        if (previous != null) previous.setManaged(false);
    }

    public void openOrFocusTab(String id, String title, Supplier<? extends Node> contentSupplier) {
        openOrFocusTab(id, title, contentSupplier, true);
    }

    public void openOrFocusTab(String id, String title,
                               Supplier<? extends Node> contentSupplier, boolean scrollable) {
        if (openTabs.containsKey(id)) {
            pageTabs.getSelectionModel().select(openTabs.get(id));
            return;
        }

        Node raw = contentSupplier.get();
        Node content = scrollable ? wrapScrollable(raw) : raw;
        Tab tab = new Tab(title, content);
        tab.setId(id);
        tab.setClosable(true);
        tab.setOnClosed(e -> {
            openTabs.remove(id);
            recentlyClosed.push(new TabInfo(id, title, contentSupplier));
            updateStatusBar();
        });
        pageTabs.getTabs().add(tab);
        openTabs.put(id, tab);
        pageTabs.getSelectionModel().select(tab);
        updateStatusBar();
    }

    private Node wrapScrollable(Node node) {
        if (node == null || node instanceof ScrollPane) return node;
        ScrollPane pane = new ScrollPane(node);
        pane.setFitToWidth(true);
        pane.setFitToHeight(false);
        pane.setPannable(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return pane;
    }

    private Node unwrapIfWrapped(Node node) {
        if (node instanceof ScrollPane pane) {
            Node child = pane.getContent();
            return child == null ? node : child;
        }
        return node;
    }

    public boolean hasRecentlyClosed() {
        return !recentlyClosed.isEmpty();
    }

    public void reopenLastClosed() {
        if (recentlyClosed.isEmpty()) return;
        TabInfo info = recentlyClosed.pop();
        openOrFocusTab(info.id(), info.title(), info.supplier());
    }

    public void clearRecentlyClosed() {
        recentlyClosed.clear();
    }

    public StackPane getRootPane() {
        return overlay;
    }

    /** Confirma a saída e só fecha após encerrar a sessão persistida. */
    public void confirmApplicationClose() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Fechar Kubata Faturação");
        confirmation.setHeaderText("Deseja fechar o Kubata Faturação?");
        confirmation.setContentText("A sessão será encerrada na base de dados antes de a aplicação sair.");
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                controllerRef.closeApplication();
            }
        });
    }

    /** Confirma antes de terminar a sessão, usado pelo cabeçalho e rodapé. */
    public void confirmLogout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Encerrar sessão");
        confirmation.setHeaderText("Deseja encerrar a sessão?");
        confirmation.setContentText("A sessão actual será terminada e será apresentada a tela de login.");
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                controllerRef.performLogoutAndShowLogin();
            }
        });
    }

    public void addNotification(String message) {
        if (titleBar != null) titleBar.addNotification(message);
    }

    private void configureTabs() {
        pageTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) ribbon.highlightButton(newTab.getId());
            updateStatusBar();
        });
        pageTabs.getTabs().addListener((ListChangeListener<Tab>) change -> updateStatusBar());
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(9);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 12, 4, 12));
        bar.setMinHeight(32);
        bar.setPrefHeight(32);
        bar.setMaxHeight(32);

        Label sessionDot = new Label("●");
        sessionDot.getStyleClass().add("status-bar-dot");
        sessionDot.setTooltip(new Tooltip("Estado da sessão actual"));

        Label sessionState = new Label("Sessão autenticada");
        sessionState.getStyleClass().add("status-bar-session");

        sessionUserLabel = new Label();
        sessionUserLabel.getStyleClass().add("status-bar-user");
        sessionUserLabel.setMinWidth(0);
        sessionUserLabel.setMaxWidth(210);
        sessionUserLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        sessionUserLabel.setTooltip(new Tooltip("Utilizador e perfil em utilização"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        activePageLabel = new Label("Área: —");
        activePageLabel.getStyleClass().add("status-bar-active-page");
        activePageLabel.setMinWidth(0);
        activePageLabel.setMaxWidth(165);
        activePageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        openTabsLabel = new Label("Separadores: 0");
        openTabsLabel.getStyleClass().add("status-bar-meta");

        companyLabel = new Label();
        companyLabel.getStyleClass().add("status-bar-meta");
        companyLabel.setMinWidth(0);
        companyLabel.setMaxWidth(190);
        companyLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        footerCaixa = new Label();
        footerCaixa.getStyleClass().add("status-bar-meta");
        footerClock = new Label();
        footerClock.getStyleClass().add("status-bar-meta");

        Button help = new Button("Ajuda", ao.allon.kubata.faturacao.ui.util.IconUtils.icon(
                org.kordamp.ikonli.feather.Feather.HELP_CIRCLE,
                ao.allon.kubata.faturacao.ui.util.IconUtils.SIZE_SMALL));
        help.getStyleClass().add("status-bar-action");
        help.setTooltip(new Tooltip("Ajuda e atalhos de utilização"));
        help.setOnAction(e -> showHelp());

        Button logout = new Button("Encerrar sessão", ao.allon.kubata.faturacao.ui.util.IconUtils.icon(
                org.kordamp.ikonli.feather.Feather.LOG_OUT,
                ao.allon.kubata.faturacao.ui.util.IconUtils.SIZE_SMALL));
        logout.getStyleClass().addAll("status-bar-action", "status-bar-logout");
        logout.setTooltip(new Tooltip("Terminar a sessão e voltar ao login"));
        logout.setOnAction(e -> confirmLogout());

        bar.getChildren().addAll(sessionDot, sessionState, divider(), sessionUserLabel,
                spacer, activePageLabel, divider(), openTabsLabel, companyLabel,
                footerCaixa, footerClock, help, logout);
        updateStatusBar();
        updateCompanyLabel();
        updateClock();
        updateCaixaStatus();
        startClock();
        startCaixaMonitor();
        return bar;
    }

    private Separator divider() {
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPrefHeight(16);
        separator.setMinHeight(16);
        separator.setMaxHeight(16);
        separator.getStyleClass().add("status-bar-divider");
        return separator;
    }

    private void updateStatusBar() {
        if (sessionUserLabel == null) return;
        var user = sessionManager.getUserObject();
        if (user == null) {
            sessionUserLabel.setText("Utilizador não autenticado");
        } else {
            String role = user.getRole() == null ? "Perfil não definido" : user.getRole().name();
            sessionUserLabel.setText(user.getNome() + " · " + role);
        }
        Tab active = pageTabs.getSelectionModel().getSelectedItem();
        if (activePageLabel != null) {
            activePageLabel.setText(active == null ? "Área: —" : "Área: " + active.getText());
        }
        if (openTabsLabel != null) {
            openTabsLabel.setText("Separadores: " + pageTabs.getTabs().size());
        }
    }

    private void updateCompanyLabel() {
        try {
            Empresa empresa = empresaService.getDadosEmpresa();
            if (companyLabel != null) {
                String nome = empresa == null || empresa.getNome() == null || empresa.getNome().isBlank()
                        ? "Empresa não definida" : empresa.getNome();
                String nif = empresa == null || empresa.getNif() == null || empresa.getNif().isBlank()
                        ? "" : " · NIF " + empresa.getNif();
                companyLabel.setText(nome + nif);
                companyLabel.setTooltip(new Tooltip(nome + nif));
            }
        } catch (Exception ex) {
            if (companyLabel != null) companyLabel.setText("Empresa indisponível");
        }
    }

    private void updateClock() {
        if (footerClock == null) return;
        footerClock.setText(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()));
    }

    private void startClock() {
        if (footerTimeline != null) footerTimeline.stop();
        footerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        footerTimeline.setCycleCount(Timeline.INDEFINITE);
        footerTimeline.play();
    }

    private void updateCaixaStatus() {
        try {
            MainController controller = getScene() != null && getScene().getUserData() instanceof MainController c
                    ? c : controllerRef;
            if (footerCaixa != null && controller != null) {
                footerCaixa.setText(controller.getCaixaStatusTexto());
            }
        } catch (Exception ignored) {
            if (footerCaixa != null) footerCaixa.setText("Caixa indisponível");
        }
    }

    private void startCaixaMonitor() {
        if (caixaTimeline != null) caixaTimeline.stop();
        caixaTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateCaixaStatus()));
        caixaTimeline.setCycleCount(Timeline.INDEFINITE);
        caixaTimeline.play();
    }

    private void showHelp() {
        Alert help = new Alert(Alert.AlertType.INFORMATION);
        help.setTitle("Ajuda do Kubata Faturação");
        help.setHeaderText("Navegação e atalhos");
        help.setContentText("Use o Ribbon para abrir as áreas de trabalho. "
                + "Pode manter vários separadores abertos, fechar separadores e reabrir o último "
                + "através do histórico no cabeçalho. A pesquisa encontra páginas e comandos disponíveis.");
        help.showAndWait();
    }

    private void performGlobalSearch(String query) {
        String normalized = normalizeSearchText(query);
        if (normalized.isBlank()) return;

        List<MenuItem> results = new ArrayList<>();
        for (Tab openTab : pageTabs.getTabs()) {
            if (matches(openTab.getText(), normalized) || matches(openTab.getId(), normalized)) {
                MenuItem item = new MenuItem("Página aberta · " + openTab.getText());
                item.setOnAction(e -> pageTabs.getSelectionModel().select(openTab));
                results.add(item);
            }
        }

        for (RibbonTab ribbonTab : ribbon.getTabs()) {
            if (matches(ribbonTab.getTitle(), normalized)) {
                MenuItem item = new MenuItem("Separador · " + ribbonTab.getTitle());
                item.setOnAction(e -> ribbon.selectTabByTitle(ribbonTab.getTitle()));
                results.add(item);
            }
        }

        commandSearch:
        for (RibbonTab ribbonTab : ribbon.getTabs()) {
            for (RibbonGroup group : ribbonTab.getGroups()) {
                for (Node columnNode : group.getContentBox().getChildren()) {
                    if (!(columnNode instanceof VBox column)) continue;
                    for (Node commandNode : column.getChildren()) {
                        if (!(commandNode instanceof RibbonButton button)) continue;
                        String label = extractVisibleText(button.getGraphic());
                        if (label.isBlank()) label = button.getId() == null ? "Comando" : button.getId();
                        String tooltip = button.getTooltip() == null ? "" : button.getTooltip().getText();
                        if (!matches(label, normalized) && !matches(tooltip, normalized)
                                && !matches(button.getId(), normalized)) continue;

                        MenuItem item = new MenuItem(label + " · " + ribbonTab.getTitle()
                                + " / " + group.getGroupTitle());
                        RibbonButton selectedButton = button;
                        RibbonTab selectedTab = ribbonTab;
                        item.setOnAction(e -> {
                            ribbon.selectTabByTitle(selectedTab.getTitle());
                            selectedButton.fire();
                        });
                        results.add(item);
                        if (results.size() >= 12) break commandSearch;
                    }
                }
            }
        }

        if (results.isEmpty()) {
            MenuItem empty = new MenuItem("Nenhuma página ou comando encontrado");
            empty.setDisable(true);
            results.add(empty);
        }
        titleBar.showSearchResults(results);
    }

    private boolean matches(String text, String normalizedQuery) {
        return text != null && !text.isBlank() && normalizeSearchText(text).contains(normalizedQuery);
    }

    private String normalizeSearchText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String extractVisibleText(Node node) {
        if (node == null) return "";
        if (node instanceof Label label) return label.getText() == null ? "" : label.getText();
        if (node instanceof Parent parent) {
            StringBuilder result = new StringBuilder();
            for (Node child : parent.getChildrenUnmodifiable()) {
                String text = extractVisibleText(child);
                if (!text.isBlank()) {
                    if (result.length() > 0) result.append(' ');
                    result.append(text);
                }
            }
            return result.toString().trim();
        }
        return "";
    }

    private void validateLayout() {
        if (getTop() == null || getCenter() == null || getBottom() == null) {
            throw new IllegalStateException("Layout incompleto: TOP/CENTER/BOTTOM devem estar definidos.");
        }
        pageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    private record TabInfo(String id, String title, Supplier<? extends Node> supplier) { }
}
