package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.RegistoPonto;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.RegistoPontoService;
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
import java.time.LocalDateTime;
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
public class RegistoPontoView extends VBox {

    @Autowired
    private RegistoPontoService registoPontoService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<RegistoPonto> tableView;
    private TableColumn<RegistoPonto, String> colColaborador;
    private TableColumn<RegistoPonto, String> colData;
    private TableColumn<RegistoPonto, String> colEntrada;
    private TableColumn<RegistoPonto, String> colSaida;
    private TableColumn<RegistoPonto, String> colEntrada2;
    private TableColumn<RegistoPonto, String> colSaida2;
    private TableColumn<RegistoPonto, String> colOrigem;
    private TableColumn<RegistoPonto, String> colAprovado;
    private TableColumn<RegistoPonto, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private DatePicker dpData;
    private DatePicker dpDataInicio;
    private DatePicker dpDataFim;
    private TextField txtDispositivo;
    private TextArea txtObservacoes;

    private Button btnNovo;
    private Button btnFiltrar;

    private ObservableList<RegistoPonto> registos;
    private ObservableList<Colaborador> colaboradores;
    private RegistoPonto registoSelecionado;
    private Validator validator;

    public RegistoPontoView() {
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

        Label title = new Label("Registo de Ponto");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filtros rápidos
        dpDataInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpDataFim = new DatePicker(LocalDate.now());
        btnFiltrar = new Button("Filtrar");
        btnFiltrar.setOnAction(e -> filtrarRegistos());
        
        btnNovo = new Button("Novo Registo");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, new Label("Início:"), dpDataInicio, new Label("Fim:"), dpDataFim, btnFiltrar, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colData = new TableColumn<>("Data");
        colEntrada = new TableColumn<>("E1");
        colSaida = new TableColumn<>("S1");
        colEntrada2 = new TableColumn<>("E2");
        colSaida2 = new TableColumn<>("S2");
        colOrigem = new TableColumn<>("Origem");
        colAprovado = new TableColumn<>("Aprov.");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colData, colEntrada, colSaida, colEntrada2, colSaida2, colOrigem, colAprovado, colAcoes);

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
        colEntrada.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getHoraEntrada() != null ? data.getValue().getHoraEntrada().format(DateTimeFormatter.ofPattern("HH:mm")) : ""));
        colSaida.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getHoraSaida() != null ? data.getValue().getHoraSaida().format(DateTimeFormatter.ofPattern("HH:mm")) : ""));
        colEntrada2.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getHoraEntrada2() != null ? data.getValue().getHoraEntrada2().format(DateTimeFormatter.ofPattern("HH:mm")) : ""));
        colSaida2.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getHoraSaida2() != null ? data.getValue().getHoraSaida2().format(DateTimeFormatter.ofPattern("HH:mm")) : ""));
        colOrigem.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getOrigem() != null ? data.getValue().getOrigem().toString() : ""));
        colAprovado.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getAprovadoPor() != null ? "Sim" : "Não"));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        registos = FXCollections.observableArrayList();
        tableView.setItems(registos);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Registar Ponto Manual");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            RegistoPonto selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemAprovar = new MenuItem("Aprovar/Desaprovar Registo");
        itemAprovar.setOnAction(e -> {
            RegistoPonto selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                aprovarRegisto(selecionado);
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
                btnAprovar.setOnAction(e -> aprovarRegisto(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RegistoPonto registo = getTableView().getItems().get(getIndex());
                    boolean isAprovado = registo.getAprovadoPor() != null;
                    FontIcon iconStatus = new FontIcon(isAprovado ? Feather.SHIELD : Feather.SHIELD_OFF);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(isAprovado ? "#ffc107" : "#28a745"));
                    btnAprovar.setGraphic(iconStatus);
                    btnAprovar.setTooltip(new Tooltip(isAprovado ? "Desfazer Aprovação" : "Aprovar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            // Carrega registos do dia atual por padrão
            List<RegistoPonto> lista = registoPontoService.findByData(LocalDate.now());
            registos.clear();
            registos.addAll(lista);

            // Carrega colaboradores ativos
            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);

        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(RegistoPonto registo) {
        this.registoSelecionado = registo;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (registo != null) cmbColaborador.getSelectionModel().select(registo.getColaborador());
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
        dpData = new DatePicker(registo != null ? registo.getData() : LocalDate.now());
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

        grid.add(new Label("Dispositivo:"), 0, 1);
        txtDispositivo = new TextField(registo != null ? registo.getDispositivo() : "MANUAL");
        grid.add(txtDispositivo, 1, 1);

        grid.add(new Label("Observações:"), 0, 2);
        txtObservacoes = new TextArea(registo != null ? registo.getObservacoes() : "");
        txtObservacoes.setPrefRowCount(2);
        grid.add(txtObservacoes, 1, 2, 3, 1);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(registo == null ? "Novo Registo de Ponto" : "Editar Registo")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarRegisto();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void salvarRegisto() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Selecione um colaborador", "warning", null);
                return;
            }

            RegistoPonto registo = registoSelecionado != null ? registoSelecionado : new RegistoPonto();
            registo.setColaborador(cmbColaborador.getValue());
            registo.setData(dpData.getValue());
            registo.setDispositivo(txtDispositivo.getText().trim());
            registo.setObservacoes(txtObservacoes.getText().trim());
            
            if (registo.getHoraEntrada() == null) {
                registo.setHoraEntrada(LocalDateTime.now());
            }
            registo.setOrigem(RegistoPonto.OrigemPonto.MANUAL);

            registoPontoService.save(registo);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o registo de ponto.", "error", e);
        }
    }

    private void aprovarRegisto(RegistoPonto registo) {
        try {
            if (registo.getAprovadoPor() == null) {
                registoPontoService.aprovar(registo.getId(), 1L); // TODO: Usar ID do usuário logado
            } else {
                registoPontoService.desaprovar(registo.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Aprovação", "Não foi possível processar a aprovação.", "error", e);
        }
    }

    private void filtrarRegistos() {
        try {
            if (dpDataInicio.getValue() != null && dpDataFim.getValue() != null) {
                List<RegistoPonto> lista = registoPontoService.findByDataBetween(dpDataInicio.getValue(), dpDataFim.getValue());
                registos.clear();
                registos.addAll(lista);
            }
        } catch (Exception e) {
            modalManager.alert("Erro de Filtro", "Não foi possível filtrar os registos.", "error", e);
        }
    }


}
