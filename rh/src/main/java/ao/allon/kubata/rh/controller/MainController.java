package ao.allon.kubata.rh.controller;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.MaintenanceModeService;
import ao.allon.kubata.core.service.PasswordChangeService;
import ao.allon.kubata.core.ui.login.KubataLoginView;
import ao.allon.kubata.core.ui.login.LoginViewConfig;
import ao.allon.kubata.rh.service.SessionManager;
import ao.allon.kubata.rh.ui.StageReadyEvent;
import ao.allon.kubata.rh.ui.modal.ModalManager;
import ao.allon.kubata.rh.ui.util.IconUtils;
import ao.allon.kubata.rh.ui.util.ThemeManager;
import ao.allon.kubata.rh.ui.views.CargoView;
import ao.allon.kubata.rh.ui.views.ColaboradorView;
import ao.allon.kubata.rh.ui.views.ContratoView;
import ao.allon.kubata.rh.ui.views.DashboardView;
import ao.allon.kubata.rh.ui.views.DepartamentoView;
import ao.allon.kubata.rh.ui.views.DocumentoColaboradorView;
import ao.allon.kubata.rh.ui.views.FaltaView;
import ao.allon.kubata.rh.ui.views.HoraExtraView;
import ao.allon.kubata.rh.ui.views.LicencaView;
import ao.allon.kubata.rh.ui.views.PedidoFeriasView;
import ao.allon.kubata.rh.ui.views.RegistoPontoView;
import com.pixelduke.control.Ribbon;
import com.pixelduke.control.ribbon.RibbonGroup;
import com.pixelduke.control.ribbon.RibbonTab;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Shell do Kubata RH. A entrada é controlada pelo Kubata Administrator:
 * login partilhado, perfis e permissões definidos no Administrator e o
 * estado do módulo (activo/inactivo) decidido na gestão de módulos.
 */
@Component
public class MainController extends StackPane {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ModalManager modalManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordChangeService passwordChangeService;

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    /** Bilhete emitido pelo Kubata Administrator quando a aplicação é lançada a partir do hub. */
    @org.springframework.beans.factory.annotation.Value("${kubata-launch-ticket:}")
    private String launchTicket;

    @Autowired(required = false)
    private ao.allon.kubata.core.service.SessionTicketService sessionTicketService;

    private BorderPane mainLayout;
    private Ribbon ribbon;
    private TabPane mainTabPane;
    private Label statusLabel;

    private Stage stage;
    private Stage loginStage;

    public MainController() {
        // A UI é montada depois da injeção do Spring.
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        this.stage = event.getStage();
        stage.setTitle("Kubata RH - Sistema de Recursos Humanos");
        if (!entrarComBilhete()) {
            switchToLogin();
        }
    }

    /**
     * Reaproveita o login já feito no Kubata Administrator. Um bilhete ausente, expirado ou
     * recusado devolve o comportamento normal de pedido de credenciais.
     */
    private boolean entrarComBilhete() {
        if (sessionTicketService == null || launchTicket == null || launchTicket.isBlank()) {
            return false;
        }
        try {
            handleLoginSuccess(sessionTicketService.consumir(launchTicket, "RH", "127.0.0.1"));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert aviso = new Alert(Alert.AlertType.WARNING);
            aviso.setTitle("Sessão do Administrator");
            aviso.setHeaderText(null);
            aviso.setContentText("Não foi possível reutilizar a sessão do Administrator. "
                    + "Introduza as credenciais normalmente.");
            aviso.showAndWait();
            return false;
        }
    }

    @PostConstruct
    public void init() {
        Platform.runLater(() -> {
            buildUI();
            setupStatusBar();
            modalManager.setRoot(this);
        });
    }

    private void buildUI() {
        mainLayout = new BorderPane();

        // Top: Ribbon (construído após o login, com as permissões do utilizador)
        ribbon = new Ribbon();
        ribbon.getStyleClass().add("kubata-ribbon-host");
        mainLayout.setTop(ribbon);

        // Center: TabPane
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        mainTabPane.getStyleClass().add("office365-tabs");
        mainLayout.setCenter(mainTabPane);

        // Bottom: Status Bar
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("rh-status-bar");
        statusBar.setPadding(new Insets(5, 12, 5, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Sistema RH pronto");
        statusLabel.getStyleClass().add("rh-status-label");

        Pane statusSpacer = new Pane();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        Label versionLabel = new Label("Kubata RH v1.0.0");
        versionLabel.getStyleClass().add("rh-status-label");

        statusBar.getChildren().addAll(statusLabel, statusSpacer, versionLabel);
        mainLayout.setBottom(statusBar);

        getChildren().setAll(mainLayout);
    }

    // ── Autenticação (login partilhado do Kubata Administrator) ─────────────

    private void switchToLogin() {
        closeLoginStage();
        loginStage = new Stage(StageStyle.TRANSPARENT);
        KubataLoginView loginView = new KubataLoginView(
                LoginViewConfig.rh(),
                authService,
                passwordChangeService,
                maintenanceModeService,
                this::handleLoginSuccess);
        javafx.geometry.Dimension2D loginSize = KubataLoginView.recommendedSceneSize();
        Scene loginScene = new Scene(loginView.createView(loginStage),
                loginSize.getWidth(), loginSize.getHeight());
        loginScene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(loginScene);
        loginStage.setScene(loginScene);
        loginStage.centerOnScreen();
        loginStage.show();
    }

    private void closeLoginStage() {
        if (loginStage != null) {
            loginStage.close();
            loginStage = null;
        }
    }

    private void handleLoginSuccess(User user) {
        sessionManager.login(user);
        closeLoginStage();
        enterMain();
    }

    private void enterMain() {
        setupRibbon();
        setupStatusBar();

        Scene scene = new Scene(this);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        stage.toFront();

        Platform.runLater(this::loadInitialView);
    }

    private void performLogoutAndShowLogin() {
        sessionManager.logout();
        stage.hide();
        switchToLogin();
    }

    // ── UI principal ────────────────────────────────────────────────────────

    private void setupRibbon() {
        ribbon.getTabs().clear();

        RibbonTab cadastrosTab = new RibbonTab("Cadastros");

        RibbonGroup estruturaGroup = new RibbonGroup();
        estruturaGroup.setTitle("Estrutura");
        if (podeVer("FUNCIONARIOS")) {
            estruturaGroup.getNodes().addAll(
                    createRibbonButton("Departamentos", Feather.MAP,
                            e -> openOrFocusTab("departamentos", "Departamentos", DepartamentoView.class)),
                    createRibbonButton("Cargos", Feather.BRIEFCASE,
                            e -> openOrFocusTab("cargos", "Cargos", CargoView.class)));
        }

        RibbonGroup pessoalGroup = new RibbonGroup();
        pessoalGroup.setTitle("Pessoal");
        if (podeVer("FUNCIONARIOS")) {
            pessoalGroup.getNodes().addAll(
                    createRibbonButton("Colaboradores", Feather.USERS,
                            e -> openOrFocusTab("colaboradores", "Colaboradores", ColaboradorView.class)),
                    createRibbonButton("Contratos", Feather.FILE_TEXT,
                            e -> openOrFocusTab("contratos", "Contratos", ContratoView.class)),
                    createRibbonButton("Documentos", Feather.FOLDER,
                            e -> openOrFocusTab("documentos", "Documentos", DocumentoColaboradorView.class)));
        }

        if (!estruturaGroup.getNodes().isEmpty()) cadastrosTab.getRibbonGroups().add(estruturaGroup);
        if (!pessoalGroup.getNodes().isEmpty()) cadastrosTab.getRibbonGroups().add(pessoalGroup);

        RibbonTab assiduidadeTab = new RibbonTab("Assiduidade");

        RibbonGroup registoGroup = new RibbonGroup();
        registoGroup.setTitle("Registos");
        if (podeVer("FUNCIONARIOS")) {
            registoGroup.getNodes().addAll(
                    createRibbonButton("Ponto", Feather.CLOCK,
                            e -> openOrFocusTab("ponto", "Registo de Ponto", RegistoPontoView.class)),
                    createRibbonButton("Faltas", Feather.CALENDAR,
                            e -> openOrFocusTab("faltas", "Faltas", FaltaView.class)),
                    createRibbonButton("Horas Extra", Feather.PLUS_CIRCLE,
                            e -> openOrFocusTab("horas_extra", "Horas Extra", HoraExtraView.class)));
        }

        if (!registoGroup.getNodes().isEmpty()) assiduidadeTab.getRibbonGroups().add(registoGroup);

        RibbonTab feriasTab = new RibbonTab("Férias e Licenças");

        RibbonGroup feriasGroup = new RibbonGroup();
        feriasGroup.setTitle("Gestão");
        if (podeVer("FUNCIONARIOS")) {
            feriasGroup.getNodes().addAll(
                    createRibbonButton("Férias", Feather.SUN,
                            e -> openOrFocusTab("ferias", "Gestão de Férias", PedidoFeriasView.class)),
                    createRibbonButton("Licenças", Feather.UMBRELLA,
                            e -> openOrFocusTab("licencas", "Licenças", LicencaView.class)));
        }

        if (!feriasGroup.getNodes().isEmpty()) feriasTab.getRibbonGroups().add(feriasGroup);

        RibbonTab relatoriosTab = new RibbonTab("Relatórios");

        RibbonGroup relPessoalGroup = new RibbonGroup();
        relPessoalGroup.setTitle("Pessoal");
        if (podeVer("FUNCIONARIOS")) {
            relPessoalGroup.getNodes().addAll(
                    createRibbonButton("Colaboradores", Feather.PRINTER, e -> gerarRelatorioColaboradores()),
                    createRibbonButton("Contratos", Feather.FILE, e -> gerarRelatorioContratosExpirar()));
        }

        RibbonGroup relFeriasGroup = new RibbonGroup();
        relFeriasGroup.setTitle("Férias");
        if (podeVer("FUNCIONARIOS")) {
            relFeriasGroup.getNodes().add(
                    createRibbonButton("Mapa Férias", Feather.MAP, e -> gerarRelatorioMapaFerias()));
        }

        if (!relPessoalGroup.getNodes().isEmpty()) relatoriosTab.getRibbonGroups().add(relPessoalGroup);
        if (!relFeriasGroup.getNodes().isEmpty()) relatoriosTab.getRibbonGroups().add(relFeriasGroup);

        RibbonTab sistemaTab = new RibbonTab("Sistema");

        RibbonGroup configGroup = new RibbonGroup();
        configGroup.setTitle("Configurações");
        configGroup.getNodes().addAll(
                createRibbonButton("Utilizadores", Feather.SHIELD, e -> showAlert(Alert.AlertType.INFORMATION, "Informação",
                        "Gestão Centralizada",
                        "Os utilizadores e permissões são geridos no Kubata Administrator.")),
                createRibbonButton("Sobre", Feather.INFO, e -> showAlert(Alert.AlertType.INFORMATION, "Sobre",
                        "Kubata RH", "Sistema de Gestão de Recursos Humanos v1.0.0")));

        RibbonGroup acoesGroup = new RibbonGroup();
        acoesGroup.setTitle("Ações");
        acoesGroup.getNodes().add(
                createRibbonButton("Sair", Feather.LOG_OUT, e -> confirmarSaida()));

        sistemaTab.getRibbonGroups().add(configGroup);
        sistemaTab.getRibbonGroups().add(acoesGroup);

        if (!cadastrosTab.getRibbonGroups().isEmpty()) ribbon.getTabs().add(cadastrosTab);
        if (!assiduidadeTab.getRibbonGroups().isEmpty()) ribbon.getTabs().add(assiduidadeTab);
        if (!feriasTab.getRibbonGroups().isEmpty()) ribbon.getTabs().add(feriasTab);
        if (!relatoriosTab.getRibbonGroups().isEmpty()) ribbon.getTabs().add(relatoriosTab);
        ribbon.getTabs().add(sistemaTab);
    }

    private boolean podeVer(String recurso) {
        return sessionManager.podeVer(recurso);
    }

    private Button createRibbonButton(
            String text,
            Feather icon,
            javafx.event.EventHandler<javafx.event.ActionEvent> event) {

        Button btn = new Button(text);
        btn.getStyleClass().add("ribbon-button");
        btn.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_LARGE));
        btn.setContentDisplay(ContentDisplay.TOP);
        btn.setOnAction(event);
        btn.setMnemonicParsing(false);
        btn.setFocusTraversable(true);
        btn.setPrefWidth(88);
        btn.setMinWidth(82);
        btn.setMaxWidth(98);

        return btn;
    }

    private void setupStatusBar() {
        User user = sessionManager.getUser();
        if (user != null) {
            String nome = user.getNome() != null ? user.getNome() : user.getEmail();
            statusLabel.setText("Sessão: " + nome + "  •  permissões definidas no Kubata Administrator");
        } else {
            statusLabel.setText("Sistema RH pronto");
        }
    }

    private void loadInitialView() {
        loadDashboardView();
    }

    private void openOrFocusTab(String tabId, String tabTitle, Class<? extends Node> viewClass) {
        Tab existingTab = mainTabPane.getTabs().stream()
                .filter(tab -> tabId.equals(tab.getId()))
                .findFirst()
                .orElse(null);

        if (existingTab != null) {
            mainTabPane.getSelectionModel().select(existingTab);
        } else if (viewClass != null) {
            try {
                Node view = applicationContext.getBean(viewClass);

                Tab newTab = new Tab(tabTitle);
                newTab.setId(tabId);
                newTab.setClosable(true);
                newTab.setContent(view);

                mainTabPane.getTabs().add(newTab);
                mainTabPane.getSelectionModel().select(newTab);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro",
                        "Não foi possível carregar: " + tabTitle, e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Informação",
                    "Funcionalidade em desenvolvimento",
                    "Esta funcionalidade será implementada em breve.");
        }
    }

    private void loadDashboardView() {
        try {
            DashboardView view = applicationContext.getBean(DashboardView.class);

            Tab tab = mainTabPane.getTabs().stream()
                    .filter(t -> "dashboard".equals(t.getId()))
                    .findFirst()
                    .orElse(null);

            if (tab == null) {
                tab = new Tab("Dashboard");
                tab.setId("dashboard");
                tab.setClosable(false);
                tab.setContent(view);
                mainTabPane.getTabs().add(tab);
            }

            mainTabPane.getSelectionModel().select(tab);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erro",
                    "Não foi possível carregar Dashboard", e.getMessage());
        }
    }

    private void confirmarSaida() {
        Label mensagem = new Label("Terminar a sessão e voltar ao ecrã de login?");
        modalManager.showModal(mensagem, new ModalManager.ModalConfig()
                .title("Terminar Sessão")
                .autoSize()
                .withConfirmButtons("Terminar Sessão", "Cancelar")
                .onConfirm(this::performLogoutAndShowLogin));
    }

    private void gerarRelatorioColaboradores() {
        modalManager.info("Relatório",
                "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void gerarRelatorioContratosExpirar() {
        modalManager.info("Relatório",
                "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void gerarRelatorioMapaFerias() {
        modalManager.info("Relatório",
                "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void salvarRelatorio(byte[] pdfBytes, String nomePadrao, String titulo) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Relatório");
        fileChooser.setInitialFileName(nomePadrao);
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                java.nio.file.Files.write(file.toPath(), pdfBytes);
                modalManager.info("Sucesso",
                        titulo + " gerado. Relatório salvo em: " + file.getAbsolutePath());
            } catch (java.io.IOException e) {
                modalManager.alert("Erro",
                        "Não foi possível salvar arquivo", "error", e);
            }
        }
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String header,
            String content) {

        String alertType = "info";
        if (type == Alert.AlertType.ERROR) {
            alertType = "error";
        } else if (type == Alert.AlertType.WARNING) {
            alertType = "warning";
        }

        modalManager.alert(
                title,
                header != null ? header + "\n" + content : content,
                alertType,
                null
        );
    }
}
