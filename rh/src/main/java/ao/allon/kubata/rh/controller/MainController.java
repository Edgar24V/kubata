package ao.allon.kubata.rh.controller;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.rh.ui.StageReadyEvent;
import ao.allon.kubata.rh.ui.event.LoginSuccessEvent;
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
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MainController extends StackPane {

    @Autowired
    private AcessoService acessoService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ModalManager modalManager;

    @Autowired
    private LoginController loginController;

    @Autowired
    private AuthService authService;

    private BorderPane mainLayout;
    private Ribbon ribbon;
    private TabPane mainTabPane;
    private Label statusLabel;

    private Stage stage;
    private User currentUser;

    public MainController() {
        // A UI é montada depois da injeção do Spring.
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        this.stage = event.getStage();

        switchToLogin();

        stage.setTitle("Kubata");
        stage.show();
    }

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        User user = event.getUser();

        if (user == null || user.getRole() == null) {
            modalManager.showErrorModal("Acesso negado", "Utilizador autenticado inválido.");
            switchToLogin();
            return;
        }

        currentUser = user;
        switchToMain();
        updateSessionStatus();
        startUI();
    }

    private void switchToLogin() {
        currentUser = null;

        StackPane login = loginController.createView(stage);
        Scene scene = new Scene(login, 1120, 720);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        stage.setScene(scene);
        stage.setTitle("Kubata");
        stage.setMinWidth(920);
        stage.setMinHeight(600);
        stage.setResizable(true);
        stage.centerOnScreen();

        stage.setOnCloseRequest(event -> {
            stage.setOnCloseRequest(null);
            loginController.shutdown();
            stage.close();
        });
    }

    private void switchToMain() {
        Scene scene = new Scene(this);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle("Kubata RH - Sistema de Recursos Humanos");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.setMaximized(true);

        stage.setOnCloseRequest(event -> {
            event.consume();
            performLogoutAndExit();
        });
    }

    public void performLogoutAndShowLogin() {
        User user = currentUser;

        try {
            if (user != null) {
                authService.logout(
                        user,
                        user.getSessionId(),
                        "127.0.0.1"
                );
            }
        } catch (Exception ignored) {
            // Limpeza local continua mesmo em caso de falha de persistência.
        } finally {
            currentUser = null;
            while (mainTabPane != null && mainTabPane.getTabs().size() > 0) {
                mainTabPane.getTabs().remove(0);
            }
            switchToLogin();
        }
    }

    public void performLogoutAndExit() {
        User user = currentUser;

        try {
            if (user != null) {
                authService.logout(
                        user,
                        user.getSessionId(),
                        "127.0.0.1"
                );
            }
        } catch (Exception ignored) {
            // Não impedir o fecho da aplicação por uma falha de persistência.
        } finally {
            currentUser = null;
            loginController.shutdown();
            stage.setOnCloseRequest(null);
            stage.close();
        }
    }

    @PostConstruct
    public void init() {
        Platform.runLater(() -> {
            buildUI();
            setupRibbon();
            setupStatusBar();
            modalManager.setRoot(this);
        });
    }

    private void buildUI() {
        mainLayout = new BorderPane();

        // Top: Ribbon
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

        statusLabel = new Label("Sessão não iniciada");
        statusLabel.getStyleClass().add("rh-status-label");

        Pane statusSpacer = new Pane();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        Label versionLabel = new Label("Kubata RH v1.0.0");
        versionLabel.getStyleClass().add("rh-status-label");

        statusBar.getChildren().addAll(statusLabel, statusSpacer, versionLabel);
        mainLayout.setBottom(statusBar);

        getChildren().setAll(mainLayout);
    }

    public void startUI() {
        Platform.runLater(() -> {
            try {
                setupUserInterface();
                loadInitialView();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupUserInterface() {
        // Reservado para configurações adicionais de interface.
    }

    private void setupRibbon() {
        RibbonTab cadastrosTab = new RibbonTab("Cadastros");

        RibbonGroup estruturaGroup = new RibbonGroup();
        estruturaGroup.setTitle("Estrutura");
        Button btnDepartamentos = createRibbonButton(
                "Departamentos", Feather.MAP,
                e -> openOrFocusTab("departamentos", "Departamentos", DepartamentoView.class));
        Button btnCargos = createRibbonButton(
                "Cargos", Feather.BRIEFCASE,
                e -> openOrFocusTab("cargos", "Cargos", CargoView.class));
        estruturaGroup.getNodes().addAll(btnDepartamentos, btnCargos);

        RibbonGroup pessoalGroup = new RibbonGroup();
        pessoalGroup.setTitle("Pessoal");
        Button btnColaboradores = createRibbonButton(
                "Colaboradores", Feather.USERS,
                e -> openOrFocusTab("colaboradores", "Colaboradores", ColaboradorView.class));
        Button btnContratos = createRibbonButton(
                "Contratos", Feather.FILE_TEXT,
                e -> openOrFocusTab("contratos", "Contratos", ContratoView.class));
        Button btnDocumentos = createRibbonButton(
                "Documentos", Feather.FOLDER,
                e -> openOrFocusTab("documentos", "Documentos", DocumentoColaboradorView.class));
        pessoalGroup.getNodes().addAll(btnColaboradores, btnContratos, btnDocumentos);

        cadastrosTab.getRibbonGroups().add(estruturaGroup);
        cadastrosTab.getRibbonGroups().add(pessoalGroup);

        RibbonTab assiduidadeTab = new RibbonTab("Assiduidade");

        RibbonGroup registoGroup = new RibbonGroup();
        registoGroup.setTitle("Registos");
        Button btnPonto = createRibbonButton(
                "Ponto", Feather.CLOCK,
                e -> openOrFocusTab("ponto", "Registo de Ponto", RegistoPontoView.class));
        Button btnFaltas = createRibbonButton(
                "Faltas", Feather.CALENDAR,
                e -> openOrFocusTab("faltas", "Faltas", FaltaView.class));
        Button btnHorasExtra = createRibbonButton(
                "Horas Extra", Feather.PLUS_CIRCLE,
                e -> openOrFocusTab("horas_extra", "Horas Extra", HoraExtraView.class));
        registoGroup.getNodes().addAll(btnPonto, btnFaltas, btnHorasExtra);

        assiduidadeTab.getRibbonGroups().add(registoGroup);

        RibbonTab feriasTab = new RibbonTab("Férias e Licenças");

        RibbonGroup feriasGroup = new RibbonGroup();
        feriasGroup.setTitle("Gestão");
        Button btnGerirFerias = createRibbonButton(
                "Férias", Feather.SUN,
                e -> openOrFocusTab("ferias", "Gestão de Férias", PedidoFeriasView.class));
        Button btnLicencas = createRibbonButton(
                "Licenças", Feather.UMBRELLA,
                e -> openOrFocusTab("licencas", "Licenças", LicencaView.class));
        feriasGroup.getNodes().addAll(btnGerirFerias, btnLicencas);

        feriasTab.getRibbonGroups().add(feriasGroup);

        RibbonTab relatoriosTab = new RibbonTab("Relatórios");

        RibbonGroup relPessoalGroup = new RibbonGroup();
        relPessoalGroup.setTitle("Pessoal");
        Button btnRelColaboradores = createRibbonButton(
                "Colaboradores", Feather.PRINTER,
                e -> gerarRelatorioColaboradores());
        Button btnRelContratos = createRibbonButton(
                "Contratos", Feather.FILE,
                e -> gerarRelatorioContratosExpirar());
        relPessoalGroup.getNodes().addAll(btnRelColaboradores, btnRelContratos);

        RibbonGroup relFeriasGroup = new RibbonGroup();
        relFeriasGroup.setTitle("Férias");
        Button btnRelFerias = createRibbonButton(
                "Mapa Férias", Feather.MAP,
                e -> gerarRelatorioMapaFerias());
        relFeriasGroup.getNodes().add(btnRelFerias);

        relatoriosTab.getRibbonGroups().add(relPessoalGroup);
        relatoriosTab.getRibbonGroups().add(relFeriasGroup);

        RibbonTab sistemaTab = new RibbonTab("Sistema");

        RibbonGroup configGroup = new RibbonGroup();
        configGroup.setTitle("Configurações");
        Button btnUsuarios = createRibbonButton(
                "Utilizadores", Feather.SHIELD,
                e -> showAlert(Alert.AlertType.INFORMATION, "Informação",
                        "Módulo Centralizado",
                        "A gestão de utilizadores é feita no módulo Core."));
        Button btnSobre = createRibbonButton(
                "Sobre", Feather.INFO,
                e -> showAlert(Alert.AlertType.INFORMATION, "Sobre",
                        "Kubata RH", "Sistema de Gestão de Recursos Humanos v1.0.0"));
        configGroup.getNodes().addAll(btnUsuarios, btnSobre);

        RibbonGroup acoesGroup = new RibbonGroup();
        acoesGroup.setTitle("Ações");
        Button btnSair = createRibbonButton(
                "Sair", Feather.LOG_OUT, e -> performLogoutAndShowLogin());
        acoesGroup.getNodes().add(btnSair);

        sistemaTab.getRibbonGroups().add(configGroup);
        sistemaTab.getRibbonGroups().add(acoesGroup);

        ribbon.getTabs().addAll(
                cadastrosTab,
                assiduidadeTab,
                feriasTab,
                relatoriosTab,
                sistemaTab
        );
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
        updateSessionStatus();
    }

    private void updateSessionStatus() {
        if (statusLabel == null) {
            return;
        }

        if (currentUser == null) {
            statusLabel.setText("Sessão não iniciada");
            return;
        }

        String empresa = currentUser.getEmpresa() == null
                ? "Empresa não seleccionada"
                : currentUser.getEmpresa().getNome();

        statusLabel.setText(
                "Utilizador: " + currentUser.getNome()
                        + "  •  Empresa: " + empresa
        );
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
