package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.ReciboAlocacao;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ReciboService;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExtratoClienteView extends BorderPane {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClienteService clienteService;
    private final FaturaService faturaService;
    private final ReciboService reciboService;

    private final ComboBox<Cliente> cbCliente = new ComboBox<>();
    private final DatePicker dpInicio = new DatePicker(LocalDate.now().minusMonths(6));
    private final DatePicker dpFim = new DatePicker(LocalDate.now());
    private final Label lblSaldo = new Label("Saldo: Kz 0,00");
    private final ObservableList<Linha> linhas = FXCollections.observableArrayList();
    private final TableView<Linha> tabela = new TableView<>(linhas);

    public ExtratoClienteView(ClienteService clienteService, FaturaService faturaService, ReciboService reciboService) {
        this.clienteService = clienteService;
        this.faturaService = faturaService;
        this.reciboService = reciboService;
        getStyleClass().add("extrato-cliente-view");
        initializeUI();
    }

    private void initializeUI() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Extrato de Cliente");
        title.getStyleClass().add(Styles.TITLE_2);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, lblSaldo);
        lblSaldo.getStyleClass().add(Styles.TITLE_4);
        root.getChildren().add(header);

        cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setPromptText("Selecionar cliente");
        dpInicio.setPromptText("Data inicial");
        dpFim.setPromptText("Data final");
        Button btnGerar = new Button("Gerar");
        btnGerar.getStyleClass().add(Styles.ACCENT);
        btnGerar.setOnAction(e -> gerar());
        HBox filters = new HBox(10,
                new Label("Cliente:"), cbCliente,
                new Label("De:"), dpInicio,
                new Label("Até:"), dpFim,
                btnGerar);
        filters.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(filters);

        TableColumn<Linha, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().data != null ? cd.getValue().data.format(DF) : "-"));
        TableColumn<Linha, String> cDoc = new TableColumn<>("Documento");
        cDoc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().documento));
        TableColumn<Linha, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().descricao));
        TableColumn<Linha, BigDecimal> cDeb = new TableColumn<>("Débito");
        cDeb.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().debito));
        TableColumn<Linha, BigDecimal> cCred = new TableColumn<>("Crédito");
        cCred.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().credito));
        TableColumn<Linha, BigDecimal> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().saldo));
        tabela.getColumns().addAll(cData, cDoc, cDesc, cDeb, cCred, cSaldo);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        root.getChildren().add(tabela);

        setCenter(root);
    }

    private void gerar() {
        Cliente cliente = cbCliente.getValue();
        if (cliente == null) return;
        LocalDate inicio = dpInicio.getValue() != null ? dpInicio.getValue() : LocalDate.now().minusYears(2);
        LocalDate fim = dpFim.getValue() != null ? dpFim.getValue() : LocalDate.now();

        List<Linha> tmp = new ArrayList<>();
        for (Fatura f : faturaService.findByCliente(cliente.getId())) {
            if (f.getDataEmissao() == null) continue;
            if (f.getDataEmissao().isBefore(inicio) || f.getDataEmissao().isAfter(fim)) continue;
            BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            if (f.getStatus() == StatusFatura.CANCELADA) continue;
            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().name() : "FT";
            boolean creditoNeg = f.getTipoDocumento() != null
                    && (f.getTipoDocumento().name().contains("NOTA_CREDITO"));
            BigDecimal valor = creditoNeg ? total.negate() : total;
            tmp.add(new Linha(f.getDataEmissao(), f.getNumero(), tipo + " " + safeStr(f.getNumero()), valor, BigDecimal.ZERO));
        }
        List<Recibo> recibos = reciboService.findByPeriodo(inicio, fim);
        for (Recibo r : recibos) {
            List<ReciboAlocacao> alocs = reciboService.findAlocacoesByRecibo(r.getId());
            if (alocs.isEmpty()) {
                if (r.getFatura() != null && r.getFatura().getCliente() != null
                        && cliente.getId().equals(r.getFatura().getCliente().getId())) {
                    BigDecimal v = r.getValor() != null ? r.getValor() : BigDecimal.ZERO;
                    tmp.add(new Linha(r.getDataRecebimento(), r.getNumero(),
                            "Recibo " + safeStr(r.getNumero()), BigDecimal.ZERO, v));
                }
            } else {
                for (ReciboAlocacao a : alocs) {
                    if (a.getFatura() != null && a.getFatura().getCliente() != null
                            && cliente.getId().equals(a.getFatura().getCliente().getId())) {
                        BigDecimal valor = a.getValor() != null ? a.getValor() : BigDecimal.ZERO;
                        tmp.add(new Linha(r.getDataRecebimento(), r.getNumero(),
                                "Recibo " + safeStr(r.getNumero()) + " (aloc. " + safeStr(a.getFatura().getNumero()) + ")",
                                BigDecimal.ZERO, valor));
                    }
                }
            }
        }

        tmp.sort(Comparator.comparing((Linha l) -> l.data).thenComparing(l -> l.documento));
        BigDecimal saldo = BigDecimal.ZERO;
        linhas.clear();
        for (Linha l : tmp) {
            saldo = saldo.add(l.debito).subtract(l.credito);
            l.saldo = saldo;
            linhas.add(l);
        }
        lblSaldo.setText("Saldo: Kz " + String.format("%,.2f", saldo));
    }

    private static String safeStr(String s) { return s == null ? "" : s; }

    private static class Linha {
        LocalDate data;
        String documento;
        String descricao;
        BigDecimal debito;
        BigDecimal credito;
        BigDecimal saldo;
        Linha(LocalDate data, String doc, String desc, BigDecimal debito, BigDecimal credito) {
            this.data = data;
            this.documento = doc;
            this.descricao = desc;
            this.debito = debito;
            this.credito = credito;
        }
    }
}
