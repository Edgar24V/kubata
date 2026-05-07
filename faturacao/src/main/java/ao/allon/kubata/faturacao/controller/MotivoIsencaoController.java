package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.service.MotivoIsencaoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class MotivoIsencaoController {

    private final MotivoIsencaoService service;
    private final ObservableList<MotivoIsencao> listaMotivos = FXCollections.observableArrayList();
    private MotivoIsencao motivoSelecionado;

    @FXML private TableView<MotivoIsencao> tabelaMotivos;
    @FXML private TableColumn<MotivoIsencao, String> colCodigo;
    @FXML private TableColumn<MotivoIsencao, String> colDescricao;
    @FXML private TableColumn<MotivoIsencao, String> colLegislacao;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescricao;
    @FXML private TextField txtLegislacao;

    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;

    public MotivoIsencaoController(MotivoIsencaoService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        carregarDados();
        limparFormulario();

        tabelaMotivos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preencherFormulario(newVal);
            }
        });
    }

    private void configurarTabela() {
        tabelaMotivos.setEditable(true);
        
        // Coluna Código - Editável
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setCellFactory(TextFieldTableCell.forTableColumn());
        colCodigo.setOnEditCommit(event -> {
            MotivoIsencao motivo = event.getRowValue();
            motivo.setCodigo(event.getNewValue());
            service.save(motivo);
        });
        colCodigo.setEditable(true);
        
        // Coluna Descrição - Editável
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescricao.setOnEditCommit(event -> {
            MotivoIsencao motivo = event.getRowValue();
            motivo.setDescricao(event.getNewValue());
            service.save(motivo);
        });
        colDescricao.setEditable(true);
        
        // Coluna Legislação - Editável
        colLegislacao.setCellValueFactory(new PropertyValueFactory<>("legislacao"));
        colLegislacao.setCellFactory(TextFieldTableCell.forTableColumn());
        colLegislacao.setOnEditCommit(event -> {
            MotivoIsencao motivo = event.getRowValue();
            motivo.setLegislacao(event.getNewValue());
            service.save(motivo);
        });
        colLegislacao.setEditable(true);
        
        tabelaMotivos.setItems(listaMotivos);
        
        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(tabelaMotivos, "Motivo de Isenção",
                motivo -> preencherFormulario(motivo),
                motivo -> excluirMotivo(motivo),
                () -> carregarDados())
            .apply();
    }
    
    private void excluirMotivo(MotivoIsencao motivo) {
        try {
            service.delete(motivo.getId());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Motivo de isenção excluído com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
        }
    }

    private void carregarDados() {
        listaMotivos.setAll(service.findAll());
    }

    @FXML
    public void salvar() {
        if (!validarCampos()) return;

        MotivoIsencao motivo = motivoSelecionado != null ? motivoSelecionado : new MotivoIsencao();
        motivo.setCodigo(txtCodigo.getText());
        motivo.setDescricao(txtDescricao.getText());
        motivo.setLegislacao(txtLegislacao.getText());

        try {
            service.save(motivo);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Motivo de Isenção salvo com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluir() {
        if (motivoSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione um item para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmação");
        confirm.setHeaderText("Excluir Motivo de Isenção");
        confirm.setContentText("Tem certeza que deseja excluir este motivo?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(motivoSelecionado.getId());
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Motivo excluído com sucesso!");
                limparFormulario();
                carregarDados();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
            }
        }
    }

    @FXML
    public void limparFormulario() {
        txtCodigo.clear();
        txtDescricao.clear();
        txtLegislacao.clear();
        motivoSelecionado = null;
        tabelaMotivos.getSelectionModel().clearSelection();
        btnExcluir.setDisable(true);
        btnSalvar.setText("Salvar");
    }

    private void preencherFormulario(MotivoIsencao motivo) {
        motivoSelecionado = motivo;
        txtCodigo.setText(motivo.getCodigo());
        txtDescricao.setText(motivo.getDescricao());
        txtLegislacao.setText(motivo.getLegislacao());
        btnExcluir.setDisable(false);
        btnSalvar.setText("Atualizar");
    }

    private boolean validarCampos() {
        if (txtCodigo.getText().isEmpty() || txtDescricao.getText().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validação", "Código e Descrição são obrigatórios.");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
