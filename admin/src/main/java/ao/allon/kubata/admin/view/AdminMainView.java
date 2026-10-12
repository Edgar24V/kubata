package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.controller.MainController;
import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.component.CustomTitleBar;
import ao.allon.kubata.admin.ui.fxribbon.Ribbon;
import ao.allon.kubata.admin.ui.fxribbon.RibbonButton;
import ao.allon.kubata.admin.ui.fxribbon.RibbonGroup;
import ao.allon.kubata.admin.ui.fxribbon.RibbonProgrammaticService;
import ao.allon.kubata.admin.ui.fxribbon.RibbonTab;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.User;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Shell principal do Kubata Administrator.
 *
 * O cabeçalho fornece pesquisa global, perfil e controlos da janela.
 * O rodapé mantém informação dinâmica da sessão, página activa e separadores,
 * além dos atalhos de ajuda e encerramento de sessão.
 */
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

    private Label sessionUserLabel;
    private Label activePageLabel;
    private Label openTabsLabel;

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

    /**
     * A vista é singleton do Spring. Ao iniciar uma nova sessão reutilizamos
     * a estrutura existente e limpamos as tabs anteriores, sem duplicar o Ribbon.
     */
    public void init(Stage stage) {
        if (titleBar == null) {
            titleBar = new CustomTitleBar(stage, "Kubata Administrator");
            titleBar.setUserName(currentUserName());
            titleBar.setSearchPrompt("Pesquisar páginas, módulos e comandos...");
            titleBar.setOnSearch(this::performGlobalSearch);
            titleBar.setOnUserInfo(this::showSessionInfo);
            titleBar.setOnLogout(this::confirmLogout);

            buildUI();
            buildRibbon();
            Tab activeTab = tabs.getSelectionModel().getSelectedItem();
            if (activeTab != null) {
                ribbon.highlightButton(activeTab.getId());
            }
            updateStatusBar();
            return;
        }

        titleBar.setUserName(currentUserName());
        titleBar.clearSearch();
        tabs.getTabs().clear();
        openOrFocusTab(
                "console",
                "Consola",
                () -> applicationContext.getBean(ConsoleView.class),
                false
        );
        updateStatusBar();
    }

    // ── Construção do shell ───────────────────────────────────────────────────

    private void buildUI() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getStyleClass().addAll("office365-tabs", "kubata-main-tabpane");
        tabs.setMinWidth(0);
        tabs.setMinHeight(0);
        tabs.setMaxWidth(Double.MAX_VALUE);
        tabs.setMaxHeight(Double.MAX_VALUE);

        // Mantém as tabs legíveis sem deixar o cabeçalho invadir os comandos.
        tabs.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth == null ? 0 : newWidth.doubleValue();
            tabs.getStyleClass().removeAll(
                    "kubata-tabs-compact",
                    "kubata-tabs-small"
            );

            if (width > 0 && width < 760) {
                tabs.getStyleClass().add("kubata-tabs-small");
            } else if (width > 0 && width < 1050) {
                tabs.getStyleClass().add("kubata-tabs-compact");
            }
        });

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                ribbon.highlightButton(newTab.getId());
            }
            updateStatusBar();
        });
        tabs.getTabs().addListener((ListChangeListener<Tab>) change -> updateStatusBar());

        VBox topArea = new VBox(0, titleBar, ribbon);
        topArea.getStyleClass().add("kubata-main-top-area");
        layout.setTop(topArea);
        layout.setCenter(tabs);
        layout.setBottom(buildStatusBar());

        getStyleClass().add("kubata-admin-shell");
        getChildren().setAll(layout);
        modalManager.setRoot(this);
        notificationService.setRoot(this);

        openOrFocusTab(
                "console",
                "Consola",
                () -> applicationContext.getBean(ConsoleView.class),
                false
        );
    }

    private Node buildStatusBar() {
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
        sessionUserLabel.setMaxWidth(205);
        sessionUserLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        sessionUserLabel.setTooltip(new Tooltip("Utilizador e perfil em utilização"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        activePageLabel = new Label("Área: Consola");
        activePageLabel.getStyleClass().add("status-bar-active-page");
        activePageLabel.setMinWidth(0);
        activePageLabel.setMaxWidth(165);
        activePageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        openTabsLabel = new Label("Separadores: 0");
        openTabsLabel.getStyleClass().add("status-bar-meta");

        Label version = new Label(
                "Kubata Administrator · v1.0.0",
                IconUtils.icon(Feather.INFO, IconUtils.SIZE_SMALL)
        );
        version.getStyleClass().add("status-bar-meta");
        version.setMinWidth(0);
        version.setMaxWidth(155);
        version.setTextOverrun(OverrunStyle.ELLIPSIS);
        version.setTooltip(new Tooltip("Versão da aplicação"));

        Button helpButton = new Button(
                "Ajuda",
                IconUtils.icon(Feather.HELP_CIRCLE, IconUtils.SIZE_SMALL)
        );
        helpButton.getStyleClass().add("status-bar-action");
        helpButton.setTooltip(new Tooltip("Ajuda e atalhos de utilização"));
        helpButton.setOnAction(e -> showHelp());

        Button logoutButton = new Button(
                "Encerrar sessão",
                IconUtils.icon(Feather.LOG_OUT, IconUtils.SIZE_SMALL)
        );
        logoutButton.getStyleClass().addAll("status-bar-action", "status-bar-logout");
        logoutButton.setTooltip(new Tooltip("Terminar a sessão e voltar ao login"));
        logoutButton.setOnAction(e -> confirmLogout());

        bar.getChildren().addAll(
                sessionDot,
                sessionState,
                statusBarDivider(),
                sessionUserLabel,
                spacer,
                activePageLabel,
                statusBarDivider(),
                openTabsLabel,
                version,
                helpButton,
                logoutButton
        );

        return bar;
    }

    private Separator statusBarDivider() {
        Separator divider = new Separator(Orientation.VERTICAL);
        divider.setPrefHeight(16);
        divider.setMinHeight(16);
        divider.setMaxHeight(16);
        divider.getStyleClass().add("status-bar-divider");
        return divider;
    }

    private void buildRibbon() {
        ribbonService.buildRibbon(
                ribbon,
                sessionManager.getUser(),
                this::openOrFocusTab,
                beanClass -> applicationContext.getBean(beanClass)
        );
    }

    // ── Pesquisa global ───────────────────────────────────────────────────────

    private void performGlobalSearch(String query) {
        String normalizedQuery = normalizeSearchText(query);
        if (normalizedQuery.isBlank()) {
            return;
        }

        List<MenuItem> results = new ArrayList<>();

        for (Tab openTab : tabs.getTabs()) {
            if (matchesSearchText(openTab.getText(), normalizedQuery)
                    || matchesSearchText(openTab.getId(), normalizedQuery)) {
                MenuItem item = new MenuItem("Página aberta  ·  " + openTab.getText());
                item.setOnAction(e -> tabs.getSelectionModel().select(openTab));
                results.add(item);
            }
        }

        for (RibbonTab ribbonTab : ribbon.getTabs()) {
            if (matchesSearchText(ribbonTab.getTitle(), normalizedQuery)) {
                MenuItem item = new MenuItem("Separador  ·  " + ribbonTab.getTitle());
                item.setOnAction(e -> ribbon.selectTabByTitle(ribbonTab.getTitle()));
                results.add(item);
            }
        }

        commandSearch:
        for (RibbonTab ribbonTab : ribbon.getTabs()) {
            for (RibbonGroup group : ribbonTab.getGroups()) {
                for (Node columnNode : group.getContentBox().getChildren()) {
                    if (!(columnNode instanceof VBox column)) {
                        continue;
                    }

                    for (Node commandNode : column.getChildren()) {
                        if (!(commandNode instanceof RibbonButton button)) {
                            continue;
                        }

                        String label = extractVisibleText(button.getGraphic());
                        if (label.isBlank()) {
                            label = button.getId() == null ? "Comando" : button.getId();
                        }

                        String tooltip = button.getTooltip() == null
                                ? ""
                                : button.getTooltip().getText();
                        boolean matches = matchesSearchText(label, normalizedQuery)
                                || matchesSearchText(tooltip, normalizedQuery)
                                || matchesSearchText(button.getId(), normalizedQuery);

                        if (!matches) {
                            continue;
                        }

                        String resultLabel = label + "  ·  "
                                + ribbonTab.getTitle() + " / " + group.getGroupTitle();
                        MenuItem item = new MenuItem(resultLabel);
                        RibbonButton selectedCommand = button;
                        RibbonTab selectedRibbonTab = ribbonTab;
                        item.setOnAction(e -> {
                            ribbon.selectTabByTitle(selectedRibbonTab.getTitle());
                            selectedCommand.fire();
                        });
                        results.add(item);

                        if (results.size() >= 12) {
                            break commandSearch;
                        }
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

    private boolean matchesSearchText(String text, String normalizedQuery) {
        return text != null
                && !text.isBlank()
                && normalizeSearchText(text).contains(normalizedQuery);
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String extractVisibleText(Node node) {
        if (node == null) {
            return "";
        }
        if (node instanceof Label label) {
            return label.getText() == null ? "" : label.getText();
        }
        if (node instanceof Parent parent) {
            StringBuilder text = new StringBuilder();
            for (Node child : parent.getChildrenUnmodifiable()) {
                String childText = extractVisibleText(child);
                if (!childText.isBlank()) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }
                    text.append(childText);
                }
            }
            return text.toString().trim();
        }
        return "";
    }

    // ── Sessão, ajuda e rodapé ────────────────────────────────────────────────

    private void updateStatusBar() {
        if (sessionUserLabel == null) {
            return;
        }

        User user = sessionManager.getUser();
        if (user == null) {
            sessionUserLabel.setText("Utilizador não autenticado");
        } else {
            String role = user.getRole() == null ? "Perfil não definido" : user.getRole().name();
            sessionUserLabel.setText(user.getNome() + " · " + role);
        }

        Tab active = tabs.getSelectionModel().getSelectedItem();
        activePageLabel.setText(active == null ? "Área: —" : "Área: " + active.getText());
        openTabsLabel.setText("Separadores: " + tabs.getTabs().size());
    }

    private String currentUserName() {
        User user = sessionManager.getUser();
        return user == null ? null : user.getNome();
    }

    private void showSessionInfo() {
        User user = sessionManager.getUser();
        if (user == null) {
            modalManager.alert(
                    "Sessão não autenticada",
                    "Não existe um utilizador autenticado no Kubata Administrator.",
                    "warning",
                    null
            );
            return;
        }

        String role = user.getRole() == null ? "Não definido" : user.getRole().name();
        modalManager.alert(
                "Informações da sessão",
                "Aplicação: Kubata Administrator\n"
                        + "Utilizador: " + user.getNome() + "\n"
                        + "Perfil: " + role + "\n"
                        + "Estado: Sessão autenticada\n"
                        + "Separadores abertos: " + tabs.getTabs().size(),
                "info",
                null
        );
    }

    private void showHelp() {
        modalManager.alert(
                "Ajuda do Kubata Administrator",
                "Pesquisa global: escreva o nome de uma página ou comando no cabeçalho e prima Enter.\n"
                        + "Ribbon: utilize as abas para aceder às áreas da plataforma; os comandos abrem páginas "
                        + "ou executam operações específicas.\n"
                        + "Separadores: seleccione um separador para regressar à página aberta e use o X "
                        + "do separador para o fechar.\n"
                        + "Sessão: utilize o menu do utilizador ou este rodapé para consultar a sessão "
                        + "ou encerrar o acesso em segurança.",
                "info",
                null
        );
    }

    private void confirmLogout() {
        if (sessionManager.getUser() == null) {
            modalManager.alert(
                    "Sessão não autenticada",
                    "Não existe uma sessão para encerrar.",
                    "info",
                    null
            );
            return;
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(4));
        Label message = new Label(
                "Pretende terminar a sessão actual? As páginas abertas serão fechadas e o Kubata "
                        + "Administrator regressará ao ecrã de login."
        );
        message.setWrapText(true);
        content.getChildren().add(message);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Encerrar sessão")
                        .icon(Feather.LOG_OUT)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(500, 230)
                        .resizable(false)
                        .withConfirmButtons("Encerrar sessão", "Cancelar")
                        .confirmStyle("button-outlined-danger")
                        .cancelStyle("button-outlined")
                        .onConfirm(() -> {
                            try {
                                applicationContext.getBean(MainController.class).logoutToLogin();
                            } catch (Exception ex) {
                                notificationService.showError(
                                        "Falha ao encerrar sessão",
                                        rootMessage(ex)
                                );
                            }
                        })
        );
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? throwable.getMessage()
                : current.getMessage();
    }

    // ── Gestão dos separadores ────────────────────────────────────────────────

    public void openOrFocusTab(String id,
                               String title,
                               Supplier<? extends Node> contentSupplier,
                               boolean closable) {
        Tab existing = tabs.getTabs().stream()
                .filter(tab -> id.equals(tab.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            tabs.getSelectionModel().select(existing);
            return;
        }

        Tab tab = new Tab(title);
        tab.setId(id);
        tab.setClosable(closable);
        tab.setContent(contentSupplier.get());
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
        updateStatusBar();
    }
}
