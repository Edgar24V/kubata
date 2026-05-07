package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.HoraExtra;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.HoraExtraService;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import javax.annotation.PostConstruct;

@Component
public class HoraExtraView extends VBox {

    @Autowired
    private HoraExtraService horaExtraService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<HoraExtra> tableView;
    private TableColumn<HoraExtra, String> colColaborador;
    private TableColumn<HoraExtra, String> colData;
    private TableColumn<HoraExtra, String> colHoras;
    private TableColumn<HoraExtra, String> colMotivo;
    private TableColumn<HoraExtra, String> colEstado;
    private TableColumn<HoraExtra, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private DatePicker dpData;
    private TextField txtQuantidadeHoras;
    private ComboBox<HoraExtra.MotivoHoraExtra> cmbMotivo;
    private TextArea txtDescricao;
    private ComboBox<HoraExtra.EstadoHoraExtra> cmbEstado;

    private Button btnNovo;

    private ObservableList<HoraExtra> horasExtra;
    private ObservableList<Colaborador> colaboradores;
    private HoraExtra horaExtraSelecionada;
    private Validator validator;

    public HoraExtraView() {
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

        Label title = new Label("Gestão de Horas Extra");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Nova Hora Extra");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colData = new TableColumn<>("Data");
        colHoras = new TableColumn<>("Horas");
        colMotivo = new TableColumn<>("Motivo");
        colEstado = new TableColumn<>("Estado");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colData, colHoras, colMotivo, colEstado, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colData.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getData() != null ? data.getValue().getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        colHoras.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getQuantidadeHoras() != null ? data.getValue().getQuantidadeHoras().toString() + "h" : ""));
        colMotivo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getMotivo() != null ? data.getValue().getMotivo().toString() : ""));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getEstado() != null ? data.getValue().getEstado().toString() : ""));

        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        horasExtra = FXCollections.observableArrayList();
        tableView.setItems(horasExtra);
    }

    private void configurarMenuContexto() {
        TableContextMenuHelper<HoraExtra> menuHelper = new TableContextMenuHelper<>(tableView)
            .withEntityName("Hora Extra")
            .onEdit(horaExtra -> abrirFormulario(horaExtra))
            .onDelete(horaExtra -> excluirHoraExtra(horaExtra))
            .onRefresh(() -> carregarDados())
            .enableExport(true)
            .enableCopy(true);

        // Adicionar ação customizada
        menuHelper.addCustomMenuItem("Aprovar/Rejeitar", horaExtra -> aprovarHoraExtra(horaExtra));
        menuHelper.addSeparator();
        menuHelper.addCustomMenuItem("Nova Hora Extra", horaExtra -> abrirFormulario(null));

        menuHelper.apply();
    }
    
    private void excluirHoraExtra(HoraExtra horaExtra) {
        try {
            horaExtraService.delete(horaExtra.getId());
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro ao Excluir", "Não foi possível excluir a hora extra.", "error", e);
        }
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnAprovar = new Button();
            private final HBox hbox = new HBox(8, btnEditar, btnAprovar);

            {
                btnEditar.setGraphic(new FontIcon(Feather.EDIT_2));
                btnEditar.setTooltip(new Tooltip("Editar"));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnAprovar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnAprovar.setOnAction(e -> aprovarHoraExtra(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HoraExtra he = getTableView().getItems().get(getIndex());
                    boolean isPendente = he.getEstado() == HoraExtra.EstadoHoraExtra.PENDENTE;
                    FontIcon iconStatus = new FontIcon(isPendente ? Feather.SHIELD : Feather.SHIELD_OFF);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(isPendente ? "#28a745" : "#ffc107"));
                    btnAprovar.setGraphic(iconStatus);
                    btnAprovar.setTooltip(new Tooltip(isPendente ? "Aprovar" : "Desfazer Aprovação"));
                    btnAprovar.setDisable(!isPendente);
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            List<HoraExtra> lista = horaExtraService.findAll();
            horasExtra.clear();
            horasExtra.addAll(lista);

            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(HoraExtra horaExtra) {
        this.horaExtraSelecionada = horaExtra;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (horaExtra != null && horaExtra.getColaborador() != null) cmbColaborador.getSelectionModel().select(horaExtra.getColaborador());
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
        dpData = new DatePicker(horaExtra != null ? horaExtra.getData() : LocalDate.now());
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

        grid.add(new Label("Qtd. Horas:"), 0, 1);
        txtQuantidadeHoras = new TextField(horaExtra != null && horaExtra.getQuantidadeHoras() != null ? horaExtra.getQuantidadeHoras().toString() : "");
        grid.add(txtQuantidadeHoras, 1, 1);

        validator.createCheck()
            .dependsOn("horas", txtQuantidadeHoras.textProperty())
            .withMethod(c -> {
                String val = c.get("horas");
                if (val == null || val.trim().isEmpty()) {
                    c.error("Quantidade de horas é obrigatória");
                } else {
                    try {
                        new BigDecimal(val);
                    } catch (Exception e) {
                        c.error("Valor numérico inválido");
                    }
                }
            })
            .decorates(txtQuantidadeHoras)
            .immediate();

        grid.add(new Label("Motivo:"), 2, 1);
        cmbMotivo = new ComboBox<>(FXCollections.observableArrayList(HoraExtra.MotivoHoraExtra.values()));
        cmbMotivo.setMaxWidth(Double.MAX_VALUE);
        if (horaExtra != null) cmbMotivo.getSelectionModel().select(horaExtra.getMotivo());
        grid.add(cmbMotivo, 3, 1);

        grid.add(new Label("Estado:"), 0, 2);
        cmbEstado = new ComboBox<>(FXCollections.observableArrayList(HoraExtra.EstadoHoraExtra.values()));
        cmbEstado.setMaxWidth(Double.MAX_VALUE);
        if (horaExtra != null) cmbEstado.getSelectionModel().select(horaExtra.getEstado());
        else cmbEstado.getSelectionModel().select(HoraExtra.EstadoHoraExtra.PENDENTE);
        grid.add(cmbEstado, 1, 2);

        grid.add(new Label("Descrição:"), 0, 3);
        txtDescricao = new TextArea(horaExtra != null ? horaExtra.getDescricao() : "");
        txtDescricao.setPrefRowCount(2);
        grid.add(txtDescricao, 1, 3, 3, 1);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(horaExtra == null ? "Nova Hora Extra" : "Editar Hora Extra")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarHoraExtra();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void salvarHoraExtra() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Colaborador é obrigatório", "warning", null);
                return;
            }

            HoraExtra horaExtra = horaExtraSelecionada != null ? horaExtraSelecionada : new HoraExtra();
            horaExtra.setColaborador(cmbColaborador.getValue());
            horaExtra.setData(dpData.getValue());
            
            try {
                horaExtra.setQuantidadeHoras(new BigDecimal(txtQuantidadeHoras.getText().trim()));
            } catch (NumberFormatException e) {
                modalManager.alert("Aviso", "Quantidade de horas inválida", "warning", e);
                return;
            }
            
            horaExtra.setMotivo(cmbMotivo.getValue());
            horaExtra.setDescricao(txtDescricao.getText().trim());
            horaExtra.setEstado(cmbEstado.getValue());

            horaExtraService.save(horaExtra);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar a hora extra.", "error", e);
        }
    }

    private void aprovarHoraExtra(HoraExtra he) {
        try {
            horaExtraService.aprovar(he.getId(), 1L); // TODO: Usar ID do usuário logado
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Aprovação", "Não foi possível aprovar a hora extra.", "error", e);
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
