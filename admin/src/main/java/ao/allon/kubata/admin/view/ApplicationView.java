package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestão da Aplicação - Inspirada no Primavera ERP V10.
 * Centraliza a gestão de módulos, base de dados, licenciamento e integrações API.
 */
@Component
public class ApplicationView extends VBox {

    private final ModuloSistemaRepository moduloRepository;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;
    private final NotificationService notificationService;

    private final TabPane tabPane = new TabPane();
    
    // Dados
    private final ObservableList<ModuloSistema> modulos = FXCollections.observableArrayList();
    private final ObservableList<DatabaseUpdate> updates = FXCollections.observableArrayList();
    private final ObservableList<DatabaseTable> dbTables = FXCollections.observableArrayList();

    public ApplicationView(ModuloSistemaRepository moduloRepository,
                           ModalManager modalManager,
                           PersistenceService persistenceService,
                           NotificationService notificationService) {
        this.moduloRepository = moduloRepository;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.notificationService = notificationService;

        buildUI();
        
        // Carregamento assíncrono para evitar erros no construtor
        Platform.runLater(this::refreshAll);
    }

    private void buildUI() {
        setSpacing(0);
        getStyleClass().add("application-view");

        // Toolbar de Engenharia
        HBox toolbar = buildToolbar();
        
        tabPane.getStyleClass().add("office365-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabModules = new Tab("Módulos & Instalação", buildModulesTab());
        tabModules.setGraphic(IconUtils.icon(Feather.PACKAGE, 14));

        Tab tabDB = new Tab("Manutenção BD", buildDatabaseTab());
        tabDB.setGraphic(IconUtils.icon(Feather.DATABASE, 14));

        Tab tabAPI = new Tab("Web API & Integração", buildApiTab());
        tabAPI.setGraphic(IconUtils.icon(Feather.SHARE_2, 14));

        Tab tabLicense = new Tab("Licenciamento", buildLicenseTab());
        tabLicense.setGraphic(IconUtils.icon(Feather.KEY, 14));

        tabPane.getTabs().addAll(tabModules, tabDB, tabAPI, tabLicense);

        getChildren().addAll(toolbar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 20, 10, 20));

        Label title = new Label("Gestão de Infraestrutura e Aplicação", IconUtils.icon(Feather.CPU, 18));
        title.getStyleClass().add("h3");
        title.setStyle("-fx-text-fill: -kubata-green-dark;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnUpdate = new Button("Procurar Atualizações", IconUtils.icon(Feather.DOWNLOAD_CLOUD, IconUtils.SIZE_SMALL));
        btnUpdate.getStyleClass().add("button-success");
        btnUpdate.setOnAction(e -> checkForUpdates());

        box.getChildren().addAll(title, spacer, btnUpdate);
        return box;
    }

    private Node buildModulesTab() {
        AdvancedTableView<ModuloSistema> table = new AdvancedTableView<>(modulos);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Módulo", m -> new SimpleStringProperty(m.getValue().getNome())));
        table.getColumns().add(TableUtils.createTextColumn("Código", m -> new SimpleStringProperty(m.getValue().getCodigo())));
        table.getColumns().add(TableUtils.createTextColumn("Versão", m -> new SimpleStringProperty(m.getValue().getVersao())));
        
        TableColumn<ModuloSistema, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getEstado().toString()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.getStyleClass().add("badge");
                    if (item.contains("Activo")) lbl.getStyleClass().add("badge-success");
                    else if (item.contains("Pendente")) lbl.getStyleClass().add("badge-warning");
                    else lbl.getStyleClass().add("badge-info");
                    setGraphic(lbl);
                }
            }
        });
        table.getColumns().add(colStatus);

        TableColumn<ModuloSistema, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnInit = new Button("Inicializar", IconUtils.icon(Feather.PLAY_CIRCLE, 12));
            {
                btnInit.getStyleClass().add("button-outlined");
                btnInit.setOnAction(e -> initializeModule(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnInit);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colActions);

        VBox content = new VBox(10, new Label("Módulos Disponíveis no Ecossistema"), table);
        content.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node buildDatabaseTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        HBox stats = new HBox(20);
        stats.getChildren().addAll(
            createStatCard("Base de Dados", "KUBATA_ERP_PROD", Feather.DATABASE),
            createStatCard("Schema Version", "2026.04.07.001", Feather.HASH),
            createStatCard("Último Backup", "Há 2 horas", Feather.CLOCK)
        );

        // Painel de Service Packs Disponíveis
        VBox spBox = new VBox(10);
        spBox.getStyleClass().add("card");
        spBox.setPadding(new Insets(15));
        
        Label lblSp = new Label("Service Packs e Patches Disponíveis", IconUtils.icon(Feather.GIFT, 14));
        lblSp.getStyleClass().add("h4");
        
        HBox spAction = new HBox(15);
        spAction.setAlignment(Pos.CENTER_LEFT);
        Label lblNewSp = new Label("Service Pack 2026 Q2 (v1.3.0) disponível para instalação.");
        lblNewSp.setStyle("-fx-text-fill: -kubata-green-dark; -fx-font-weight: bold;");
        Button btnInstallSp = new Button("Instalar Agora", IconUtils.icon(Feather.DOWNLOAD, 12));
        btnInstallSp.getStyleClass().add("button-success");
        btnInstallSp.setOnAction(e -> installServicePack("v1.3.0"));
        
        spAction.getChildren().addAll(lblNewSp, btnInstallSp);
        spBox.getChildren().addAll(lblSp, new Separator(), spAction);

        // Histórico de Updates
        AdvancedTableView<DatabaseUpdate> table = new AdvancedTableView<>(updates);
        TableUtils.standardize(table);
        table.getColumns().add(TableUtils.createTextColumn("Script / Service Pack", u -> new SimpleStringProperty(u.getValue().getName())));
        table.getColumns().add(TableUtils.createTextColumn("Data", u -> new SimpleStringProperty(u.getValue().getDate())));
        table.getColumns().add(TableUtils.createTextColumn("Estado", u -> new SimpleStringProperty(u.getValue().getStatus())));

        Button btnUpdateSchema = new Button("Atualizar Estrutura de Dados (Force)", IconUtils.icon(Feather.REFRESH_CW, 14));
        btnUpdateSchema.getStyleClass().add("button-primary");
        btnUpdateSchema.setOnAction(e -> updateDatabaseSchema());

        // Nova Secção: Tabelas e Índices
        VBox tableStatsBox = new VBox(10);
        tableStatsBox.getStyleClass().add("card");
        tableStatsBox.setPadding(new Insets(15));
        
        Label lblTableStats = new Label("Estatísticas de Armazenamento por Tabela", IconUtils.icon(Feather.BAR_CHART_2, 14));
        lblTableStats.getStyleClass().add("h4");
        
        AdvancedTableView<DatabaseTable> tableStats = new AdvancedTableView<>(dbTables);
        TableUtils.standardize(tableStats);
        tableStats.getColumns().add(TableUtils.createTextColumn("Tabela", t -> new SimpleStringProperty(t.getValue().getName())));
        tableStats.getColumns().add(TableUtils.createTextColumn("Registos", t -> new SimpleStringProperty(t.getValue().getRows())));
        tableStats.getColumns().add(TableUtils.createTextColumn("Tamanho", t -> new SimpleStringProperty(t.getValue().getSize())));
        tableStats.getColumns().add(TableUtils.createTextColumn("Índices", t -> new SimpleStringProperty(t.getValue().getIndexes())));
        tableStats.setPrefHeight(250);

        tableStatsBox.getChildren().addAll(lblTableStats, new Separator(), tableStats);

        content.getChildren().addAll(new Label("Estado da Infraestrutura de Dados"), stats, spBox, new Separator(), 
                                   new Label("Histórico de Atualizações de Schema"), table, btnUpdateSchema, 
                                   new Separator(), tableStatsBox);
        VBox.setVgrow(table, Priority.ALWAYS);
        return new ScrollPane(content) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private void installServicePack(String version) {
        modalManager.showConfirmModal(new Label("Deseja instalar o Service Pack " + version + "?\n\nEsta operação irá:\n1. Colocar o sistema em Modo Manutenção\n2. Realizar backup preventivo\n3. Atualizar binários e base de dados"), 
                "Instalação de Service Pack", () -> {
            persistenceService.executeAsync(() -> {
                try {
                    // Simulação de passos de instalação
                    Thread.sleep(1000); // Backup
                    Thread.sleep(2000); // DB Updates
                    Thread.sleep(1000); // Binários
                } catch (Exception e) {}
            }, "SP_INSTALL", "APPLICATION", "Service Pack " + version + " instalado com sucesso", () -> refreshAll());
        }, null);
    }

    private Node buildApiTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.getStyleClass().add("card");
        grid.setPadding(new Insets(20));

        CheckBox chkApiEnabled = new CheckBox("Activar Web API (REST)");
        chkApiEnabled.setSelected(true);
        
        TextField txtPort = new TextField("8080");
        txtPort.setPrefWidth(100);
        
        TextField txtEndpoint = new TextField("/api/v1/kubata");
        txtEndpoint.setEditable(false);
        txtEndpoint.getStyleClass().add("text-muted");

        grid.add(new Label("Configurações de Conetividade Externa (ECHO v10 Style)"), 0, 0, 2, 1);
        grid.add(new Separator(), 0, 1, 2, 1);
        grid.add(chkApiEnabled, 0, 2);
        grid.add(new Label("Porta do Serviço:"), 0, 3);
        grid.add(txtPort, 1, 3);
        grid.add(new Label("Endpoint Base:"), 0, 4);
        grid.add(txtEndpoint, 1, 4);
        
        // Segurança da API
        VBox securityBox = new VBox(15);
        securityBox.getStyleClass().add("card");
        securityBox.setPadding(new Insets(20));
        
        Label lblSec = new Label("Segurança e Autenticação", IconUtils.icon(Feather.SHIELD, 14));
        lblSec.getStyleClass().add("h4");
        
        TextField txtApiKey = new TextField("KBT-7890-X12-PROD");
        txtApiKey.setEditable(false);
        txtApiKey.setStyle("-fx-font-family: 'Consolas';");
        
        Button btnRegen = new Button("Regenerar API Key", IconUtils.icon(Feather.REFRESH_CW, 12));
        btnRegen.getStyleClass().add("button-outlined");
        btnRegen.setOnAction(e -> {
            txtApiKey.setText("KBT-" + (1000 + new java.util.Random().nextInt(8999)) + "-X" + (10 + new java.util.Random().nextInt(89)) + "-PROD");
            notificationService.showInfo("API Key Regenerada", "Lembre-se de atualizar os seus clientes externos.");
        });
        
        securityBox.getChildren().addAll(lblSec, new Label("Chave de Acesso Principal:"), new HBox(10, txtApiKey, btnRegen));

        Button btnSaveApi = new Button("Guardar & Reiniciar API", IconUtils.icon(Feather.SAVE, 14));
        btnSaveApi.getStyleClass().add("button-primary");
        btnSaveApi.setOnAction(e -> saveApiConfig(chkApiEnabled.isSelected(), txtPort.getText(), txtApiKey.getText()));
        
        content.getChildren().addAll(grid, securityBox, btnSaveApi);
        return content;
    }

    private void saveApiConfig(boolean enabled, String port, String apiKey) {
        persistenceService.executeAsync(() -> {
            try {
                // Simulação de salvamento de parâmetros
                Thread.sleep(1500);
            } catch (Exception e) {}
        }, "API_CONFIG", "APPLICATION", "API ECHO v10 configurada na porta " + port, () -> refreshAll());
    }

    private Node buildLicenseTab() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        Label hint = new Label("Resumo local: use o separador «Licenciamento» no ribbon (Segurança) para validade por módulo, alertas e edição de chaves.");
        hint.setWrapText(true);
        hint.getStyleClass().add("text-muted");
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(560);
        card.getChildren().addAll(
                new Label("Política de licenciamento", IconUtils.icon(Feather.SHIELD, 18)),
                new Separator(),
                hint,
                new Label("Os detalhes por módulo (adm_modulo_sistema) são geridos na vista dedicada para manter paridade com o BSS Primavera e alertas de expiração.")
        );
        content.getChildren().add(card);
        return content;
    }

    private VBox createStatCard(String title, String value, Feather icon) {
        VBox box = new VBox(5);
        box.getStyleClass().add("card");
        box.setPrefWidth(200);
        box.getChildren().addAll(
            new Label(title, IconUtils.icon(icon, 12)),
            new Label(value) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }}
        );
        return box;
    }

    private void refreshAll() {
        persistenceService.executeAsync(() -> {
            List<ModuloSistema> mods = moduloRepository.findAll();

            Platform.runLater(() -> {
                modulos.setAll(mods);
                
                updates.clear();
                updates.add(new DatabaseUpdate("SP_2026_001", "2026-04-01", "Aplicado"));
                updates.add(new DatabaseUpdate("FIX_VAT_CALC", "2026-03-15", "Aplicado"));

                dbTables.clear();
                dbTables.add(new DatabaseTable("TBL_FACTURACAO", "1.240.500", "450 MB", "8"));
                dbTables.add(new DatabaseTable("TBL_CLIENTES", "15.200", "12 MB", "3"));
                dbTables.add(new DatabaseTable("TBL_ARTIGOS", "45.000", "85 MB", "5"));
                dbTables.add(new DatabaseTable("TBL_MOVIMENTOS", "2.500.000", "1.2 GB", "12"));
            });
        }, "APP_REFRESH", "APPLICATION", "Atualização de dados de engenharia", null);
    }

    private void initializeModule(ModuloSistema modulo) {
        modalManager.showConfirmModal(new Label("Deseja inicializar o módulo " + modulo.getNome() + "?\n\nIsto criará as tabelas e views necessárias na base de dados."), 
                "Inicializar Módulo", () -> {
            persistenceService.executeAsync(() -> {
                try {
                    modulo.setEstado(ModuloSistema.EstadoModulo.ACTIVO);
                    modulo.setInstaladoEm(LocalDateTime.now());
                    moduloRepository.save(modulo);
                    Thread.sleep(1500); // Simulação de criação de tabelas
                } catch (Exception e) {}
            }, "MOD_INIT", "APPLICATION", "Módulo " + modulo.getNome() + " inicializado com sucesso", () -> refreshAll());
        }, null);
    }

    private void updateDatabaseSchema() {
        modalManager.showConfirmModal(new Label("ATENÇÃO: Deseja forçar a atualização do schema da base de dados?\nEsta operação pode demorar alguns minutos."), 
                "Atualizar Schema", () -> {
            notificationService.showInfo("Atualização Iniciada", "O motor de dados está a aplicar os scripts de engenharia.");
        }, null);
    }

    private void checkForUpdates() {
        notificationService.showInfo("Atualizações", "O sistema está na versão mais recente (v1.2.4).");
    }

    // Classes Auxiliares
    public static class DatabaseUpdate {
        private final String name, date, status;
        public DatabaseUpdate(String n, String d, String s) { this.name = n; this.date = d; this.status = s; }
        public String getName() { return name; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }

    public static class DatabaseTable {
        private final String name, rows, size, indexes;
        public DatabaseTable(String n, String r, String s, String i) {
            this.name = n; this.rows = r; this.size = s; this.indexes = i;
        }
        public String getName() { return name; }
        public String getRows() { return rows; }
        public String getSize() { return size; }
        public String getIndexes() { return indexes; }
    }
}
