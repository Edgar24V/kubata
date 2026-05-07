package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.PermissaoPerfilRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
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

    private AdvancedTableView<PerfilAcesso> table;
    private ObservableList<PerfilAcesso> perfis;
    private TextField searchField;

    private static final Map<String, Map<String, List<PermissaoPerfil.Operacao>>> ESTRUTURA_PERMISSOES = new LinkedHashMap<>();
    static {
        Map<String, List<PermissaoPerfil.Operacao>> faturacao = new LinkedHashMap<>();
        faturacao.put("FATURAS", Arrays.asList(
            PermissaoPerfil.Operacao.VER, 
            PermissaoPerfil.Operacao.CRIAR, 
            PermissaoPerfil.Operacao.EDITAR, 
            PermissaoPerfil.Operacao.ANULAR, 
            PermissaoPerfil.Operacao.IMPRIMIR,
            PermissaoPerfil.Operacao.ALTERAR_PRECO_MARGEM,
            PermissaoPerfil.Operacao.FORCAR_CREDITO
        ));
        faturacao.put("CLIENTES", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        faturacao.put("NOTAS_CREDITO", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.IMPRIMIR, PermissaoPerfil.Operacao.APROVACAO_INTERNA));
        ESTRUTURA_PERMISSOES.put("FATURACAO", faturacao);

        Map<String, List<PermissaoPerfil.Operacao>> estoque = new LinkedHashMap<>();
        estoque.put("ARTIGOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        estoque.put("MOVIMENTOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.ANULAR));
        estoque.put("ARMAZENS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        ESTRUTURA_PERMISSOES.put("ESTOQUE", estoque);

        Map<String, List<PermissaoPerfil.Operacao>> financeiro = new LinkedHashMap<>();
        financeiro.put("CAIXA", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        financeiro.put("BANCOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        financeiro.put("PAGAMENTOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.APROVAR));
        ESTRUTURA_PERMISSOES.put("FINANCEIRO", financeiro);

        Map<String, List<PermissaoPerfil.Operacao>> rh = new LinkedHashMap<>();
        rh.put("FUNCIONARIOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        rh.put("PROCESSAMENTO", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.APROVAR));
        ESTRUTURA_PERMISSOES.put("RH", rh);
        
        Map<String, List<PermissaoPerfil.Operacao>> admin = new LinkedHashMap<>();
        admin.put("UTILIZADORES", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        admin.put("PERFIS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        admin.put("EMPRESAS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        admin.put("EXERCICIOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        admin.put("SERIES", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        admin.put("APLICACAO", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR));
        admin.put("PARAMETROS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.EDITAR, PermissaoPerfil.Operacao.APAGAR));
        admin.put("LICENCAS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR));
        admin.put("INTEGRACOES", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR));
        admin.put("FISCAL_AGT", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR));
        admin.put("MONITOR", Arrays.asList(PermissaoPerfil.Operacao.VER));
        admin.put("INFRAESTRUTURA", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EDITAR));
        admin.put("AUDITORIA", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.EXPORTAR));
        admin.put("BACKUP", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.CRIAR, PermissaoPerfil.Operacao.APROVAR));
        admin.put("RELATORIOS", Arrays.asList(PermissaoPerfil.Operacao.VER, PermissaoPerfil.Operacao.IMPRIMIR, PermissaoPerfil.Operacao.EXPORTAR));
        ESTRUTURA_PERMISSOES.put("ADMINISTRATOR", admin);
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

        perfis = FXCollections.observableArrayList();
        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadPerfis();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        HBox toolbar = buildToolbar();
        table = buildTable();

        getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(10);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestão de Perfis de Acesso");
        title.getStyleClass().add("h3");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por código ou descrição...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterPerfis(newVal));

        Button btnNovo = new Button("Novo Perfil", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-success");
        btnNovo.setOnAction(e -> showPerfilDialog(null));

        Button btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEditar.getStyleClass().add("button-primary");
        btnEditar.setOnAction(e -> {
            PerfilAcesso selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showPerfilDialog(selected);
            else modalManager.alert("Aviso", "Selecione um perfil para editar.", "warning", null);
        });

        Button btnRemover = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnRemover.getStyleClass().add("button-danger");
        btnRemover.setOnAction(e -> removeSelectedPerfil());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> loadPerfis());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, searchField, btnNovo, btnEditar, btnRemover, btnRefresh);
        return box;
    }

    private AdvancedTableView<PerfilAcesso> buildTable() {
        AdvancedTableView<PerfilAcesso> tv = new AdvancedTableView<>();
        tv.setData(perfis);
        
        TableUtils.standardize(tv);

        TableColumn<PerfilAcesso, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(120);

        TableColumn<PerfilAcesso, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setPrefWidth(250);

        TableColumn<PerfilAcesso, String> colEmpresa = new TableColumn<>("Empresa");
        colEmpresa.setCellValueFactory(col -> {
            PerfilAcesso perfil = col.getValue();
            // Evitar LazyInitializationException verificando se a empresa está carregada
            String empresaNome = "Global (Todas)";
            if (perfil.getEmpresa() != null) {
                try {
                    empresaNome = perfil.getEmpresa().getNome();
                } catch (org.hibernate.LazyInitializationException e) {
                    // Empresa não está carregada, usar ID como fallback
                    empresaNome = "Empresa #" + perfil.getEmpresa().getId();
                }
            }
            return new SimpleStringProperty(empresaNome);
        });
        colEmpresa.setPrefWidth(200);

        TableColumn<PerfilAcesso, Boolean> colAtivo = TableUtils.createCheckColumn("Ativo", col -> new SimpleBooleanProperty(col.getValue().getActivo()));
        colAtivo.setPrefWidth(80);

        TableColumn<PerfilAcesso, Boolean> colSistema = TableUtils.createCheckColumn("Sistema", col -> new SimpleBooleanProperty(col.getValue().getSistema()));
        colSistema.setPrefWidth(80);

        tv.getColumns().addAll(colCodigo, colDescricao, colEmpresa, colAtivo, colSistema);
        return tv;
    }

    private void loadPerfis() {
        perfis.setAll(perfilRepository.findAll());
    }

    private void filterPerfis(String query) {
        if (query == null || query.isBlank()) {
            table.setFilter(p -> true);
            return;
        }
        String lower = query.toLowerCase();
        table.setFilter(p -> p.getCodigo().toLowerCase().contains(lower) ||
                             p.getDescricao().toLowerCase().contains(lower));
    }

    private void showPerfilDialog(PerfilAcesso perfil) {
        boolean isNew = perfil == null;
        PerfilAcesso target = isNew ? new PerfilAcesso() : perfil;

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Dados Gerais
        VBox geral = new VBox(15);
        geral.setPadding(new Insets(20));

        TextField txtCodigo = new TextField(target.getCodigo());
        txtCodigo.setPromptText("Ex: PRF_VENDAS");
        txtCodigo.setDisable(!isNew && Boolean.TRUE.equals(target.getSistema()));

        TextField txtDescricao = new TextField(target.getDescricao());
        txtDescricao.setPromptText("Descrição do perfil...");

        ComboBox<Empresa> cbEmpresa = new ComboBox<>();
        cbEmpresa.setItems(FXCollections.observableArrayList(empresaRepository.findAll()));
        cbEmpresa.setValue(target.getEmpresa());
        cbEmpresa.setPromptText("Selecione a Empresa (Opcional para Global)");
        cbEmpresa.setPrefWidth(Double.MAX_VALUE);

        TextArea txtObs = new TextArea(target.getObservacoes());
        txtObs.setPrefHeight(120);
        txtObs.setMinHeight(100);

        CheckBox cbAtivo = new CheckBox("Perfil Ativo");
        cbAtivo.setSelected(target.getActivo() != null ? target.getActivo() : true);

        geral.getChildren().addAll(
            new Label("Código:"), txtCodigo,
            new Label("Descrição:"), txtDescricao,
            new Label("Empresa:"), cbEmpresa,
            new Label("Observações:"), txtObs,
            cbAtivo
        );

        Tab tabGeral = new Tab("Dados Gerais", geral);

        // Tab 2: Permissões
        VBox permissoesBox = new VBox(10);
        permissoesBox.setPadding(new Insets(10));
        
        Set<String> currentPerms = new HashSet<>();
        if (!isNew) {
            permissaoRepository.findByPerfil(target).forEach(p -> {
                if (Boolean.TRUE.equals(p.getPermitido())) {
                    currentPerms.add(p.getModulo() + "|" + p.getRecurso() + "|" + p.getOperacao());
                }
            });
        }

        Accordion accordion = new Accordion();
        for (Map.Entry<String, Map<String, List<PermissaoPerfil.Operacao>>> moduloEntry : ESTRUTURA_PERMISSOES.entrySet()) {
            VBox recursosBox = new VBox(10);
            recursosBox.setPadding(new Insets(10));

            for (Map.Entry<String, List<PermissaoPerfil.Operacao>> recursoEntry : moduloEntry.getValue().entrySet()) {
                VBox recursoItem = new VBox(5);
                recursoItem.getStyleClass().add("card-item");
                
                Label lblRecurso = new Label(recursoEntry.getKey());
                lblRecurso.setStyle("-fx-font-weight: bold;");
                
                HBox opsBox = new HBox(15);
                
                for (PermissaoPerfil.Operacao op : recursoEntry.getValue()) {
                    CheckBox cb = new CheckBox(op.name());
                    cb.setUserData(moduloEntry.getKey() + "|" + recursoEntry.getKey() + "|" + op.name());
                    cb.setSelected(currentPerms.contains(moduloEntry.getKey() + "|" + recursoEntry.getKey() + "|" + op.name()));
                    opsBox.getChildren().add(cb);
                }
                
                recursoItem.getChildren().addAll(lblRecurso, opsBox);
                recursosBox.getChildren().add(recursoItem);
            }

            TitledPane pane = new TitledPane(moduloEntry.getKey(), recursosBox);
            accordion.getPanes().add(pane);
        }

        ScrollPane scroll = new ScrollPane(accordion);
        scroll.setFitToWidth(true);
        Tab tabPermissoes = new Tab("Permissões (RBAC)", scroll);

        tabs.getTabs().addAll(tabGeral, tabPermissoes);

        // Configuração com tamanho fixo para o modal
        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(880, 600)
                .minSize(750, 550);
        
        modalManager.showConfirmModal(tabs, (isNew ? "Novo Perfil" : "Editar Perfil: " + target.getCodigo()), () -> {
            target.setCodigo(txtCodigo.getText());
            target.setDescricao(txtDescricao.getText());
            target.setEmpresa(cbEmpresa.getValue());
            target.setObservacoes(txtObs.getText());
            target.setActivo(cbAtivo.isSelected());

            List<PermissaoPerfil> newPerms = new ArrayList<>();
            for (TitledPane tp : accordion.getPanes()) {
                VBox rb = (VBox) tp.getContent();
                for (javafx.scene.Node node : rb.getChildren()) {
                    if (node instanceof VBox) {
                        VBox ri = (VBox) node;
                        HBox ob = (HBox) ri.getChildren().get(1);
                        for (javafx.scene.Node cbNode : ob.getChildren()) {
                            if (cbNode instanceof CheckBox) {
                                CheckBox cb = (CheckBox) cbNode;
                                String[] parts = ((String) cb.getUserData()).split("\\|");
                                if (cb.isSelected()) {
                                    newPerms.add(PermissaoPerfil.builder()
                                        .modulo(parts[0])
                                        .recurso(parts[1])
                                        .operacao(PermissaoPerfil.Operacao.valueOf(parts[2]))
                                        .permitido(true)
                                        .build());
                                }
                            }
                        }
                    }
                }
            }

            try {
                PerfilAcesso saved = acessoService.salvarPerfil(target);
                acessoService.salvarPermissoesPerfil(saved, newPerms);
                modalManager.alert("Sucesso", "Perfil guardado com sucesso.", "info", null);
                loadPerfis();
            } catch (Exception ex) {
                modalManager.alert("Erro", "Erro ao guardar perfil: " + ex.getMessage(), "error", ex);
            }
        }, null, config);
    }

    private void removeSelectedPerfil() {
        PerfilAcesso selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione um perfil para remover.", "warning", null);
            return;
        }

        modalManager.showConfirm("Confirmar Remoção", "Deseja realmente remover o perfil '" + selected.getCodigo() + "'?", () -> {
            try {
                acessoService.excluirPerfil(selected);
                loadPerfis();
                modalManager.alert("Sucesso", "Perfil removido com sucesso.", "info", null);
            } catch (Exception ex) {
                modalManager.alert("Erro", "Erro ao remover: " + ex.getMessage(), "error", ex);
            }
        });
    }
}
