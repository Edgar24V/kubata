package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.PermissaoPerfilRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PerfisView extends VBox {

    private final PerfilAcessoRepository perfilRepository;
    private final PermissaoPerfilRepository permissaoRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;

    private final ObservableList<PerfilAcesso> perfis = FXCollections.observableArrayList();

    private AdvancedTableView<PerfilAcesso> table;
    private TextField searchField;
    private ComboBox<Empresa> empresaFilter;
    private ComboBox<String> estadoFilter;

    private Label totalValue;
    private Label activeValue;
    private Label globalValue;
    private Label systemValue;
    private Label detailCode;
    private Label detailDescription;
    private Label detailScope;
    private Label detailState;
    private Label detailSystem;
    private Label detailPermissionCount;
    private Label detailHighRisk;
    private Label detailUserHint;

    // Edição rápida directamente no painel de detalhes.
    private TextField inlineCode;
    private TextField inlineDescription;
    private ComboBox<Empresa> inlineEmpresa;
    private TextArea inlineObservacoes;
    private CheckBox inlineActivo;
    private Button btnInlineGuardar;
    private Button btnInlineCancelar;
    private VBox inlineEditor;
    private Button btnConfigurarPermissoes;
    private PerfilAcesso inlineEditingProfile;

    private Button btnNovoPerfil;
    private Button btnEditar;
    private Button btnDuplicar;
    private Button btnRemover;

    private boolean dataLoaded;

    private static final Map<String, Map<String, List<PermissaoPerfil.Operacao>>> ESTRUTURA_PERMISSOES =
            new LinkedHashMap<>();

    static {
        ESTRUTURA_PERMISSOES.put("FATURACAO", mapOf(
                entry("FATURAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.ANULAR,
                        PermissaoPerfil.Operacao.IMPRIMIR, PermissaoPerfil.Operacao.ALTERAR_PRECO_MARGEM,
                        PermissaoPerfil.Operacao.FORCAR_CREDITO),
                entry("CLIENTES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("NOTAS_CREDITO", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.IMPRIMIR, PermissaoPerfil.Operacao.APROVACAO_INTERNA)
        ));

        ESTRUTURA_PERMISSOES.put("ESTOQUE", mapOf(
                entry("ARTIGOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("MOVIMENTOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.ANULAR),
                entry("ARMAZENS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR)
        ));

        ESTRUTURA_PERMISSOES.put("FINANCEIRO", mapOf(
                entry("CAIXA", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("BANCOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("PAGAMENTOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.APROVAR)
        ));

        ESTRUTURA_PERMISSOES.put("RH", mapOf(
                entry("FUNCIONARIOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("PROCESSAMENTO", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.APROVAR)
        ));

        ESTRUTURA_PERMISSOES.put("COMPRAS", mapOf(
                entry("FORNECEDORES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("ENCOMENDAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.ANULAR)
        ));

        ESTRUTURA_PERMISSOES.put("VENDAS", mapOf(
                entry("CLIENTES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("DOCUMENTOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.ANULAR,
                        PermissaoPerfil.Operacao.IMPRIMIR)
        ));

        ESTRUTURA_PERMISSOES.put("CONTABILIDADE", mapOf(
                entry("LANCAMENTOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("FECHOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.APROVAR)
        ));

        ESTRUTURA_PERMISSOES.put("ADMINISTRATOR", mapOf(
                entry("UTILIZADORES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("PERFIS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("EMPRESAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("EXERCICIOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("SERIES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("PARAMETROS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("LICENCAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("INTEGRACOES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("FISCAL_AGT", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("AUDITORIA", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EXPORTAR),
                entry("BACKUP", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.APROVAR),
                entry("RELATORIOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.IMPRIMIR,
                        PermissaoPerfil.Operacao.EXPORTAR),
                entry("MOEDAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR),
                entry("OPERACOES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("SCHEDULER", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR,
                        PermissaoPerfil.Operacao.EDITAR),
                entry("ALERTAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("SEGURANCA_AVANCADA", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("CERTIFICADOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("DOCUMENTOS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("COMUNICACOES", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("PREFERENCIAS", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("PERSONALIZACAO", PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR),
                entry("SESSOES", PermissaoPerfil.Operacao.VER)
        ));
    }

    @SafeVarargs
    private static Map<String, List<PermissaoPerfil.Operacao>> mapOf(
            Map.Entry<String, List<PermissaoPerfil.Operacao>>... entries) {
        Map<String, List<PermissaoPerfil.Operacao>> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<PermissaoPerfil.Operacao>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static Map.Entry<String, List<PermissaoPerfil.Operacao>> entry(
            String resource,
            PermissaoPerfil.Operacao... operations) {
        return Map.entry(resource, List.of(operations));
    }

    public PerfisView(PerfilAcessoRepository perfilRepository,
                      PermissaoPerfilRepository permissaoRepository,
                      EmpresaRepository empresaRepository,
                      AcessoService acessoService,
                      SessionManager sessionManager,
                      ModalManager modalManager,
                      PersistenceService persistenceService) {
        this.perfilRepository = perfilRepository;
        this.permissaoRepository = permissaoRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            refreshActionPermissions();
            loadPerfis();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-profiles-page");

        VBox header = buildHeader();
        BorderPane workspace = new BorderPane();
        workspace.setTop(buildFilters());
        workspace.setCenter(buildMainArea());

        getChildren().addAll(header, workspace, buildStatusBar());
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-profiles-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.SHIELD, 24));
        icon.getStyleClass().add("kubata-profiles-title-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Perfis e Segurança");
        title.getStyleClass().add("kubata-profiles-title");
        Label subtitle = new Label(
                "Centro de controlo de acessos, funções, permissões e políticas do Kubata."
        );
        subtitle.getStyleClass().add("kubata-profiles-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> loadPerfis());

        btnNovoPerfil = new Button(
                "Novo Perfil",
                IconUtils.icon(Feather.SHIELD, 13)
        );
        btnNovoPerfil.getStyleClass().add("button-primary");
        btnNovoPerfil.setOnAction(e -> showPerfilDialog(null));
        btnNovoPerfil.setDisable(true);

        titleLine.getChildren().addAll(icon, titleBox, spacer, refresh, btnNovoPerfil);

        totalValue = new Label("0");
        activeValue = new Label("0");
        globalValue = new Label("0");
        systemValue = new Label("0");

        HBox kpis = new HBox(10,
                kpi("PERFIS", Feather.SHIELD, totalValue),
                kpi("ACTIVOS", Feather.CHECK_CIRCLE, activeValue),
                kpi("GLOBAIS", Feather.GLOBE, globalValue),
                kpi("SISTEMA", Feather.LOCK, systemValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("kubata-profiles-kpi");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMinWidth(160);

        HBox top = new HBox(7);
        top.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 14));
        i.getStyleClass().add("kubata-profiles-kpi-icon");
        Label t = new Label(title);
        t.getStyleClass().add("kubata-profiles-kpi-title");
        top.getChildren().addAll(i, t);

        value.getStyleClass().add("kubata-profiles-kpi-value");
        card.getChildren().addAll(top, value);
        return card;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-profiles-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar código ou descrição...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        empresaFilter = new ComboBox<>();
        empresaFilter.getItems().add(null);
        empresaFilter.getItems().addAll(empresaRepository.findAll());
        empresaFilter.setPromptText("Empresa");
        empresaFilter.setPrefWidth(190);
        empresaFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        estadoFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Activos", "Inactivos", "Sistema", "Personalizados"
        ));
        estadoFilter.setValue("Todos");
        estadoFilter.setPrefWidth(150);
        estadoFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button(
                "Limpar filtros",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            empresaFilter.setValue(null);
            estadoFilter.setValue("Todos");
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Duplo clique abre o assistente");
        hint.getStyleClass().add("kubata-profiles-filter-hint");

        bar.getChildren().addAll(
                searchField, empresaFilter, estadoFilter, clear, spacer, hint
        );
        return bar;
    }

    private SplitPane buildMainArea() {
        table = buildTable();
        VBox details = buildDetails();

        SplitPane split = new SplitPane(
                new StackPane(table),
                new ScrollPane(details)
        );
        split.setDividerPositions(0.72);
        split.getItems().get(0).getStyleClass().add("kubata-profiles-table-pane");
        split.getItems().get(1).getStyleClass().add("kubata-profiles-details-wrapper");
        return split;
    }

    private AdvancedTableView<PerfilAcesso> buildTable() {
        AdvancedTableView<PerfilAcesso> tv = new AdvancedTableView<>();
        tv.setData(perfis);
        tv.setEntityName("Perfil");
        tv.setPlaceholder(new Label("Nenhum perfil corresponde aos filtros."));

        TableUtils.standardize(tv);

        TableColumn<PerfilAcesso, String> code = new TableColumn<>("Código");
        code.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        code.setPrefWidth(150);

        TableColumn<PerfilAcesso, String> description = new TableColumn<>("Descrição");
        description.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        description.setPrefWidth(260);

        TableColumn<PerfilAcesso, String> scope = new TableColumn<>("Âmbito");
        scope.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getEmpresa() == null
                                ? "Global"
                                : companyName(cell.getValue().getEmpresa())
                ));
        scope.setPrefWidth(190);

        TableColumn<PerfilAcesso, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        Boolean.TRUE.equals(cell.getValue().getActivo())
                                ? "Activo" : "Inactivo"
                ));
        state.setPrefWidth(100);

        TableColumn<PerfilAcesso, Boolean> system = TableUtils.createCheckColumn(
                "Sistema",
                cell -> new SimpleBooleanProperty(
                        Boolean.TRUE.equals(cell.getValue().getSistema())
                )
        );
        system.setPrefWidth(85);

        tv.getColumns().addAll(code, description, scope, state, system);

        tv.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateDetails(selected));

        // "Editar" na tabela entra no modo de edição rápida;
        // o duplo clique/detalhe abre a configuração completa.
        tv.setOnEdit(this::startInlineEdit);
        tv.setOnViewDetails(this::showPerfilDialog);
        tv.setOnDelete(selected -> removeSelectedPerfil());
        tv.setOnRefresh(this::loadPerfis);

        return tv;
    }

    private VBox buildDetails() {
        VBox details = new VBox(13);
        details.setPadding(new Insets(16));
        details.getStyleClass().add("kubata-profiles-details");

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-profiles-detail-icon");
        iconBox.getChildren().add(
                new Label("", IconUtils.icon(Feather.SHIELD, 17))
        );

        VBox identityText = new VBox(2);
        detailCode = new Label("Nenhum perfil seleccionado");
        detailCode.getStyleClass().add("kubata-profiles-detail-title");

        detailDescription = new Label("Seleccione um perfil para consultar o contexto.");
        detailDescription.getStyleClass().add("kubata-profiles-detail-subtitle");

        identityText.getChildren().addAll(detailCode, detailDescription);
        identity.getChildren().addAll(iconBox, identityText);

        detailScope = detailValue(details, "ÂMBITO", "-");
        detailState = detailValue(details, "ESTADO", "-");
        detailSystem = detailValue(details, "TIPO", "-");
        detailPermissionCount = detailValue(details, "PERMISSÕES", "-");
        detailHighRisk = detailValue(details, "OPERAÇÕES SENSÍVEIS", "-");
        detailUserHint = detailValue(details, "UTILIZAÇÃO", "-");

        VBox actions = new VBox(7);
        Label actionTitle = new Label("Operações");
        actionTitle.getStyleClass().add("kubata-profiles-section-title");

        btnEditar = profileAction("Editar na tela", Feather.EDIT_2);
        btnConfigurarPermissoes = profileAction("Configurar permissões", Feather.KEY);
        btnDuplicar = profileAction("Duplicar perfil", Feather.COPY);
        btnRemover = profileAction("Remover perfil", Feather.TRASH_2);

        btnEditar.setOnAction(e -> selectedPerfil().ifPresent(this::startInlineEdit));
        btnConfigurarPermissoes.setOnAction(e -> selectedPerfil().ifPresent(this::showPerfilDialog));
        btnDuplicar.setOnAction(e -> selectedPerfil().ifPresent(this::duplicatePerfil));
        btnRemover.setOnAction(e -> removeSelectedPerfil());

        actions.getChildren().addAll(
                actionTitle,
                btnEditar,
                btnConfigurarPermissoes,
                btnDuplicar,
                btnRemover
        );
        inlineEditor = buildInlineEditor();

        details.getChildren().addAll(
                identity,
                new Separator(),
                inlineEditor,
                actions
        );

        updateDetails(null);
        return details;
    }

    private VBox buildInlineEditor() {
        VBox card = new VBox(9);
        card.getStyleClass().add("kubata-profiles-inline-editor");
        card.setManaged(false);
        card.setVisible(false);

        Label title = new Label("Edição rápida");
        title.getStyleClass().add("kubata-profiles-inline-title");

        Label hint = new Label(
                "Altere os dados básicos sem sair do ecrã. A matriz de permissões é configurada separadamente."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("kubata-profiles-inline-hint");

        GridPane grid = new GridPane();
        grid.setHgap(9);
        grid.setVgap(8);

        inlineCode = new TextField();
        inlineCode.setPromptText("Código");
        inlineCode.setMaxWidth(Double.MAX_VALUE);

        inlineDescription = new TextField();
        inlineDescription.setPromptText("Descrição");
        inlineDescription.setMaxWidth(Double.MAX_VALUE);

        inlineEmpresa = new ComboBox<>();
        inlineEmpresa.getItems().add(null);
        inlineEmpresa.getItems().addAll(empresaRepository.findAll());
        inlineEmpresa.setPromptText("Global / empresa");
        inlineEmpresa.setMaxWidth(Double.MAX_VALUE);
        inlineEmpresa.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Empresa object) {
                return object == null
                        ? "Global — todas as empresas"
                        : companyName(object);
            }

            @Override
            public Empresa fromString(String string) {
                return null;
            }
        });

        inlineObservacoes = new TextArea();
        inlineObservacoes.setPromptText("Observações");
        inlineObservacoes.setWrapText(true);
        inlineObservacoes.setPrefRowCount(3);

        inlineActivo = new CheckBox("Perfil activo");
        inlineActivo.setSelected(true);

        grid.add(fieldLabel("Código"), 0, 0);
        grid.add(inlineCode, 1, 0);
        grid.add(fieldLabel("Descrição"), 0, 1);
        grid.add(inlineDescription, 1, 1);
        grid.add(fieldLabel("Empresa"), 0, 2);
        grid.add(inlineEmpresa, 1, 2);
        grid.add(fieldLabel("Observações"), 0, 3);
        grid.add(inlineObservacoes, 1, 3);

        ColumnConstraints labelColumn = new ColumnConstraints(90);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);

        btnInlineGuardar = new Button(
                "Guardar",
                IconUtils.icon(Feather.CHECK, 12)
        );
        btnInlineGuardar.getStyleClass().add("button-primary");
        btnInlineGuardar.setOnAction(e -> saveInlineEdit());

        btnInlineCancelar = new Button(
                "Cancelar",
                IconUtils.icon(Feather.X, 12)
        );
        btnInlineCancelar.getStyleClass().add("button-outlined");
        btnInlineCancelar.setOnAction(e -> cancelInlineEdit());

        HBox editorActions = new HBox(
                7,
                inlineActivo,
                new Pane(),
                btnInlineCancelar,
                btnInlineGuardar
        );
        HBox.setHgrow(editorActions.getChildren().get(1), Priority.ALWAYS);
        editorActions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, hint, grid, editorActions);
        return card;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("kubata-profiles-inline-label");
        return label;
    }

    private void startInlineEdit(PerfilAcesso perfil) {
        if (perfil == null || !can("EDITAR")) {
            return;
        }

        if (Boolean.TRUE.equals(perfil.getSistema())) {
            modalManager.alert(
                    "Perfil protegido",
                    "Perfis de sistema não podem ser editados directamente. Pode criar uma cópia para personalização.",
                    "info",
                    null
            );
            return;
        }

        inlineEditingProfile = perfil;
        inlineCode.setText(safe(perfil.getCodigo(), ""));
        inlineDescription.setText(safe(perfil.getDescricao(), ""));
        inlineEmpresa.setValue(perfil.getEmpresa());
        inlineObservacoes.setText(
                perfil.getObservacoes() == null ? "" : perfil.getObservacoes()
        );
        inlineActivo.setSelected(Boolean.TRUE.equals(perfil.getActivo()));

        inlineEditor.setManaged(true);
        inlineEditor.setVisible(true);
        inlineCode.requestFocus();
        inlineCode.selectAll();
    }

    private void cancelInlineEdit() {
        inlineEditingProfile = null;
        if (inlineEditor != null) {
            inlineEditor.setManaged(false);
            inlineEditor.setVisible(false);
        }
    }

    private void saveInlineEdit() {
        PerfilAcesso target = inlineEditingProfile;
        if (target == null || !can("EDITAR")) {
            return;
        }

        String code = inlineCode.getText() == null
                ? ""
                : inlineCode.getText().trim().toUpperCase(Locale.ROOT);
        String description = inlineDescription.getText() == null
                ? ""
                : inlineDescription.getText().trim();
        String observations = inlineObservacoes.getText() == null
                ? ""
                : inlineObservacoes.getText().trim();

        if (!code.matches("[A-Z0-9_-]{3,20}")) {
            modalManager.alert(
                    "Código inválido",
                    "Use entre 3 e 20 caracteres: A-Z, números, hífen ou underscore.",
                    "warning",
                    null
            );
            inlineCode.requestFocus();
            return;
        }

        if (description.isBlank()) {
            modalManager.alert(
                    "Descrição obrigatória",
                    "Introduza uma descrição clara para o perfil.",
                    "warning",
                    null
            );
            inlineDescription.requestFocus();
            return;
        }

        if (observations.length() > 500) {
            modalManager.alert(
                    "Observações demasiado longas",
                    "As observações não podem ultrapassar 500 caracteres.",
                    "warning",
                    null
            );
            return;
        }

        Optional<PerfilAcesso> existing = perfilRepository.findByCodigo(code);
        if (existing.isPresent()
                && !Objects.equals(existing.get().getId(), target.getId())) {
            modalManager.alert(
                    "Código já utilizado",
                    "Já existe outro perfil com o código " + code + ".",
                    "warning",
                    null
            );
            inlineCode.requestFocus();
            return;
        }

        try {
            target.setCodigo(code);
            target.setDescricao(description);
            target.setEmpresa(inlineEmpresa.getValue());
            target.setObservacoes(observations.isBlank() ? null : observations);
            target.setActivo(inlineActivo.isSelected());

            PerfilAcesso saved = acessoService.salvarPerfil(target);
            Long selectedId = saved.getId();

            cancelInlineEdit();
            loadPerfis();

            perfis.stream()
                    .filter(p -> Objects.equals(p.getId(), selectedId))
                    .findFirst()
                    .ifPresent(p -> table.getSelectionModel().select(p));

            modalManager.success(
                    "Perfil actualizado",
                    "Os dados básicos do perfil " + saved.getCodigo()
                            + " foram guardados."
            );
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "Erro ao guardar perfil",
                    "Não foi possível guardar as alterações do perfil.",
                    ex
            );
        }
    }

    private Label detailValue(VBox target, String label, String value) {
        VBox row = new VBox(2);
        row.getStyleClass().add("kubata-profiles-detail-row");

        Label title = new Label(label);
        title.getStyleClass().add("kubata-profiles-detail-label");

        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("kubata-profiles-detail-value");

        row.getChildren().addAll(title, valueLabel);
        target.getChildren().add(row);
        return valueLabel;
    }

    private Button profileAction(String text, Feather icon) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(32);
        button.getStyleClass().add("button-outlined");
        return button;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("kubata-profiles-statusbar");

        Label status = new Label();
        status.getStyleClass().add("kubata-profiles-status-text");

        Button help = new Button(
                "Como funciona",
                IconUtils.icon(Feather.HELP_CIRCLE, 11)
        );
        help.getStyleClass().add("button-outlined");
        help.setOnAction(e -> showSecurityGuide());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(status, spacer, help);

        perfis.addListener((javafx.collections.ListChangeListener<PerfilAcesso>) c ->
                Platform.runLater(() -> status.setText(
                        perfis.size() + " perfil(is) configurado(s)"
                ))
        );
        status.setText(perfis.size() + " perfil(is) configurado(s)");

        return bar;
    }

    private void loadPerfis() {
        refreshActionPermissions();
        try {
            perfis.setAll(perfilRepository.findAllWithEmpresa());
            refreshFilters();
            applyFilters();
            updateKpis();
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "Erro ao carregar perfis",
                    "Não foi possível carregar os perfis de acesso.",
                    ex
            );
        }
    }

    private void refreshFilters() {
        if (empresaFilter == null) return;
        Empresa selected = empresaFilter.getValue();
        empresaFilter.getItems().setAll((Empresa) null);
        empresaFilter.getItems().addAll(empresaRepository.findAll());
        empresaFilter.setValue(selected);
    }

    private void applyFilters() {
        if (table == null) return;

        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        Empresa empresa = empresaFilter == null ? null : empresaFilter.getValue();
        String state = estadoFilter == null ? "Todos" : estadoFilter.getValue();

        table.setFilter(profile -> {
            if (profile == null) return false;

            boolean textMatch = query.isBlank()
                    || contains(profile.getCodigo(), query)
                    || contains(profile.getDescricao(), query);

            boolean empresaMatch = empresa == null
                    || (profile.getEmpresa() != null
                    && Objects.equals(profile.getEmpresa().getId(), empresa.getId()));

            boolean stateMatch = switch (state) {
                case "Activos" -> Boolean.TRUE.equals(profile.getActivo());
                case "Inactivos" -> !Boolean.TRUE.equals(profile.getActivo());
                case "Sistema" -> Boolean.TRUE.equals(profile.getSistema());
                case "Personalizados" -> !Boolean.TRUE.equals(profile.getSistema());
                default -> true;
            };

            return textMatch && empresaMatch && stateMatch;
        });
    }

    private void updateKpis() {
        long total = perfis.size();
        long active = perfis.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count();
        long global = perfis.stream().filter(p -> p.getEmpresa() == null).count();
        long system = perfis.stream().filter(p -> Boolean.TRUE.equals(p.getSistema())).count();

        totalValue.setText(String.valueOf(total));
        activeValue.setText(String.valueOf(active));
        globalValue.setText(String.valueOf(global));
        systemValue.setText(String.valueOf(system));
    }

    private void updateDetails(PerfilAcesso selected) {
        if (detailCode == null) return;

        if (inlineEditingProfile != null
                && !Objects.equals(
                inlineEditingProfile.getId(),
                selected == null ? null : selected.getId()
        )) {
            cancelInlineEdit();
        }

        boolean has = selected != null;
        if (!has) {
            detailCode.setText("Nenhum perfil seleccionado");
            detailDescription.setText("Seleccione um perfil para consultar o contexto.");
            detailScope.setText("-");
            detailState.setText("-");
            detailSystem.setText("-");
            detailPermissionCount.setText("-");
            detailHighRisk.setText("-");
            detailUserHint.setText("-");
        } else {
            List<PermissaoPerfil> permissions = safePermissions(selected);
            long activePermissions = permissions.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getPermitido()))
                    .count();
            long sensitive = permissions.stream()
                    .filter(this::isSensitive)
                    .filter(p -> Boolean.TRUE.equals(p.getPermitido()))
                    .count();

            detailCode.setText(safe(selected.getCodigo(), "PERFIL"));
            detailDescription.setText(safe(selected.getDescricao(), "Sem descrição"));
            detailScope.setText(
                    selected.getEmpresa() == null
                            ? "Global — todas as empresas"
                            : companyName(selected.getEmpresa())
            );
            detailState.setText(Boolean.TRUE.equals(selected.getActivo())
                    ? "Activo" : "Inactivo");
            detailSystem.setText(Boolean.TRUE.equals(selected.getSistema())
                    ? "Perfil de sistema" : "Perfil personalizado");
            detailPermissionCount.setText(activePermissions + " permissões activas");
            detailHighRisk.setText(
                    sensitive == 0
                            ? "Nenhuma"
                            : sensitive + " operação(ões) sensível(eis)"
            );
            detailUserHint.setText(
                    "As permissões são herdadas pelos utilizadores associados."
            );
        }

        updateActionState(selected);
    }

    private List<PermissaoPerfil> safePermissions(PerfilAcesso perfil) {
        if (perfil == null || perfil.getId() == null) {
            return List.of();
        }

        try {
            return new ArrayList<>(permissaoRepository.findByPerfil(perfil));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void showPerfilDialog(PerfilAcesso perfil) {
        showPerfilDialog(perfil, null);
    }

    private void showPerfilDialog(
            PerfilAcesso perfil,
            Set<String> initialPermissionKeys) {
        boolean isNew = perfil == null || perfil.getId() == null;
        if (!can(isNew ? "CRIAR" : "EDITAR")) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para configurar perfis de acesso.",
                    "warning",
                    null
            );
            return;
        }

        if (!isNew && Boolean.TRUE.equals(perfil.getSistema())) {
            modalManager.alert(
                    "Perfil protegido",
                    "Perfis de sistema não podem ser alterados. Pode criar uma cópia para personalização.",
                    "info",
                    null
            );
            return;
        }

        PerfilAcesso target = isNew
                ? PerfilAcesso.builder().activo(true).sistema(false).build()
                : perfil;

        Set<String> currentKeys = new HashSet<>();
        if (initialPermissionKeys != null) {
            currentKeys.addAll(initialPermissionKeys);
        } else {
            safePermissions(target).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getPermitido()))
                    .forEach(p -> currentKeys.add(
                            key(p.getModulo(), p.getRecurso(), p.getOperacao())
                    ));
        }

        StackPane pages = new StackPane();
        pages.setMinHeight(470);

        VBox pageIdentity = buildWizardIdentity(target, pages);
        VBox pagePermissions = buildWizardPermissions(currentKeys);
        VBox pageTemplate = buildWizardTemplate();
        wireTemplateToPermissions(pageTemplate, pagePermissions);
        VBox pageReview = buildWizardReview(target);

        List<Node> wizardPages = List.of(
                pageIdentity,
                pageTemplate,
                pagePermissions,
                pageReview
        );

        // O StackPane funciona como contentor de páginas sobrepostas.
        // Sem adicionar explicitamente as páginas aos filhos, o assistente
        // apresentava apenas a área branca do contentor.
        pages.getChildren().setAll(wizardPages);
        pages.setAlignment(Pos.TOP_LEFT);

        wizardPages.forEach(page -> {
            page.setVisible(false);
            page.setManaged(false);
        });
        pageIdentity.setVisible(true);
        pageIdentity.setManaged(true);

        HBox stepper = buildWizardStepper();
        Label stepTitle = new Label();
        stepTitle.getStyleClass().add("kubata-profile-wizard-step-title");

        Label stepHint = new Label();
        stepHint.getStyleClass().add("kubata-profile-wizard-step-hint");
        stepHint.setWrapText(true);

        Button back = new Button(
                "Anterior",
                IconUtils.icon(Feather.CHEVRON_LEFT, 12)
        );
        back.getStyleClass().add("button-outlined");

        Button next = new Button(
                "Continuar",
                IconUtils.icon(Feather.CHEVRON_RIGHT, 12)
        );
        next.getStyleClass().add("button-primary");

        Button finish = new Button(
                "Criar perfil",
                IconUtils.icon(Feather.CHECK, 12)
        );
        finish.getStyleClass().add("button-primary");
        finish.setVisible(false);
        finish.setManaged(false);

        Button cancel = new Button(
                "Cancelar",
                IconUtils.icon(Feather.X, 12)
        );
        cancel.getStyleClass().add("button-outlined");

        int[] step = {0};

        Runnable refreshPage = () -> {
            for (int i = 0; i < wizardPages.size(); i++) {
                boolean visible = i == step[0];
                wizardPages.get(i).setVisible(visible);
                wizardPages.get(i).setManaged(visible);
            }

            String[] titles = {
                    "1. Identificação",
                    "2. Modelo de acesso",
                    "3. Permissões",
                    "4. Revisão e instalação"
            };
            String[] hints = {
                    "Defina identidade, empresa e finalidade do perfil.",
                    "Comece com um modelo seguro e personalize-o no passo seguinte.",
                    "Configure exactamente o que o perfil pode ver e executar.",
                    "Confirme o resultado antes de aplicar o perfil ao sistema."
            };

            stepTitle.setText(titles[step[0]]);
            stepHint.setText(hints[step[0]]);

            for (int i = 0; i < stepper.getChildren().size(); i++) {
                Node node = stepper.getChildren().get(i);
                node.getStyleClass().remove("kubata-profile-wizard-step-active");
                if (i == step[0]) {
                    node.getStyleClass().add("kubata-profile-wizard-step-active");
                }
            }

            back.setDisable(step[0] == 0);
            next.setVisible(step[0] < wizardPages.size() - 1);
            next.setManaged(step[0] < wizardPages.size() - 1);
            finish.setVisible(step[0] == wizardPages.size() - 1);
            finish.setManaged(step[0] == wizardPages.size() - 1);
            finish.setText(isNew ? "Criar perfil" : "Guardar alterações");
            stepper.getChildren().get(0).setAccessibleText("Passo " + (step[0] + 1));
        };

        next.setOnAction(e -> {
            if (!validateWizardStep(step[0], target, pageIdentity, pagePermissions)) {
                return;
            }
            step[0]++;
            refreshReview(pageReview, target, pagePermissions);
            refreshPage.run();
        });

        back.setOnAction(e -> {
            if (step[0] > 0) {
                step[0]--;
                refreshPage.run();
            }
        });

        cancel.setOnAction(e -> modalManager.hideModal());

        finish.setOnAction(e -> {
            if (!validateWizardStep(0, target, pageIdentity, pagePermissions)
                    || !validateWizardStep(2, target, pageIdentity, pagePermissions)) {
                return;
            }

            persistWizard(target, pageIdentity, pagePermissions, isNew);
        });

        HBox footer = new HBox(8, cancel, new Pane(), back, next, finish);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-profile-wizard-footer");

        VBox content = new VBox(10, stepper, stepTitle, stepHint, pages, footer);
        content.setPadding(new Insets(4));
        VBox.setVgrow(pages, Priority.ALWAYS);

        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(1120, 760)
                .minSize(900, 650)
                .maxSize(1400, 920)
                .maximizable(true)
                .minimizable(true)
                .windowControls(true)
                .closeOnOverlayClick(false)
                .title(isNew
                        ? "Assistente de Instalação — Novo Perfil"
                        : "Assistente de Configuração — " + safe(target.getCodigo(), ""));

        modalManager.showModal(content, config);
        refreshPage.run();
    }

    private VBox buildWizardIdentity(PerfilAcesso target, StackPane pages) {
        VBox page = wizardPage();

        VBox intro = wizardSection(
                "Identidade do perfil",
                "O código é a referência técnica. A descrição ajuda os administradores a entenderem a finalidade."
        );

        GridPane grid = formGrid();

        TextField code = new TextField(safe(target.getCodigo(), ""));
        code.setPromptText("Ex.: OPERADOR_VENDAS");
        code.setId("profile-code");

        TextField description = new TextField(safe(target.getDescricao(), ""));
        description.setPromptText("Ex.: Operador de vendas e clientes");
        description.setId("profile-description");

        ComboBox<Empresa> empresa = new ComboBox<>();
        empresa.getItems().addAll(empresaRepository.findAll());
        empresa.setValue(target.getEmpresa());
        empresa.setPromptText("Global — todas as empresas");
        empresa.setMaxWidth(Double.MAX_VALUE);
        empresa.setId("profile-company");

        TextArea observations = new TextArea(safe(target.getObservacoes(), ""));
        observations.setPromptText("Notas internas sobre a utilização deste perfil...");
        observations.setWrapText(true);
        observations.setPrefRowCount(5);
        observations.setId("profile-observations");

        CheckBox active = new CheckBox("Perfil disponível para utilização");
        active.setSelected(target.getActivo() == null || target.getActivo());

        grid.add(new Label("Código:*"), 0, 0);
        grid.add(code, 1, 0);
        GridPane.setHgrow(code, Priority.ALWAYS);

        grid.add(new Label("Descrição:*"), 0, 1);
        grid.add(description, 1, 1);
        GridPane.setHgrow(description, Priority.ALWAYS);

        grid.add(new Label("Empresa:"), 0, 2);
        grid.add(empresa, 1, 2);
        GridPane.setHgrow(empresa, Priority.ALWAYS);

        grid.add(new Label("Observações:"), 0, 3);
        grid.add(observations, 1, 3);
        GridPane.setHgrow(observations, Priority.ALWAYS);

        intro.getChildren().addAll(grid, active);
        page.getChildren().addAll(
                intro,
                infoCard(
                        "Escopo",
                        "Sem empresa = perfil global. Com empresa = perfil disponível apenas nesse contexto empresarial."
                )
        );

        page.setUserData(new Object[]{code, description, empresa, observations, active});
        return page;
    }

    private VBox buildWizardTemplate() {
        VBox page = wizardPage();

        VBox intro = wizardSection(
                "Modelo de acesso",
                "Escolha uma política inicial. O Kubata preenche a matriz e permite ajustar tudo antes de guardar."
        );

        ComboBox<String> template = new ComboBox<>(FXCollections.observableArrayList(
                "Personalizado — começar do zero",
                "Consulta — apenas leitura",
                "Operador — executar tarefas",
                "Gestor — operação + administração",
                "Administrador — acesso amplo"
        ));
        template.setValue("Personalizado — começar do zero");
        template.setMaxWidth(Double.MAX_VALUE);
        template.setId("profile-template");

        VBox preview = new VBox(8);
        preview.getStyleClass().add("kubata-profile-template-preview");

        Label title = new Label("Personalizado");
        title.getStyleClass().add("kubata-profile-template-title");
        Label description = new Label(
                "Nenhuma permissão será pré-seleccionada."
        );
        description.setWrapText(true);
        description.getStyleClass().add("kubata-profile-template-description");

        Label security = new Label(
                "Recomendação: comece com o mínimo necessário e aumente apenas quando a função exigir."
        );
        security.setWrapText(true);
        security.getStyleClass().add("kubata-profile-template-security");

        preview.getChildren().addAll(title, description, security);
        page.getChildren().addAll(intro, template, preview);
        page.setUserData(template);

        template.setOnAction(e -> {
            String selected = template.getValue();
            switch (selected) {
                case "Consulta — apenas leitura" -> {
                    title.setText("Consulta");
                    description.setText("Selecciona VER para os recursos disponíveis.");
                    security.setText("Adequado para utilizadores que consultam informação sem alterar dados.");
                }
                case "Operador — executar tarefas" -> {
                    title.setText("Operador");
                    description.setText("Selecciona VER, CRIAR e EDITAR onde aplicável.");
                    security.setText("Bom ponto de partida para funções operacionais. APAGAR e APROVAR ficam fora do modelo.");
                }
                case "Gestor — operação + administração" -> {
                    title.setText("Gestor");
                    description.setText("Inclui operações de gestão, com confirmação na matriz.");
                    security.setText("Inclui operações com maior impacto; reveja o passo de permissões antes de guardar.");
                }
                case "Administrador — acesso amplo" -> {
                    title.setText("Administrador");
                    description.setText("Selecciona todas as operações conhecidas na matriz.");
                    security.setText("Perfil de alta confiança. Deve ser atribuído apenas a funções que realmente necessitem deste nível.");
                }
                default -> {
                    title.setText("Personalizado");
                    description.setText("Nenhuma permissão será pré-seleccionada.");
                    security.setText("Comece pelo princípio do menor privilégio.");
                }
            }
        });

        return page;
    }

    @SuppressWarnings("unchecked")
    private void wireTemplateToPermissions(
            VBox templatePage,
            VBox permissionsPage) {

        Object value = templatePage.getUserData();
        if (!(value instanceof ComboBox<?> combo)) {
            return;
        }

        ComboBox<String> template = (ComboBox<String>) combo;
        template.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null || selected.startsWith("Personalizado")) {
                return;
            }

            List<CheckBox> checks = permissionBoxes(permissionsPage);
            String preset = switch (selected) {
                case "Consulta — apenas leitura" -> "Só leitura";
                case "Operador — executar tarefas" -> "Operacional";
                case "Gestor — operação + administração" -> "Gestão";
                case "Administrador — acesso amplo" -> "Acesso total";
                default -> "Sem alterações";
            };

            applyPermissionPreset(preset, checks);
        });
    }

    private VBox buildWizardPermissions(Set<String> currentKeys) {
        VBox page = wizardPage();

        HBox tools = new HBox(8);
        tools.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> preset = new ComboBox<>(FXCollections.observableArrayList(
                "Sem alterações",
                "Limpar tudo",
                "Só leitura",
                "Operacional",
                "Gestão",
                "Acesso total"
        ));
        preset.setValue("Sem alterações");
        preset.setPrefWidth(165);

        TextField search = new TextField();
        search.setPromptText("Filtrar recurso...");
        search.setPrefWidth(250);

        Label count = new Label("0 permissões activas");
        count.getStyleClass().add("kubata-profile-permission-count");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Accordion accordion = new Accordion();
        accordion.setId("profile-permission-accordion");

        Map<String, List<CheckBox>> moduleChecks = new LinkedHashMap<>();
        List<CheckBox> allChecks = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<PermissaoPerfil.Operacao>>> module : ESTRUTURA_PERMISSOES.entrySet()) {
            VBox moduleBox = new VBox(8);
            moduleBox.setPadding(new Insets(9));

            HBox moduleTools = new HBox(7);
            moduleTools.setAlignment(Pos.CENTER_LEFT);

            Label moduleName = new Label(module.getKey());
            moduleName.getStyleClass().add("kubata-profile-module-name");

            Button selectAll = new Button("Tudo");
            selectAll.getStyleClass().add("button-outlined");
            selectAll.setOnAction(e ->
                    moduleChecks.get(module.getKey()).forEach(cb -> cb.setSelected(true))
            );

            Button clear = new Button("Limpar");
            clear.getStyleClass().add("button-outlined");
            clear.setOnAction(e ->
                    moduleChecks.get(module.getKey()).forEach(cb -> cb.setSelected(false))
            );

            moduleTools.getChildren().addAll(moduleName, new Pane(), selectAll, clear);
            HBox.setHgrow(moduleTools.getChildren().get(1), Priority.ALWAYS);
            moduleBox.getChildren().add(moduleTools);

            List<CheckBox> moduleCheckList = new ArrayList<>();
            moduleChecks.put(module.getKey(), moduleCheckList);

            for (Map.Entry<String, List<PermissaoPerfil.Operacao>> resource : module.getValue().entrySet()) {
                VBox resourceBox = new VBox(6);
                resourceBox.getStyleClass().add("kubata-profile-resource");

                Label resourceName = new Label(resource.getKey());
                resourceName.getStyleClass().add("kubata-profile-resource-name");

                HBox ops = new HBox(10);
                ops.setAlignment(Pos.CENTER_LEFT);

                for (PermissaoPerfil.Operacao operation : resource.getValue()) {
                    CheckBox cb = new CheckBox(operationLabel(operation));
                    cb.setUserData(key(module.getKey(), resource.getKey(), operation));
                    cb.setSelected(currentKeys.contains(key(
                            module.getKey(), resource.getKey(), operation
                    )));
                    cb.getStyleClass().add(operationCss(operation));

                    cb.selectedProperty().addListener((obs, old, selected) ->
                            count.setText(
                                    selectedPermissionCount(allChecks) + " permissões activas"
                            )
                    );

                    moduleCheckList.add(cb);
                    allChecks.add(cb);
                    ops.getChildren().add(cb);
                }

                resourceBox.getChildren().addAll(resourceName, ops);
                moduleBox.getChildren().add(resourceBox);
            }

            TitledPane pane = new TitledPane(module.getKey(), moduleBox);
            pane.setGraphic(IconUtils.icon(moduleIcon(module.getKey()), 13));
            accordion.getPanes().add(pane);
        }

        // Liga os templates a todos os checkboxes.
        preset.setOnAction(e -> applyPermissionPreset(
                preset.getValue(), allChecks
        ));

        tools.getChildren().addAll(preset, search, spacer, count);
        VBox.setVgrow(accordion, Priority.ALWAYS);

        search.textProperty().addListener((obs, old, value) -> {
            String q = value == null ? "" : value.trim().toLowerCase();
            accordion.getPanes().forEach(pane -> {
                VBox moduleBox = (VBox) pane.getContent();
                moduleBox.getChildren().stream()
                        .filter(node -> node instanceof VBox)
                        .map(node -> (VBox) node)
                        .forEach(resourceBox -> {
                            if (resourceBox.getStyleClass().contains("kubata-profile-resource")) {
                                String resourceText = "";
                                if (!resourceBox.getChildren().isEmpty()
                                        && resourceBox.getChildren().get(0) instanceof Label label) {
                                    resourceText = label.getText().toLowerCase(Locale.ROOT);
                                }
                                resourceBox.setVisible(q.isBlank() || resourceText.contains(q));
                                resourceBox.setManaged(resourceBox.isVisible());
                            }
                        });
            });
        });

        page.getChildren().addAll(
                wizardSection(
                        "Matriz de permissões",
                        "Cada caixa representa uma operação real. Use os atalhos por módulo para acelerar a configuração."
                ),
                tools,
                accordion,
                infoCard(
                        "Operações sensíveis",
                        "APAGAR, ANULAR, APROVAR, FORÇAR CRÉDITO e ALTERAR PREÇO/MARGEM possuem maior impacto e devem ser concedidas apenas quando necessárias."
                )
        );

        page.setUserData(allChecks);
        return page;
    }

    private VBox buildWizardReview(PerfilAcesso target) {
        VBox page = wizardPage();
        page.setId("profile-review-page");
        return page;
    }

    private HBox buildWizardStepper() {
        HBox steps = new HBox(7);
        steps.getStyleClass().add("kubata-profile-wizard-stepper");
        for (String title : new String[]{
                "1 · Identificação",
                "2 · Modelo",
                "3 · Permissões",
                "4 · Revisão"
        }) {
            Label label = new Label(title);
            label.getStyleClass().add("kubata-profile-wizard-step");
            steps.getChildren().add(label);
        }
        return steps;
    }

    private boolean validateWizardStep(
            int step,
            PerfilAcesso target,
            VBox identityPage,
            VBox permissionsPage) {

        if (step == 0) {
            Object[] data = (Object[]) identityPage.getUserData();
            TextField code = (TextField) data[0];
            TextField description = (TextField) data[1];

            String normalizedCode = code.getText().trim().toUpperCase(Locale.ROOT);

            if (normalizedCode.isBlank() || description.getText().trim().isBlank()) {
                modalManager.alert(
                        "Dados incompletos",
                        "Código e descrição são obrigatórios.",
                        "warning",
                        null
                );
                return false;
            }

            if (!normalizedCode.matches("[A-Z0-9_\\-]{3,20}")) {
                modalManager.alert(
                        "Código inválido",
                        "Use entre 3 e 20 caracteres: A-Z, números, hífen ou underscore.",
                        "warning",
                        null
                );
                return false;
            }

            Optional<PerfilAcesso> existing = perfilRepository.findByCodigo(normalizedCode);
            if (existing.isPresent()
                    && (target.getId() == null
                    || !Objects.equals(existing.get().getId(), target.getId()))) {
                modalManager.alert(
                        "Código já utilizado",
                        "Já existe um perfil com o código " + normalizedCode + ".",
                        "warning",
                        null
                );
                return false;
            }

            target.setCodigo(normalizedCode);
            target.setDescricao(description.getText().trim());

            ComboBox<Empresa> empresa = (ComboBox<Empresa>) data[2];
            target.setEmpresa(empresa.getValue());

            TextArea observations = (TextArea) data[3];
            target.setObservacoes(observations.getText().trim());

            CheckBox active = (CheckBox) data[4];
            target.setActivo(active.isSelected());

            return true;
        }

        if (step == 2) {
            List<CheckBox> checks = permissionBoxes(permissionsPage);

            if (checks.isEmpty()) {
                modalManager.alert(
                        "Matriz vazia",
                        "Não foi possível carregar a matriz de permissões.",
                        "warning",
                        null
                );
                return false;
            }

            if (checks.stream().noneMatch(CheckBox::isSelected)) {
                modalManager.alert(
                        "Perfil sem permissões",
                        "Seleccione pelo menos uma permissão ou regresse ao passo anterior.",
                        "warning",
                        null
                );
                return false;
            }
        }

        return true;
    }

    private void refreshReview(VBox review, PerfilAcesso target, VBox permissionsPage) {
        review.getChildren().clear();
        review.setPadding(new Insets(16));

        VBox summary = wizardSection(
                "Perfil pronto para aplicação",
                "Reveja os dados abaixo. O sistema só grava depois de confirmar."
        );

        summary.getChildren().addAll(
                reviewRow("Código", safe(target.getCodigo(), "-")),
                reviewRow("Descrição", safe(target.getDescricao(), "-")),
                reviewRow(
                        "Âmbito",
                        target.getEmpresa() == null
                                ? "Global"
                                : companyName(target.getEmpresa())
                ),
                reviewRow(
                        "Estado",
                        Boolean.TRUE.equals(target.getActivo())
                                ? "Activo"
                                : "Inactivo"
                )
        );

        List<CheckBox> checks = permissionBoxes(permissionsPage);
        long selected = selectedPermissionCount(checks);
        long sensitive = checks.stream()
                .filter(CheckBox::isSelected)
                .filter(cb -> isSensitiveKey(String.valueOf(cb.getUserData())))
                .count();

        long modules = checks.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> String.valueOf(cb.getUserData()).split("\\|", -1))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[0])
                .distinct()
                .count();

        summary.getChildren().addAll(
                reviewRow("Módulos", modules + " módulo(s) com acesso"),
                reviewRow("Permissões", selected + " operações activas")
        );

        String riskTitle;
        String riskMessage;
        if (sensitive >= 5) {
            riskTitle = "Nível de acesso elevado";
            riskMessage = sensitive + " operações sensíveis foram seleccionadas. "
                    + "Reveja especialmente APROVAR, ANULAR, APAGAR e operações financeiras.";
        } else if (sensitive > 0) {
            riskTitle = "Atenção de segurança";
            riskMessage = sensitive + " operação(ões) sensível(eis) seleccionada(s). "
                    + "Confirme que fazem parte da função.";
        } else {
            riskTitle = "Princípio do menor privilégio";
            riskMessage = "Não foram seleccionadas operações sensíveis.";
        }

        VBox risk = infoCard(riskTitle, riskMessage);

        VBox guidance = infoCard(
                "Próximo passo",
                "Depois de guardar, associe este perfil aos utilizadores na área Segurança → Utilizadores."
        );

        review.getChildren().addAll(summary, risk, guidance);
    }

    private void persistWizard(
            PerfilAcesso target,
            VBox identityPage,
            VBox permissionsPage,
            boolean isNew) {

        if (!can(isNew ? "CRIAR" : "EDITAR")) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para guardar este perfil.",
                    "warning",
                    null
            );
            return;
        }

        List<PermissaoPerfil> permissions = new ArrayList<>();
        for (CheckBox cb : permissionBoxes(permissionsPage)) {
            if (!cb.isSelected()) {
                continue;
            }

            String[] parts = String.valueOf(cb.getUserData()).split("\\|", -1);
            permissions.add(
                    PermissaoPerfil.builder()
                            .modulo(parts[0])
                            .recurso(parts[1])
                            .operacao(PermissaoPerfil.Operacao.valueOf(parts[2]))
                            .permitido(true)
                            .build()
            );
        }

        try {
            PerfilAcesso saved = acessoService.salvarPerfilComPermissoes(
                    target,
                    permissions
            );

            modalManager.hideModal();
            loadPerfis();

            modalManager.success(
                    isNew ? "Perfil criado" : "Perfil actualizado",
                    "O perfil " + saved.getCodigo()
                            + " foi configurado com " + permissions.size()
                            + " permissões."
            );
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "Erro ao guardar perfil",
                    "Não foi possível aplicar o perfil e as suas permissões.",
                    ex
            );
        }
    }

    private void duplicatePerfil(PerfilAcesso original) {
        if (original == null || !can("CRIAR")) return;

        String baseCode = safe(original.getCodigo(), "PERFIL")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "_");

        if (baseCode.length() > 14) {
            baseCode = baseCode.substring(0, 14);
        }

        String duplicateCode = baseCode + "_COPY";
        int suffix = 2;
        while (perfilRepository.findByCodigo(duplicateCode).isPresent()) {
            String suffixText = "_" + suffix++;
            int maxBase = Math.max(3, 20 - suffixText.length());
            duplicateCode = baseCode.substring(0, Math.min(baseCode.length(), maxBase))
                    + suffixText;
        }

        PerfilAcesso copy = PerfilAcesso.builder()
                .codigo(duplicateCode)
                .descricao(safe(original.getDescricao(), "Perfil") + " — Cópia")
                .observacoes(original.getObservacoes())
                .sistema(false)
                .activo(true)
                .empresa(original.getEmpresa())
                .build();

        Set<String> permissions = safePermissions(original).stream()
                .filter(p -> Boolean.TRUE.equals(p.getPermitido()))
                .map(p -> key(p.getModulo(), p.getRecurso(), p.getOperacao()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        showPerfilDialog(copy, permissions);
    }

    private void removeSelectedPerfil() {
        PerfilAcesso selected = selectedPerfil().orElse(null);
        if (selected == null) {
            modalManager.alert(
                    "Perfil não seleccionado",
                    "Seleccione um perfil para remover.",
                    "warning",
                    null
            );
            return;
        }

        if (!can("APAGAR")) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para remover perfis.",
                    "warning",
                    null
            );
            return;
        }

        if (Boolean.TRUE.equals(selected.getSistema())) {
            modalManager.alert(
                    "Perfil protegido",
                    "Perfis de sistema não podem ser removidos.",
                    "warning",
                    null
            );
            return;
        }

        modalManager.showConfirm(
                "Remover perfil",
                "Confirma a remoção do perfil " + selected.getCodigo() + "?",
                () -> {
                    try {
                        acessoService.excluirPerfil(selected);
                        loadPerfis();
                    } catch (Exception ex) {
                        modalManager.showErrorModal(
                                "Erro ao remover perfil",
                                "O perfil não pôde ser removido.",
                                ex
                        );
                    }
                }
        );
    }

    private void showSecurityGuide() {
        VBox content = new VBox(12);
        content.getChildren().addAll(
                guideCard("1", "Crie o perfil",
                        "Defina um código e uma finalidade claros."),
                guideCard("2", "Escolha o modelo",
                        "Comece por consulta, operação ou gestão."),
                guideCard("3", "Ajuste permissões",
                        "Conceda apenas as operações necessárias."),
                guideCard("4", "Reveja e guarde",
                        "Confirme as operações sensíveis antes de aplicar.")
        );

        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(650, 520)
                .minSize(560, 460)
                .maximizable(false)
                .minimizable(false);

        modalManager.showModal(
                content,
                config
                        .title("Modelo de governação de acessos")
                        .icon(Feather.SHIELD)
        );
    }

    private VBox guideCard(String step, String title, String description) {
        HBox row = new HBox(11);
        row.setAlignment(Pos.CENTER_LEFT);

        Label number = new Label(step);
        number.getStyleClass().add("kubata-profile-guide-number");

        VBox text = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-profile-guide-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("kubata-profile-guide-description");
        text.getChildren().addAll(titleLabel, descriptionLabel);

        row.getChildren().addAll(number, text);

        VBox card = new VBox(row);
        card.getStyleClass().add("kubata-profile-guide-card");
        return card;
    }

    private VBox wizardPage() {
        VBox page = new VBox(12);
        page.setPadding(new Insets(2));
        page.getStyleClass().add("kubata-profile-wizard-page");
        return page;
    }

    private VBox wizardSection(String title, String description) {
        VBox section = new VBox(6);
        section.getStyleClass().add("kubata-profile-wizard-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-profile-wizard-section-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("kubata-profile-wizard-section-description");

        section.getChildren().addAll(titleLabel, descriptionLabel);
        return section;
    }

    private VBox infoCard(String title, String message) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kubata-profile-info-card");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-profile-info-title");

        Label m = new Label(message);
        m.setWrapText(true);
        m.getStyleClass().add("kubata-profile-info-message");

        card.getChildren().addAll(t, m);
        return card;
    }

    private VBox reviewRow(String label, String value) {
        VBox row = new VBox(2);
        row.getStyleClass().add("kubata-profile-review-row");

        Label l = new Label(label.toUpperCase(Locale.ROOT));
        l.getStyleClass().add("kubata-profile-review-label");

        Label v = new Label(value);
        v.setWrapText(true);
        v.getStyleClass().add("kubata-profile-review-value");

        row.getChildren().addAll(l, v);
        return row;
    }

    @SuppressWarnings("unchecked")
    private List<CheckBox> permissionBoxes(VBox permissionsPage) {
        Object userData = permissionsPage.getUserData();
        if (userData instanceof List<?>) {
            return (List<CheckBox>) userData;
        }
        return List.of();
    }

    private void applyPermissionPreset(String preset, List<CheckBox> boxes) {
        if (preset == null || "Sem alterações".equals(preset)) {
            return;
        }

        for (CheckBox cb : boxes) {
            String key = String.valueOf(cb.getUserData());
            String[] parts = key.split("\\|", -1);
            String operation = parts.length >= 3 ? parts[2] : "";

            boolean selected = switch (preset) {
                case "Limpar tudo" -> false;
                case "Só leitura" -> "VER".equals(operation);
                case "Operacional" -> List.of("VER", "CRIAR", "EDITAR", "IMPRIMIR").contains(operation);
                case "Gestão" -> List.of(
                        "VER", "CRIAR", "EDITAR", "APAGAR",
                        "IMPRIMIR", "EXPORTAR", "APROVAR", "ANULAR"
                ).contains(operation);
                case "Acesso total" -> true;
                default -> cb.isSelected();
            };

            cb.setSelected(selected);
        }
    }

    private int selectedPermissionCount(List<CheckBox> boxes) {
        return (int) boxes.stream().filter(CheckBox::isSelected).count();
    }

    private String key(String module, String resource, PermissaoPerfil.Operacao operation) {
        return module + "|" + resource + "|" + operation.name();
    }

    private boolean isSensitive(PermissaoPerfil permission) {
        return permission != null
                && isSensitiveOperation(permission.getOperacao());
    }

    private boolean isSensitiveKey(String key) {
        String[] parts = key.split("\\|", -1);
        if (parts.length < 3) return false;

        try {
            return isSensitiveOperation(
                    PermissaoPerfil.Operacao.valueOf(parts[2])
            );
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isSensitiveOperation(PermissaoPerfil.Operacao operation) {
        return switch (operation) {
            case APAGAR, ANULAR, APROVAR, FORCAR_CREDITO,
                 ALTERAR_PRECO_MARGEM, APROVACAO_INTERNA -> true;
            default -> false;
        };
    }

    private Feather moduleIcon(String module) {
        return switch (module) {
            case "FATURACAO" -> Feather.FILE_TEXT;
            case "ESTOQUE" -> Feather.DATABASE;
            case "FINANCEIRO" -> Feather.DOLLAR_SIGN;
            case "RH" -> Feather.BRIEFCASE;
            case "COMPRAS" -> Feather.SHOPPING_CART;
            case "VENDAS" -> Feather.TAG;
            case "CONTABILIDADE" -> Feather.BOOK_OPEN;
            case "ADMINISTRATOR" -> Feather.SHIELD;
            default -> Feather.LAYERS;
        };
    }

    private String operationLabel(PermissaoPerfil.Operacao operation) {
        return switch (operation) {
            case VER -> "Ver";
            case CRIAR -> "Criar";
            case EDITAR -> "Editar";
            case APAGAR -> "Apagar";
            case IMPRIMIR -> "Imprimir";
            case EXPORTAR -> "Exportar";
            case APROVAR -> "Aprovar";
            case ANULAR -> "Anular";
            case ALTERAR_PRECO_MARGEM -> "Alterar preço/margem";
            case FORCAR_CREDITO -> "Forçar crédito";
            case APROVACAO_INTERNA -> "Aprovação interna";
        };
    }

    private String operationCss(PermissaoPerfil.Operacao operation) {
        if (isSensitiveOperation(operation)) {
            return "kubata-profile-permission-sensitive";
        }
        return "kubata-profile-permission";
    }

    private Optional<PerfilAcesso> selectedPerfil() {
        return table == null || table.getSelectionModel().getSelectedItem() == null
                ? Optional.empty()
                : Optional.of(table.getSelectionModel().getSelectedItem());
    }

    private void refreshActionPermissions() {
        if (btnNovoPerfil != null) {
            btnNovoPerfil.setDisable(!can("CRIAR"));
        }

        if (table != null && btnEditar != null) {
            PerfilAcesso selected = table.getSelectionModel().getSelectedItem();
            updateActionState(selected);
        }
    }

    private void updateActionState(PerfilAcesso selected) {
        boolean has = selected != null;
        boolean system = has && Boolean.TRUE.equals(selected.getSistema());

        btnEditar.setDisable(!has || system || !can("EDITAR"));
        btnConfigurarPermissoes.setDisable(!has || system || !can("EDITAR"));
        btnDuplicar.setDisable(!has || !can("CRIAR"));
        btnRemover.setDisable(!has || system || !can("APAGAR"));
    }

    private boolean can(String operation) {
        var user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) {
            return true;
        }

        try {
            PermissaoPerfil.Operacao op =
                    "REMOVER".equalsIgnoreCase(operation)
                            ? PermissaoPerfil.Operacao.APAGAR
                            : PermissaoPerfil.Operacao.valueOf(operation.toUpperCase(Locale.ROOT));

            return acessoService.temAcesso(
                    user,
                    "ADMINISTRATOR",
                    "PERFIS",
                    op
            );
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private VBox formGridWrapper(Control control) {
        VBox box = new VBox(5);
        box.getChildren().add(control);
        return box;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        ColumnConstraints labels = new ColumnConstraints(100);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labels, values);
        return grid;
    }

    private String companyName(Empresa empresa) {
        return empresa == null ? "Global" : safe(empresa.getNome(), "Empresa");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean contains(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
