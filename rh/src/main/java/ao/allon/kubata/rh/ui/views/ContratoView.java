package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.Contrato;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.ContratoService;
import ao.allon.kubata.rh.service.ColaboradorService;
import ao.allon.kubata.rh.ui.modal.ModalManager;
import net.synedra.validatorfx.Validator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javax.annotation.PostConstruct;

@Component
public class ContratoView extends VBox {

    @Autowired
    private ContratoService contratoService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<Contrato> tableView;
    private TableColumn<Contrato, String> colColaborador;
    private TableColumn<Contrato, String> colTipo;
    private TableColumn<Contrato, String> colDataInicio;
    private TableColumn<Contrato, String> colDataFim;
    private TableColumn<Contrato, String> colSalario;
    private TableColumn<Contrato, String> colSituacao;
    private TableColumn<Contrato, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private ComboBox<Contrato.TipoContrato> cmbTipoContrato;
    private DatePicker dpDataInicio;
    private DatePicker dpDataFim;
    private TextField txtHorario;
    private ComboBox<Contrato.RegimeContrato> cmbRegime;
    private TextField txtSalarioBase;
    private TextField txtSubsidioAlimentacao;
    private TextField txtSubsidioTransporte;
    private TextArea txtObservacoes;
    private ComboBox<Contrato.SituacaoContrato> cmbSituacao;

    private Button btnNovo;

    private ObservableList<Contrato> contratos;
    private ObservableList<Colaborador> colaboradores;
    private Contrato contratoSelecionado;
    private Validator validator;

    public ContratoView() {
        setSpacing(10);
        setPadding(new Insets(10));
    }

    @PostConstruct
    public void init() {
        Platform.runLater(() -> {
            buildUI();
            configurarTabela();
            carregarDados();
        });
    }

    private void buildUI() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label title = new Label("Gestão de Contratos");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Contrato");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colTipo = new TableColumn<>("Tipo");
        colDataInicio = new TableColumn<>("Início");
        colDataFim = new TableColumn<>("Fim");
        colSalario = new TableColumn<>("Salário Base");
        colSituacao = new TableColumn<>("Situação");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colTipo, colDataInicio, colDataFim, colSalario, colSituacao, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configura colunas
        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoContrato().toString()));
        colDataInicio.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDataInicio() != null ? data.getValue().getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        colDataFim.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDataFim() != null ? data.getValue().getDataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Indeterminado"));
        colSalario.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getSalarioBase() != null ? String.format("Kz %,.2f", data.getValue().getSalarioBase()) : ""));
        colSituacao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSituacao().toString()));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        contratos = FXCollections.observableArrayList();
        tableView.setItems(contratos);
    }

    private void configurarMenuContexto() {
        TableContextMenuHelper<Contrato> menuHelper = new TableContextMenuHelper<>(tableView)
            .withEntityName("Contrato")
            .onEdit(contrato -> abrirFormulario(contrato))
            .onRefresh(() -> carregarDados())
            .enableDelete(false) // Desabilitar delete - usar suspensão em vez disso
            .enableExport(true)
            .enableCopy(true);

        // Adicionar ações customizadas
        menuHelper.addCustomMenuItem("Alternar Situação (Ativar/Suspender)", contrato -> toggleSituacao(contrato));
        menuHelper.addSeparator();
        menuHelper.addCustomMenuItem("Novo Contrato", contrato -> abrirFormulario(null));

        menuHelper.apply();
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnSuspender = new Button();
            private final HBox hbox = new HBox(5, btnEditar, btnSuspender);

            {
                btnEditar.getStyleClass().add("button-primary");
                btnSuspender.getStyleClass().add("button-secondary");
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnSuspender.setOnAction(e -> toggleSituacao(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Contrato contrato = getTableView().getItems().get(getIndex());
                    if (contrato.getSituacao() == Contrato.SituacaoContrato.ATIVO) {
                        btnSuspender.setText("Suspender");
                        btnSuspender.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
                        btnSuspender.setDisable(false);
                    } else if (contrato.getSituacao() == Contrato.SituacaoContrato.SUSPENSO) {
                        btnSuspender.setText("Reativar");
                        btnSuspender.setStyle("-fx-background-color: #28a745;");
                        btnSuspender.setDisable(false);
                    } else {
                        btnSuspender.setText("Rescindido");
                        btnSuspender.setDisable(true);
                        btnSuspender.setStyle("-fx-background-color: #6c757d;");
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            // Carrega contratos
            List<Contrato> lista = contratoService.findAll();
            contratos.clear();
            contratos.addAll(lista);

            // Carrega colaboradores ativos
            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);

        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Contrato contrato) {
        this.contratoSelecionado = contrato;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) cmbColaborador.getSelectionModel().select(contrato.getColaborador());
        grid.add(cmbColaborador, 1, 0);

        validator.createCheck()
            .dependsOn("colaborador", cmbColaborador.valueProperty())
            .withMethod(c -> {
                if (c.get("colaborador") == null) {
                    c.error("O colaborador é obrigatório");
                }
            })
            .decorates(cmbColaborador)
            .immediate();

        grid.add(new Label("Tipo Contrato:"), 2, 0);
        cmbTipoContrato = new ComboBox<>(FXCollections.observableArrayList(Contrato.TipoContrato.values()));
        cmbTipoContrato.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) cmbTipoContrato.getSelectionModel().select(contrato.getTipoContrato());
        grid.add(cmbTipoContrato, 3, 0);

        grid.add(new Label("Data Início:"), 0, 1);
        dpDataInicio = new DatePicker();
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) dpDataInicio.setValue(contrato.getDataInicio());
        grid.add(dpDataInicio, 1, 1);

        validator.createCheck()
            .dependsOn("dataInicio", dpDataInicio.valueProperty())
            .withMethod(c -> {
                if (c.get("dataInicio") == null) {
                    c.error("A data de início é obrigatória");
                }
            })
            .decorates(dpDataInicio)
            .immediate();

        grid.add(new Label("Data Fim:"), 2, 1);
        dpDataFim = new DatePicker();
        dpDataFim.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) dpDataFim.setValue(contrato.getDataFim());
        grid.add(dpDataFim, 3, 1);

        grid.add(new Label("Horário:"), 0, 2);
        txtHorario = new TextField();
        if (contrato != null) txtHorario.setText(contrato.getHorario() != null ? contrato.getHorario() : "");
        grid.add(txtHorario, 1, 2);

        grid.add(new Label("Regime:"), 2, 2);
        cmbRegime = new ComboBox<>(FXCollections.observableArrayList(Contrato.RegimeContrato.values()));
        cmbRegime.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) cmbRegime.getSelectionModel().select(contrato.getRegime());
        grid.add(cmbRegime, 3, 2);

        grid.add(new Label("Salário Base:"), 0, 3);
        txtSalarioBase = new TextField();
        if (contrato != null && contrato.getSalarioBase() != null) txtSalarioBase.setText(contrato.getSalarioBase().toString());
        grid.add(txtSalarioBase, 1, 3);

        validator.createCheck()
            .dependsOn("salario", txtSalarioBase.textProperty())
            .withMethod(c -> {
                String val = c.get("salario");
                if (val == null || val.trim().isEmpty()) {
                    c.error("Salário base é obrigatório");
                } else {
                    try {
                        new BigDecimal(val);
                    } catch (Exception e) {
                        c.error("Valor numérico inválido");
                    }
                }
            })
            .decorates(txtSalarioBase)
            .immediate();

        grid.add(new Label("Subs. Alim.:"), 2, 3);
        txtSubsidioAlimentacao = new TextField();
        if (contrato != null && contrato.getSubsidioAlimentacao() != null) txtSubsidioAlimentacao.setText(contrato.getSubsidioAlimentacao().toString());
        grid.add(txtSubsidioAlimentacao, 3, 3);

        grid.add(new Label("Subs. Transp.:"), 0, 4);
        txtSubsidioTransporte = new TextField();
        if (contrato != null && contrato.getSubsidioTransporte() != null) txtSubsidioTransporte.setText(contrato.getSubsidioTransporte().toString());
        grid.add(txtSubsidioTransporte, 1, 4);

        grid.add(new Label("Situação:"), 2, 4);
        cmbSituacao = new ComboBox<>(FXCollections.observableArrayList(Contrato.SituacaoContrato.values()));
        cmbSituacao.setMaxWidth(Double.MAX_VALUE);
        if (contrato != null) cmbSituacao.getSelectionModel().select(contrato.getSituacao());
        else cmbSituacao.getSelectionModel().select(Contrato.SituacaoContrato.ATIVO);
        grid.add(cmbSituacao, 3, 4);

        grid.add(new Label("Observações:"), 0, 5);
        txtObservacoes = new TextArea();
        txtObservacoes.setPrefRowCount(3);
        if (contrato != null) txtObservacoes.setText(contrato.getObservacoes() != null ? contrato.getObservacoes() : "");
        grid.add(txtObservacoes, 1, 5, 3, 1);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(contrato == null ? "Novo Contrato" : "Editar Contrato")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarContrato();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void salvarContrato() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Selecione um colaborador", "warning", null);
                return;
            }

            Contrato contrato = contratoSelecionado != null ? contratoSelecionado : new Contrato();
            contrato.setColaborador(cmbColaborador.getValue());
            contrato.setTipoContrato(cmbTipoContrato.getValue());
            contrato.setDataInicio(dpDataInicio.getValue());
            contrato.setDataFim(dpDataFim.getValue());
            contrato.setHorario(txtHorario.getText().trim().isEmpty() ? null : txtHorario.getText().trim());
            contrato.setRegime(cmbRegime.getValue());
            
            try {
                if (!txtSalarioBase.getText().trim().isEmpty()) {
                    contrato.setSalarioBase(new BigDecimal(txtSalarioBase.getText().trim()));
                }
                if (!txtSubsidioAlimentacao.getText().trim().isEmpty()) {
                    contrato.setSubsidioAlimentacao(new BigDecimal(txtSubsidioAlimentacao.getText().trim()));
                }
                if (!txtSubsidioTransporte.getText().trim().isEmpty()) {
                    contrato.setSubsidioTransporte(new BigDecimal(txtSubsidioTransporte.getText().trim()));
                }
            } catch (NumberFormatException e) {
                modalManager.alert("Aviso", "Valores numéricos inválidos", "warning", e);
                return;
            }
            
            contrato.setObservacoes(txtObservacoes.getText().trim().isEmpty() ? null : txtObservacoes.getText().trim());
            contrato.setSituacao(cmbSituacao.getValue());

            contratoService.save(contrato);
            carregarDados();

        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o contrato.", "error", e);
        }
    }

    private void toggleSituacao(Contrato contrato) {
        try {
            if (contrato.getSituacao() == Contrato.SituacaoContrato.ATIVO) {
                contratoService.suspender(contrato.getId());
            } else if (contrato.getSituacao() == Contrato.SituacaoContrato.SUSPENSO) {
                contratoService.reativar(contrato.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Situação", "Não foi possível alterar a situação do contrato.", "error", e);
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String cabecalho, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecalho);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
