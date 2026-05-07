package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.domain.StatusDespesa;
import ao.allon.kubata.faturacao.service.DespesaService;
import ao.allon.kubata.faturacao.service.FornecedorService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ContasPagarView extends BorderPane {

    private final DespesaService despesaService;
    private final FornecedorService fornecedorService;
    private final ao.allon.kubata.faturacao.service.ContaBancariaService contaBancariaService;
    private final SessionManager session;
    private final ao.allon.kubata.faturacao.service.JasperReportService jasperService;
    private final ModalService modalService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));

    private final ObservableList<Despesa> despesasData = FXCollections.observableArrayList();

    private AdvancedTableView<Despesa> table;
    private TextField txtSearch;
    private ComboBox<Fornecedor> cbFornecedor;
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<String> cbStatus;

    private Label lblAberto;
    private Label lblVencido;
    private Label lblPagoHoje;

    public ContasPagarView(DespesaService despesaService, FornecedorService fornecedorService, ao.allon.kubata.faturacao.service.ContaBancariaService contaBancariaService, SessionManager session, ao.allon.kubata.faturacao.service.JasperReportService jasperService, ModalService modalService) {
        this.despesaService = despesaService;
        this.fornecedorService = fornecedorService;
        this.contaBancariaService = contaBancariaService;
        this.session = session;
        this.jasperService = jasperService;
        this.modalService = modalService;
        getStyleClass().add("contas-pagar-view");
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(5);
        Label title = new Label("Contas a Pagar");
        title.getStyleClass().addAll(Styles.TITLE_2);
        Label subtitle = new Label(LocalDate.now().format(dateFormatter));
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnPagar = new Button("Registrar Pagamento", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        btnPagar.getStyleClass().addAll(Styles.SUCCESS);
        btnPagar.setOnAction(e -> showPagamentoModal(getSelectedDespesa()));
        Button btnRelatorio = new Button("Relatórios", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnRelatorio.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRelatorio.setOnAction(e -> showRelatoriosModal());
        Button btnExport = new Button("Exportar CSV", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExport.setOnAction(e -> exportCsvPagar());
        header.getChildren().addAll(titleBox, spacer, btnRelatorio, btnExport, btnPagar);
        container.getChildren().add(header);

        FlowPane kpi = new FlowPane(20, 20);
        kpi.setAlignment(Pos.TOP_LEFT);
        lblAberto = new Label("Kz 0,00");
        lblVencido = new Label("Kz 0,00");
        lblPagoHoje = new Label("Kz 0,00");
        kpi.getChildren().add(new MetricCard("Em Aberto", lblAberto, Feather.DOLLAR_SIGN, Styles.WARNING));
        kpi.getChildren().add(new MetricCard("Vencido", lblVencido, Feather.ALERT_TRIANGLE, Styles.DANGER));
        kpi.getChildren().add(new MetricCard("Pago Hoje", lblPagoHoje, Feather.CHECK_CIRCLE, Styles.SUCCESS));
        container.getChildren().add(kpi);

        container.getChildren().add(new Separator());

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        txtSearch = new TextField();
        txtSearch.setPromptText("Buscar por descrição/fornecedor...");
        txtSearch.setPrefWidth(240);
        cbFornecedor = new ComboBox<>(FXCollections.observableArrayList(fornecedorService.findAll()));
        cbFornecedor.setPromptText("Todos Fornecedores");
        cbFornecedor.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Fornecedor f) { return f == null ? "" : f.getNome(); }
            @Override
            public Fornecedor fromString(String s) { return null; }
        });
        dpInicio = new DatePicker();
        dpInicio.setPromptText("Emissão início");
        dpFim = new DatePicker();
        dpFim.setPromptText("Emissão fim");
        cbStatus = new ComboBox<>(FXCollections.observableArrayList("Todos", "ABERTA", "PAGA", "CANCELADA"));
        cbStatus.getSelectionModel().select("ABERTA");
        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.setOnAction(e -> refreshData());
        filters.getChildren().addAll(txtSearch, cbFornecedor, new Separator(), dpInicio, new Label("até"), dpFim, cbStatus, btnRefresh);
        container.getChildren().add(filters);

        table = new AdvancedTableView<>();
        table.setData(despesasData);
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Despesa, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        TableColumn<Despesa, String> colFornecedor = new TableColumn<>("Fornecedor");
        colFornecedor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFornecedor() != null ? cell.getValue().getFornecedor().getNome() : "-"));
        TableColumn<Despesa, LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        TableColumn<Despesa, LocalDate> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        TableColumn<Despesa, BigDecimal> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colValor.setCellFactory(col -> currencyCell());
        TableColumn<Despesa, BigDecimal> colPago = new TableColumn<>("Pago");
        colPago.setCellValueFactory(new PropertyValueFactory<>("valorPago"));
        colPago.setCellFactory(col -> currencyCell());
        TableColumn<Despesa, BigDecimal> colAberto = new TableColumn<>("Aberto");
        colAberto.setCellValueFactory(cell -> {
            BigDecimal valor = cell.getValue().getValor() != null ? cell.getValue().getValor() : BigDecimal.ZERO;
            BigDecimal pago = cell.getValue().getValorPago() != null ? cell.getValue().getValorPago() : BigDecimal.ZERO;
            return new SimpleObjectProperty<>(valor.subtract(pago));
        });
        colAberto.setCellFactory(col -> currencyCell());
        TableColumn<Despesa, StatusDespesa> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(colDesc, colFornecedor, colEmissao, colVenc, colValor, colPago, colAberto, colStatus);

        ContextMenu ctx = new ContextMenu();
        MenuItem itemPagar = new MenuItem("Registrar Pagamento", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        itemPagar.setOnAction(e -> showPagamentoModal(getSelectedDespesa()));
        ctx.getItems().addAll(itemPagar);
        table.setContextMenu(ctx);

        container.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        txtSearch.textProperty().addListener((o, ov, nv) -> applyFilters());
        cbFornecedor.valueProperty().addListener((o, ov, nv) -> applyFilters());
        dpInicio.valueProperty().addListener((o, ov, nv) -> applyFilters());
        dpFim.valueProperty().addListener((o, ov, nv) -> applyFilters());
        cbStatus.valueProperty().addListener((o, ov, nv) -> applyFilters());

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scroll);
    }

    private TableCell<Despesa, BigDecimal> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Kz %.2f", item));
                }
            }
        };
    }

    public void refreshData() {
        despesasData.setAll(despesaService.findAll());
        table.setData(despesasData);
        applyFilters();
        updateKpis();
    }

    private void applyFilters() {
        String search = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        Fornecedor fornecedor = cbFornecedor.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        String status = cbStatus.getValue();
        
        table.setFilter(d -> {
            boolean matchSearch = search.isEmpty() ||
                    (d.getDescricao() != null && d.getDescricao().toLowerCase().contains(search)) ||
                    (d.getFornecedor() != null && d.getFornecedor().getNome() != null && d.getFornecedor().getNome().toLowerCase().contains(search));
            boolean matchFornecedor = fornecedor == null || (d.getFornecedor() != null && d.getFornecedor().getId().equals(fornecedor.getId()));
            boolean matchDate = true;
            if (inicio != null && d.getDataEmissao() != null && d.getDataEmissao().isBefore(inicio)) matchDate = false;
            if (fim != null && d.getDataEmissao() != null && d.getDataEmissao().isAfter(fim)) matchDate = false;
            boolean matchStatus = status == null || "Todos".equals(status) || (d.getStatus() != null && d.getStatus().name().equals(status));
            return matchSearch && matchFornecedor && matchDate && matchStatus;
        });
        updateKpis();
    }

    private void updateKpis() {
        BigDecimal aberto = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        LocalDate hoje = LocalDate.now();
        for (Despesa d : table.getItems()) {
            BigDecimal valor = d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
            BigDecimal pago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
            BigDecimal emAberto = valor.subtract(pago);
            if (emAberto.compareTo(BigDecimal.ZERO) > 0) {
                aberto = aberto.add(emAberto);
                if (d.getDataVencimento() != null && d.getDataVencimento().isBefore(hoje)) {
                    vencido = vencido.add(emAberto);
                }
            }
        }
        BigDecimal pagosHoje = despesaService.sumPagoPeriodo(hoje, hoje);
        lblAberto.setText(String.format("Kz %.2f", aberto));
        lblVencido.setText(String.format("Kz %.2f", vencido));
        lblPagoHoje.setText(String.format("Kz %.2f", pagosHoje));
    }

    private Despesa getSelectedDespesa() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void showPagamentoModal(Despesa despesa) {
        if (despesa == null) {
            AlertUtils.showErrorAlert("Seleção necessária", "Selecione uma despesa.");
            return;
        }
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        Label title = new Label("Pagamento - " + despesa.getDescricao());
        title.getStyleClass().add(Styles.TITLE_3);

        BigDecimal valor = despesa.getValor() != null ? despesa.getValor() : BigDecimal.ZERO;
        BigDecimal pago = despesa.getValorPago() != null ? despesa.getValorPago() : BigDecimal.ZERO;
        BigDecimal aberto = valor.subtract(pago);

        Label lblResumo = new Label("Em aberto: " + String.format("Kz %.2f", aberto));
        lblResumo.getStyleClass().add(Styles.TEXT_MUTED);

        ComboBox<String> cbForma = new ComboBox<>(FXCollections.observableArrayList("Transferência", "POS", "Dinheiro", "Cheque"));
        cbForma.setPromptText("Forma de Pagamento");
        
        ComboBox<ao.allon.kubata.faturacao.domain.ContaBancaria> cbConta = new ComboBox<>();
        cbConta.setItems(FXCollections.observableArrayList(contaBancariaService.findAll()));
        cbConta.setPromptText("Conta de Origem (Opcional)");
        cbConta.setMaxWidth(Double.MAX_VALUE);

        TextField txtValor = new TextField(aberto.toString());
        TextField txtReferencia = new TextField();
        txtReferencia.setPromptText("Referência/Comprovativo");
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações");
        txtObs.setPrefRowCount(3);

        root.getChildren().addAll(title, lblResumo, cbForma, cbConta, txtValor, txtReferencia, txtObs);

        modalService.create()
            .title("Registrar Pagamento")
            .content(root)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valorInformado = new BigDecimal(txtValor.getText().replace(",", "."));
                    if (valorInformado.compareTo(BigDecimal.ZERO) <= 0 || valorInformado.compareTo(aberto) > 0) {
                        throw new IllegalArgumentException("Valor inválido");
                    }
                    
                    Long contaId = cbConta.getValue() != null ? cbConta.getValue().getId() : null;
                    
                    despesaService.registrarPagamento(despesa.getId(), valorInformado, txtReferencia.getText(), txtObs.getText(), contaId);
                    AlertUtils.showInfoAlert("Sucesso", "Pagamento registado.");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void exportCsvPagar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Contas a Pagar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("contas_a_pagar_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("Descricao,Fornecedor,Emissao,Vencimento,Valor,Pago,Aberto,Status\n");
                for (Despesa d : table.getItems()) {
                    BigDecimal valor = d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
                    BigDecimal pago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
                    BigDecimal aberto = valor.subtract(pago);
                    String linha = String.join(",",
                            safe(d.getDescricao()),
                            safe(d.getFornecedor() != null ? d.getFornecedor().getNome() : ""),
                            safeDate(d.getDataEmissao()),
                            safeDate(d.getDataVencimento()),
                            valor.toString(),
                            pago.toString(),
                            aberto.toString(),
                            d.getStatus() != null ? d.getStatus().name() : ""
                    );
                    w.write(linha + "\n");
                }
                AlertUtils.showInfoAlert("Exportação", "CSV exportado com sucesso: \n" + file.getAbsolutePath());
            } catch (IOException ex) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar CSV: " + ex.getMessage());
            }
        }
    }

    private String safe(String s) { return s == null ? "" : s.replace("\n", " ").replace(",", " "); }
    private String safeDate(LocalDate d) { return d == null ? "" : d.toString(); }

    private void exportExtratoFornecedoresPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Extrato por Fornecedor (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("extrato_fornecedores_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                java.util.List<Despesa> list = new java.util.ArrayList<>();
                for (Despesa d : table.getItems()) list.add(d);
                jasperService.gerarExtratoFornecedoresPdf(list, null, file);
                AlertUtils.showInfoAlert("Exportação", "PDF exportado com sucesso: \n" + file.getAbsolutePath());
            } catch (Exception ex) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar PDF: " + ex.getMessage());
            }
        }
    }

    private void showRelatoriosModal() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        Label title = new Label("Relatórios - Contas a Pagar");
        title.getStyleClass().add(Styles.TITLE_4);
        Button btnCsv = new Button("Exportar lista (CSV)", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnCsv.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnCsv.setOnAction(e -> exportCsvPagar());
        Button btnExtratoFornecedores = new Button("Extrato por Fornecedor (CSV)", IconUtils.icon(Feather.TRUCK, IconUtils.SIZE_SMALL));
        btnExtratoFornecedores.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExtratoFornecedores.setOnAction(e -> exportExtratoFornecedoresCsv());
        Button btnPdf = new Button("Extrato por Fornecedor (PDF)", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnPdf.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnPdf.setOnAction(e -> exportExtratoFornecedoresPdf());
        root.getChildren().addAll(title, new Separator(), btnCsv, btnExtratoFornecedores, btnPdf);
        modalService.create()
            .title("Relatórios - Contas a Pagar")
            .content(root)
            .autoSize()
            .withCancelButton("Fechar")
            .buildAndShow();
    }

    private void exportExtratoFornecedoresCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Extrato por Fornecedor");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("extrato_fornecedores_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("Fornecedor,Valor,Pago,Aberto\n");
                java.util.Map<String, java.math.BigDecimal[]> map = new java.util.HashMap<>();
                for (Despesa d : table.getItems()) {
                    String fornecedor = d.getFornecedor() != null ? safe(d.getFornecedor().getNome()) : "Sem Fornecedor";
                    BigDecimal valor = d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
                    BigDecimal pago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
                    BigDecimal aberto = valor.subtract(pago);
                    java.math.BigDecimal[] acc = map.getOrDefault(fornecedor, new java.math.BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    acc[0] = acc[0].add(valor);
                    acc[1] = acc[1].add(pago);
                    acc[2] = acc[2].add(aberto);
                    map.put(fornecedor, acc);
                }
                for (java.util.Map.Entry<String, java.math.BigDecimal[]> e : map.entrySet()) {
                    java.math.BigDecimal[] v = e.getValue();
                    w.write(String.join(",", e.getKey(), v[0].toString(), v[1].toString(), v[2].toString()) + "\n");
                }
                AlertUtils.showInfoAlert("Exportação", "Extrato por fornecedor exportado: \n" + file.getAbsolutePath());
            } catch (IOException ex) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar CSV: " + ex.getMessage());
            }
        }
    }



    private static class MetricCard extends Card {
        public MetricCard(String title, Label valueLabel, Feather icon, String colorStyle) {
            getStyleClass().add(Styles.ELEVATED_1);
            setPrefWidth(280);
            VBox body = new VBox(10);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(15));
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(24);
            fontIcon.getStyleClass().addAll(colorStyle);
            Label lblTitle = new Label(title);
            lblTitle.getStyleClass().add(Styles.TEXT_MUTED);
            lblTitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
            headerBox.getChildren().addAll(fontIcon, lblTitle);
            valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
            valueLabel.getStyleClass().add(colorStyle);
            body.getChildren().addAll(headerBox, valueLabel);
            setBody(body);
        }
    }
}
