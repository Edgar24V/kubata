package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.Licenca;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.LicencaService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import javax.annotation.PostConstruct;

@Component
public class LicencaView extends VBox {

    @Autowired
    private LicencaService licencaService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<Licenca> tableView;
    private TableColumn<Licenca, String> colColaborador;
    private TableColumn<Licenca, String> colTipo;
    private TableColumn<Licenca, String> colPeriodo;
    private TableColumn<Licenca, String> colDias;
    private TableColumn<Licenca, String> colEstado;
    private TableColumn<Licenca, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private ComboBox<Licenca.TipoLicenca> cmbTipo;
    private DatePicker dpDataInicio;
    private DatePicker dpDataFim;
    private TextField txtQuantidadeDias;
    private TextArea txtDescricao;
    private ComboBox<Licenca.EstadoLicenca> cmbEstado;

    private Button btnNovo;

    private ObservableList<Licenca> licencas;
    private ObservableList<Colaborador> colaboradores;
    private Licenca licencaSelecionada;
    private Validator validator;

    public LicencaView() {
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

        Label title = new Label("Gestão de Licenças");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Nova Licença");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colTipo = new TableColumn<>("Tipo");
        colPeriodo = new TableColumn<>("Período");
        colDias = new TableColumn<>("Dias");
        colEstado = new TableColumn<>("Estado");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colTipo, colPeriodo, colDias, colEstado, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getTipo() != null ? data.getValue().getTipo().toString() : ""));
        colPeriodo.setCellValueFactory(data -> {
            if (data.getValue().getDataInicio() != null) {
                String fim = data.getValue().getDataFim() != null ? 
                    data.getValue().getDataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Indefinido";
                return new SimpleStringProperty(
                    data.getValue().getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + fim);
            }
            return new SimpleStringProperty("");
        });
        colDias.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getQuantidadeDias() != null ? data.getValue().getQuantidadeDias().toString() : ""));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getEstado() != null ? data.getValue().getEstado().toString() : ""));

        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        licencas = FXCollections.observableArrayList();
        tableView.setItems(licencas);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Nova Licença");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionada");
        itemEditar.setOnAction(e -> {
            Licenca selecionada = tableView.getSelectionModel().getSelectedItem();
            if (selecionada != null) {
                abrirFormulario(selecionada);
            }
        });

        MenuItem itemAprovar = new MenuItem("Aprovar Licença");
        itemAprovar.setOnAction(e -> {
            Licenca selecionada = tableView.getSelectionModel().getSelectedItem();
            if (selecionada != null && selecionada.getEstado() == Licenca.EstadoLicenca.PENDENTE) {
                aprovarLicenca(selecionada);
            }
        });

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem itemAtualizar = new MenuItem("Atualizar Tabela");
        itemAtualizar.setOnAction(e -> carregarDados());

        contextMenu.getItems().addAll(itemNovo, itemEditar, itemAprovar, separator, itemAtualizar);
        tableView.setContextMenu(contextMenu);
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
                btnAprovar.setOnAction(e -> aprovarLicenca(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Licenca lic = getTableView().getItems().get(getIndex());
                    boolean isPendente = lic.getEstado() == Licenca.EstadoLicenca.PENDENTE;
                    FontIcon iconStatus = new FontIcon(isPendente ? Feather.SHIELD : Feather.SHIELD_OFF);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(isPendente ? "#28a745" : "#ffc107"));
                    btnAprovar.setGraphic(iconStatus);
                    btnAprovar.setTooltip(new Tooltip(isPendente ? "Aprovar" : "Aprovado"));
                    btnAprovar.setDisable(!isPendente);
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            List<Licenca> lista = licencaService.findAll();
            licencas.clear();
            licencas.addAll(lista);

            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Licenca licenca) {
        this.licencaSelecionada = licenca;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (licenca != null && licenca.getColaborador() != null) cmbColaborador.getSelectionModel().select(licenca.getColaborador());
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

        grid.add(new Label("Tipo Licença:"), 2, 0);
        cmbTipo = new ComboBox<>(FXCollections.observableArrayList(Licenca.TipoLicenca.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        if (licenca != null) cmbTipo.getSelectionModel().select(licenca.getTipo());
        grid.add(cmbTipo, 3, 0);

        grid.add(new Label("Data Início:"), 0, 1);
        dpDataInicio = new DatePicker(licenca != null ? licenca.getDataInicio() : null);
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
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
        dpDataFim = new DatePicker(licenca != null ? licenca.getDataFim() : null);
        dpDataFim.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataFim, 3, 1);

        grid.add(new Label("Qtd. Dias:"), 0, 2);
        txtQuantidadeDias = new TextField(licenca != null && licenca.getQuantidadeDias() != null ? licenca.getQuantidadeDias().toString() : "");
        txtQuantidadeDias.setEditable(false);
        grid.add(txtQuantidadeDias, 1, 2);

        grid.add(new Label("Estado:"), 2, 2);
        cmbEstado = new ComboBox<>(FXCollections.observableArrayList(Licenca.EstadoLicenca.values()));
        cmbEstado.setMaxWidth(Double.MAX_VALUE);
        if (licenca != null) cmbEstado.getSelectionModel().select(licenca.getEstado());
        else cmbEstado.getSelectionModel().select(Licenca.EstadoLicenca.PENDENTE);
        grid.add(cmbEstado, 3, 2);

        grid.add(new Label("Descrição:"), 0, 4);
        txtDescricao = new TextArea(licenca != null ? licenca.getDescricao() : "");
        txtDescricao.setPrefRowCount(3);
        grid.add(txtDescricao, 1, 4, 3, 1);

        // Listener para cálculo automático de dias
        dpDataInicio.valueProperty().addListener((obs, oldVal, newVal) -> calcularDias());
        dpDataFim.valueProperty().addListener((obs, oldVal, newVal) -> calcularDias());

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(licenca == null ? "Nova Licença" : "Editar Licença")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarLicenca();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void calcularDias() {
        if (dpDataInicio.getValue() != null && dpDataFim.getValue() != null) {
            long dias = ChronoUnit.DAYS.between(dpDataInicio.getValue(), dpDataFim.getValue()) + 1;
            txtQuantidadeDias.setText(String.valueOf(dias));
        }
    }

    private void salvarLicenca() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Colaborador é obrigatório", "warning", null);
                return;
            }

            Licenca licenca = licencaSelecionada != null ? licencaSelecionada : new Licenca();
            licenca.setColaborador(cmbColaborador.getValue());
            licenca.setTipo(cmbTipo.getValue());
            licenca.setDataInicio(dpDataInicio.getValue());
            licenca.setDataFim(dpDataFim.getValue());
            
            if (!txtQuantidadeDias.getText().trim().isEmpty()) {
                licenca.setQuantidadeDias(Integer.parseInt(txtQuantidadeDias.getText().trim()));
            }
            
            licenca.setDescricao(txtDescricao.getText().trim());
            licenca.setEstado(cmbEstado.getValue());

            licencaService.save(licenca);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar a licença.", "error", e);
        }
    }

    private void aprovarLicenca(Licenca l) {
        try {
            licencaService.aprovar(l.getId(), 1L); // TODO: Usar ID do usuário logado
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Aprovação", "Não foi possível aprovar a licença.", "error", e);
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
