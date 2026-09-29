package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.ModuleInstallationService;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
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
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestão da Aplicação - Inspirada no Primavera ERP V10.
 * Centraliza a gestão de módulos, base de dados, licenciamento e integrações API.
 */
@Component
public class ApplicationView extends VBox {

    private final ModuloSistemaRepository moduloRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;
    private final NotificationService notificationService;
    private final ParametroSistemaRepository parametroRepository;
    private final ObjectProvider<Flyway> flywayProvider;
    private final ModuleRegistry moduleRegistry;
    private final ModuleInstallationService moduleInstallationService;
    private final DataSource dataSource;
    private final JrxmlStudioView jrxmlStudioView;

    private final TabPane tabPane = new TabPane();
    
    // Dados
    private final ObservableList<ModuloSistema> modulos = FXCollections.observableArrayList();
    private final ObservableList<DatabaseUpdate> updates = FXCollections.observableArrayList();
    private final ObservableList<DatabaseTable> dbTables = FXCollections.observableArrayList();

    private Label databaseNameValue;
    private Label schemaVersionValue;
    private Label lastBackupValue;
    private Label migrationStatusLabel;
    private Button btnApplyMigrations;

    public ApplicationView(ModuloSistemaRepository moduloRepository,
                           BackupRecordRepository backupRecordRepository,
                           ModalManager modalManager,
                           PersistenceService persistenceService,
                           NotificationService notificationService,
                           ParametroSistemaRepository parametroRepository,
                           ObjectProvider<Flyway> flywayProvider,
                           ModuleRegistry moduleRegistry,
                           ModuleInstallationService moduleInstallationService,
                           DataSource dataSource,
                           JrxmlStudioView jrxmlStudioView) {
        this.moduloRepository = moduloRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.notificationService = notificationService;
        this.parametroRepository = parametroRepository;
        this.flywayProvider = flywayProvider;
        this.moduleRegistry = moduleRegistry;
        this.moduleInstallationService = moduleInstallationService;
        this.dataSource = dataSource;
        this.jrxmlStudioView = jrxmlStudioView;

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

        Tab tabJrxml = new Tab("JRXML & Relatórios", jrxmlStudioView);
        tabJrxml.setGraphic(IconUtils.icon(Feather.FILE_TEXT, 14));

        tabPane.getTabs().addAll(tabModules, tabDB, tabAPI, tabLicense, tabJrxml);

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
        VBox content = new VBox(12);
        content.setPadding(new Insets(15));

        HBox heading = new HBox(10);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox headingText = new VBox(2);
        Label title = new Label("Módulos Kubata registados no runtime");
        title.getStyleClass().add("h4");
        Label subtitle = new Label(
                "O catálogo é construído a partir dos módulos reais registados pelo Spring/ModuleRegistry."
        );
        subtitle.getStyleClass().add("text-muted");
        subtitle.setWrapText(true);
        headingText.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSync = new Button(
                "Sincronizar Módulos",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        btnSync.getStyleClass().add("button-outlined");
        btnSync.setOnAction(e -> synchronizeModules());

        heading.getChildren().addAll(headingText, spacer, btnSync);

        AdvancedTableView<ModuloSistema> table = new AdvancedTableView<>(modulos);
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ModuloSistema, String> colModule = TableUtils.createTextColumn(
                "Módulo",
                m -> new SimpleStringProperty(m.getValue().getNome())
        );
        colModule.setPrefWidth(185);

        TableColumn<ModuloSistema, String> colCode = TableUtils.createTextColumn(
                "Código",
                m -> new SimpleStringProperty(m.getValue().getCodigo())
        );
        colCode.setPrefWidth(100);

        TableColumn<ModuloSistema, String> colVersion = TableUtils.createTextColumn(
                "Versão",
                m -> new SimpleStringProperty(m.getValue().getVersao())
        );
        colVersion.setPrefWidth(85);

        TableColumn<ModuloSistema, String> colViews = new TableColumn<>("Funcionalidades");
        colViews.setCellValueFactory(cell -> {
            ModuloSistema modulo = cell.getValue();
            int count = moduleRegistry.getModule(modulo.getCodigo())
                    .map(m -> m.getModuleViews().size())
                    .orElse(0);
            return new SimpleStringProperty(String.valueOf(count));
        });
        colViews.setPrefWidth(105);

        TableColumn<ModuloSistema, String> colRuntime = new TableColumn<>("Runtime");
        colRuntime.setCellValueFactory(cell -> {
            ModuloSistema modulo = cell.getValue();
            boolean registered = moduleRegistry.isModuleRegistered(modulo.getCodigo());
            return new SimpleStringProperty(registered ? "Registado" : "Não registado");
        });
        colRuntime.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().addAll(
                        "badge",
                        "badge-".concat("Registado".equals(item) ? "success" : "danger")
                );
                setGraphic(badge);
            }
        });
        colRuntime.setPrefWidth(110);

        TableColumn<ModuloSistema, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getEstado() == null
                                ? "Desconhecido"
                                : cell.getValue().getEstado().toString()
                )
        );
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(item.toUpperCase());
                badge.getStyleClass().add("badge");

                if (item.equalsIgnoreCase("Activo")) {
                    badge.getStyleClass().add("badge-success");
                } else if (item.equalsIgnoreCase("Disponível")
                        || item.equalsIgnoreCase("Actualização Pendente")) {
                    badge.getStyleClass().add("badge-warning");
                } else if (item.equalsIgnoreCase("Erro")) {
                    badge.getStyleClass().add("badge-danger");
                } else {
                    badge.getStyleClass().add("badge-info");
                }

                setGraphic(badge);
            }
        });
        colStatus.setPrefWidth(125);

        TableColumn<ModuloSistema, String> colInstalled = new TableColumn<>("Instalado em");
        colInstalled.setCellValueFactory(cell -> {
            LocalDateTime installedAt = cell.getValue().getInstaladoEm();
            return new SimpleStringProperty(
                    installedAt == null
                            ? "Não instalado"
                            : installedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
        });
        colInstalled.setPrefWidth(130);

        TableColumn<ModuloSistema, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button actionButton = new Button();

            {
                actionButton.getStyleClass().add("button-outlined");
                actionButton.setOnAction(e -> {
                    ModuloSistema selected = getTableView().getItems().get(getIndex());
                    if (selected == null) {
                        return;
                    }

                    boolean installed = selected.getEstado() == ModuloSistema.EstadoModulo.ACTIVO
                            && selected.getInstaladoEm() != null;

                    openModuleInstallation(selected, installed);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                ModuloSistema modulo = getTableView().getItems().get(getIndex());
                boolean installed = modulo.getEstado() == ModuloSistema.EstadoModulo.ACTIVO
                        && modulo.getInstaladoEm() != null;

                actionButton.setText(installed ? "Reinicializar" : "Instalar");
                actionButton.setGraphic(
                        IconUtils.icon(
                                installed ? Feather.REFRESH_CW : Feather.PLAY_CIRCLE,
                                12
                        )
                );

                setGraphic(actionButton);
                setAlignment(Pos.CENTER);
            }
        });
        colActions.setPrefWidth(125);

        table.getColumns().addAll(
                colModule,
                colCode,
                colVersion,
                colViews,
                colRuntime,
                colStatus,
                colInstalled,
                colActions
        );

        Label footer = new Label(
                "Nota: os módulos são componentes reais do projecto Kubata. "
                        + "A instalação aqui não descarrega ficheiros externos; regista o módulo, "
                        + "executa a inicialização real e persiste o estado administrativo."
        );
        footer.getStyleClass().add("text-muted");
        footer.setWrapText(true);

        content.getChildren().addAll(heading, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private void synchronizeModules() {
        persistenceService.executeAsync(
                () -> moduleInstallationService.synchronizeCatalog(),
                "MODULE_SYNC",
                "MODULE",
                "Sincronização do catálogo de módulos Kubata com o ModuleRegistry",
                this::refreshAll
        );
    }

    private void openModuleInstallation(ModuloSistema modulo, boolean installed) {
        if (modulo == null) {
            return;
        }

        String action = installed ? "reinicializar" : "instalar e inicializar";
        String details =
                "Módulo: " + modulo.getNome() + "\n"
                        + "Código: " + modulo.getCodigo() + "\n"
                        + "Versão: " + modulo.getVersao() + "\n\n"
                        + "A operação será executada contra o módulo real registado no ModuleRegistry. "
                        + "Depois da inicialização, o estado será persistido em adm_modulo_sistema.";

        modalManager.showConfirmModal(
                new Label(details),
                "Confirmar " + action.substring(0, 1).toUpperCase() + action.substring(1),
                () -> persistenceService.executeAsync(
                        () -> {
                            if (installed) {
                                moduleInstallationService.initializeInstalledModule(modulo.getCodigo());
                            } else {
                                moduleInstallationService.installAndInitialize(modulo.getCodigo());
                            }
                        },
                        installed ? "MODULE_REINIT" : "MODULE_INSTALL",
                        "MODULE",
                        (installed ? "Reinicialização" : "Instalação")
                                + " do módulo " + modulo.getCodigo(),
                        this::refreshAll
                ),
                null
        );
    }

    private Node buildDatabaseTab() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        HBox heading = new HBox(10);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox headingText = new VBox(2);
        Label title = new Label(
                "Manutenção da Base de Dados",
                IconUtils.icon(Feather.DATABASE, 16)
        );
        title.getStyleClass().add("h4");

        Label subtitle = new Label(
                "Monitorização da ligação, versão do schema, migrações Flyway e histórico da estrutura de dados."
        );
        subtitle.getStyleClass().add("text-muted");
        subtitle.setWrapText(true);

        headingText.getChildren().addAll(title, subtitle);

        Region headingSpacer = new Region();
        HBox.setHgrow(headingSpacer, Priority.ALWAYS);

        Button btnTestConnection = new Button(
                "Testar Ligação",
                IconUtils.icon(Feather.DISC, 13)
        );
        btnTestConnection.getStyleClass().add("button-outlined");
        btnTestConnection.setOnAction(e -> testDatabaseConnection());

        Button btnRefreshDatabase = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        btnRefreshDatabase.getStyleClass().add("button-outlined");
        btnRefreshDatabase.setOnAction(e -> refreshAll());

        heading.getChildren().addAll(
                headingText,
                headingSpacer,
                btnTestConnection,
                btnRefreshDatabase
        );

        HBox stats = new HBox(12);
        databaseNameValue = createValueLabel("A carregar...");
        schemaVersionValue = createValueLabel("A carregar...");
        lastBackupValue = createValueLabel("A carregar...");

        stats.getChildren().addAll(
                createStatCard("Base de Dados", databaseNameValue, Feather.DATABASE),
                createStatCard("Schema Version", schemaVersionValue, Feather.HASH),
                createStatCard("Último Backup", lastBackupValue, Feather.ARCHIVE)
        );

        VBox migrationBox = new VBox(10);
        migrationBox.getStyleClass().add("card");
        migrationBox.setPadding(new Insets(15));

        Label migrationTitle = new Label(
                "Migrações da Base de Dados",
                IconUtils.icon(Feather.GIT_COMMIT, 14)
        );
        migrationTitle.getStyleClass().add("h4");

        HBox migrationAction = new HBox(12);
        migrationAction.setAlignment(Pos.CENTER_LEFT);

        migrationStatusLabel = new Label("A verificar migrações...");
        migrationStatusLabel.getStyleClass().add("text-muted");
        migrationStatusLabel.setWrapText(true);

        Region migrationSpacer = new Region();
        HBox.setHgrow(migrationSpacer, Priority.ALWAYS);

        btnApplyMigrations = new Button(
                "Aplicar Pendentes",
                IconUtils.icon(Feather.DOWNLOAD_CLOUD, 12)
        );
        btnApplyMigrations.getStyleClass().add("button-primary");
        btnApplyMigrations.setOnAction(e -> updateDatabaseSchema());

        migrationAction.getChildren().addAll(
                migrationStatusLabel,
                migrationSpacer,
                btnApplyMigrations
        );

        migrationBox.getChildren().addAll(
                migrationTitle,
                new Separator(),
                migrationAction
        );

        AdvancedTableView<DatabaseUpdate> table = new AdvancedTableView<>(updates);
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(
                TableUtils.createTextColumn(
                        "Script / Migração",
                        u -> new SimpleStringProperty(u.getValue().getName())
                )
        );
        table.getColumns().add(
                TableUtils.createTextColumn(
                        "Data",
                        u -> new SimpleStringProperty(u.getValue().getDate())
                )
        );
        table.getColumns().add(
                TableUtils.createTextColumn(
                        "Estado",
                        u -> new SimpleStringProperty(u.getValue().getStatus())
                )
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox schemaActions = new HBox(10);
        schemaActions.setAlignment(Pos.CENTER_LEFT);

        Button btnUpdateSchema = new Button(
                "Actualizar Estrutura da Base de Dados",
                IconUtils.icon(Feather.REFRESH_CW, 14)
        );
        btnUpdateSchema.getStyleClass().add("button-primary");
        btnUpdateSchema.setOnAction(e -> updateDatabaseSchema());

        Button btnCheckUpdates = new Button(
                "Verificar Pendências",
                IconUtils.icon(Feather.SEARCH, 14)
        );
        btnCheckUpdates.getStyleClass().add("button-outlined");
        btnCheckUpdates.setOnAction(e -> checkForUpdates());

        schemaActions.getChildren().addAll(btnUpdateSchema, btnCheckUpdates);

        VBox tableStatsBox = new VBox(10);
        tableStatsBox.getStyleClass().add("card");
        tableStatsBox.setPadding(new Insets(15));

        Label lblTableStats = new Label(
                "Tabelas e Índices",
                IconUtils.icon(Feather.BAR_CHART_2, 14)
        );
        lblTableStats.getStyleClass().add("h4");

        Label tableStatsHint = new Label(
                "Contagem real de registos e índices obtida através dos metadados JDBC."
        );
        tableStatsHint.getStyleClass().add("text-muted");

        AdvancedTableView<DatabaseTable> tableStats = new AdvancedTableView<>(dbTables);
        TableUtils.standardize(tableStats);
        tableStats.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableStats.getColumns().add(
                TableUtils.createTextColumn(
                        "Tabela",
                        t -> new SimpleStringProperty(t.getValue().getName())
                )
        );
        tableStats.getColumns().add(
                TableUtils.createTextColumn(
                        "Registos",
                        t -> new SimpleStringProperty(t.getValue().getRows())
                )
        );
        tableStats.getColumns().add(
                TableUtils.createTextColumn(
                        "Tamanho",
                        t -> new SimpleStringProperty(t.getValue().getSize())
                )
        );
        tableStats.getColumns().add(
                TableUtils.createTextColumn(
                        "Índices",
                        t -> new SimpleStringProperty(t.getValue().getIndexes())
                )
        );
        tableStats.setPrefHeight(260);

        tableStatsBox.getChildren().addAll(
                lblTableStats,
                tableStatsHint,
                new Separator(),
                tableStats
        );

        content.getChildren().addAll(
                heading,
                stats,
                migrationBox,
                new Label("Histórico de Migrações"),
                table,
                schemaActions,
                tableStatsBox
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
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
        chkApiEnabled.setSelected(Boolean.parseBoolean(readGlobalParameter("API_ENABLED", "false")));
        
        TextField txtPort = new TextField(readGlobalParameter("API_PORT", "8080"));
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
        
        TextField txtApiKey = new TextField(readGlobalParameter("API_KEY", "Não configurada"));
        txtApiKey.setEditable(false);
        txtApiKey.setStyle("-fx-font-family: 'Consolas';");
        
        Button btnRegen = new Button("Regenerar API Key", IconUtils.icon(Feather.REFRESH_CW, 12));
        btnRegen.getStyleClass().add("button-outlined");
        btnRegen.setOnAction(e -> {
            txtApiKey.setText(generateApiKey());
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
        int parsedPort;

        try {
            parsedPort = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            modalManager.alert("Configuração inválida",
                    "A porta da API deve ser numérica.",
                    "warning", null);
            return;
        }

        if (parsedPort < 1 || parsedPort > 65535) {
            modalManager.alert("Configuração inválida",
                    "A porta deve estar entre 1 e 65535.",
                    "warning", null);
            return;
        }

        persistenceService.executeAsync(() -> {
            saveGlobalParameter("API_ENABLED", String.valueOf(enabled), "BOOLEAN",
                    "Estado da Web API", "SISTEMA");
            saveGlobalParameter("API_PORT", String.valueOf(parsedPort), "INTEGER",
                    "Porta de escuta da Web API", "SISTEMA");
            saveGlobalParameter("API_KEY", apiKey == null ? "" : apiKey.trim(), "STRING",
                    "Chave principal da Web API", "SISTEMA");
        }, "API_CONFIG", "APPLICATION",
                "Configuração da Web API na porta " + parsedPort,
                this::refreshAll);
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

    private VBox createStatCard(String title, Label value, Feather icon) {
        VBox box = new VBox(5);
        box.getStyleClass().add("card");
        box.setPrefWidth(220);
        box.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title, IconUtils.icon(icon, 12));
        titleLabel.getStyleClass().add("text-muted");

        box.getChildren().addAll(titleLabel, value);
        return box;
    }

    private Label createValueLabel(String value) {
        Label label = new Label(value);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        label.setWrapText(true);
        return label;
    }

    private void refreshAll() {
        persistenceService.executeAsync(() -> {
            List<ModuloSistema> mods = moduleInstallationService.synchronizeCatalog();
            List<DatabaseUpdate> migrationRows = loadMigrationHistory();
            List<DatabaseTable> tableRows = loadDatabaseTables();
            DatabaseSummary summary = loadDatabaseSummary();

            Platform.runLater(() -> {
                modulos.setAll(mods);
                updates.setAll(migrationRows);
                dbTables.setAll(tableRows);

                if (databaseNameValue != null) {
                    databaseNameValue.setText(summary.databaseName());
                }
                if (schemaVersionValue != null) {
                    schemaVersionValue.setText(summary.schemaVersion());
                }
                if (lastBackupValue != null) {
                    lastBackupValue.setText(summary.lastBackup());
                }
                if (migrationStatusLabel != null) {
                    migrationStatusLabel.setText(summary.pendingMigrations() == 0
                            ? "A base de dados está actualizada. Não existem migrações pendentes."
                            : summary.pendingMigrations() + " migração(ões) pendente(s) disponível(eis) para aplicação.");
                }
                if (btnApplyMigrations != null) {
                    btnApplyMigrations.setDisable(summary.pendingMigrations() == 0);
                }
            });
        }, "APP_REFRESH", "APPLICATION",
                "Atualização de dados de engenharia e infraestrutura", null);
    }

    private void initializeModule(ModuloSistema modulo) {
        if (modulo == null) {
            return;
        }

        boolean installed = modulo.getEstado() == ModuloSistema.EstadoModulo.ACTIVO
                && modulo.getInstaladoEm() != null;

        openModuleInstallation(modulo, installed);
    }

    private DatabaseSummary loadDatabaseSummary() {
        String databaseName = "Indisponível";
        String schemaVersion = "Indisponível";

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();

            String product = meta.getDatabaseProductName();
            String version = meta.getDatabaseProductVersion();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            String location = !isBlank(catalog)
                    ? catalog
                    : (!isBlank(schema) ? schema : meta.getURL());

            databaseName = product + (isBlank(location) ? "" : " · " + location);
            if (!isBlank(version)) {
                databaseName = databaseName + " (" + version + ")";
            }
        } catch (Exception ex) {
            databaseName = "Erro: " + ex.getClass().getSimpleName();
        }

        Flyway flyway = flywayProvider.getIfAvailable();
        int pendingMigrations = 0;

        if (flyway != null) {
            MigrationInfo current = flyway.info().current();
            MigrationInfo[] pending = flyway.info().pending();

            pendingMigrations = pending == null ? 0 : pending.length;

            if (current != null && current.getVersion() != null) {
                schemaVersion = String.valueOf(current.getVersion());
                if (!isBlank(current.getDescription())) {
                    schemaVersion += " · " + current.getDescription();
                }
            } else {
                schemaVersion = "Nenhuma migração aplicada";
            }
        }

        String lastBackup = "Nunca";
        try {
            lastBackup = backupRecordRepository
                    .findTopByStatusOrderByStartTimeDesc(
                            ao.allon.kubata.core.domain.BackupRecord.BackupStatus.COMPLETED
                    )
                    .map(record -> record.getEndTime() != null
                            ? record.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : record.getStartTime() != null
                                    ? record.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                    : "Sem data")
                    .orElse("Nunca");
        } catch (Exception ignored) {
            // A manutenção da BD continua disponível mesmo que o histórico de backups não esteja acessível.
        }

        return new DatabaseSummary(databaseName, schemaVersion, lastBackup, pendingMigrations);
    }

    private void testDatabaseConnection() {
        persistenceService.executeAsync(() -> {
            if (dataSource == null) {
                throw new IllegalStateException("DataSource não está disponível.");
            }

            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT 1")) {

                if (!rs.next()) {
                    throw new SQLException("A ligação não devolveu um resultado.");
                }

                DatabaseMetaData meta = connection.getMetaData();
                String message = meta.getDatabaseProductName()
                        + " "
                        + meta.getDatabaseProductVersion();

                Platform.runLater(() -> notificationService.showSuccess(
                        "Ligação à BD OK",
                        "A ligação foi validada com sucesso. Motor: " + message
                ));
            } catch (Exception ex) {
                Platform.runLater(() -> notificationService.showWarning(
                        "Falha na ligação à BD",
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                ));
            }
        }, "DB_CONNECTION_TEST", "APPLICATION",
                "Teste de conectividade da base de dados", null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void updateDatabaseSchema() {
        modalManager.showConfirmModal(
                new Label(
                        "Aplicar agora todas as migrações Flyway pendentes?\n\n"
                                + "A operação será executada em segundo plano e registada no histórico."
                ),
                "Atualizar Estrutura da Base de Dados",
                () -> persistenceService.executeAsync(() -> {
                    Flyway flyway = flywayProvider.getIfAvailable();

                    if (flyway == null) {
                        throw new IllegalStateException(
                                "Flyway não está disponível nesta configuração da aplicação.");
                    }

                    flyway.migrate();
                },
                "DB_MIGRATE",
                "Aplicação manual das migrações Flyway",
                this::refreshAll),
                null
        );
    }

    private void checkForUpdates() {
        persistenceService.executeAsync(() -> {
            Flyway flyway = flywayProvider.getIfAvailable();
            if (flyway == null) {
                throw new IllegalStateException("Flyway não está disponível.");
            }

            MigrationInfo[] pending = flyway.info().pending();

            Platform.runLater(() -> {
                if (pending.length == 0) {
                    notificationService.showSuccess(
                            "Sistema Atualizado",
                            "Não existem migrações pendentes para a base de dados.");
                } else {
                    notificationService.showInfo(
                            "Atualizações Disponíveis",
                            pending.length + " migração(ões) pendente(s). Aceda a «Manutenção BD» para aplicar.");
                }
            });
        }, "DB_UPDATE_CHECK", "APPLICATION",
                "Verificação de migrações pendentes", null);
    }

    // Classes Auxiliares
    public static class DatabaseUpdate {
        private final String name, date, status;
        public DatabaseUpdate(String n, String d, String s) { this.name = n; this.date = d; this.status = s; }
        public String getName() { return name; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }

    private record DatabaseSummary(
            String databaseName,
            String schemaVersion,
            String lastBackup,
            int pendingMigrations
    ) {}

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
    private void saveGlobalParameter(String key,
                                     String value,
                                     String type,
                                     String description,
                                     String group) {
        ParametroSistema parametro = parametroRepository
                .findByChaveAndEmpresaIdIsNull(key)
                .orElseGet(ParametroSistema::new);

        parametro.setEmpresa(null);
        parametro.setChave(key);
        parametro.setValor(value);
        parametro.setTipoValor(type);
        parametro.setDescricao(description);
        parametro.setGrupo(group);
        parametro.setEditavel(true);
        parametro.setAtualizadoEm(LocalDateTime.now());
        parametro.setAtualizadoPor("Kubata Administrator");

        parametroRepository.save(parametro);
    }

    private List<DatabaseUpdate> loadMigrationHistory() {
        Flyway flyway = flywayProvider.getIfAvailable();
        if (flyway == null) {
            return List.of(new DatabaseUpdate(
                    "Flyway",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "Indisponível"
            ));
        }

        return java.util.Arrays.stream(flyway.info().all())
                .filter(info -> info != null && info.getVersion() != null)
                .sorted(java.util.Comparator.comparing(
                        MigrationInfo::getInstalledRank,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                ))
                .map(info -> new DatabaseUpdate(
                        String.valueOf(info.getVersion()) + " - " + info.getDescription(),
                        info.getInstalledOn() != null
                                ? info.getInstalledOn().toInstant()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime()
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "—",
                        info.getState() != null ? info.getState().getDisplayName() : "Desconhecido"
                ))
                .collect(Collectors.toList());
    }

    private List<DatabaseTable> loadDatabaseTables() {
        if (dataSource == null) {
            return List.of();
        }

        List<DatabaseTable> rows = new java.util.ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();

            try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next() && rows.size() < 100) {
                    String name = tables.getString("TABLE_NAME");

                    if (name == null || name.isBlank() || name.startsWith("sqlite_")
                            || name.startsWith("flyway_")) {
                        continue;
                    }

                    long rowCount = countRows(connection, name);
                    int indexCount = countIndexes(meta, name);

                    rows.add(new DatabaseTable(
                            name,
                            String.valueOf(rowCount),
                            "N/D",
                            String.valueOf(indexCount)
                    ));
                }
            }
        } catch (Exception ex) {
            notificationService.showWarning(
                    "Metadados da base de dados",
                    "Não foi possível obter todas as estatísticas: " + ex.getMessage()
            );
        }

        return rows;
    }

    private long countRows(Connection connection, String tableName) {
        String identifier = "\"" + tableName.replace("\"", "\"\"") + "\"";

        try (Statement statement = connection.createStatement();
             var rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + identifier)) {

            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ignored) {
            return 0L;
        }
    }

    private int countIndexes(DatabaseMetaData meta, String tableName) {
        Set<String> names = new LinkedHashSet<>();

        try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        } catch (SQLException ignored) {
            // Alguns drivers JDBC não suportam metadata de índices.
        }

        return names.size();
    }

    private String readGlobalParameter(String key, String fallback) {
        try {
            return parametroRepository.findByChaveAndEmpresaIdIsNull(key)
                    .map(ParametroSistema::getValor)
                    .filter(value -> value != null && !value.isBlank())
                    .orElse(fallback);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);

        StringBuilder sb = new StringBuilder("KBT-");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.substring(0, Math.min(sb.length(), 35)).toUpperCase();
    }

}
