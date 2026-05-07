package ao.allon.kubata.rh.controller;

import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.domain.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ao.allon.kubata.rh.ui.StageReadyEvent;
import ao.allon.kubata.rh.ui.util.ThemeManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.ResourceBundle;
import java.net.URL;

import ao.allon.kubata.rh.ui.modal.ModalManager;
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
import com.pixelduke.control.ribbon.Column;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.rh.ui.util.IconUtils;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

@Component
public class MainController extends StackPane {

    @Autowired
    private AcessoService acessoService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ModalManager modalManager;

    private BorderPane mainLayout;
    private Ribbon ribbon;
    private TabPane mainTabPane;
    private Label statusLabel;

    private Stage stage;
    private User currentUser;

    public MainController() {
        // O construtor é chamado pelo Spring, mas a UI deve ser montada após a injeção
    }

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        this.stage = event.getStage();
        
        Scene scene = new Scene(this);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle("Kubata RH - Sistema de Recursos Humanos");
        stage.setMaximized(true);
        stage.show();
        
        startUI();
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
        mainLayout.setTop(ribbon);
        
        // Center: TabPane
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        mainTabPane.getStyleClass().add("office365-tabs");
        mainLayout.setCenter(mainTabPane);
        
        // Bottom: Status Bar
        HBox statusBar = new HBox();
        statusBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        statusBar.setPadding(new Insets(5, 20, 5, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Sistema RH pronto");
        
        Pane statusSpacer = new Pane();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        
        Label versionLabel = new Label("Kubata RH v1.0.0");
        versionLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
        
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
        // Aplica tema JMetro se necessário ou outras configurações de UI
    }

    private void setupRibbon() {
        // Tab Cadastros
        RibbonTab cadastrosTab = new RibbonTab("Cadastros");
        
        RibbonGroup estruturaGroup = new RibbonGroup();
        estruturaGroup.setTitle("Estrutura");
        Button btnDepartamentos = createRibbonButton("Departamentos", Feather.MAP, e -> openOrFocusTab("departamentos", "Departamentos", DepartamentoView.class));
        Button btnCargos = createRibbonButton("Cargos", Feather.BRIEFCASE, e -> openOrFocusTab("cargos", "Cargos", CargoView.class));
        estruturaGroup.getNodes().addAll(btnDepartamentos, btnCargos);
        
        RibbonGroup pessoalGroup = new RibbonGroup();
        pessoalGroup.setTitle("Pessoal");
        Button btnColaboradores = createRibbonButton("Colaboradores", Feather.USERS, e -> openOrFocusTab("colaboradores", "Colaboradores", ColaboradorView.class));
        Button btnContratos = createRibbonButton("Contratos", Feather.FILE_TEXT, e -> openOrFocusTab("contratos", "Contratos", ContratoView.class));
        Button btnDocumentos = createRibbonButton("Documentos", Feather.FOLDER, e -> openOrFocusTab("documentos", "Documentos", DocumentoColaboradorView.class));
        pessoalGroup.getNodes().addAll(btnColaboradores, btnContratos, btnDocumentos);
        
        cadastrosTab.getRibbonGroups().add(estruturaGroup);
        cadastrosTab.getRibbonGroups().add(pessoalGroup);
        
        // Tab Assiduidade
        RibbonTab assiduidadeTab = new RibbonTab("Assiduidade");
        
        RibbonGroup registoGroup = new RibbonGroup();
        registoGroup.setTitle("Registos");
        Button btnPonto = createRibbonButton("Ponto", Feather.CLOCK, e -> openOrFocusTab("ponto", "Registo de Ponto", RegistoPontoView.class));
        Button btnFaltas = createRibbonButton("Faltas", Feather.CALENDAR, e -> openOrFocusTab("faltas", "Faltas", FaltaView.class));
        Button btnHorasExtra = createRibbonButton("Horas Extra", Feather.PLUS_CIRCLE, e -> openOrFocusTab("horas_extra", "Horas Extra", HoraExtraView.class));
        registoGroup.getNodes().addAll(btnPonto, btnFaltas, btnHorasExtra);
        
        assiduidadeTab.getRibbonGroups().add(registoGroup);
        
        // Tab Férias e Licenças
        RibbonTab feriasTab = new RibbonTab("Férias e Licenças");
        
        RibbonGroup feriasGroup = new RibbonGroup();
        feriasGroup.setTitle("Gestão");
        Button btnGerirFerias = createRibbonButton("Férias", Feather.SUN, e -> openOrFocusTab("ferias", "Gestão de Férias", PedidoFeriasView.class));
        Button btnLicencas = createRibbonButton("Licenças", Feather.UMBRELLA, e -> openOrFocusTab("licencas", "Licenças", LicencaView.class));
        feriasGroup.getNodes().addAll(btnGerirFerias, btnLicencas);
        
        feriasTab.getRibbonGroups().add(feriasGroup);
        
        // Tab Relatórios
        RibbonTab relatoriosTab = new RibbonTab("Relatórios");
        
        RibbonGroup relPessoalGroup = new RibbonGroup();
        relPessoalGroup.setTitle("Pessoal");
        Button btnRelColaboradores = createRibbonButton("Colaboradores", Feather.PRINTER, e -> gerarRelatorioColaboradores());
        Button btnRelContratos = createRibbonButton("Contratos", Feather.FILE, e -> gerarRelatorioContratosExpirar());
        relPessoalGroup.getNodes().addAll(btnRelColaboradores, btnRelContratos);
        
        RibbonGroup relFeriasGroup = new RibbonGroup();
        relFeriasGroup.setTitle("Férias");
        Button btnRelFerias = createRibbonButton("Mapa Férias", Feather.MAP, e -> gerarRelatorioMapaFerias());
        relFeriasGroup.getNodes().add(btnRelFerias);
        
        relatoriosTab.getRibbonGroups().add(relPessoalGroup);
        relatoriosTab.getRibbonGroups().add(relFeriasGroup);
        
        // Tab Sistema
        RibbonTab sistemaTab = new RibbonTab("Sistema");
        RibbonGroup configGroup = new RibbonGroup();
        configGroup.setTitle("Configurações");
        Button btnUsuarios = createRibbonButton("Usuários", Feather.SHIELD, e -> showAlert(Alert.AlertType.INFORMATION, "Info", "Módulo Centralizado", "A gestão de usuários é feita no módulo Core."));
        Button btnSobre = createRibbonButton("Sobre", Feather.INFO, e -> showAlert(Alert.AlertType.INFORMATION, "Sobre", "Kubata RH", "Sistema de Gestão de Recursos Humanos v1.0.0"));
        configGroup.getNodes().addAll(btnUsuarios, btnSobre);
        
        RibbonGroup acoesGroup = new RibbonGroup();
        acoesGroup.setTitle("Ações");
        Button btnSair = createRibbonButton("Sair", Feather.LOG_OUT, e -> Platform.exit());
        acoesGroup.getNodes().add(btnSair);
        
        sistemaTab.getRibbonGroups().add(configGroup);
        sistemaTab.getRibbonGroups().add(acoesGroup);
        
        ribbon.getTabs().addAll(cadastrosTab, assiduidadeTab, feriasTab, relatoriosTab, sistemaTab);
    }

    private Button createRibbonButton(String text, Feather icon, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button btn = new Button(text);
        btn.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_LARGE));
        btn.setContentDisplay(ContentDisplay.TOP);
        btn.setOnAction(event);
        btn.setPrefWidth(100);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        return btn;
    }

    private void setupMenu() {
        // Removido - Agora usamos setupRibbon
    }

    private void controlarAcessoMenus() {
        // TODO: Implementar controle de acesso baseado em permissões do usuário
        // Exemplo: if (!acessoService.hasAccess("RH_DEPARTAMENTOS/Ver")) { departamentosItem.setDisable(true); }
    }

    private void setupStatusBar() {
        statusLabel.setText("Sistema RH pronto");
    }

    private void loadInitialView() {
        loadDashboardView();
    }

    private void openOrFocusTab(String tabId, String tabTitle, Class<? extends Node> viewClass) {
        // Procura tab existente
        Tab existingTab = mainTabPane.getTabs().stream()
            .filter(tab -> tabId.equals(tab.getId()))
            .findFirst()
            .orElse(null);
        
        if (existingTab != null) {
            // Foca na tab existente
            mainTabPane.getSelectionModel().select(existingTab);
        } else if (viewClass != null) {
            // Cria nova tab
            try {
                Node view = applicationContext.getBean(viewClass);
                
                Tab newTab = new Tab(tabTitle);
                newTab.setId(tabId);
                newTab.setClosable(true);
                newTab.setContent(view);
                
                mainTabPane.getTabs().add(newTab);
                mainTabPane.getSelectionModel().select(newTab);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Não foi possível carregar: " + tabTitle, e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Funcionalidade em desenvolvimento", "Esta funcionalidade será implementada em breve.");
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
            showAlert(Alert.AlertType.ERROR, "Erro", "Não foi possível carregar Dashboard", e.getMessage());
        }
    }

    // Removidos métodos loadXXXView redundantes

    private void gerarRelatorioColaboradores() {
        modalManager.info("Relatório", "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void gerarRelatorioContratosExpirar() {
        modalManager.info("Relatório", "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void gerarRelatorioMapaFerias() {
        modalManager.info("Relatório", "Funcionalidade temporariamente desativada. Módulo de relatórios em manutenção.");
    }

    private void salvarRelatorio(byte[] pdfBytes, String nomePadrao, String titulo) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Salvar Relatório");
        fileChooser.setInitialFileName(nomePadrao);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                java.nio.file.Files.write(file.toPath(), pdfBytes);
                modalManager.info("Sucesso", titulo + " gerado. Relatório salvo em: " + file.getAbsolutePath());
            } catch (java.io.IOException e) {
                modalManager.alert("Erro", "Não foi possível salvar arquivo", "error", e);
            }
        }
    }

    private void mostrarSobre() {
        modalManager.info("Sobre", "Kubata RH - Sistema de Recursos Humanos\nVersão 1.0.0");
    }

    private void sair() {
        modalManager.showConfirmModal(
            new Label("Deseja realmente sair do sistema?"),
            "Confirmar saída",
            () -> {
                Platform.exit();
                System.exit(0);
            },
            null
        );
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        String alertType = "info";
        if (type == Alert.AlertType.ERROR) alertType = "error";
        if (type == Alert.AlertType.WARNING) alertType = "warning";
        modalManager.alert(title, header != null ? header + "\n" + content : content, alertType, null);
    }
}
