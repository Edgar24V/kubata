package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.admin.ui.wizard.EmpresaWizardView;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.service.AcessoService;
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
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmpresaView extends VBox {

    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final EmpresaWizardView empresaWizardView;
    private final PersistenceService persistenceService;

    private AdvancedTableView<Empresa> table;
    private ObservableList<Empresa> empresas;
    private TextField searchField;

    public EmpresaView(EmpresaRepository empresaRepository, AcessoService acessoService, SessionManager sessionManager, 
                       ModalManager modalManager, EmpresaWizardView empresaWizardView, PersistenceService persistenceService) {
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.empresaWizardView = empresaWizardView;
        this.persistenceService = persistenceService;

        empresas = FXCollections.observableArrayList();
        buildUI();
    }

    @PostConstruct
    private void init() {
        Platform.runLater(() -> {
            try {
                loadEmpresas();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

        Label title = new Label("Gestão de Empresas");
        title.getStyleClass().add("h3");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome ou NIF...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterEmpresas(newVal));

        Button btnNovo = new Button("Nova Empresa", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showEmpresaDialog(null));

        Button btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
        btnEditar.getStyleClass().add("button-outlined");
        btnEditar.setOnAction(e -> {
            Empresa selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showEmpresaDialog(selected);
        });

        Button btnRemover = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnRemover.getStyleClass().add("button-danger");
        btnRemover.setDisable(true);
        btnRemover.setOnAction(e -> removeEmpresa());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> {
            loadEmpresas();
            modalManager.alert("Atualização", "Lista de empresas atualizada com sucesso.", "info", null);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, searchField, btnNovo, btnEditar, btnRemover, btnRefresh);
        return box;
    }

    private AdvancedTableView<Empresa> buildTable() {
        AdvancedTableView<Empresa> tv = new AdvancedTableView<>();
        tv.setData(empresas);
        
        TableUtils.standardize(tv);
        tv.setEditable(true);
        
        TableColumn<Empresa, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setCellFactory(tc -> TextTableCell.create());
        colNome.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setNome(event.getNewValue());
            saveEmpresaInline(e);
        });
        colNome.setPrefWidth(200);

        TableColumn<Empresa, String> colNomeComercial = new TableColumn<>("Nome Comercial");
        colNomeComercial.setCellValueFactory(new PropertyValueFactory<>("nomeComercial"));
        colNomeComercial.setCellFactory(tc -> TextTableCell.create());
        colNomeComercial.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setNomeComercial(event.getNewValue());
            saveEmpresaInline(e);
        });
        colNomeComercial.setPrefWidth(180);

        TableColumn<Empresa, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colNif.setCellFactory(tc -> TextTableCell.create());
        colNif.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setNif(event.getNewValue());
            saveEmpresaInline(e);
        });
        colNif.setPrefWidth(120);

        TableColumn<Empresa, String> colTipoContribuinte = TableUtils.createTextColumn("Tipo Contribuinte", col -> new SimpleStringProperty(col.getValue().getTipoContribuinte()));
        colTipoContribuinte.setPrefWidth(150);

        TableColumn<Empresa, String> colRegime = TableUtils.createTextColumn("Regime Fiscal", col -> new SimpleStringProperty(col.getValue().getRegimeFiscal()));
        colRegime.setPrefWidth(140);

        TableColumn<Empresa, java.math.BigDecimal> colCapital = new TableColumn<>("Cap. Social");
        colCapital.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getCapitalSocial()));
        colCapital.setPrefWidth(140);

        TableColumn<Empresa, String> colMorada = new TableColumn<>("Endereço");
        colMorada.setCellValueFactory(new PropertyValueFactory<>("morada"));
        colMorada.setCellFactory(tc -> TextTableCell.create());
        colMorada.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setMorada(event.getNewValue());
            saveEmpresaInline(e);
        });
        colMorada.setPrefWidth(300);

        TableColumn<Empresa, String> colMunicipio = new TableColumn<>("Município");
        colMunicipio.setCellValueFactory(new PropertyValueFactory<>("municipio"));
        colMunicipio.setCellFactory(tc -> TextTableCell.create());
        colMunicipio.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setMunicipio(event.getNewValue());
            saveEmpresaInline(e);
        });
        colMunicipio.setPrefWidth(150);

        TableColumn<Empresa, String> colProvincia = new TableColumn<>("Província");
        colProvincia.setCellValueFactory(new PropertyValueFactory<>("provincia"));
        colProvincia.setCellFactory(tc -> TextTableCell.create());
        colProvincia.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setProvincia(event.getNewValue());
            saveEmpresaInline(e);
        });
        colProvincia.setPrefWidth(150);

        TableColumn<Empresa, String> colEmail = new TableColumn<>("E-mail");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(tc -> TextTableCell.create());
        colEmail.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setEmail(event.getNewValue());
            saveEmpresaInline(e);
        });
        colEmail.setPrefWidth(180);

        TableColumn<Empresa, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colTelefone.setCellFactory(tc -> TextTableCell.create());
        colTelefone.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setTelefone(event.getNewValue());
            saveEmpresaInline(e);
        });
        colTelefone.setPrefWidth(120);

        TableColumn<Empresa, String> colIban = new TableColumn<>("IBAN");
        colIban.setCellValueFactory(new PropertyValueFactory<>("iban"));
        colIban.setCellFactory(tc -> TextTableCell.create());
        colIban.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setIban(event.getNewValue());
            saveEmpresaInline(e);
        });
        colIban.setPrefWidth(200);

        TableColumn<Empresa, String> colCertificado = new TableColumn<>("Certificado AGT");
        colCertificado.setCellValueFactory(new PropertyValueFactory<>("numeroCertificadoAGT"));
        colCertificado.setCellFactory(tc -> TextTableCell.create());
        colCertificado.setOnEditCommit(event -> {
            Empresa e = event.getRowValue();
            e.setNumeroCertificadoAGT(event.getNewValue());
            saveEmpresaInline(e);
        });
        colCertificado.setPrefWidth(150);

        TableColumn<Empresa, Boolean> colAtiva = TableUtils.createCheckColumn("Ativa", col -> new SimpleBooleanProperty(col.getValue().getAtiva()));
        colAtiva.setPrefWidth(80);
        colAtiva.setEditable(true);

        tv.getColumns().addAll(colNome, colNomeComercial, colNif, colTipoContribuinte, colRegime, colCapital, 
                              colMorada, colMunicipio, colProvincia, colEmail, colTelefone, 
                              colIban, colCertificado, colAtiva);
        return tv;
    }

    private void loadEmpresas() {
        table.setLoading(true);
        Platform.runLater(() -> {
            try {
                List<Empresa> all = empresaRepository.findAll();
                empresas.setAll(all);
            } catch (Exception e) {
                e.printStackTrace();
                modalManager.alert("Erro de Dados", "Detetados registos com formato de data incompatível. O sistema tentará corrigir automaticamente no próximo salvamento.", "warning", e);
            } finally {
                table.setLoading(false);
            }
        });
    }

    private void filterEmpresas(String query) {
        if (query == null || query.isBlank()) {
            table.setFilter(e -> true);
            return;
        }
        String lower = query.toLowerCase();
        table.setFilter(e -> (e.getNome() != null && e.getNome().toLowerCase().contains(lower)) ||
                             (e.getNif() != null && e.getNif().toLowerCase().contains(lower)));
    }

    public void showEmpresaDialog(Empresa empresa) {
        boolean isNew = (empresa == null);
        ValidationSupport validationSupport = new ValidationSupport();
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setPrefWidth(750);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(isNew ? "Registo de Nova Empresa" : "Edição de Empresa");
        title.getStyleClass().add("h2");
        header.getChildren().add(title);
        root.getChildren().add(header);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("bg-transparent");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(5, 5, 20, 5));
        grid.getStyleClass().add("bg-default");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(30);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(20);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(30);
        grid.getColumnConstraints().addAll(col1, col2, col3, col4);

        int row = 0;

        // Seção: Identificação
        addSectionTitle(grid, "Identificação", row++);
        
        TextField txtIdentificador = createStyledTextField("ID (Ex: KBT)");
        txtIdentificador.setText(empresa != null ? empresa.getIdentificador() : "");
        txtIdentificador.setMaxWidth(80);
        addField(grid, "Identificador:*", txtIdentificador, row, 0);
        validationSupport.registerValidator(txtIdentificador, Validator.createEmptyValidator("O identificador é obrigatório"));

        TextField txtNome = createStyledTextField("Nome da empresa");
        txtNome.setText(empresa != null ? empresa.getNome() : "");
        addField(grid, "Nome:*", txtNome, row++, 2);
        validationSupport.registerValidator(txtNome, Validator.createEmptyValidator("O nome é obrigatório"));

        TextField txtNif = createStyledTextField("NIF / NIPC");
        txtNif.setText(empresa != null ? empresa.getNif() : "");
        addField(grid, "NIF:*", txtNif, row, 0);
        validationSupport.registerValidator(txtNif, Validator.createEmptyValidator("O NIF é obrigatório"));

        TextField txtNomeComercial = createStyledTextField("Nome comercial");
        txtNomeComercial.setText(empresa != null ? empresa.getNomeComercial() : "");
        addField(grid, "Nome Comercial:", txtNomeComercial, row++, 2);

        ComboBox<String> cmbTipoContribuinte = new ComboBox<>(FXCollections.observableArrayList(
                "Pessoa Singular", "Pessoa Colectiva", "Não Residente"
        ));
        cmbTipoContribuinte.setMaxWidth(Double.MAX_VALUE);
        cmbTipoContribuinte.setValue(empresa != null ? empresa.getTipoContribuinte() : "Pessoa Colectiva");
        addField(grid, "Tipo Contribuinte:*", cmbTipoContribuinte, row, 0);
        validationSupport.registerValidator(cmbTipoContribuinte, Validator.createEmptyValidator("O tipo de contribuinte é obrigatório"));

        TextField txtCapital = createStyledTextField("Capital social");
        txtCapital.setText(empresa != null && empresa.getCapitalSocial() != null ? empresa.getCapitalSocial().toString() : "");
        addField(grid, "Capital Social:", txtCapital, row++, 2);

        Spinner<Integer> spAnoInicio = new Spinner<>(1900, 2100, empresa != null && empresa.getAnoInicio() != null ? empresa.getAnoInicio() : 2024);
        spAnoInicio.setMaxWidth(Double.MAX_VALUE);
        addField(grid, "Ano Início Actividade:", spAnoInicio, row, 0);
        row++;

        // Seção: Localização
        addSectionTitle(grid, "Localização e Endereço", row++);
        
        TextField txtMorada = createStyledTextField("Morada completa");
        txtMorada.setText(empresa != null ? empresa.getMorada() : "");
        addField(grid, "Endereço:", txtMorada, row, 0, 3);
        row++;

        ComboBox<String> cmbProvincia = new ComboBox<>(FXCollections.observableArrayList(
                "Bengo", "Benguela", "Bié", "Cabinda", "Cuando Cubango",
                "Cuanza Norte", "Cuanza Sul", "Cunene", "Huambo", "Huíla",
                "Luanda", "Lunda Norte", "Lunda Sul", "Malanje", "Moxico",
                "Namibe", "Uíge", "Zaire"
        ));
        cmbProvincia.setMaxWidth(Double.MAX_VALUE);
        cmbProvincia.setValue(empresa != null ? empresa.getProvincia() : "Luanda");
        addField(grid, "Província:", cmbProvincia, row, 0);

        TextField txtMunicipio = createStyledTextField("Município");
        txtMunicipio.setText(empresa != null ? empresa.getMunicipio() : "");
        addField(grid, "Município:", txtMunicipio, row++, 2);

        // Seção: Contactos
        addSectionTitle(grid, "Contactos e Presença Web", row++);

        TextField txtTelefone = createStyledTextField("Telefone principal");
        txtTelefone.setText(empresa != null ? empresa.getTelefone() : "");
        addField(grid, "Telefone:", txtTelefone, row, 0);

        TextField txtTelemovel = createStyledTextField("Telemóvel");
        txtTelemovel.setText(empresa != null ? empresa.getTelemovel() : "");
        addField(grid, "Telemóvel:", txtTelemovel, row++, 2);

        TextField txtEmail = createStyledTextField("Email de contacto");
        txtEmail.setText(empresa != null ? empresa.getEmail() : "");
        addField(grid, "Email:", txtEmail, row, 0);
        validationSupport.registerValidator(txtEmail, Validator.createRegexValidator("Email inválido", "^[A-Za-z0-9+_.-]+@(.+)$", org.controlsfx.validation.Severity.ERROR));

        TextField txtWebsite = createStyledTextField("URL do Website");
        txtWebsite.setText(empresa != null ? empresa.getWebsite() : "");
        addField(grid, "Website:", txtWebsite, row++, 2);

        // Seção: Fiscal e Bancária
        addSectionTitle(grid, "Informação Fiscal e Bancária", row++);

        TextField txtIban = createStyledTextField("IBAN");
        txtIban.setText(empresa != null ? empresa.getIban() : "");
        addField(grid, "IBAN:", txtIban, row, 0);

        TextField txtBanco = createStyledTextField("Nome do Banco");
        txtBanco.setText(empresa != null ? empresa.getBanco() : "");
        addField(grid, "Banco:", txtBanco, row++, 2);

        TextField txtCertificadoAGT = createStyledTextField("Nº Certificado AGT");
        txtCertificadoAGT.setText(empresa != null ? empresa.getNumeroCertificadoAGT() : "");
        addField(grid, "Certificado AGT:", txtCertificadoAGT, row, 0);

        ComboBox<String> cmbRegime = new ComboBox<>(FXCollections.observableArrayList(
                "Geral - 14%", "Simples", "Isento", "Especial"
        ));
        cmbRegime.setMaxWidth(Double.MAX_VALUE);
        cmbRegime.setValue(empresa != null ? empresa.getRegimeFiscal() : "Geral - 14%");
        addField(grid, "Regime Fiscal:*", cmbRegime, row++, 2);
        validationSupport.registerValidator(cmbRegime, Validator.createEmptyValidator("O regime fiscal é obrigatório"));

        // Seção: Fiscal e Comercial Avançado
        addSectionTitle(grid, "Aspectos Fiscais e Comerciais", row++);

        TextField txtCae = createStyledTextField("Código CAE");
        txtCae.setText(empresa != null ? empresa.getCae() : "");
        addField(grid, "CAE:", txtCae, row, 0);

        TextField txtBairroFiscal = createStyledTextField("Bairro Fiscal");
        txtBairroFiscal.setText(empresa != null ? empresa.getBairroFiscal() : "");
        addField(grid, "Bairro Fiscal:", txtBairroFiscal, row++, 2);

        TextField txtVolumeNegocios = createStyledTextField("Volume previsto");
        txtVolumeNegocios.setText(empresa != null && empresa.getVolumeNegociosPrevisto() != null ? empresa.getVolumeNegociosPrevisto().toString() : "");
        addField(grid, "Vol. Negócios:", txtVolumeNegocios, row, 0);

        HBox capitalBox = new HBox(5);
        capitalBox.setAlignment(Pos.CENTER_LEFT);
        TextField txtCapNac = new TextField(); txtCapNac.setPromptText("Nac%"); txtCapNac.setPrefWidth(60);
        txtCapNac.setText(empresa != null && empresa.getCapitalNacional() != null ? empresa.getCapitalNacional().toString() : "100");
        TextField txtCapEst = new TextField(); txtCapEst.setPromptText("Est%"); txtCapEst.setPrefWidth(60);
        txtCapEst.setText(empresa != null && empresa.getCapitalEstrangeiro() != null ? empresa.getCapitalEstrangeiro().toString() : "0");
        TextField txtCapPub = new TextField(); txtCapPub.setPromptText("Pub%"); txtCapPub.setPrefWidth(60);
        txtCapPub.setText(empresa != null && empresa.getCapitalPublico() != null ? empresa.getCapitalPublico().toString() : "0");
        capitalBox.getChildren().addAll(new Label("Nac"), txtCapNac, new Label("Est"), txtCapEst, new Label("Pub"), txtCapPub);
        addField(grid, "Origem Capital%:", capitalBox, row++, 2);

        // Seção: Configurações do Sistema
        addSectionTitle(grid, "Configurações do Sistema", row++);

        Spinner<Integer> spExercicio = new Spinner<>(2000, 2099, empresa != null && empresa.getExercicioActual() != null ? empresa.getExercicioActual() : 2024);
        spExercicio.setMaxWidth(Double.MAX_VALUE);
        addField(grid, "Exercício Actual:", spExercicio, row, 0);

        HBox moedasBox = new HBox(10);
        ComboBox<String> cmbMoeda = new ComboBox<>(FXCollections.observableArrayList("AOA", "USD", "EUR", "ZAR"));
        cmbMoeda.setPrefWidth(85);
        cmbMoeda.setValue(empresa != null ? empresa.getMoedaBase() : "AOA");
        ComboBox<String> cmbMoedaAlt = new ComboBox<>(FXCollections.observableArrayList("AOA", "USD", "EUR", "ZAR"));
        cmbMoedaAlt.setPrefWidth(85);
        cmbMoedaAlt.setValue(empresa != null ? empresa.getMoedaAlternativa() : "USD");
        moedasBox.getChildren().addAll(new Label("Base:"), cmbMoeda, new Label("Alt:"), cmbMoedaAlt);
        addField(grid, "Moedas (Base/Alt):", moedasBox, row++, 2);

        // Seção: Módulos e Setup Inicial
        addSectionTitle(grid, "Ativação de Módulos e Setup Inicial", row++);

        HBox setupBox = new HBox(20);
        setupBox.setAlignment(Pos.CENTER_LEFT);
        CheckBox chkAbrirExercicio = createStyledCheckBox("Abrir Exercício Contabilístico");
        chkAbrirExercicio.setSelected(true);
        CheckBox chkAbrirAnoRH = createStyledCheckBox("Abrir Ano RH");
        chkAbrirAnoRH.setSelected(true);
        setupBox.getChildren().addAll(chkAbrirExercicio, chkAbrirAnoRH);
        grid.add(setupBox, 0, row++, 4, 1);

        FlowPane modulosFlow = new FlowPane(15, 10);
        modulosFlow.setPadding(new Insets(5, 0, 10, 0));
        
        CheckBox chkFaturacao = createStyledCheckBox("Faturação");
        chkFaturacao.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("FATURACAO")));

        CheckBox chkEstoque = createStyledCheckBox("Estoque");
        chkEstoque.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("ESTOQUE")));

        CheckBox chkCompras = createStyledCheckBox("Compras");
        chkCompras.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("COMPRAS")));

        CheckBox chkRH = createStyledCheckBox("Recursos Humanos");
        chkRH.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("RH")));

        CheckBox chkFinanceiro = createStyledCheckBox("Financeiro");
        chkFinanceiro.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("FINANCEIRO")));

        CheckBox chkBanking = createStyledCheckBox("Banking");
        chkBanking.setSelected(empresa == null || (empresa.getModulos() != null && empresa.getModulos().contains("BANKING")));

        modulosFlow.getChildren().addAll(chkFaturacao, chkEstoque, chkCompras, chkRH, chkFinanceiro, chkBanking);
        grid.add(modulosFlow, 0, row, 4, 1);
        row++;

        CheckBox chkAtiva = createStyledCheckBox("Empresa Ativa e Operacional");
        chkAtiva.setSelected(empresa == null || empresa.getAtiva());
        chkAtiva.getStyleClass().add("text-success");
        chkAtiva.getStyleClass().add("text-bold");
        grid.add(chkAtiva, 0, row, 4, 1);
        row++;

        scroll.setContent(grid);
        root.getChildren().add(scroll);

        modalManager.showConfirmModal(root, isNew ? "Nova Empresa" : "Editar Empresa", () -> {
            if (validationSupport.isInvalid()) {
                modalManager.alert("Erro de Validação", "Por favor, corrija os erros nos campos destacados.", "error", null);
                return;
            }

            try {
                String nome = txtNome.getText();
                String nif = txtNif.getText();
                
                Empresa e = empresa != null ? empresa : new Empresa();
                e.setIdentificador(txtIdentificador.getText().trim());
                e.setNome(nome.trim());
                e.setNif(nif.trim());
                e.setNomeComercial(txtNomeComercial.getText());
                e.setTipoContribuinte(cmbTipoContribuinte.getValue());
                e.setMorada(txtMorada.getText().isBlank() ? null : txtMorada.getText().trim());
                e.setProvincia(cmbProvincia.getValue());
                e.setMunicipio(txtMunicipio.getText().isBlank() ? null : txtMunicipio.getText().trim());
                e.setTelefone(txtTelefone.getText().isBlank() ? null : txtTelefone.getText().trim());
                e.setTelemovel(txtTelemovel.getText().isBlank() ? null : txtTelemovel.getText().trim());
                e.setEmail(txtEmail.getText().isBlank() ? null : txtEmail.getText().trim());
                e.setWebsite(txtWebsite.getText().isBlank() ? null : txtWebsite.getText().trim());
                e.setIban(txtIban.getText().isBlank() ? null : txtIban.getText().trim());
                e.setBanco(txtBanco.getText().isBlank() ? null : txtBanco.getText().trim());
                e.setNumeroCertificadoAGT(txtCertificadoAGT.getText().isBlank() ? null : txtCertificadoAGT.getText().trim());
                e.setRegimeFiscal(cmbRegime.getValue());
                e.setExercicioActual(spExercicio.getValue());
                e.setMoedaBase(cmbMoeda.getValue());
                e.setMoedaAlternativa(cmbMoedaAlt.getValue());
                e.setAnoInicio(spAnoInicio.getValue());
                e.setCae(txtCae.getText().trim());
                e.setBairroFiscal(txtBairroFiscal.getText().trim());
                e.setAtiva(chkAtiva.isSelected());

                try {
                    if (!txtVolumeNegocios.getText().isBlank()) e.setVolumeNegociosPrevisto(new java.math.BigDecimal(txtVolumeNegocios.getText().replace(",", ".")));
                    if (!txtCapNac.getText().isBlank()) e.setCapitalNacional(new java.math.BigDecimal(txtCapNac.getText().replace(",", ".")));
                    if (!txtCapEst.getText().isBlank()) e.setCapitalEstrangeiro(new java.math.BigDecimal(txtCapEst.getText().replace(",", ".")));
                    if (!txtCapPub.getText().isBlank()) e.setCapitalPublico(new java.math.BigDecimal(txtCapPub.getText().replace(",", ".")));
                } catch (Exception ex) { /* ignore parse errors */ }

                if (isNew && chkAbrirExercicio.isSelected()) {
                    System.out.println("DEBUG: Abrindo exercício contabilístico para " + e.getNome());
                }
                if (isNew && chkAbrirAnoRH.isSelected()) {
                    System.out.println("DEBUG: Abrindo ano RH para " + e.getNome());
                }

                e.setMensagemFatura("");

                StringBuilder modulos = new StringBuilder();
                if (chkFaturacao.isSelected()) modulos.append("FATURACAO,");
                if (chkEstoque.isSelected()) modulos.append("ESTOQUE,");
                if (chkCompras.isSelected()) modulos.append("COMPRAS,");
                if (chkRH.isSelected()) modulos.append("RH,");
                if (chkFinanceiro.isSelected()) modulos.append("FINANCEIRO,");
                if (chkBanking.isSelected()) modulos.append("BANKING,");
                e.setModulos(modulos.length() > 0 ? modulos.substring(0, modulos.length() - 1) : "");

                if (!txtCapital.getText().isBlank()) {
                    try {
                        e.setCapitalSocial(new java.math.BigDecimal(txtCapital.getText().replace(",", ".")));
                    } catch (NumberFormatException ex) { }
                }

                persistenceService.saveAsync(empresaRepository, e, "EMPRESA", 
                        (isNew ? "Criada" : "Atualizada") + " empresa: " + e.getNome(),
                        saved -> {
                            loadEmpresas();
                            if (isNew) {
                                Platform.runLater(() -> empresaWizardView.start(saved));
                            }
                        });
            } catch (Exception ex) {
                modalManager.alert("Erro", "Erro ao preparar salvamento: " + ex.getMessage(), "error", ex);
            }
        }, null);
    }

    private void addSectionTitle(GridPane grid, String title, int row) {
        Label label = new Label(title.toUpperCase());
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 10 0 5 0; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        label.setMaxWidth(Double.MAX_VALUE);
        grid.add(label, 0, row, 4, 1);
    }

    private void addField(GridPane grid, String labelText, javafx.scene.Node field, int row, int col) {
        addField(grid, labelText, field, row, col, 1);
    }

    private void addField(GridPane grid, String labelText, javafx.scene.Node field, int row, int col, int colspan) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #7f8c8d;");
        grid.add(label, col, row);
        grid.add(field, col + 1, row, colspan, 1);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private CheckBox createStyledCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-cursor: hand;");
        return cb;
    }

    private void saveEmpresaInline(Empresa e) {
        persistenceService.saveAsync(empresaRepository, e, "EMPRESA", 
                "Atualização inline da empresa: " + e.getNome(), null);
    }

    private void removeEmpresa() {
        Empresa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione uma empresa para remover.", "warning", null);
            return;
        }

        modalManager.showConfirmModal(new Label("Tem certeza que deseja remover a empresa: " + selected.getNome() + "?"),
                "Remover Empresa", () -> {
            persistenceService.deleteAsync(empresaRepository, selected, null, "EMPRESA", 
                    "Removida empresa: " + selected.getNome(), this::loadEmpresas);
        }, null);
    }

    private void toggleEmpresa() {
        Empresa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione uma empresa para ativar/desativar.", "warning", null);
            return;
        }

        selected.setAtiva(!selected.getAtiva());
        persistenceService.saveAsync(empresaRepository, selected, "EMPRESA", 
                (selected.getAtiva() ? "Ativada" : "Desativada") + " empresa: " + selected.getNome(),
                saved -> loadEmpresas());
    }

}
