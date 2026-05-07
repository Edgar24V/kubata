package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.PedidoFerias;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.PedidoFeriasService;
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
public class PedidoFeriasView extends VBox {

    @Autowired
    private PedidoFeriasService pedidoFeriasService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<PedidoFerias> tableView;
    private TableColumn<PedidoFerias, String> colColaborador;
    private TableColumn<PedidoFerias, String> colPeriodo;
    private TableColumn<PedidoFerias, String> colDias;
    private TableColumn<PedidoFerias, String> colEstado;
    private TableColumn<PedidoFerias, String> colAprovado;
    private TableColumn<PedidoFerias, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private DatePicker dpDataInicio;
    private DatePicker dpDataFim;
    private TextField txtQuantidadeDias;
    private TextArea txtJustificativa;
    private ComboBox<PedidoFerias.EstadoPedido> cmbEstado;

    private Button btnNovo;

    private ObservableList<PedidoFerias> pedidos;
    private ObservableList<Colaborador> colaboradores;
    private PedidoFerias pedidoSelecionado;
    private Validator validator;

    public PedidoFeriasView() {
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

        Label title = new Label("Pedidos de Férias");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Pedido");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colPeriodo = new TableColumn<>("Período");
        colDias = new TableColumn<>("Dias");
        colEstado = new TableColumn<>("Estado");
        colAprovado = new TableColumn<>("Aprov.");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colPeriodo, colDias, colEstado, colAprovado, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colPeriodo.setCellValueFactory(data -> {
            if (data.getValue().getDataInicio() != null && data.getValue().getDataFim() != null) {
                return new SimpleStringProperty(
                    data.getValue().getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " +
                    data.getValue().getDataFim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            return new SimpleStringProperty("");
        });
        colDias.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getQuantidadeDias() != null ? data.getValue().getQuantidadeDias().toString() : ""));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getEstado() != null ? data.getValue().getEstado().toString() : ""));
        colAprovado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getAprovadoPor() != null ? "Sim" : "Não"));

        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        pedidos = FXCollections.observableArrayList();
        tableView.setItems(pedidos);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Novo Pedido de Férias");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            PedidoFerias selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemAprovar = new MenuItem("Aprovar Pedido");
        itemAprovar.setOnAction(e -> {
            PedidoFerias selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null && selecionado.getEstado() == PedidoFerias.EstadoPedido.PENDENTE) {
                aprovarPedido(selecionado);
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
                btnAprovar.setOnAction(e -> aprovarPedido(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PedidoFerias pf = getTableView().getItems().get(getIndex());
                    boolean isPendente = pf.getEstado() == PedidoFerias.EstadoPedido.PENDENTE;
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
            List<PedidoFerias> lista = pedidoFeriasService.findAll();
            pedidos.clear();
            pedidos.addAll(lista);

            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(PedidoFerias pedido) {
        this.pedidoSelecionado = pedido;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (pedido != null && pedido.getColaborador() != null) cmbColaborador.getSelectionModel().select(pedido.getColaborador());
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

        grid.add(new Label("Data Início:"), 2, 0);
        dpDataInicio = new DatePicker(pedido != null ? pedido.getDataInicio() : null);
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataInicio, 3, 0);

        validator.createCheck()
            .dependsOn("dataInicio", dpDataInicio.valueProperty())
            .withMethod(c -> {
                if (c.get("dataInicio") == null) {
                    c.error("A data de início é obrigatória");
                }
            })
            .decorates(dpDataInicio)
            .immediate();

        grid.add(new Label("Data Fim:"), 0, 1);
        dpDataFim = new DatePicker(pedido != null ? pedido.getDataFim() : null);
        dpDataFim.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataFim, 1, 1);

        grid.add(new Label("Quantidade Dias:"), 2, 1);
        txtQuantidadeDias = new TextField(pedido != null && pedido.getQuantidadeDias() != null ? String.valueOf(pedido.getQuantidadeDias()) : "");
        txtQuantidadeDias.setEditable(false);
        grid.add(txtQuantidadeDias, 3, 1);

        grid.add(new Label("Estado:"), 0, 2);
        cmbEstado = new ComboBox<>(FXCollections.observableArrayList(PedidoFerias.EstadoPedido.values()));
        cmbEstado.setMaxWidth(Double.MAX_VALUE);
        if (pedido != null) cmbEstado.getSelectionModel().select(pedido.getEstado());
        else cmbEstado.getSelectionModel().select(PedidoFerias.EstadoPedido.PENDENTE);
        grid.add(cmbEstado, 1, 2);

        grid.add(new Label("Justificativa:"), 0, 3);
        txtJustificativa = new TextArea(pedido != null ? pedido.getJustificativa() : "");
        txtJustificativa.setPrefRowCount(2);
        grid.add(txtJustificativa, 1, 3, 3, 1);

        // Listener para cálculo automático de dias
        dpDataInicio.valueProperty().addListener((obs, oldVal, newVal) -> calcularDias());
        dpDataFim.valueProperty().addListener((obs, oldVal, newVal) -> calcularDias());

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(pedido == null ? "Novo Pedido de Férias" : "Editar Pedido")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarPedido();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void calcularDias() {
        if (dpDataInicio.getValue() != null && dpDataFim.getValue() != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(dpDataInicio.getValue(), dpDataFim.getValue()) + 1;
            txtQuantidadeDias.setText(String.valueOf(dias));
        }
    }

    private void salvarPedido() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Colaborador é obrigatório", "warning", null);
                return;
            }

            PedidoFerias pedido = pedidoSelecionado != null ? pedidoSelecionado : new PedidoFerias();
            pedido.setColaborador(cmbColaborador.getValue());
            pedido.setDataInicio(dpDataInicio.getValue());
            pedido.setDataFim(dpDataFim.getValue());
            
            try {
                pedido.setQuantidadeDias(Integer.parseInt(txtQuantidadeDias.getText().trim()));
            } catch (NumberFormatException e) {
                modalManager.alert("Aviso", "Quantidade de dias inválida", "warning", e);
                return;
            }
            
            pedido.setJustificativa(txtJustificativa.getText().trim());
            pedido.setEstado(cmbEstado.getValue());

            pedidoFeriasService.save(pedido);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o pedido de férias.", "error", e);
        }
    }

    private void aprovarPedido(PedidoFerias p) {
        try {
            pedidoFeriasService.aprovar(p.getId(), 1L); // TODO: Usar ID do usuário logado
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Aprovação", "Não foi possível aprovar o pedido de férias.", "error", e);
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
