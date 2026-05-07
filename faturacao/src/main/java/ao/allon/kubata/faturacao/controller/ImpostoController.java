package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.core.ui.table.EditableTableManager;
import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.MotivoIsencaoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;

@Controller
public class ImpostoController {

    private final ImpostoService service;
    private final MotivoIsencaoService motivoService;
    private final ObservableList<Imposto> listaImpostos = FXCollections.observableArrayList();
    private final ObservableList<MotivoIsencao> listaMotivos = FXCollections.observableArrayList();
    private Imposto impostoSelecionado;

    @FXML private TableView<Imposto> tabelaImpostos;
    @FXML private TableColumn<Imposto, String> colCodigo;
    @FXML private TableColumn<Imposto, String> colDescricao;
    @FXML private TableColumn<Imposto, String> colTipo;
    @FXML private TableColumn<Imposto, BigDecimal> colPercentual;
    @FXML private TableColumn<Imposto, String> colMotivo;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescricao;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtPercentual;
    @FXML private ComboBox<MotivoIsencao> cmbMotivo;
    
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;

    public ImpostoController(ImpostoService service, MotivoIsencaoService motivoService) {
        this.service = service;
        this.motivoService = motivoService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarCombos();
        carregarDados();
        limparFormulario();

        tabelaImpostos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preencherFormulario(newVal);
            }
        });
        
        // Validar percentual para aceitar apenas números
        txtPercentual.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtPercentual.setText(newValue.replaceAll("[^\\d.]", ""));
            }
        });
    }

    private void configurarTabela() {
        tabelaImpostos.setEditable(true);
        
        EditableTableManager<Imposto> editManager = new EditableTableManager<>(tabelaImpostos);
        
        // Coluna Código - Editável
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setCellFactory(TextFieldTableCell.forTableColumn());
        colCodigo.setOnEditCommit(event -> {
            Imposto imposto = event.getRowValue();
            imposto.setCodigo(event.getNewValue());
            service.save(imposto);
        });
        colCodigo.setEditable(true);
        
        // Coluna Descrição - Editável
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescricao.setOnEditCommit(event -> {
            Imposto imposto = event.getRowValue();
            imposto.setDescricao(event.getNewValue());
            service.save(imposto);
        });
        colDescricao.setEditable(true);
        
        // Coluna Tipo - Não editável diretamente (usar ComboBox no formulário)
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        
        // Coluna Percentual - Editável
        colPercentual.setCellValueFactory(new PropertyValueFactory<>("percentual"));
        colPercentual.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        colPercentual.setOnEditCommit(event -> {
            Imposto imposto = event.getRowValue();
            imposto.setPercentual(event.getNewValue());
            service.save(imposto);
        });
        colPercentual.setEditable(true);
        
        colMotivo.setCellValueFactory(cellData -> {
            MotivoIsencao m = cellData.getValue().getMotivoIsencao();
            return new javafx.beans.property.SimpleStringProperty(m != null ? m.getCodigo() : "");
        });
        
        tabelaImpostos.setItems(listaImpostos);
        
        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(tabelaImpostos, "Imposto",
                imposto -> preencherFormulario(imposto),
                imposto -> excluirImposto(imposto),
                () -> carregarDados())
            .apply();
    }
    
    private void excluirImposto(Imposto imposto) {
        try {
            service.softDelete(imposto.getId());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Imposto excluído com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
        }
    }

    private void configurarCombos() {
        cmbTipo.setItems(FXCollections.observableArrayList("IVA", "IS", "II"));
        
        cmbMotivo.setItems(listaMotivos);
        cmbMotivo.setConverter(new StringConverter<MotivoIsencao>() {
            @Override
            public String toString(MotivoIsencao object) {
                return object != null ? object.getCodigo() + " - " + object.getDescricao() : "";
            }

            @Override
            public MotivoIsencao fromString(String string) {
                return cmbMotivo.getItems().stream()
                        .filter(item -> (item.getCodigo() + " - " + item.getDescricao()).equals(string))
                        .findFirst().orElse(null);
            }
        });
    }

    private void carregarDados() {
        listaImpostos.setAll(service.findActive());
        listaMotivos.setAll(motivoService.findActive());
    }

    @FXML
    public void salvar() {
        if (!validarCampos()) return;

        Imposto imposto = impostoSelecionado != null ? impostoSelecionado : new Imposto();
        imposto.setCodigo(txtCodigo.getText());
        imposto.setDescricao(txtDescricao.getText());
        imposto.setTipo(cmbTipo.getValue());
        imposto.setPercentual(new BigDecimal(txtPercentual.getText()));
        imposto.setMotivoIsencao(cmbMotivo.getValue());

        try {
            service.save(imposto);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Imposto salvo com sucesso!");
            limparFormulario();
            carregarDados();
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluir() {
        if (impostoSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione um item para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmação");
        confirm.setHeaderText("Excluir Imposto");
        confirm.setContentText("Tem certeza que deseja excluir este imposto?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.softDelete(impostoSelecionado.getId());
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Imposto excluído com sucesso!");
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
        cmbTipo.getSelectionModel().clearSelection();
        txtPercentual.clear();
        cmbMotivo.getSelectionModel().clearSelection();
        impostoSelecionado = null;
        tabelaImpostos.getSelectionModel().clearSelection();
        btnExcluir.setDisable(true);
        btnSalvar.setText("Salvar");
    }

    private void preencherFormulario(Imposto imposto) {
        impostoSelecionado = imposto;
        txtCodigo.setText(imposto.getCodigo());
        txtDescricao.setText(imposto.getDescricao());
        cmbTipo.setValue(imposto.getTipo());
        txtPercentual.setText(imposto.getPercentual().toString());
        cmbMotivo.setValue(imposto.getMotivoIsencao());
        btnExcluir.setDisable(false);
        btnSalvar.setText("Atualizar");
    }

    private boolean validarCampos() {
        if (txtCodigo.getText().isEmpty() || txtDescricao.getText().isEmpty() || 
            cmbTipo.getValue() == null || txtPercentual.getText().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validação", "Todos os campos obrigatórios devem ser preenchidos.");
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
