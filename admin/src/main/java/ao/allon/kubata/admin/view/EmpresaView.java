package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.wizard.EmpresaWizardView;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centro de gestão empresarial do Kubata Administrator.
 *
 * <p>O Administrator é a camada central do ecossistema: gere empresas,
 * estado operacional, módulos disponíveis para cada empresa e acesso ao
 * Assistente de Instalação/Configuração.</p>
 */
@Component
public class EmpresaView extends VBox {

    private final EmpresaRepository empresaRepository;
    private final ModuloSistemaRepository moduloSistemaRepository;
    private final ModuleRegistry moduleRegistry;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final EmpresaWizardView empresaWizardView;
    private final PersistenceService persistenceService;

    private final ObservableList<Empresa> empresas = FXCollections.observableArrayList();

    private AdvancedTableView<Empresa> table;
    private TextField searchField;

    private Label totalLabel;
    private Label activeLabel;
    private Label inactiveLabel;
    private Label modulesLabel;

    private VBox detailsPane;
    private Label detailsTitle;
    private Label detailsStatus;
    private Label detailsNif;
    private Label detailsLocation;
    private Label detailsFiscal;
    private Label detailsExercise;
    private Label detailsModules;

    public EmpresaView(EmpresaRepository empresaRepository,
                       ModuloSistemaRepository moduloSistemaRepository,
                       ModuleRegistry moduleRegistry,
                       AcessoService acessoService,
                       SessionManager sessionManager,
                       ModalManager modalManager,
                       EmpresaWizardView empresaWizardView,
                       PersistenceService persistenceService) {
        this.empresaRepository = empresaRepository;
        this.moduloSistemaRepository = moduloSistemaRepository;
        this.moduleRegistry = moduleRegistry;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.empresaWizardView = empresaWizardView;
        this.persistenceService = persistenceService;

        buildUI();
    }

    @PostConstruct
    private void init() {
        Platform.runLater(this::loadEmpresas);
    }

    private void buildUI() {
        getStyleClass().add("empresa-view");
        setSpacing(0);
        setFillWidth(true);

        VBox header = buildHeader();
        HBox kpis = buildKpis();

        BorderPane body = new BorderPane();
        body.setPadding(new Insets(0, 18, 18, 18));

        table = buildTable();
        detailsPane = buildDetailsPane();

        SplitPane split = new SplitPane(table, detailsPane);
        split.setDividerPositions(0.71);
        split.getStyleClass().add("empresa-content-split");
        BorderPane.setMargin(split, new Insets(12, 0, 0, 0));
        body.setCenter(split);

        getChildren().addAll(header, kpis, body);
        VBox.setVgrow(body, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16, 18, 10, 18));
        root.getStyleClass().add("empresa-header");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("▣");
        icon.getStyleClass().add("empresa-title-icon");

        VBox text = new VBox(3);
        Label title = new Label("Empresas");
        title.getStyleClass().add("h2");

        Label subtitle = new Label(
                "Centro empresarial do Kubata Administrator · empresas, módulos e configuração central"
        );
        subtitle.getStyleClass().add("text-muted");

        text.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome, NIF, identificador, província...");
        searchField.setPrefWidth(340);
        searchField.getStyleClass().add("empresa-search");
        searchField.textProperty().addListener((obs, old, value) -> filter(value));

        Button newButton = new Button(
                "Nova empresa",
                IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_SMALL)
        );
        newButton.getStyleClass().add("button-primary");
        newButton.setOnAction(e -> openWizard(new Empresa()));

        Button editButton = new Button(
                "Configurar",
                IconUtils.icon(Feather.SETTINGS, IconUtils.SIZE_SMALL)
        );
        editButton.getStyleClass().add("button-outlined");
        editButton.setOnAction(e -> openSelectedWizard());

        Button duplicateButton = new Button(
                "Duplicar",
                IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL)
        );
        duplicateButton.getStyleClass().add("button-outlined");
        duplicateButton.setOnAction(e -> duplicateSelected());

        Button toggleButton = new Button(
                "Activar / desactivar",
                IconUtils.icon(Feather.POWER, IconUtils.SIZE_SMALL)
        );
        toggleButton.getStyleClass().add("button-outlined");
        toggleButton.setOnAction(e -> toggleSelected());

        Button refreshButton = new Button(
                "",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        refreshButton.getStyleClass().add("button-outlined");
        refreshButton.setTooltip(new Tooltip("Actualizar empresas"));
        refreshButton.setOnAction(e -> loadEmpresas());

        titleRow.getChildren().addAll(
                icon, text, spacer, searchField, newButton,
                editButton, duplicateButton, toggleButton, refreshButton
        );

        root.getChildren().add(titleRow);
        return root;
    }

    private HBox buildKpis() {
        HBox row = new HBox(12);
        row.setPadding(new Insets(4, 18, 8, 18));
        row.getStyleClass().add("empresa-kpi-row");

        totalLabel = new Label("0");
        activeLabel = new Label("0");
        inactiveLabel = new Label("0");
        modulesLabel = new Label("0");

        row.getChildren().addAll(
                kpi("Empresas", totalLabel, Feather.BRIEFCASE),
                kpi("Operacionais", activeLabel, Feather.CHECK_CIRCLE),
                kpi("Inactivas", inactiveLabel, Feather.PAUSE_CIRCLE),
                kpi("Módulos activos no runtime", modulesLabel, Feather.LAYERS)
        );

        return row;
    }

    private VBox kpi(String title, Label value, Feather iconCode) {
        Label icon = new Label("", IconUtils.icon(iconCode, 16));
        icon.getStyleClass().add("empresa-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empresa-kpi-title");

        value.getStyleClass().add("empresa-kpi-value");

        VBox text = new VBox(1, titleLabel, value);
        VBox card = new VBox(4, new HBox(8, icon, text));
        card.getStyleClass().add("empresa-kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private AdvancedTableView<Empresa> buildTable() {
        AdvancedTableView<Empresa> tv = new AdvancedTableView<>();
        tv.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tv.setData(empresas);
        TableUtils.standardize(tv);

        tv.setOnViewDetails(this::showDetails);
        tv.setOnEdit(this::openWizard);
        tv.setOnDelete(this::removeEmpresa);
        tv.setOnRefresh(this::loadEmpresas);
        tv.setEntityName("Empresa");

        TableColumn<Empresa, String> identifier = textColumn("ID", "identificador", 75);
        TableColumn<Empresa, String> name = textColumn("Razão Social", "nome", 220);
        TableColumn<Empresa, String> commercial = textColumn("Nome Comercial", "nomeComercial", 170);
        TableColumn<Empresa, String> nif = textColumn("NIF", "nif", 120);
        TableColumn<Empresa, String> province = textColumn("Província", "provincia", 115);
        TableColumn<Empresa, String> municipality = textColumn("Município", "municipio", 125);
        TableColumn<Empresa, String> regime = textColumn("Regime Fiscal", "regimeFiscal", 150);
        TableColumn<Empresa, Integer> year = new TableColumn<>("Exercício");
        year.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getExercicioActual()));
        year.setPrefWidth(90);

        TableColumn<Empresa, Integer> moduleCount = new TableColumn<>("Módulos");
        moduleCount.setCellValueFactory(c -> new SimpleObjectProperty<>(countCompanyModules(c.getValue())));
        moduleCount.setPrefWidth(85);

        TableColumn<Empresa, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAtiva() ? "Activa" : "Inactiva"));
        state.setPrefWidth(95);

        TableColumn<Empresa, String> currency = textColumn("Moeda", "moedaBase", 90);

        tv.getColumns().addAll(
                identifier, name, commercial, nif, province, municipality,
                regime, year, currency, moduleCount, state
        );

        tv.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, selected) -> showDetails(selected)
        );

        tv.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                Empresa selected = tv.getSelectionModel().getSelectedItem();
                if (selected != null) openWizard(selected);
            }
        });

        return tv;
    }

    private <T> TableColumn<Empresa, T> textColumn(String title, String property, double width) {
        TableColumn<Empresa, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private VBox buildDetailsPane() {
        VBox root = new VBox(12);
        root.getStyleClass().add("empresa-details");
        root.setPadding(new Insets(16));
        root.setMinWidth(310);

        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.BRIEFCASE, 18));
        detailsTitle = new Label("Nenhuma empresa seleccionada");
        detailsTitle.getStyleClass().add("h3");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        detailsStatus = new Label("—");
        detailsStatus.getStyleClass().add("empresa-status");

        titleRow.getChildren().addAll(icon, detailsTitle, spacer, detailsStatus);

        VBox facts = new VBox(8);
        facts.getStyleClass().add("empresa-details-facts");

        detailsNif = fact("NIF");
        detailsLocation = fact("Localização");
        detailsFiscal = fact("Fiscalidade");
        detailsExercise = fact("Exercício / moeda");
        detailsModules = fact("Módulos");

        facts.getChildren().addAll(
                factRow("NIF", detailsNif),
                factRow("Localização", detailsLocation),
                factRow("Fiscal", detailsFiscal),
                factRow("Exercício", detailsExercise),
                factRow("Módulos", detailsModules)
        );

        Label modulesTitle = new Label("Módulos disponíveis para a empresa");
        modulesTitle.getStyleClass().add("empresa-details-section");

        VBox moduleList = new VBox(7);
        ScrollPane moduleScroll = new ScrollPane(moduleList);
        moduleScroll.setFitToWidth(true);
        moduleScroll.setPrefViewportHeight(260);
        moduleScroll.getStyleClass().add("empresa-details-scroll");
        VBox.setVgrow(moduleScroll, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button configure = new Button(
                "Abrir assistente",
                IconUtils.icon(Feather.SETTINGS, 14)
        );
        configure.getStyleClass().add("button-primary");
        configure.setOnAction(e -> openSelectedWizard());

        Button saveModules = new Button(
                "Guardar módulos",
                IconUtils.icon(Feather.CHECK, 14)
        );
        saveModules.getStyleClass().add("button-outlined");
        saveModules.setOnAction(e -> saveModuleSelection(moduleList));

        actions.getChildren().addAll(configure, saveModules);

        root.getChildren().addAll(titleRow, new Separator(), facts, modulesTitle, moduleScroll, actions);

        root.getProperties().put("moduleList", moduleList);
        return root;
    }

    private Label fact(String label) {
        Label value = new Label("—");
        value.getStyleClass().add("empresa-fact-value");
        return value;
    }

    private HBox factRow(String label, Label value) {
        Label title = new Label(label);
        title.getStyleClass().add("empresa-fact-label");
        title.setMinWidth(88);

        value.setWrapText(true);
        HBox row = new HBox(8, title, value);
        HBox.setHgrow(value, Priority.ALWAYS);
        return row;
    }

    private void showDetails(Empresa empresa) {
        if (empresa == null) {
            detailsTitle.setText("Nenhuma empresa seleccionada");
            detailsStatus.setText("—");
            detailsNif.setText("—");
            detailsLocation.setText("—");
            detailsFiscal.setText("—");
            detailsExercise.setText("—");
            detailsModules.setText("—");
            moduleList().getChildren().clear();
            return;
        }

        detailsTitle.setText(
                empresa.getNome() == null || empresa.getNome().isBlank()
                        ? "Empresa sem nome"
                        : empresa.getNome()
        );

        detailsStatus.setText(empresa.getAtiva() ? "ACTIVA" : "INACTIVA");
        detailsStatus.getStyleClass().removeAll("active", "inactive");
        detailsStatus.getStyleClass().add(empresa.getAtiva() ? "active" : "inactive");

        detailsNif.setText(
                join(" · ", empresa.getNif(), empresa.getIdentificador())
        );
        detailsLocation.setText(
                join(" · ", empresa.getProvincia(), empresa.getMunicipio(), empresa.getLocalidade())
        );
        detailsFiscal.setText(
                join(" · ", empresa.getRegimeFiscal(), empresa.getCae())
        );
        detailsExercise.setText(
                join(" · ",
                        empresa.getExercicioActual() == null ? null : String.valueOf(empresa.getExercicioActual()),
                        empresa.getMoedaBase()
                )
        );

        List<KubataModule> modules = orderedModules();
        Set<String> selected = companyModuleIds(empresa);

        detailsModules.setText(
                selected.isEmpty()
                        ? "Nenhum módulo seleccionado"
                        : selected.stream()
                        .map(this::moduleName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.joining(", "))
        );

        VBox list = moduleList();
        list.getChildren().clear();

        for (KubataModule module : modules) {
            CheckBox check = new CheckBox(module.getModuleName());
            check.setSelected(selected.contains(module.getModuleId()));
            check.setUserData(module.getModuleId());
            check.setDisable(!module.isActive());

            Label state = new Label(module.isActive() ? "Runtime activo" : "Não instalado/activo");
            state.getStyleClass().add(module.isActive() ? "empresa-module-runtime-ok" : "empresa-module-runtime-off");

            HBox row = new HBox(8, check, new Region(), state);
            HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(row);
        }
    }

    @SuppressWarnings("unchecked")
    private VBox moduleList() {
        return (VBox) detailsPane.getProperties().get("moduleList");
    }

    private void saveModuleSelection(VBox list) {
        Empresa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Empresa", "Seleccione uma empresa antes de guardar os módulos.", "warning", null);
            return;
        }

        Set<String> ids = new LinkedHashSet<>();
        for (Node node : list.getChildren()) {
            if (!(node instanceof HBox row)) continue;
            for (Node child : row.getChildren()) {
                if (child instanceof CheckBox check && check.isSelected() && check.getUserData() != null) {
                    ids.add(String.valueOf(check.getUserData()));
                }
            }
        }

        if (ids.isEmpty()) {
            modalManager.alert(
                    "Módulos",
                    "Seleccione pelo menos um módulo para a empresa.",
                    "warning",
                    null
            );
            return;
        }

        selected.setModulos(String.join(",", ids));
        persistenceService.saveAsync(
                empresaRepository,
                selected,
                "EMPRESA",
                "Actualização dos módulos da empresa " + selected.getNome(),
                saved -> {
                    empresas.setAll(empresaRepository.findAll());
                    table.getSelectionModel().select(saved);
                    showDetails(saved);
                }
        );
    }

    private void openSelectedWizard() {
        Empresa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Empresa", "Seleccione uma empresa para configurar.", "warning", null);
            return;
        }
        openWizard(selected);
    }

    /**
     * Compatibilidade com chamadas antigas do Ribbon e de outras views.
     * Nova empresa abre directamente o Assistente de Instalação.
     */
    public void showEmpresaDialog(Empresa empresa) {
        openWizard(empresa == null ? new Empresa() : empresa);
    }

    private void openWizard(Empresa empresa) {
        empresaWizardView.start(empresa, this::loadEmpresas);
    }

    private void duplicateSelected() {
        Empresa source = table.getSelectionModel().getSelectedItem();
        if (source == null) {
            modalManager.alert("Empresa", "Seleccione uma empresa para duplicar.", "warning", null);
            return;
        }

        Empresa copy = Empresa.builder()
                .nome(source.getNome() + " - Cópia")
                .nomeComercial(source.getNomeComercial())
                .tipoContribuinte(source.getTipoContribuinte())
                .morada(source.getMorada())
                .codigoPostal(source.getCodigoPostal())
                .locality(source.getLocalidade())
                .telefone(source.getTelefone())
                .fax(source.getFax())
                .email(source.getEmail())
                .website(source.getWebsite())
                .nifFiscal(source.getNifFiscal())
                .nifSegurancaSocial(source.getNifSegurancaSocial())
                .regimeFiscal(source.getRegimeFiscal())
                .cae(source.getCae())
                .municipio(source.getMunicipio())
                .provincia(source.getProvincia())
                .pais(source.getPais())
                .caixaPostal(source.getCaixaPostal())
                .telemovel(source.getTelemovel())
                .iban(source.getIban())
                .banco(source.getBanco())
                .contaBancaria(source.getContaBancaria())
                .descricaoActividade(source.getDescricaoActividade())
                .capitalSocial(source.getCapitalSocial())
                .volumeNegociosPrevisto(source.getVolumeNegociosPrevisto())
                .capitalNacional(source.getCapitalNacional())
                .capitalEstrangeiro(source.getCapitalEstrangeiro())
                .capitalPublico(source.getCapitalPublico())
                .anoInicio(source.getAnoInicio())
                .moedaBase(source.getMoedaBase())
                .moedaAlternativa(source.getMoedaAlternativa())
                .casasDecimaisValor(source.getCasasDecimaisValor())
                .casasDecimaisQuantidade(source.getCasasDecimaisQuantidade())
                .exercicioActual(source.getExercicioActual())
                .rodapeDocumento(source.getRodapeDocumento())
                .mensagemFatura(source.getMensagemFatura())
                .ativa(false)
                .modulos(source.getModulos())
                .setores(source.getSetores())
                .build();

        copy.setIdentificador("");
        copy.setNif("");

        openWizard(copy);
    }

    private void toggleSelected() {
        Empresa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Empresa", "Seleccione uma empresa para alterar o estado.", "warning", null);
            return;
        }

        selected.setAtiva(!selected.getAtiva());
        persistenceService.saveAsync(
                empresaRepository,
                selected,
                "EMPRESA",
                (selected.getAtiva() ? "Activação" : "Desactivação") + " da empresa " + selected.getNome(),
                saved -> {
                    showDetails(saved);
                    loadEmpresas();
                }
        );
    }

    private void removeEmpresa(Empresa empresa) {
        if (empresa == null) {
            empresa = table.getSelectionModel().getSelectedItem();
        }
        if (empresa == null) {
            modalManager.alert("Empresa", "Seleccione uma empresa para remover.", "warning", null);
            return;
        }

        Empresa target = empresa;
        VBox content = new VBox(10,
                new Label("Está prestes a remover a empresa:"),
                new Label(target.getNome()),
                new Label("A remoção física pode afectar dados relacionados. Para ambientes produtivos, prefira desactivar a empresa.")
        );
        content.setPadding(new Insets(10));

        modalManager.showConfirmModal(
                content,
                "Remover empresa",
                () -> persistenceService.deleteAsync(
                        empresaRepository,
                        target,
                        null,
                        "EMPRESA",
                        "Remoção da empresa " + target.getNome(),
                        this::loadEmpresas
                ),
                null
        );
    }

    private void loadEmpresas() {
        if (table != null) table.setLoading(true);

        Task<List<Empresa>> task = new Task<>() {
            @Override
            protected List<Empresa> call() {
                return empresaRepository.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            empresas.setAll(task.getValue());
            updateKpis();
            if (table != null) {
                table.setLoading(false);
                Empresa selected = table.getSelectionModel().getSelectedItem();
                showDetails(selected);
            }
        });

        task.setOnFailed(e -> {
            if (table != null) table.setLoading(false);
            Throwable error = task.getException();
            modalManager.alert(
                    "Erro ao carregar empresas",
                    "Não foi possível obter a lista de empresas: " + safeMessage(error),
                    "error",
                    error
            );
        });

        Thread thread = new Thread(task, "kubata-empresa-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        table.setFilter(company -> {
            if (q.isBlank()) return true;

            return contains(company.getNome(), q)
                    || contains(company.getNomeComercial(), q)
                    || contains(company.getNif(), q)
                    || contains(company.getIdentificador(), q)
                    || contains(company.getProvincia(), q)
                    || contains(company.getMunicipio(), q)
                    || contains(company.getRegimeFiscal(), q);
        });
        updateKpis();
    }

    private void updateKpis() {
        List<Empresa> source = table == null
                ? empresas
                : new ArrayList<>(table.getItems());

        int total = source.size();
        long active = source.stream().filter(Empresa::getAtiva).count();

        totalLabel.setText(String.valueOf(total));
        activeLabel.setText(String.valueOf(active));
        inactiveLabel.setText(String.valueOf(Math.max(0, total - active)));

        long activeRuntimeModules = moduleRegistry.getAllModules().stream()
                .filter(KubataModule::isActive)
                .count();
        modulesLabel.setText(String.valueOf(activeRuntimeModules));
    }

    private int countCompanyModules(Empresa empresa) {
        if (empresa == null) return 0;
        Set<String> ids = companyModuleIds(empresa);
        return (int) ids.stream()
                .filter(this::isKnownModule)
                .count();
    }

    private Set<String> companyModuleIds(Empresa empresa) {
        if (empresa == null || empresa.getModulos() == null || empresa.getModulos().isBlank()) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(empresa.getModulos().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::legacyModuleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String legacyModuleId(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "estoque", "stock" -> "inventario";
            case "billing", "faturacao" -> "vendas";
            case "pos" -> "vendas";
            case "finance" -> "financeiro";
            case "accounting" -> "contabilidade";
            default -> id.toLowerCase(Locale.ROOT);
        };
    }

    private boolean isKnownModule(String id) {
        return moduleRegistry.getModule(id).isPresent();
    }

    private String moduleName(String id) {
        return moduleRegistry.getModule(id)
                .map(KubataModule::getModuleName)
                .orElse(id);
    }

    private List<KubataModule> orderedModules() {
        return moduleRegistry.getAllModules().stream()
                .sorted(Comparator.comparing(KubataModule::getModuleName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String join(String separator, String... values) {
        return Arrays.stream(values)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(separator));
    }

    private String safeMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return error == null ? "erro desconhecido" : error.getClass().getSimpleName();
        }
        return error.getMessage();
    }
}
