package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;
import ao.allon.kubata.faturacao.ui.modal.ModalService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class NovaFaturaController {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ao.allon.kubata.faturacao.service.JasperReportService jasperReportService;
    private final ModalService modalService;

    public NovaFaturaController(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ao.allon.kubata.faturacao.service.JasperReportService jasperReportService, ModalService modalService) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.jasperReportService = jasperReportService;
        this.modalService = modalService;
    }

    @FXML
    private ComboBox<Cliente> cbCliente;
    @FXML
    private DatePicker dpEmissao;
    @FXML
    private DatePicker dpVencimento;

    @FXML
    private ComboBox<Produto> cbProduto;
    @FXML
    private TextField txtQuantidade;
    
    @FXML
    private TableView<ItemFatura> tabelaItens;
    @FXML
    private TableColumn<ItemFatura, String> colItemProduto;
    @FXML
    private TableColumn<ItemFatura, Integer> colItemQtd;
    @FXML
    private TableColumn<ItemFatura, String> colItemPreco;
    @FXML
    private TableColumn<ItemFatura, String> colItemIva;
    @FXML
    private TableColumn<ItemFatura, String> colItemTotal;
    @FXML
    private TableColumn<ItemFatura, Void> colItemAcao;

    @FXML
    private Label lblSubtotal;
    @FXML
    private Label lblTotalIva;
    @FXML
    private Label lblTotalGeral;

    private Fatura faturaAtual;
    private ObservableList<ItemFatura> itensObservable = FXCollections.observableArrayList();
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private static boolean isFracionavel(Produto p) {
        if (p == null || p.getUnidadeMedida() == null) return false;
        return switch (p.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private static String unidadeLabel(Produto p) {
        if (p == null || p.getUnidadeMedida() == null) return "un";
        return switch (p.getUnidadeMedida()) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        };
    }

    private static int scaleFor(Produto p) {
        if (p != null && p.getCasasDecimaisQuantidade() != null) {
            return Math.max(0, Math.min(p.getCasasDecimaisQuantidade(), 6));
        }
        return isFracionavel(p) ? 3 : 0;
    }

    private static BigDecimal stockToDisplay(Produto p, Integer stockMin) {
        if (stockMin == null) return BigDecimal.ZERO;
        if (!isFracionavel(p) || p == null || p.getUnidadeMedida() == null) return BigDecimal.valueOf(stockMin);
        return switch (p.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO -> BigDecimal.valueOf(stockMin).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
            case HORA, SERVICO -> BigDecimal.valueOf(stockMin).divide(new BigDecimal("60"), 3, RoundingMode.HALF_UP);
            default -> BigDecimal.valueOf(stockMin);
        };
    }

    private static int toUnidadesMinimas(Produto p, BigDecimal qtd) {
        if (p == null || qtd == null) return 0;
        if (!isFracionavel(p) || p.getUnidadeMedida() == null) return qtd.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (p.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
            default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
        };
    }

    @FXML
    public void initialize() {
        faturaAtual = new Fatura();
        faturaAtual.setDataEmissao(LocalDate.now());
        faturaAtual.setDataVencimento(LocalDate.now().plusDays(30));
        
        setupFormulario();
        setupTabela();
        atualizarTotais();
    }

    private void setupFormulario() {
        dpEmissao.setValue(faturaAtual.getDataEmissao());
        dpVencimento.setValue(faturaAtual.getDataVencimento());
        
        cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c != null ? c.getNome() + " (" + c.getNif() + ")" : "";
            }
            @Override
            public Cliente fromString(String s) { return null; }
        });

        cbProduto.setItems(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produto p) {
                if (p == null) return "";
                BigDecimal shown = stockToDisplay(p, p.getStock());
                String stockLabel = isFracionavel(p)
                        ? shown.setScale(scaleFor(p), RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace(".", ",") + " " + unidadeLabel(p)
                        : String.valueOf(p.getStock() != null ? p.getStock() : 0);
                return p.getNome() + " | Stock: " + stockLabel;
            }
            @Override
            public Produto fromString(String s) { return null; }
        });
    }

    private void setupTabela() {
        colItemProduto.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemQtd.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                ItemFatura row = getTableRow() != null ? getTableRow().getItem() : null;
                if (row == null) {
                    setText(null);
                    return;
                }
                Produto p = row.getProduto();
                if (p != null && isFracionavel(p)) {
                    BigDecimal q = row.getQuantidadeDecimal() != null ? row.getQuantidadeDecimal() : stockToDisplay(p, row.getQuantidade());
                    q = q.setScale(scaleFor(p), RoundingMode.HALF_UP).stripTrailingZeros();
                    setText(q.toPlainString().replace(".", ",") + " " + unidadeLabel(p));
                } else {
                    setText(String.valueOf(value != null ? value : 0));
                }
            }
        });
        
        colItemPreco.setCellValueFactory(cellData -> 
            new SimpleStringProperty(CURRENCY_FORMAT.format(cellData.getValue().getPrecoUnitario())));
            
        colItemIva.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPercentualIva() + "%"));
            
        colItemTotal.setCellValueFactory(cellData -> 
            new SimpleStringProperty(CURRENCY_FORMAT.format(cellData.getValue().getTotal())));

        colItemAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemover = new Button("X");
            {
                btnRemover.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: transparent; -fx-border-color: #ffcccc; -fx-border-radius: 0;");
                btnRemover.setOnAction(event -> {
                    ItemFatura item = getTableView().getItems().get(getIndex());
                    removerItem(item);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemover);
            }
        });

        tabelaItens.setItems(itensObservable);
    }

    @FXML
    public void handleAdicionarItem() {
        Produto produto = cbProduto.getValue();
        if (produto == null) {
            mostrarErro("Selecione um produto.");
            return;
        }

        try {
            String raw = txtQuantidade.getText() != null ? txtQuantidade.getText().trim() : "";
            if (raw.isBlank()) throw new NumberFormatException();
            if (isFracionavel(produto)) {
                BigDecimal qDec = new BigDecimal(raw.replace(",", "."));
                if (qDec.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                qDec = qDec.setScale(scaleFor(produto), RoundingMode.HALF_UP);
                int qMin = toUnidadesMinimas(produto, qDec);
                if (produto.getStock() != null && produto.getStock() < qMin) {
                    BigDecimal disp = stockToDisplay(produto, produto.getStock());
                    mostrarErro("Stock insuficiente. Disponível: " + disp.stripTrailingZeros().toPlainString().replace(".", ",") + " " + unidadeLabel(produto));
                    return;
                }
                ItemFatura item = new ItemFatura();
                item.setDescricao(produto.getNome());
                item.setQuantidadeDecimal(qDec);
                item.setQuantidade(qMin);
                item.setPrecoUnitario(produto.getPrecoUnitario());
                item.setPercentualIva(produto.getPercentualIva());
                item.setProduto(produto);
                item.calculateTotals();
                faturaAtual.addItem(item);
                itensObservable.add(item);
                atualizarTotais();
                cbProduto.setValue(null);
                txtQuantidade.setText("1");
                cbProduto.requestFocus();
                return;
            }
            int qtd = Integer.parseInt(raw);
            if (qtd <= 0) throw new NumberFormatException();
            if (produto.getStock() != null && produto.getStock() < qtd) {
                mostrarErro("Stock insuficiente. Disponível: " + produto.getStock());
                return;
            }
            ItemFatura item = new ItemFatura();
            item.setDescricao(produto.getNome());
            item.setQuantidade(qtd);
            item.setPrecoUnitario(produto.getPrecoUnitario());
            item.setPercentualIva(produto.getPercentualIva());
            item.setProduto(produto);
            item.calculateTotals();
            faturaAtual.addItem(item);
            itensObservable.add(item);
            atualizarTotais();
            cbProduto.setValue(null);
            txtQuantidade.setText("1");
            cbProduto.requestFocus();
        } catch (NumberFormatException e) {
            mostrarErro("Quantidade inválida.");
            return;
        }
    }
    
    private void removerItem(ItemFatura item) {
        faturaAtual.removeItem(item);
        itensObservable.remove(item);
        atualizarTotais();
    }
    
    private void atualizarTotais() {
        faturaAtual.recalculateTotals();
        lblSubtotal.setText(CURRENCY_FORMAT.format(faturaAtual.getSubtotal()));
        lblTotalIva.setText(CURRENCY_FORMAT.format(faturaAtual.getIva()));
        lblTotalGeral.setText(CURRENCY_FORMAT.format(faturaAtual.getTotal()));
    }

    @FXML
    public void handleSalvarRascunho() {
        salvarFatura();
    }

    public void salvarFatura() {
        if (!validarFatura()) return;
        
        try {
            prepararFatura();
            faturaService.salvar(faturaAtual);
            mostrarSucesso("Rascunho salvo com sucesso!");
            fecharJanela();
        } catch (Exception e) {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void handleEmitir() {
        if (!validarFatura()) return;
        
        if (faturaAtual.getItens().isEmpty()) {
            mostrarErro("Adicione pelo menos um item para emitir.");
            return;
        }

        try {
            prepararFatura();
            Fatura emitida = faturaService.salvarEEmitir(faturaAtual);
            
            // Ask to print
            Label msg = new Label("Fatura emitida com sucesso! Deseja imprimir a Fatura?");
            modalService.create()
                .title("Sucesso")
                .content(msg)
                .autoSize()
                .withConfirmButton("Imprimir", () -> {
                    try {
                         java.io.File tempFile = java.io.File.createTempFile("fatura_" + emitida.getNumero().replace("/", "_"), ".pdf");
                         jasperReportService.gerarFaturaPdf(emitida, tempFile);
                         if (java.awt.Desktop.isDesktopSupported()) {
                             java.awt.Desktop.getDesktop().open(tempFile);
                         } else {
                             mostrarSucesso("Fatura gerada em: " + tempFile.getAbsolutePath());
                         }
                         
                         // Pergunta sobre recibo
                         perguntarSobreRecibo(emitida);
                         
                    } catch (Exception e) {
                        mostrarErro("Erro ao gerar PDF: " + e.getMessage());
                    }
                })
                .withCancelButton("Não", () -> fecharJanela())
                .buildAndShow();
            
            fecharJanela();
        } catch (Exception e) {
            mostrarErro("Erro ao emitir: " + e.getMessage());
        }
    }
    
    private void perguntarSobreRecibo(Fatura fatura) {
        // Pequeno delay para não sobrepor modais se o primeiro ainda estiver fechando (embora seja callback)
        Label msg = new Label("Deseja imprimir o Recibo?");
        modalService.create()
            .title("Recibo")
            .content(msg)
            .autoSize()
            .withConfirmButton("Imprimir", () -> {
                try {
                    java.io.File tempFile = java.io.File.createTempFile("recibo_" + fatura.getNumero().replace("/", "_"), ".pdf");
                    jasperReportService.gerarReciboPdf(fatura, tempFile);
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(tempFile);
                    }
                } catch (Exception e) {
                    mostrarErro("Erro ao gerar Recibo: " + e.getMessage());
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
    
    private void prepararFatura() {
        faturaAtual.setCliente(cbCliente.getValue());
        faturaAtual.setDataEmissao(dpEmissao.getValue());
        faturaAtual.setDataVencimento(dpVencimento.getValue());
    }

    private boolean validarFatura() {
        if (cbCliente.getValue() == null) {
            mostrarErro("Selecione o cliente.");
            return false;
        }
        if (dpEmissao.getValue() == null || dpVencimento.getValue() == null) {
             mostrarErro("Datas são obrigatórias.");
             return false;
        }
        return true;
    }

    @FXML
    public void handleCancelar() {
        fecharJanela();
    }
    
    private void fecharJanela() {
        Stage stage = (Stage) cbCliente.getScene().getWindow();
        stage.close();
    }

    private void mostrarErro(String msg) {
        modalService.create()
            .title("Erro")
            .content(new Label(msg))
            .autoSize()
            .withConfirmButton("OK", () -> {})
            .buildAndShow();
    }
    
    private void mostrarSucesso(String msg) {
        modalService.create()
            .title("Sucesso")
            .content(new Label(msg))
            .autoSize()
            .withConfirmButton("OK", () -> {})
            .buildAndShow();
    }
}
