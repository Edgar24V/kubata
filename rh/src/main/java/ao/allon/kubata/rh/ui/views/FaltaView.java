package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.Falta;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.FaltaService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import javax.annotation.PostConstruct;

@Component
public class FaltaView extends VBox {

    @Autowired
    private FaltaService faltaService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<Falta> tableView;
    private TableColumn<Falta, String> colColaborador;
    private TableColumn<Falta, String> colData;
    private TableColumn<Falta, String> colTipo;
    private TableColumn<Falta, String> colMotivo;
    private TableColumn<Falta, String> colJustificada;
    private TableColumn<Falta, String> colAprovado;
    private TableColumn<Falta, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private DatePicker dpData;
    private ComboBox<Falta.TipoFalta> cmbTipo;
    private TextField txtMotivo;
    private DatePicker dpDataJustificacao;
    private TextArea txtObservacoes;

    private DatePicker dpDataInicio;
    private DatePicker dpDataFim;
    private ComboBox<Falta.TipoFalta> cmbFiltroTipo;
    private CheckBox chkFiltroJustificadas;

    private Button btnNovo;
    private Button btnFiltrar;

    private ObservableList<Falta> faltas;
    private ObservableList<Colaborador> colaboradores;
    private Falta faltaSelecionada;
    private Validator validator;

    public FaltaView() {
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

        Label title = new Label("Gestão de Faltas");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filtros
        dpDataInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpDataFim = new DatePicker(LocalDate.now());
        cmbFiltroTipo = new ComboBox<>();
        cmbFiltroTipo.setPromptText("Tipo");
        cmbFiltroTipo.getItems().addAll(Falta.TipoFalta.values());
        chkFiltroJustificadas = new CheckBox("Justif.");
        btnFiltrar = new Button("Filtrar");
        btnFiltrar.setOnAction(e -> filtrarFaltas());
        
        btnNovo = new Button("Nova Falta");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, new Label("Início:"), dpDataInicio, new Label("Fim:"), dpDataFim, cmbFiltroTipo, chkFiltroJustificadas, btnFiltrar, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colData = new TableColumn<>("Data");
        colTipo = new TableColumn<>("Tipo");
        colMotivo = new TableColumn<>("Motivo");
        colJustificada = new TableColumn<>("Justif.");
        colAprovado = new TableColumn<>("Aprov.");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colData, colTipo, colMotivo, colJustificada, colAprovado, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configura colunas
        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colData.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getData() != null ? data.getValue().getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getTipo() != null ? data.getValue().getTipo().toString() : ""));
        colMotivo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getMotivo() != null ? data.getValue().getMotivo() : ""));
        colJustificada.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getJustificada() ? "Sim" : "Não"));
        colAprovado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getAprovadoPor() != null ? "Sim" : "Não"));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        faltas = FXCollections.observableArrayList();
        tableView.setItems(faltas);
    }

    private void configurarMenuContexto() {
        TableContextMenuHelper<Falta> menuHelper = new TableContextMenuHelper<>(tableView)
            .withEntityName("Falta")
            .onEdit(falta -> abrirFormulario(falta))
            .onDelete(falta -> excluirFalta(falta))
            .onRefresh(() -> carregarDados())
            .enableExport(true)
            .enableCopy(true);

        // Adicionar ações customizadas
        menuHelper.addCustomMenuItem("Alternar Justificação (Sim/Não)", falta -> toggleJustificada(falta));
        menuHelper.addSeparator();
        menuHelper.addCustomMenuItem("Registar Nova Falta", falta -> abrirFormulario(null));

        menuHelper.apply();
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnJustificar = new Button();
            private final HBox hbox = new HBox(8, btnEditar, btnJustificar);

            {
                btnEditar.setGraphic(new FontIcon(Feather.EDIT_2));
                btnEditar.setTooltip(new Tooltip("Editar"));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnJustificar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnJustificar.setOnAction(e -> toggleJustificada(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Falta falta = getTableView().getItems().get(getIndex());
                    FontIcon iconStatus = new FontIcon(falta.getJustificada() ? Feather.CHECK_SQUARE : Feather.SQUARE);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(falta.getJustificada() ? "#28a745" : "#dc3545"));
                    btnJustificar.setGraphic(iconStatus);
                    btnJustificar.setTooltip(new Tooltip(falta.getJustificada() ? "Desfazer Justificação" : "Justificar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            // Carrega faltas do mês atual
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            List<Falta> lista = faltaService.findByDataBetween(inicioMes, fimMes);
            faltas.clear();
            faltas.addAll(lista);

            // Carrega colaboradores ativos
            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);

        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Falta falta) {
        this.faltaSelecionada = falta;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (falta != null) cmbColaborador.getSelectionModel().select(falta.getColaborador());
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

        grid.add(new Label("Data:"), 2, 0);
        dpData = new DatePicker(falta != null ? falta.getData() : LocalDate.now());
        dpData.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpData, 3, 0);

        validator.createCheck()
            .dependsOn("data", dpData.valueProperty())
            .withMethod(c -> {
                if (c.get("data") == null) {
                    c.error("A data é obrigatória");
                }
            })
            .decorates(dpData)
            .immediate();

        grid.add(new Label("Tipo Falta:"), 0, 1);
        cmbTipo = new ComboBox<>(FXCollections.observableArrayList(Falta.TipoFalta.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        if (falta != null) cmbTipo.getSelectionModel().select(falta.getTipo());
        grid.add(cmbTipo, 1, 1);

        grid.add(new Label("Motivo:"), 2, 1);
        txtMotivo = new TextField(falta != null ? falta.getMotivo() : "");
        grid.add(txtMotivo, 3, 1);

        grid.add(new Label("Justificação:"), 0, 2);
        dpDataJustificacao = new DatePicker(falta != null ? falta.getDataJustificacao() : null);
        dpDataJustificacao.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataJustificacao, 1, 2);

        grid.add(new Label("Observações:"), 0, 3);
        txtObservacoes = new TextArea(falta != null ? falta.getObservacoes() : "");
        txtObservacoes.setPrefRowCount(2);
        grid.add(txtObservacoes, 1, 3, 3, 1);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(falta == null ? "Nova Falta" : "Editar Falta")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarFalta();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void salvarFalta() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Colaborador é obrigatório", "warning", null);
                return;
            }

            Falta falta = faltaSelecionada != null ? faltaSelecionada : new Falta();
            falta.setColaborador(cmbColaborador.getValue());
            falta.setData(dpData.getValue());
            falta.setTipo(cmbTipo.getValue());
            falta.setMotivo(txtMotivo.getText().trim().isEmpty() ? null : txtMotivo.getText().trim());
            falta.setDataJustificacao(dpDataJustificacao.getValue());
            falta.setObservacoes(txtObservacoes.getText().trim().isEmpty() ? null : txtObservacoes.getText().trim());

            faltaService.save(falta);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar a falta.", "error", e);
        }
    }

    private void toggleJustificada(Falta falta) {
        try {
            if (falta.getJustificada()) {
                faltaService.desjustificar(falta.getId());
            } else {
                faltaService.justificar(falta.getId(), LocalDate.now(), "Justificado via interface");
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Justificação", "Não foi possível alterar a justificativa.", "error", e);
        }
    }

    private void filtrarFaltas() {
        try {
            if (dpDataInicio.getValue() != null && dpDataFim.getValue() != null) {
                List<Falta> lista = faltaService.findByFiltros(null, cmbFiltroTipo.getValue(), dpDataInicio.getValue(), dpDataFim.getValue(), chkFiltroJustificadas.isSelected());
                faltas.clear();
                faltas.addAll(lista);
            }
        } catch (Exception e) {
            modalManager.alert("Erro de Filtro", "Não foi possível filtrar as faltas.", "error", e);
        }
    }

    private void excluirFalta(Falta falta) {
        try {
            faltaService.delete(falta.getId());
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro ao Excluir", "Não foi possível excluir a falta.", "error", e);
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
