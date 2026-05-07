package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.rh.domain.DocumentoColaborador;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.service.DocumentoColaboradorService;
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
public class DocumentoColaboradorView extends VBox {

    @Autowired
    private DocumentoColaboradorService documentoService;
    
    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ModalManager modalManager;

    private TableView<DocumentoColaborador> tableView;
    private TableColumn<DocumentoColaborador, String> colColaborador;
    private TableColumn<DocumentoColaborador, String> colTipo;
    private TableColumn<DocumentoColaborador, String> colNome;
    private TableColumn<DocumentoColaborador, String> colValidade;
    private TableColumn<DocumentoColaborador, String> colAtivo;
    private TableColumn<DocumentoColaborador, Void> colAcoes;

    private ComboBox<Colaborador> cmbColaborador;
    private ComboBox<DocumentoColaborador.TipoDocumento> cmbTipo;
    private TextField txtNomeDocumento;
    private TextField txtCaminhoArquivo;
    private DatePicker dpDataEmissao;
    private DatePicker dpDataValidade;
    private TextField txtEmissor;
    private TextField txtNumeroDocumento;
    private TextArea txtDescricao;

    private Button btnNovo;

    private ObservableList<DocumentoColaborador> documentos;
    private ObservableList<Colaborador> colaboradores;
    private DocumentoColaborador documentoSelecionado;
    private Validator validator;

    public DocumentoColaboradorView() {
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

        Label title = new Label("Gestão de Documentos");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Documento");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colColaborador = new TableColumn<>("Colaborador");
        colTipo = new TableColumn<>("Tipo");
        colNome = new TableColumn<>("Nome");
        colValidade = new TableColumn<>("Validade");
        colAtivo = new TableColumn<>("Ativo");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colColaborador, colTipo, colNome, colValidade, colAtivo, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        // Redimensiona as colunas para ocupar toda a largura disponível
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colColaborador.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getColaborador() != null ? data.getValue().getColaborador().getNomeCompleto() : ""));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getTipo() != null ? data.getValue().getTipo().toString() : ""));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getNomeDocumento() != null ? data.getValue().getNomeDocumento() : ""));
        colValidade.setCellValueFactory(data -> {
            if (data.getValue().getDataValidade() != null) {
                return new SimpleStringProperty(data.getValue().getDataValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            return new SimpleStringProperty("Sem validade");
        });
        colAtivo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getActive() ? "Sim" : "Não"));

        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        documentos = FXCollections.observableArrayList();
        tableView.setItems(documentos);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Novo Documento");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            DocumentoColaborador selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemToggleAtivo = new MenuItem("Alternar Status (Ativo/Inativo)");
        itemToggleAtivo.setOnAction(e -> {
            DocumentoColaborador selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                toggleAtivo(selecionado);
            }
        });

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem itemAtualizar = new MenuItem("Atualizar Tabela");
        itemAtualizar.setOnAction(e -> carregarDados());

        contextMenu.getItems().addAll(itemNovo, itemEditar, itemToggleAtivo, separator, itemAtualizar);
        tableView.setContextMenu(contextMenu);
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnAtivar = new Button();
            private final HBox hbox = new HBox(8, btnEditar, btnAtivar);

            {
                btnEditar.setGraphic(new FontIcon(Feather.EDIT_2));
                btnEditar.setTooltip(new Tooltip("Editar"));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnAtivar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnAtivar.setOnAction(e -> toggleAtivo(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DocumentoColaborador doc = getTableView().getItems().get(getIndex());
                    FontIcon iconStatus = new FontIcon(doc.getActive() ? Feather.CHECK_CIRCLE : Feather.X_CIRCLE);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(doc.getActive() ? "#28a745" : "#dc3545"));
                    btnAtivar.setGraphic(iconStatus);
                    btnAtivar.setTooltip(new Tooltip(doc.getActive() ? "Inativar" : "Ativar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            List<DocumentoColaborador> lista = documentoService.findAll();
            documentos.clear();
            documentos.addAll(lista);

            List<Colaborador> listaColaboradores = colaboradorService.findAtivos();
            colaboradores = FXCollections.observableArrayList(listaColaboradores);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(DocumentoColaborador documento) {
        this.documentoSelecionado = documento;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Colaborador:"), 0, 0);
        cmbColaborador = new ComboBox<>(colaboradores);
        cmbColaborador.setMaxWidth(Double.MAX_VALUE);
        if (documento != null) cmbColaborador.getSelectionModel().select(documento.getColaborador());
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

        grid.add(new Label("Tipo Documento:"), 2, 0);
        cmbTipo = new ComboBox<>(FXCollections.observableArrayList(DocumentoColaborador.TipoDocumento.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        if (documento != null) cmbTipo.getSelectionModel().select(documento.getTipo());
        grid.add(cmbTipo, 3, 0);

        grid.add(new Label("Nome Doc.:"), 0, 1);
        txtNomeDocumento = new TextField(documento != null ? documento.getNomeDocumento() : "");
        grid.add(txtNomeDocumento, 1, 1);

        validator.createCheck()
            .dependsOn("nome", txtNomeDocumento.textProperty())
            .withMethod(c -> {
                String val = c.get("nome");
                if (val == null || val.trim().isEmpty()) {
                    c.error("O nome do documento é obrigatório");
                }
            })
            .decorates(txtNomeDocumento)
            .immediate();

        grid.add(new Label("Nº Documento:"), 2, 1);
        txtNumeroDocumento = new TextField(documento != null ? documento.getNumeroDocumento() : "");
        grid.add(txtNumeroDocumento, 3, 1);

        grid.add(new Label("Data Emissão:"), 0, 2);
        dpDataEmissao = new DatePicker(documento != null ? documento.getDataEmissao() : null);
        dpDataEmissao.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataEmissao, 1, 2);

        grid.add(new Label("Data Validade:"), 2, 2);
        dpDataValidade = new DatePicker(documento != null ? documento.getDataValidade() : null);
        dpDataValidade.setMaxWidth(Double.MAX_VALUE);
        grid.add(dpDataValidade, 3, 2);

        grid.add(new Label("Emissor:"), 0, 3);
        txtEmissor = new TextField(documento != null ? documento.getEmissor() : "");
        grid.add(txtEmissor, 1, 3);

        grid.add(new Label("Arquivo:"), 2, 3);
        HBox fileBox = new HBox(5);
        txtCaminhoArquivo = new TextField(documento != null ? documento.getCaminhoArquivo() : "");
        txtCaminhoArquivo.setEditable(false);
        Button btnSelecionarArquivo = new Button("...");
        btnSelecionarArquivo.setOnAction(e -> selecionarArquivo());
        HBox.setHgrow(txtCaminhoArquivo, Priority.ALWAYS);
        fileBox.getChildren().addAll(txtCaminhoArquivo, btnSelecionarArquivo);
        grid.add(fileBox, 3, 3);

        grid.add(new Label("Descrição:"), 0, 4);
        txtDescricao = new TextArea(documento != null ? documento.getDescricao() : "");
        txtDescricao.setPrefRowCount(2);
        grid.add(txtDescricao, 1, 4, 3, 1);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(documento == null ? "Novo Documento" : "Editar Documento")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarDocumento();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
    }

    private void selecionarArquivo() {
        modalManager.info("Info", "A funcionalidade de upload de arquivos será implementada em versão futura.");
    }

    private void salvarDocumento() {
        try {
            if (cmbColaborador.getValue() == null) {
                modalManager.alert("Aviso", "Colaborador é obrigatório", "warning", null);
                return;
            }

            DocumentoColaborador documento = documentoSelecionado != null ? documentoSelecionado : new DocumentoColaborador();
            documento.setColaborador(cmbColaborador.getValue());
            documento.setTipo(cmbTipo.getValue());
            documento.setNomeDocumento(txtNomeDocumento.getText().trim());
            documento.setCaminhoArquivo(txtCaminhoArquivo.getText().trim());
            documento.setDataEmissao(dpDataEmissao.getValue());
            documento.setDataValidade(dpDataValidade.getValue());
            documento.setEmissor(txtEmissor.getText().trim());
            documento.setNumeroDocumento(txtNumeroDocumento.getText().trim());
            documento.setDescricao(txtDescricao.getText().trim());

            documentoService.save(documento);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o documento.", "error", e);
        }
    }

    private void toggleAtivo(DocumentoColaborador documento) {
        try {
            if (documento.getActive()) {
                documentoService.desativar(documento.getId());
            } else {
                documentoService.ativar(documento.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Estado", "Não foi possível alterar o estado do documento.", "error", e);
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
