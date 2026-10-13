package ao.allon.kubata.financeiro.view;

import ao.allon.kubata.financeiro.service.RelatorioFinanceiroService;
import ao.allon.kubata.financeiro.service.RelatorioFinanceiroService.LinhaMovimento;
import ao.allon.kubata.financeiro.service.RelatorioFinanceiroService.Relatorio;
import ao.allon.kubata.financeiro.ui.FinanceiroUiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Relatório consolidado de entradas e saídas de caixa e bancos, com exportação CSV. */
@Component
public class RelatoriosFinanceirosView extends BorderPane {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_FICHEIRO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RelatorioFinanceiroService relatorioService;
    private final DatePicker inicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker fim = new DatePicker(LocalDate.now());
    private final ObservableList<LinhaMovimento> dados = FXCollections.observableArrayList();
    private final TableView<LinhaMovimento> tabela = new TableView<>(dados);
    private final Label entradas = new Label("—");
    private final Label saidas = new Label("—");
    private final Label liquido = new Label("—");
    private final Label resumo = new Label("Seleccione o período e aplique os filtros.");
    private Relatorio ultimoRelatorio;

    public RelatoriosFinanceirosView(RelatorioFinanceiroService relatorioService) {
        this.relatorioService = relatorioService;
        buildUi();
        aplicarFiltros();
    }

    private void buildUi() {
        Button actualizar = FinanceiroUiSupport.secondary("Actualizar");
        actualizar.setOnAction(event -> aplicarFiltros());
        Button exportar = FinanceiroUiSupport.primary("Exportar CSV");
        exportar.setOnAction(event -> exportarCsv());

        VBox cabecalho = FinanceiroUiSupport.page(
                "Relatórios Financeiros",
                "Consolidação de movimentos dos caixas e contas bancárias num intervalo de datas.",
                FinanceiroUiSupport.actions(actualizar, exportar));

        Button aplicar = FinanceiroUiSupport.primary("Aplicar filtros");
        aplicar.setOnAction(event -> aplicarFiltros());
        Button mesActual = FinanceiroUiSupport.secondary("Mês actual");
        mesActual.setOnAction(event -> {
            inicio.setValue(LocalDate.now().withDayOfMonth(1));
            fim.setValue(LocalDate.now());
            aplicarFiltros();
        });
        Button ultimos30 = FinanceiroUiSupport.secondary("Últimos 30 dias");
        ultimos30.setOnAction(event -> {
            inicio.setValue(LocalDate.now().minusDays(29));
            fim.setValue(LocalDate.now());
            aplicarFiltros();
        });
        inicio.setEditable(false);
        fim.setEditable(false);
        inicio.setPrefWidth(150);
        fim.setPrefWidth(150);
        HBox filtros = new HBox(10,
                new Label("De"), inicio,
                new Label("Até"), fim,
                aplicar, mesActual, ultimos30);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.setPadding(new Insets(12, 22, 8, 22));
        filtros.setStyle("-fx-background-color: #F5F7F6;");

        HBox cartoes = new HBox(12,
                FinanceiroUiSupport.metric("TOTAL DE ENTRADAS", "—", "Créditos no período", FinanceiroUiSupport.GREEN),
                FinanceiroUiSupport.metric("TOTAL DE SAÍDAS", "—", "Débitos no período", "#B42318"),
                FinanceiroUiSupport.metric("SALDO LÍQUIDO DO PERÍODO", "—", "Entradas menos saídas", "#2563EB"));
        substituirValorCartao(cartoes, 0, entradas);
        substituirValorCartao(cartoes, 1, saidas);
        substituirValorCartao(cartoes, 2, liquido);
        cartoes.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        cartoes.setPadding(new Insets(10, 22, 14, 22));
        cartoes.setAlignment(Pos.CENTER_LEFT);

        VBox topo = new VBox(cabecalho, filtros, cartoes);
        topo.setStyle("-fx-background-color: #F5F7F6;");
        setTop(topo);

        configurarTabela();
        resumo.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        HBox rodape = new HBox(resumo);
        rodape.setPadding(new Insets(10, 18, 12, 18));
        rodape.setAlignment(Pos.CENTER_LEFT);
        rodape.setStyle("-fx-background-color: white; -fx-border-color: #E3E9E5 transparent transparent transparent;");

        VBox conteudo = new VBox(0, tabela, rodape);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        conteudo.setPadding(new Insets(0, 22, 18, 22));
        setCenter(conteudo);
        setStyle("-fx-background-color: #F5F7F6;");
    }

    private void substituirValorCartao(HBox cartoes, int indice, Label valor) {
        VBox cartao = (VBox) cartoes.getChildren().get(indice);
        cartao.getChildren().set(1, valor);
        valor.setWrapText(true);
        valor.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #17352B;");
    }

    private void configurarTabela() {
        FinanceiroUiSupport.styleTable(tabela);
        tabela.setMinHeight(260);

        TableColumn<LinhaMovimento, String> data = new TableColumn<>("Data e hora");
        data.setCellValueFactory(cell -> new SimpleStringProperty(formatarData(cell.getValue().data())));
        data.setPrefWidth(135);

        TableColumn<LinhaMovimento, String> tipo = new TableColumn<>("Natureza");
        tipo.setCellValueFactory(cell -> new SimpleStringProperty(seguro(cell.getValue().natureza())));
        tipo.setPrefWidth(105);

        TableColumn<LinhaMovimento, String> origem = new TableColumn<>("Origem");
        origem.setCellValueFactory(cell -> new SimpleStringProperty(seguro(cell.getValue().origem())));
        origem.setPrefWidth(195);

        TableColumn<LinhaMovimento, String> descricao = new TableColumn<>("Descrição");
        descricao.setCellValueFactory(cell -> new SimpleStringProperty(seguro(cell.getValue().descricao())));

        TableColumn<LinhaMovimento, String> referencia = new TableColumn<>("Documento");
        referencia.setCellValueFactory(cell -> new SimpleStringProperty(seguro(cell.getValue().referencia())));
        referencia.setPrefWidth(130);

        TableColumn<LinhaMovimento, String> valor = new TableColumn<>("Valor");
        valor.setCellValueFactory(cell -> new SimpleStringProperty(
                ("Saída".equals(cell.getValue().natureza()) || "Débito".equals(cell.getValue().natureza())
                        ? "− " : "+ ") + FinanceiroUiSupport.money(cell.getValue().valor())));
        valor.setPrefWidth(155);

        TableColumn<LinhaMovimento, String> saldo = new TableColumn<>("Saldo após movimento");
        saldo.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().saldoApos())));
        saldo.setPrefWidth(170);

        tabela.getColumns().setAll(data, tipo, origem, descricao, referencia, valor, saldo);
    }

    private void aplicarFiltros() {
        try {
            Relatorio relatorio = relatorioService.gerar(inicio.getValue(), fim.getValue());
            ultimoRelatorio = relatorio;
            dados.setAll(relatorio.movimentos());
            entradas.setText(FinanceiroUiSupport.money(relatorio.totalEntradas()));
            saidas.setText(FinanceiroUiSupport.money(relatorio.totalSaidas()));
            liquido.setText(FinanceiroUiSupport.money(relatorio.saldoLiquido()));
            resumo.setText(relatorio.movimentos().size() + " movimento(s) · "
                    + relatorio.caixasConsultados() + " caixa(s) activo(s) · "
                    + relatorio.contasBancariasConsultadas() + " conta(s) bancária(s) activa(s) · "
                    + relatorio.inicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " a " + relatorio.fim().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível gerar o relatório financeiro", ex);
        }
    }

    private void exportarCsv() {
        if (ultimoRelatorio == null) {
            FinanceiroUiSupport.info("Relatório indisponível", "Gere primeiro o relatório para poder exportá-lo.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar relatório financeiro");
        chooser.setInitialFileName("relatorio-financeiro-"
                + ultimoRelatorio.inicio().format(DATA_FICHEIRO) + "-a-"
                + ultimoRelatorio.fim().format(DATA_FICHEIRO) + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiro CSV (*.csv)", "*.csv"));
        java.io.File destino = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (destino == null) {
            return;
        }

        try {
            List<String> linhas = new ArrayList<>();
            linhas.add(csv("Data e hora", "Natureza", "Origem", "Descrição", "Documento", "Valor (Kz)", "Saldo após (Kz)"));
            for (LinhaMovimento linha : ultimoRelatorio.movimentos()) {
                linhas.add(csv(formatarData(linha.data()), linha.natureza(), linha.origem(),
                        linha.descricao(), linha.referencia(), decimalCsv(linha.valor()), decimalCsv(linha.saldoApos())));
            }
            linhas.add("");
            linhas.add(csv("Total de entradas (Kz)", decimalCsv(ultimoRelatorio.totalEntradas())));
            linhas.add(csv("Total de saídas (Kz)", decimalCsv(ultimoRelatorio.totalSaidas())));
            linhas.add(csv("Saldo líquido do período (Kz)", decimalCsv(ultimoRelatorio.saldoLiquido())));
            // BOM UTF-8 melhora a abertura directa do ficheiro no Excel em Windows.
            Files.writeString(destino.toPath(), "\uFEFF" + String.join("\r\n", linhas) + "\r\n",
                    StandardCharsets.UTF_8);
            FinanceiroUiSupport.info("Exportação concluída", "Relatório exportado para:\n" + destino.getAbsolutePath());
        } catch (IOException ex) {
            FinanceiroUiSupport.error("Não foi possível exportar o relatório", ex);
        }
    }

    private String csv(String... valores) {
        List<String> campos = new ArrayList<>();
        for (String valor : valores) {
            String campo = valor == null ? "" : valor;
            campos.add("\"" + campo.replace("\"", "\"\"") + "\"");
        }
        return String.join(";", campos);
    }

    private String decimalCsv(BigDecimal valor) {
        return valor == null ? "0,00" : valor.setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');
    }

    private String formatarData(LocalDateTime data) {
        return data == null ? "—" : DATA_HORA.format(data);
    }

    private String seguro(String valor) {
        return valor == null ? "" : valor;
    }
}
