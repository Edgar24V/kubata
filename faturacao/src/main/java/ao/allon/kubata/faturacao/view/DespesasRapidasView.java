package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.ContaBancariaService;
import ao.allon.kubata.faturacao.service.DespesaService;
import ao.allon.kubata.faturacao.service.FornecedorService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DespesasRapidasView extends BorderPane {

    private final DespesaService despesaService;
    private final CategoriaService categoriaService;
    private final FornecedorService fornecedorService;
    private final ContaBancariaService contaService;
    private final ModalService modalService;

    private TableView<Despesa> table;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DespesasRapidasView(DespesaService despesaService, CategoriaService categoriaService, FornecedorService fornecedorService, ContaBancariaService contaService, ModalService modalService) {
        this.despesaService = despesaService;
        this.categoriaService = categoriaService;
        this.fornecedorService = fornecedorService;
        this.contaService = contaService;
        this.modalService = modalService;
        
        getStyleClass().add("despesas-rapidas-view");
        setPadding(new Insets(20));
        
        setTop(buildHeader());
        setCenter(buildTable());
        
        refreshData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(0, 0, 15, 0));

        HBox top = new HBox(15);
        top.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Despesas Rápidas");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Registo simplificado de despesas e pagamentos");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNova = new Button("Nova Despesa", new FontIcon(Feather.MINUS_CIRCLE));
        btnNova.getStyleClass().add(Styles.ACCENT);
        btnNova.setOnAction(e -> showNovaDespesaDialog());

        top.getChildren().addAll(titleBox, spacer, btnNova);
        header.getChildren().add(top);
        return header;
    }

    private TableView<Despesa> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Despesa, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataEmissao().format(DATE_FMT)));
        
        TableColumn<Despesa, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        
        TableColumn<Despesa, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoria() != null ? c.getValue().getCategoria().getNome() : "-"));
        
        TableColumn<Despesa, String> colForn = new TableColumn<>("Fornecedor");
        colForn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFornecedor() != null ? c.getValue().getFornecedor().getNome() : "Diversos"));

        TableColumn<Despesa, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(String.format("Kz %.2f", c.getValue().getValor())));
        colValor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        
        TableColumn<Despesa, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().toString()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PAGA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -color-warning-fg;");
                    }
                }
            }
        });

        table.getColumns().addAll(colData, colDesc, colCat, colForn, colValor, colStatus);
        return table;
    }

    private void refreshData() {
        List<Despesa> despesas = despesaService.findAll();
        // Ordenar por data desc
        despesas.sort((d1, d2) -> d2.getDataEmissao().compareTo(d1.getDataEmissao()));
        table.setItems(FXCollections.observableArrayList(despesas));
    }

    private void showNovaDespesaDialog() {
        VBox form = new VBox(15);
        
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descrição da despesa (ex: Táxi, Almoço)");
        
        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        cbCategoria.setItems(FXCollections.observableArrayList(categoriaService.findAll()));
        cbCategoria.setPromptText("Categoria");
        cbCategoria.setMaxWidth(Double.MAX_VALUE);
        
        ComboBox<Fornecedor> cbFornecedor = new ComboBox<>();
        cbFornecedor.setItems(FXCollections.observableArrayList(fornecedorService.findAll()));
        cbFornecedor.setPromptText("Fornecedor (Opcional)");
        cbFornecedor.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        
        DatePicker dtData = new DatePicker(LocalDate.now());
        dtData.setMaxWidth(Double.MAX_VALUE);
        
        CheckBox chkPago = new CheckBox("Pagamento Imediato?");
        chkPago.setSelected(true);
        
        ComboBox<ContaBancaria> cbConta = new ComboBox<>();
        cbConta.setItems(FXCollections.observableArrayList(contaService.findAll()));
        cbConta.setPromptText("Conta / Caixa");
        cbConta.setMaxWidth(Double.MAX_VALUE);
        
        chkPago.selectedProperty().addListener((obs, oldV, newV) -> cbConta.setDisable(!newV));

        form.getChildren().addAll(
            new Label("Descrição"), txtDesc,
            new Label("Categoria"), cbCategoria,
            new Label("Fornecedor"), cbFornecedor,
            new Label("Valor (Kz)"), txtValor,
            new Label("Data"), dtData,
            chkPago, cbConta
        );

        modalService.create()
            .title("Registar Despesa Rápida")
            .content(form)
            .width(400)
            .withConfirmButton("Registar", () -> {
                try {
                    if (txtDesc.getText().isEmpty()) throw new IllegalArgumentException("Descrição obrigatória");
                    if (txtValor.getText().isEmpty()) throw new IllegalArgumentException("Valor obrigatório");
                    if (chkPago.isSelected() && cbConta.getValue() == null) throw new IllegalArgumentException("Selecione a conta de pagamento");
                    
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    
                    Despesa d = new Despesa();
                    d.setDescricao(txtDesc.getText());
                    d.setCategoria(cbCategoria.getValue());
                    d.setFornecedor(cbFornecedor.getValue());
                    d.setValor(valor);
                    d.setDataEmissao(dtData.getValue());
                    d.setDataVencimento(dtData.getValue());
                    
                    Long contaId = chkPago.isSelected() ? cbConta.getValue().getId() : null;
                    
                    despesaService.registrarDespesaRapida(d, contaId);
                    
                    refreshData();
                    AlertUtils.showInfoAlert("Sucesso", "Despesa registada com sucesso.");
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Falha ao registar despesa", ex);
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
}
