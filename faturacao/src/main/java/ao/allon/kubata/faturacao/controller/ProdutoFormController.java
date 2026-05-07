package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Controller;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;

import java.math.BigDecimal;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;

@Controller
public class ProdutoFormController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNome;
    @FXML private TextField txtCodigoBarra;
    @FXML private ComboBox<Categoria> comboCategoria;
    @FXML private TextField txtPreco;
    @FXML private TextField txtIva;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private TextArea txtDescricao;
    @FXML private ComboBox<UnidadeMedida> comboUnidade;

    private Produto produto;
    private Runnable onSaveCallback;

    public ProdutoFormController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    @FXML
    public void initialize() {
        carregarCategorias();
        carregarUnidades();
        configurarMascaraNumerica(txtPreco);
        configurarMascaraNumerica(txtIva);
        configurarMascaraInteiro(txtStock);
        configurarMascaraInteiro(txtStockMinimo);
        
        // Configurar ComboBox para mostrar nome da categoria
        comboCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria object) {
                return object == null ? null : object.getNome();
            }

            @Override
            public Categoria fromString(String string) {
                return null; // Não usado
            }
        });
    }
    
    private void carregarUnidades() {
        comboUnidade.getItems().setAll(UnidadeMedida.values());
    }
    
    private void carregarCategorias() {
        comboCategoria.getItems().setAll(categoriaService.findAll());
    }

    private void configurarMascaraNumerica(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldValue);
            }
        });
    }

    private void configurarMascaraInteiro(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(oldValue);
            }
        });
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null) {
            lblTitulo.setText("Editar Produto");
            txtNome.setText(produto.getNome());
            txtCodigoBarra.setText(produto.getCodigoBarra());
            txtPreco.setText(produto.getPrecoUnitario() != null ? produto.getPrecoUnitario().toString() : "");
            txtIva.setText(produto.getPercentualIva() != null ? produto.getPercentualIva().toString() : "14.00");
            txtStock.setText(produto.getStock() != null ? produto.getStock().toString() : "0");
            txtStockMinimo.setText(produto.getStockMinimo() != null ? produto.getStockMinimo().toString() : "5");
            txtDescricao.setText(produto.getDescricao());
            comboCategoria.setValue(produto.getCategoria());
            comboUnidade.setValue(produto.getUnidadeMedida());
            
            // Em edição, stock é gerido via movimentos, mas permitimos ajuste inicial aqui se for novo
            if (produto.getId() != null) {
                txtStock.setDisable(true); // Bloquear stock em edição
            }
        } else {
            lblTitulo.setText("Novo Produto");
            this.produto = new Produto();
            txtStock.setDisable(false);
            comboUnidade.getSelectionModel().select(UnidadeMedida.UNIDADE);
        }
    }
    
    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void handleSalvar() {
        if (!validarCampos()) return;

        try {
            produto.setNome(txtNome.getText().trim());
            produto.setCodigoBarra(txtCodigoBarra.getText().trim());
            produto.setPrecoUnitario(new BigDecimal(txtPreco.getText()));
            produto.setPercentualIva(new BigDecimal(txtIva.getText()));
            produto.setStockMinimo(Integer.parseInt(txtStockMinimo.getText()));
            produto.setDescricao(txtDescricao.getText());
            produto.setCategoria(comboCategoria.getValue());
            produto.setUnidadeMedida(comboUnidade.getValue());
            
            // Apenas define stock se for novo produto (id null)
            if (produto.getId() == null) {
                produto.setStock(Integer.parseInt(txtStock.getText()));
            }

            produtoService.save(produto);
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            fecharJanela();
            
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Erro ao salvar produto", e);
        }
    }

    private boolean validarCampos() {
        if (txtNome.getText().isEmpty() || txtCodigoBarra.getText().isEmpty() || 
            txtPreco.getText().isEmpty() || txtIva.getText().isEmpty()) {
            
            AlertUtils.showWarningAlert("Campos Obrigatórios", "Preencha todos os campos obrigatórios (*)");
            return false;
        }
        return true;
    }

    @FXML
    public void handleCancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) txtNome.getScene().getWindow();
        stage.close();
    }
}
