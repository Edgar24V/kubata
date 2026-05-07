package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.core.ui.table.EditableTableManager;
import ao.allon.kubata.faturacao.util.Money;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.FornecedorService;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.view.ProdutoFormView;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Controller
public class InventarioController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final ImpostoService impostoService;
    private final FornecedorService fornecedorService;
    private final ApplicationContext applicationContext;
    private final ModalService modalService;
    private final ObservableList<Produto> produtosList = FXCollections.observableArrayList();

    @FXML private TextField txtPesquisa;
    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, Long> colId;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, String> colCategoria;
    @FXML private TableColumn<Produto, BigDecimal> colPreco;
    @FXML private TableColumn<Produto, Integer> colStock;
    @FXML private TableColumn<Produto, BigDecimal> colIva;
    @FXML private Label lblTotalProdutos;
    
    @org.springframework.beans.factory.annotation.Value("classpath:/fxml/categorias.fxml")
    private Resource categoriasFxml;

    public InventarioController(ProdutoService produtoService,
                               CategoriaService categoriaService,
                               ImpostoService impostoService,
                               FornecedorService fornecedorService,
                               ApplicationContext applicationContext,
                               ModalService modalService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.impostoService = impostoService;
        this.fornecedorService = fornecedorService;
        this.applicationContext = applicationContext;
        this.modalService = modalService;
    }

    @FXML
    public void initialize() {
        configurarColunas();
        carregarProdutos();
        
        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> filtrarProdutos(newValue));
    }

    private void configurarColunas() {
        tabelaProdutos.setEditable(true);
        
        // Manager para colunas editáveis
        EditableTableManager<Produto> editManager = new EditableTableManager<>(tabelaProdutos);
        
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setEditable(false);
        
        // Coluna Código - Editável
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarra"));
        colCodigo.setCellFactory(TextFieldTableCell.forTableColumn());
        colCodigo.setOnEditCommit(event -> {
            Produto p = event.getRowValue();
            p.setCodigoBarra(event.getNewValue());
            produtoService.save(p);
        });
        colCodigo.setEditable(true);
        
        // Coluna Nome - Editável
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setCellFactory(TextFieldTableCell.forTableColumn());
        colNome.setOnEditCommit(event -> {
            Produto p = event.getRowValue();
            p.setNome(event.getNewValue());
            produtoService.save(p);
        });
        colNome.setEditable(true);
        colCategoria.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCategoria() != null) {
                return new SimpleStringProperty(cellData.getValue().getCategoria().getNome());
            }
            return new SimpleStringProperty("-");
        });
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colPreco.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(Money.formatAOA(item));
                }
            }
        });
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("taxaIva"));
        
        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(tabelaProdutos, "Produto",
                produto -> abrirFormularioProduto(produto),
                produto -> excluirProduto(produto),
                () -> carregarProdutos())
            .apply();
    }

    private void carregarProdutos() {
        produtosList.setAll(produtoService.findAll());
        tabelaProdutos.setItems(produtosList);
        atualizarContador();
    }
    
    private void filtrarProdutos(String termo) {
        if (termo == null || termo.isEmpty()) {
            tabelaProdutos.setItems(produtosList);
        } else {
            ObservableList<Produto> filtrados = produtosList.filtered(p -> 
                p.getNome().toLowerCase().contains(termo.toLowerCase()) ||
                p.getCodigoBarra().toLowerCase().contains(termo.toLowerCase())
            );
            tabelaProdutos.setItems(filtrados);
        }
        atualizarContador();
    }

    private void atualizarContador() {
        lblTotalProdutos.setText("Total de produtos: " + tabelaProdutos.getItems().size());
    }

    @FXML
    public void handleNovoProduto() {
        abrirFormularioProduto(null);
    }

    @FXML
    public void handleEditarProduto() {
        Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um produto para editar.");
            return;
        }
        abrirFormularioProduto(selecionado);
    }
    
    private void abrirFormularioProduto(Produto produto) {
        try {
            ProdutoFormView form = new ProdutoFormView(
                produtoService,
                categoriaService,
                impostoService,
                fornecedorService,
                produto,
                (saved) -> carregarProdutos()
            );
            form.setPrefSize(900, 650);

            modalService.create()
                .title(produto == null ? "Novo Produto" : "Editar Produto")
                .content(form)
                .autoSize()
                .buildAndShow();
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao abrir formulário de produto", e);
        }
    }

    @FXML
    public void handleExcluirProduto() {
        Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            excluirProduto(selecionado);
        } else {
            AlertUtils.showWarningAlert("Aviso", "Selecione um produto para excluir.");
        }
    }
    
    private void excluirProduto(Produto produto) {
        boolean confirm = AlertUtils.showConfirmationAlert("Excluir produto: " + produto.getNome(), 
            "Tem certeza? Esta ação não pode ser desfeita.");
            
        if (confirm) {
            try {
                produtoService.delete(produto.getId());
                carregarProdutos();
                AlertUtils.showInfoAlert("Sucesso", "Produto excluído com sucesso.");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro ao excluir", e.getMessage());
            }
        }
    }

    @FXML
    public void handleEntradaStock() {
        Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um produto para registrar entrada.");
            return;
        }

        TextField qtdField = new TextField();
        boolean fracionavel = selecionado.getUnidadeMedida() != null && switch (selecionado.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
        String unidadeLabel = selecionado.getUnidadeMedida() != null ? switch (selecionado.getUnidadeMedida()) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        } : "un";

        Label lblQtd = new Label(fracionavel ? ("Quantidade a adicionar (" + unidadeLabel + "):") : "Quantidade a adicionar:");
        VBox content = new VBox(8, new Label("Produto: " + selecionado.getNome()), lblQtd, qtdField);
        content.setPadding(new Insets(10));
        
        // Usando ModalService para input dialog complexo
        modalService.create()
                .title("Entrada de Stock")
                .content(content)
                .dynamicSize()
                .withConfirmButton("Confirmar", () -> {
                    try {
                        int quantidade;
                        if (fracionavel) {
                            BigDecimal q = new BigDecimal(qtdField.getText().replace(",", "."));
                            quantidade = switch (selecionado.getUnidadeMedida()) {
                                case KILOGRAMA, LITRO, METRO -> q.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                                case HORA, SERVICO -> q.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                                default -> q.setScale(0, RoundingMode.HALF_UP).intValue();
                            };
                        } else {
                            quantidade = Integer.parseInt(qtdField.getText());
                        }
                        produtoService.registarEntrada(selecionado.getId(), quantidade, "Entrada manual via sistema");
                        carregarProdutos();
                        AlertUtils.showInfoAlert("Sucesso", "Stock atualizado com sucesso.");
                        return true;
                    } catch (NumberFormatException e) {
                        AlertUtils.showErrorAlert("Erro", "Quantidade inválida.");
                        return false;
                    } catch (ArithmeticException e) {
                        AlertUtils.showErrorAlert("Erro", "Quantidade inválida.");
                        return false;
                    } catch (Exception e) {
                        AlertUtils.showErrorAlert("Erro", e.getMessage());
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    @FXML
    public void handleSaidaStock() {
        Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um produto para registrar saída.");
            return;
        }

        TextField qtdField = new TextField();
        boolean fracionavel = selecionado.getUnidadeMedida() != null && switch (selecionado.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
        String unidadeLabel = selecionado.getUnidadeMedida() != null ? switch (selecionado.getUnidadeMedida()) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        } : "un";

        Label lblQtd = new Label(fracionavel ? ("Quantidade a remover (" + unidadeLabel + "):") : "Quantidade a remover:");
        VBox content = new VBox(8, new Label("Produto: " + selecionado.getNome()), lblQtd, qtdField);
        content.setPadding(new Insets(10));
        
        modalService.create()
                .title("Saída de Stock")
                .content(content)
                .width(480)
                .withConfirmButton("Confirmar", () -> {
                    try {
                        int quantidade;
                        if (fracionavel) {
                            BigDecimal q = new BigDecimal(qtdField.getText().replace(",", "."));
                            quantidade = switch (selecionado.getUnidadeMedida()) {
                                case KILOGRAMA, LITRO, METRO -> q.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                                case HORA, SERVICO -> q.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                                default -> q.setScale(0, RoundingMode.HALF_UP).intValue();
                            };
                        } else {
                            quantidade = Integer.parseInt(qtdField.getText());
                        }
                        produtoService.registarSaida(selecionado.getId(), quantidade, "Saída manual via sistema");
                        carregarProdutos();
                        AlertUtils.showInfoAlert("Sucesso", "Stock atualizado com sucesso.");
                        return true;
                    } catch (NumberFormatException e) {
                        AlertUtils.showErrorAlert("Erro", "Quantidade inválida.");
                        return false;
                    } catch (ArithmeticException e) {
                        AlertUtils.showErrorAlert("Erro", "Quantidade inválida.");
                        return false;
                    } catch (Exception e) {
                        AlertUtils.showErrorAlert("Erro", e.getMessage());
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    @FXML
    public void handleGerirCategorias() {
        try {
            FXMLLoader loader = new FXMLLoader(categoriasFxml.getURL());
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Gestão de Categorias");
            stage.setScene(new Scene(root, 600, 400));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Recarregar produtos para atualizar nomes de categorias se necessário
            carregarProdutos();
            
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao abrir gestão de categorias", e);
        }
    }
    
    @FXML
    public void handlePesquisar() {
        filtrarProdutos(txtPesquisa.getText());
    }


}
