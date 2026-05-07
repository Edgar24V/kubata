package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ReciboService;
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
import javafx.scene.Node;
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

public class ContasReceberView extends BorderPane {

    private final FaturaService faturaService;
    private final ReciboService reciboService;
    private final ClienteService clienteService;
    private final SessionManager session;
    private final ao.allon.kubata.faturacao.service.JasperReportService jasperService;
    private final ModalService modalService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));

    private final ObservableList<Fatura> faturasData = FXCollections.observableArrayList();

    private AdvancedTableView<Fatura> tableFaturas;
    private TextField txtSearch;
    private ComboBox<Cliente> cbCliente;
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<String> cbStatus;

    private Label lblTotalAberto;
    private Label lblAbertoVencido;
    private Label lblRecebidoHoje;

    public ContasReceberView(FaturaService faturaService, ReciboService reciboService, ClienteService clienteService, SessionManager session, ao.allon.kubata.faturacao.service.JasperReportService jasperService, ModalService modalService) {
        this.faturaService = faturaService;
        this.reciboService = reciboService;
        this.clienteService = clienteService;
        this.session = session;
        this.jasperService = jasperService;
        this.modalService = modalService;
        getStyleClass().add("contas-receber-view");
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
        Label title = new Label("Contas a Receber");
        title.getStyleClass().addAll(Styles.TITLE_2);
        Label subtitle = new Label(LocalDate.now().format(dateFormatter));
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnReceber = new Button("Registrar Recebimento", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        btnReceber.getStyleClass().addAll(Styles.SUCCESS);
        btnReceber.setOnAction(e -> showRecebimentoModal(getSelectedFatura()));
        Button btnRelatorio = new Button("Relatórios", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnRelatorio.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRelatorio.setOnAction(e -> showRelatoriosModal());
        Button btnExport = new Button("Exportar CSV", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExport.setOnAction(e -> exportCsvReceber());
        header.getChildren().addAll(titleBox, spacer, btnRelatorio, btnExport, btnReceber);
        container.getChildren().add(header);

        FlowPane kpiPane = new FlowPane(20, 20);
        kpiPane.setAlignment(Pos.TOP_LEFT);
        lblTotalAberto = new Label("Kz 0,00");
        lblAbertoVencido = new Label("Kz 0,00");
        lblRecebidoHoje = new Label("Kz 0,00");
        kpiPane.getChildren().add(new MetricCard("Total em Aberto", lblTotalAberto, Feather.DOLLAR_SIGN, Styles.WARNING));
        kpiPane.getChildren().add(new MetricCard("Vencido", lblAbertoVencido, Feather.ALERT_TRIANGLE, Styles.DANGER));
        kpiPane.getChildren().add(new MetricCard("Recebido Hoje", lblRecebidoHoje, Feather.CHECK_CIRCLE, Styles.SUCCESS));
        container.getChildren().add(kpiPane);

        container.getChildren().add(new Separator());

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        txtSearch = new TextField();
        txtSearch.setPromptText("Buscar por número/cliente...");
        txtSearch.setPrefWidth(240);
        cbCliente = new ComboBox<>(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setPromptText("Todos Clientes");
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        cbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Cliente c) { return c == null ? "" : c.getNome(); }
            @Override
            public Cliente fromString(String s) { return null; }
        });
        dpInicio = new DatePicker();
        dpInicio.setPromptText("Emissão início");
        dpFim = new DatePicker();
        dpFim.setPromptText("Emissão fim");
        cbStatus = new ComboBox<>(FXCollections.observableArrayList("Todos", "EMITIDA", "PAGA", "CANCELADA", "RASCUNHO"));
        cbStatus.getSelectionModel().select("EMITIDA");
        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.setOnAction(e -> refreshData());
        filters.getChildren().addAll(txtSearch, cbCliente, new Separator(), dpInicio, new Label("até"), dpFim, cbStatus, btnRefresh);
        container.getChildren().add(filters);

        tableFaturas = new AdvancedTableView<>();
        tableFaturas.setData(faturasData);
        TableUtils.standardize(tableFaturas);
        tableFaturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCliente() != null ? cell.getValue().getCliente().getNome() : "-"));
        TableColumn<Fatura, LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        TableColumn<Fatura, LocalDate> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        TableColumn<Fatura, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> currencyCell());
        TableColumn<Fatura, BigDecimal> colRecebido = new TableColumn<>("Recebido");
        colRecebido.setCellValueFactory(cell -> new SimpleObjectProperty<>(reciboService.totalRecebidoFatura(cell.getValue().getId())));
        colRecebido.setCellFactory(col -> currencyCell());
        TableColumn<Fatura, BigDecimal> colAberto = new TableColumn<>("Em Aberto");
        colAberto.setCellValueFactory(cell -> {
            BigDecimal total = cell.getValue().getTotal() != null ? cell.getValue().getTotal() : BigDecimal.ZERO;
            BigDecimal recebido = reciboService.totalRecebidoFatura(cell.getValue().getId());
            return new SimpleObjectProperty<>(total.subtract(recebido));
        });
        colAberto.setCellFactory(col -> currencyCell());
        TableColumn<Fatura, StatusFatura> colStatusF = new TableColumn<>("Status");
        colStatusF.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableFaturas.getColumns().addAll(colNumero, colCliente, colEmissao, colVenc, colTotal, colRecebido, colAberto, colStatusF);

        ContextMenu ctx = new ContextMenu();
        MenuItem itemReceber = new MenuItem("Registrar Recebimento", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        itemReceber.setOnAction(e -> showRecebimentoModal(getSelectedFatura()));
        ctx.getItems().addAll(itemReceber);
        tableFaturas.setContextMenu(ctx);

        container.getChildren().add(tableFaturas);
        VBox.setVgrow(tableFaturas, Priority.ALWAYS);

        txtSearch.textProperty().addListener((o, ov, nv) -> applyFilters());
        cbCliente.valueProperty().addListener((o, ov, nv) -> applyFilters());
        dpInicio.valueProperty().addListener((o, ov, nv) -> applyFilters());
        dpFim.valueProperty().addListener((o, ov, nv) -> applyFilters());
        cbStatus.valueProperty().addListener((o, ov, nv) -> applyFilters());

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scroll);
    }

    private TableCell<Fatura, BigDecimal> currencyCell() {
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

    public void refreshData() {
        faturasData.setAll(faturaService.findAll());
        tableFaturas.setData(faturasData);
        applyFilters();
        updateKpis();
    }

    private void applyFilters() {
        String search = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        Cliente cliente = cbCliente.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        String status = cbStatus.getValue();
        
        tableFaturas.setFilter(f -> {
            boolean matchSearch = search.isEmpty() ||
                    (f.getNumero() != null && f.getNumero().toLowerCase().contains(search)) ||
                    (f.getCliente() != null && f.getCliente().getNome() != null && f.getCliente().getNome().toLowerCase().contains(search));
            boolean matchCliente = cliente == null || (f.getCliente() != null && f.getCliente().getId().equals(cliente.getId()));
            boolean matchDate = true;
            if (inicio != null && f.getDataEmissao() != null && f.getDataEmissao().isBefore(inicio)) matchDate = false;
            if (fim != null && f.getDataEmissao() != null && f.getDataEmissao().isAfter(fim)) matchDate = false;
            boolean matchStatus = status == null || status.equals("Todos") || (f.getStatus() != null && f.getStatus().name().equals(status));
            return matchSearch && matchCliente && matchDate && matchStatus;
        });
        updateKpis();
    }

    private void updateKpis() {
        BigDecimal totalAberto = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        LocalDate hoje = LocalDate.now();
        for (Fatura f : tableFaturas.getItems()) {
            BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
            BigDecimal aberto = total.subtract(recebido);
            if (aberto.compareTo(BigDecimal.ZERO) > 0) {
                totalAberto = totalAberto.add(aberto);
                if (f.getDataVencimento() != null && f.getDataVencimento().isBefore(hoje)) {
                    vencido = vencido.add(aberto);
                }
            }
        }
        BigDecimal recebidoHoje = reciboService.findByPeriodo(hoje, hoje).stream()
                .map(Recibo::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalAberto.setText(String.format("Kz %.2f", totalAberto));
        lblAbertoVencido.setText(String.format("Kz %.2f", vencido));
        lblRecebidoHoje.setText(String.format("Kz %.2f", recebidoHoje));
    }

    private Fatura getSelectedFatura() {
        return tableFaturas.getSelectionModel().getSelectedItem();
    }

    private void showRecebimentoModal(Fatura fatura) {
        if (fatura == null) {
            AlertUtils.showErrorAlert("Seleção necessária", "Selecione uma fatura para receber.");
            return;
        }
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        Label title = new Label("Recebimento - " + fatura.getNumero());
        title.getStyleClass().add(Styles.TITLE_3);

        BigDecimal total = fatura.getTotal() != null ? fatura.getTotal() : BigDecimal.ZERO;
        BigDecimal recebido = reciboService.totalRecebidoFatura(fatura.getId());
        BigDecimal aberto = total.subtract(recebido);

        Label lblResumo = new Label("Em aberto: " + String.format("Kz %.2f", aberto));
        lblResumo.getStyleClass().add(Styles.TEXT_MUTED);

        ComboBox<String> cbForma = new ComboBox<>(FXCollections.observableArrayList("Dinheiro", "Transferência", "POS", "Cheque"));
        cbForma.setPromptText("Forma de Pagamento");
        TextField txtValor = new TextField(aberto.toString());
        TextField txtReferencia = new TextField();
        txtReferencia.setPromptText("Referência/Comprovativo");
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações");
        txtObs.setPrefRowCount(3);

        root.getChildren().addAll(title, lblResumo, cbForma, txtValor, txtReferencia, txtObs);
        
        modalService.create()
            .title("Registrar Recebimento")
            .content(root)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    if (valor.compareTo(BigDecimal.ZERO) <= 0 || valor.compareTo(aberto) > 0) {
                        throw new IllegalArgumentException("Valor inválido");
                    }
                    reciboService.registrar(fatura.getId(), valor, txtReferencia.getText(), txtObs.getText());
                    faturaService.registrarRecebimento(fatura.getId(), valor, txtReferencia.getText());
                    AlertUtils.showInfoAlert("Sucesso", "Recebimento registado.");
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

    private void exportCsvReceber() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Contas a Receber");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("contas_a_receber_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("Numero,Cliente,Emissao,Vencimento,Total,Recebido,Aberto,Status\n");
                for (Fatura f : tableFaturas.getItems()) {
                    BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
                    BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
                    BigDecimal aberto = total.subtract(recebido);
                    String linha = String.join(",",
                            safe(f.getNumero()),
                            safe(f.getCliente() != null ? f.getCliente().getNome() : ""),
                            safeDate(f.getDataEmissao()),
                            safeDate(f.getDataVencimento()),
                            total.toString(),
                            recebido.toString(),
                            aberto.toString(),
                            f.getStatus() != null ? f.getStatus().name() : ""
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

    private void exportExtratoClientesPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Extrato por Cliente (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("extrato_clientes_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("periodoInicio", dpInicio.getValue());
                params.put("periodoFim", dpFim.getValue());
                jasperService.gerarExtratoClientesPdf(faturasDataFiltered(), params, file);
                AlertUtils.showInfoAlert("Exportação", "PDF exportado com sucesso: \n" + file.getAbsolutePath());
            } catch (Exception ex) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar PDF: " + ex.getMessage());
            }
        }
    }

    private java.util.List<Fatura> faturasDataFiltered() {
        java.util.List<Fatura> list = new java.util.ArrayList<>();
        for (Fatura f : tableFaturas.getItems()) list.add(f);
        return list;
    }

    private void showRelatoriosModal() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        Label title = new Label("Relatórios - Contas a Receber");
        title.getStyleClass().add(Styles.TITLE_4);
        Button btnCsv = new Button("Exportar lista (CSV)", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnCsv.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnCsv.setOnAction(e -> exportCsvReceber());
        Button btnPdf = new Button("Extrato por Cliente (PDF)", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnPdf.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnPdf.setOnAction(e -> exportExtratoClientesPdf());
        Button btnExtratoClientes = new Button("Extrato por Cliente (CSV)", IconUtils.icon(Feather.USERS, IconUtils.SIZE_SMALL));
        btnExtratoClientes.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExtratoClientes.setOnAction(e -> exportExtratoClientesCsv());
        root.getChildren().addAll(title, new Separator(), btnCsv, btnExtratoClientes, btnPdf);
        modalService.create()
            .title("Relatórios - Contas a Receber")
            .content(root)
            .autoSize()
            .withCancelButton("Fechar")
            .buildAndShow();
    }

    private void exportExtratoClientesCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Extrato por Cliente");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("extrato_clientes_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.write("Cliente,Total,Recebido,Aberto\n");
                java.util.Map<String, java.math.BigDecimal[]> map = new java.util.HashMap<>();
                for (Fatura f : tableFaturas.getItems()) {
                    String cliente = f.getCliente() != null ? safe(f.getCliente().getNome()) : "Sem Cliente";
                    BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
                    BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
                    BigDecimal aberto = total.subtract(recebido);
                    java.math.BigDecimal[] acc = map.getOrDefault(cliente, new java.math.BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    acc[0] = acc[0].add(total);
                    acc[1] = acc[1].add(recebido);
                    acc[2] = acc[2].add(aberto);
                    map.put(cliente, acc);
                }
                for (java.util.Map.Entry<String, java.math.BigDecimal[]> e : map.entrySet()) {
                    java.math.BigDecimal[] v = e.getValue();
                    w.write(String.join(",", e.getKey(), v[0].toString(), v[1].toString(), v[2].toString()) + "\n");
                }
                AlertUtils.showInfoAlert("Exportação", "Extrato por cliente exportado: \n" + file.getAbsolutePath());
            } catch (IOException ex) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar CSV: " + ex.getMessage());
            }
        }
    }
}
