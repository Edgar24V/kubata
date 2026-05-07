package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.faturacao.domain.RetencaoFonte;
import ao.allon.kubata.faturacao.service.RetencaoFonteService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;

@Controller
public class RetencaoFonteController {

    private final RetencaoFonteService service;
    private final ObservableList<RetencaoFonte> listaRetencoes = FXCollections.observableArrayList();
    private RetencaoFonte retencaoSelecionada;

    @FXML private TableView<RetencaoFonte> tabelaRetencoes;
    @FXML private TableColumn<RetencaoFonte, String> colCodigo;
    @FXML private TableColumn<RetencaoFonte, String> colDescricao;
    @FXML private TableColumn<RetencaoFonte, BigDecimal> colTaxa;
    @FXML private TableColumn<RetencaoFonte, String> colTipo;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescricao;
    @FXML private TextField txtTaxa;
    @FXML private ComboBox<String> cmbTipo;

    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;

    public RetencaoFonteController(RetencaoFonteService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarCombos();
        carregarDados();
        limparFormulario();

        tabelaRetencoes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preencherFormulario(newVal);
            }
        });
        
        // Validação numérica para taxa
        txtTaxa.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtTaxa.setText(newValue.replaceAll("[^\\d.]", ""));
            }
        });
    }

    private void configurarTabela() {
        tabelaRetencoes.setEditable(true);
        
        // Coluna Código - Editável
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setCellFactory(TextFieldTableCell.forTableColumn());
        colCodigo.setOnEditCommit(event -> {
            RetencaoFonte r = event.getRowValue();
            r.setCodigo(event.getNewValue());
            service.save(r);
        });
        colCodigo.setEditable(true);
        
        // Coluna Descrição - Editável
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescricao.setOnEditCommit(event -> {
            RetencaoFonte r = event.getRowValue();
            r.setDescricao(event.getNewValue());
            service.save(r);
        });
        colDescricao.setEditable(true);
        
        // Coluna Taxa - Editável
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));
        colTaxa.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        colTaxa.setOnEditCommit(event -> {
            RetencaoFonte r = event.getRowValue();
            r.setTaxa(event.getNewValue());
            service.save(r);
        });
        colTaxa.setEditable(true);
        
        // Coluna Tipo - Não editável (usar ComboBox no formulário)
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoRendimento"));
        
        tabelaRetencoes.setItems(listaRetencoes);
        
        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(tabelaRetencoes, "Retenção na Fonte",
                retencao -> preencherFormulario(retencao),
                retencao -> excluirRetencao(retencao),
                () -> carregarDados())
            .apply();
    }
    
    private void excluirRetencao(RetencaoFonte retencao) {
        try {
            service.softDelete(retencao.getId());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Retenção excluída com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
        }
    }
    
    private void configurarCombos() {
        cmbTipo.setItems(FXCollections.observableArrayList(
            "Trabalho Dependente", 
            "Trabalho Independente", 
            "Prestação de Serviços", 
            "Royalties", 
            "Capitais", 
            "Predial"
        ));
    }

    private void carregarDados() {
        listaRetencoes.setAll(service.findActive());
    }

    @FXML
    public void salvar() {
        if (!validarCampos()) return;

        RetencaoFonte retencao = retencaoSelecionada != null ? retencaoSelecionada : new RetencaoFonte();
        retencao.setCodigo(txtCodigo.getText());
        retencao.setDescricao(txtDescricao.getText());
        retencao.setTaxa(new BigDecimal(txtTaxa.getText()));
        retencao.setTipoRendimento(cmbTipo.getValue());

        try {
            service.save(retencao);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Retenção na Fonte salva com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluir() {
        if (retencaoSelecionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione um item para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmação");
        confirm.setHeaderText("Excluir Retenção");
        confirm.setContentText("Tem certeza que deseja excluir esta retenção?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.softDelete(retencaoSelecionada.getId());
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Retenção excluída com sucesso!");
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
        txtTaxa.clear();
        cmbTipo.getSelectionModel().clearSelection();
        retencaoSelecionada = null;
        tabelaRetencoes.getSelectionModel().clearSelection();
        btnExcluir.setDisable(true);
        btnSalvar.setText("Salvar");
    }

    private void preencherFormulario(RetencaoFonte retencao) {
        retencaoSelecionada = retencao;
        txtCodigo.setText(retencao.getCodigo());
        txtDescricao.setText(retencao.getDescricao());
        txtTaxa.setText(retencao.getTaxa().toString());
        cmbTipo.setValue(retencao.getTipoRendimento());
        btnExcluir.setDisable(false);
        btnSalvar.setText("Atualizar");
    }

    private boolean validarCampos() {
        if (txtCodigo.getText().isEmpty() || txtDescricao.getText().isEmpty() || 
            txtTaxa.getText().isEmpty() || cmbTipo.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validação", "Todos os campos são obrigatórios.");
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
