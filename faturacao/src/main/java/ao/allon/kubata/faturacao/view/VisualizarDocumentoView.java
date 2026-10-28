package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.EstadoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.service.DocumentoEstadoService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.report.InvoiceItemData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportService;
import ao.allon.kubata.faturacao.ui.loading.LoadingScreen;
import ao.allon.kubata.faturacao.ui.loading.LoadingService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JasperPrint;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Leitura de um documento de faturação: cabeçalho, itens, totais, bloco fiscal
 * (hash-cadeia, JWS, AGT) e recibos, com ações guardadas por estado.
 */
public class VisualizarDocumentoView extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long faturaId;
    private final FaturaService faturaService;
    private final ReciboService reciboService;
    private final DocumentoEstadoService documentoEstadoService;
    private final JasperReportService jasperReportService;
    private final InvoiceReportService invoiceReportService;
    private final ModalService modalService;
    private final LoadingService loadingService;
    private final SessionManager sessionManager;
    private final Consumer<Long> onChange;

    private Label lblTitulo;
    private Label lblSubtitulo;
    private VBox corpo;

    public VisualizarDocumentoView(Long faturaId,
                                   FaturaService faturaService,
                                   ReciboService reciboService,
                                   DocumentoEstadoService documentoEstadoService,
                                   JasperReportService jasperReportService,
                                   InvoiceReportService invoiceReportService,
                                   ModalService modalService,
                                   LoadingService loadingService,
                                   SessionManager sessionManager,
                                   Consumer<Long> onChange) {
        this.faturaId = faturaId;
        this.faturaService = faturaService;
        this.reciboService = reciboService;
        this.documentoEstadoService = documentoEstadoService;
        this.jasperReportService = jasperReportService;
        this.invoiceReportService = invoiceReportService;
        this.modalService = modalService;
        this.loadingService = loadingService;
        this.sessionManager = sessionManager;
        this.onChange = onChange;

        getStyleClass().add("visualizar-documento-view");
        initializeUI();
        recarregar();
    }

    private void initializeUI() {
        setPadding(new Insets(0));

        corpo = new VBox(20);
        corpo.setPadding(new Insets(20, 30, 30, 30));

        setTop(criarCabecalho());
        setCenter(corpo);
    }

    private Node criarCabecalho() {
        HBox header = new HBox(15);
        header.setPadding(new Insets(20, 30, 10, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: transparent; -fx-background: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        Button btnFechar = new Button("", IconUtils.icon(Feather.ARROW_LEFT, IconUtils.SIZE_SMALL));
        btnFechar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnFechar.setOnAction(e -> fechar());

        VBox titleBox = new VBox(5);
        lblTitulo = new Label("Documento");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        lblSubtitulo = new Label();
        lblSubtitulo.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(lblTitulo, lblSubtitulo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnImprimir = new Button("Imprimir A4", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnImprimir.getStyleClass().addAll(Styles.SUCCESS);
        btnImprimir.setOnAction(e -> imprimirA4());

        Button btnTermico = new Button("Térmico", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnTermico.setOnAction(e -> imprimirTermico());

        Button btnRecibo = new Button("Registar Recibo", IconUtils.icon(Feather.DOLLAR_SIGN, IconUtils.SIZE_SMALL));
        btnRecibo.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRecibo.setOnAction(e -> registarRecibo());

        MenuButton btnMais = new MenuButton("Mais", IconUtils.icon(Feather.MORE_VERTICAL, IconUtils.SIZE_SMALL));
        btnMais.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        MenuItem miDuplicado = new MenuItem("Gerar Duplicado", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        miDuplicado.setOnAction(e -> gerarCopia(EstadoDocumento.DUPLICADO));
        MenuItem miSegundaVia = new MenuItem("Gerar 2ª Via", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        miSegundaVia.setOnAction(e -> gerarCopia(EstadoDocumento.SEGUNDA_VIA));
        MenuItem miAnular = new MenuItem("Anular Documento", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        miAnular.setOnAction(e -> anular());
        MenuItem miNcParcial = new MenuItem("NC Parcial...", IconUtils.icon(Feather.MINUS_CIRCLE, IconUtils.SIZE_SMALL));
        miNcParcial.setOnAction(e -> abrirNcParcial());
        btnMais.getItems().addAll(miDuplicado, miSegundaVia, new SeparatorMenuItem(), miNcParcial, miAnular);

        header.getChildren().addAll(btnFechar, titleBox, spacer, btnImprimir, btnTermico, btnRecibo, btnMais);
        return header;
    }

    private void recarregar() {
        new Thread(() -> {
            try {
                Fatura f = carregar();
                List<Recibo> recibos = reciboService.findByFatura(faturaId);
                BigDecimal recebido = reciboService.totalRecebidoFatura(faturaId);
                Platform.runLater(() -> renderizar(f, recibos, recebido));
            } catch (Exception ex) {
                Platform.runLater(() ->
                        AlertUtils.showExceptionAlert("Erro", "Não foi possível carregar o documento.", ex));
            }
        }).start();
    }

    private void renderizar(Fatura f, List<Recibo> recibos, BigDecimal recebido) {
        String serie = f.getSerie() != null ? f.getSerie().getDesignacao() + " " : "";
        lblTitulo.setText(serie + f.getNumero());
        lblSubtitulo.setText((f.getTipoDocumento() != null ? f.getTipoDocumento().name() : "DOCUMENTO") + " · "
                + (f.getDataEmissao() != null ? f.getDataEmissao().format(DATE_FMT) : "-"));

        Card cartaoCliente = cardCliente(f);
        Card cartaoDados = cardDados(f);
        Card cartaoItens = cardItens(f);
        Card cartaoTotais = cardTotais(f, recebido);
        Card cartaoFiscal = cardFiscal(f);
        Card cartaoRecibos = cardRecibos(recibos);

        HBox colunaEsquerda = new HBox(20, empilhar(cartaoCliente, cartaoDados));
        HBox.setHgrow(colunaEsquerda, Priority.ALWAYS);
        colunaEsquerda.setMaxWidth(620);

        HBox topo = new HBox(20, colunaEsquerda, cartaoFiscal);
        topo.setAlignment(Pos.TOP_LEFT);

        corpo.getChildren().setAll(topo, cartaoItens,
                empilhar(cartaoTotais, cartaoRecibos));
    }

    private VBox empilhar(Node... nos) {
        VBox box = new VBox(20, nos);
        for (Node n : nos) {
            if (n instanceof Card c) c.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private Card cardCliente(Fatura f) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(15));
        if (f.getCliente() != null) {
            Label nome = new Label(f.getCliente().getNome());
            nome.setFont(Font.font("System", FontWeight.BOLD, 14));
            content.getChildren().add(nome);
            content.getChildren().add(linha("NIF", f.getCliente().getNif()));
            content.getChildren().add(linha("Telefone", f.getCliente().getTelefone()));
            content.getChildren().add(linha("Endereço", f.getCliente().getEndereco()));
        } else {
            content.getChildren().add(new Label("Venda a dinheiro (Consumidor Final)"));
        }
        return card("Cliente", content);
    }

    private Card cardDados(Fatura f) {
        VBox content = new VBox(6);
        content.setPadding(new Insets(15));
        content.getChildren().add(linha("Data de emissão", f.getDataEmissao() != null ? f.getDataEmissao().format(DATE_FMT) : "-"));
        content.getChildren().add(linha("Vencimento", f.getDataVencimento() != null ? f.getDataVencimento().format(DATE_FMT) : "-"));
        content.getChildren().add(linha("Condição de pagamento",
                f.getCondicaoPagamento() != null ? f.getCondicaoPagamento().getCodigo() + " - " + f.getCondicaoPagamento().getDescricao() : "-"));
        content.getChildren().add(linha("Estado", f.getStatus() != null ? f.getStatus().name() : "-"));
        content.getChildren().add(linha("Operador", f.getUsuario() != null ? f.getUsuario().getNome() : "-"));
        if (f.getObservacoes() != null && !f.getObservacoes().isBlank()) {
            content.getChildren().add(linha("Observações", f.getObservacoes()));
        }
        return card("Dados do documento", content);
    }

    private Card cardItens(Fatura f) {
        TableView<ItemFatura> table = new TableView<>(FXCollections.observableArrayList(f.getItens()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Sem itens"));
        table.getColumns().addAll(
                coluna("Descrição", "descricao", 260),
                coluna("Produto", "produto.codigo", 110),
                colunaDecimal("Qtd", "quantidadeDecimal", 80),
                colunaDecimal("Preço", "precoUnitario", 100),
                colunaDecimal("IVA %", "percentualIva", 70),
                colunaDecimal("Subtotal", "subtotal", 110),
                colunaDecimal("IVA", "valorIva", 110),
                colunaDecimal("Total", "total", 120)
        );

        VBox content = new VBox(10, table);
        content.setPadding(new Insets(15));
        return card("Itens (" + f.getItens().size() + ")", content);
    }

    private Card cardTotais(Fatura f, BigDecimal recebido) {
        BigDecimal emAberto = f.getTotal().subtract(recebido);
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(6);
        grid.setPadding(new Insets(15));
        int row = 0;
        grid.add(rotuloTotais("Subtotal"), 0, row);
        grid.add(valorTotais(moeda(f.getSubtotal())), 1, row++);
        grid.add(rotuloTotais("IVA"), 0, row);
        grid.add(valorTotais(moeda(f.getIva())), 1, row++);
        if (f.getTotalRetencao() != null && f.getTotalRetencao().compareTo(BigDecimal.ZERO) > 0) {
            grid.add(rotuloTotais("Retenção (0,6%)"), 0, row);
            grid.add(valorTotais(moeda(f.getTotalRetencao())), 1, row++);
        }
        grid.add(rotuloTotais("Total"), 0, row);
        Label lblTotal = valorTotais(moeda(f.getTotal()));
        lblTotal.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(lblTotal, 1, row++);
        grid.add(rotuloTotais("Recebido"), 0, row);
        Label lblRecebido = valorTotais(moeda(recebido));
        lblRecebido.setStyle("-fx-text-fill: #217346;");
        grid.add(lblRecebido, 1, row++);
        grid.add(rotuloTotais("Em aberto"), 0, row);
        Label lblAberto = valorTotais(moeda(emAberto));
        if (emAberto.compareTo(BigDecimal.ZERO) > 0) {
            lblAberto.setStyle("-fx-text-fill: #d18616;");
        }
        grid.add(lblAberto, 1, row);

        return card("Totais", grid);
    }

    private Card cardFiscal(Fatura f) {
        VBox content = new VBox(8);
        content.setPadding(new Insets(15));
        content.setMaxWidth(460);

        content.getChildren().add(fiscalLinha("Document Status",
                f.getDocumentStatus() != null ? f.getDocumentStatus().getDescricao() : "-"));
        content.getChildren().add(fiscalLinha("Estado do documento",
                f.getEstadoDocumento() != null ? f.getEstadoDocumento().getDescricao() : "-"));
        if (documentoEstadoService.requerIndicacaoVisual(f)) {
            content.getChildren().add(fiscalLinha("Indicação visual",
                    f.getEstadoDocumento().getTextoDocumento()));
        }
        if (f.getRejectedDocumentNo() != null && !f.getRejectedDocumentNo().isBlank()) {
            content.getChildren().add(fiscalLinha("Documento corrigido", f.getRejectedDocumentNo()));
        }
        content.getChildren().add(fiscalLinha("Sigilo", f.getHash()));
        content.getChildren().add(fiscalLinha("Hash anterior", f.getHashAnterior()));
        content.getChildren().add(fiscalLinha("Hash Control", f.getHashControl()));
        content.getChildren().add(fiscalLinha("Assinatura JWS", f.getJwsDocumentSignature()));
        content.getChildren().add(fiscalLinha("AGT — submissão", f.getAgtSubmissionStatus()));
        content.getChildren().add(fiscalLinha("AGT — data",
                f.getAgtSubmissionDate() != null ? f.getAgtSubmissionDate().format(DATE_TIME_FMT) : "-"));
        content.getChildren().add(fiscalLinha("AGT — código de validação", f.getAgtValidationCode()));

        return card("Bloco fiscal", content);
    }

    private Card cardRecibos(List<Recibo> recibos) {
        TableView<Recibo> table = new TableView<>(FXCollections.observableArrayList(recibos));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Sem recibos registados"));
        table.getColumns().addAll(
                coluna("Recibo", "numero", 140),
                colunaData("Data", "dataRecebimento", 110),
                coluna("Método", "metodoPagamento", 110),
                coluna("Referência", "referencia", 160),
                colunaDecimal("Valor", "valor", 120)
        );

        VBox content = new VBox(10, table);
        content.setPadding(new Insets(15));
        return card("Recibos", content);
    }

    // ── Ações ────────────────────────────────────────────────────────────

    private void imprimirA4() {
        Fatura f = atual();
        if (f == null) return;
        if (f.getStatus() != StatusFatura.EMITIDA && f.getStatus() != StatusFatura.PAGA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas documentos emitidos podem ser impressos em A4.");
            return;
        }
        try {
            JasperPrint jasperPrint = jasperReportService.prepararFatura(carregar());
            jasperReportService.showReport(jasperPrint);
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro ao Imprimir", "Não foi possível gerar o relatório A4.", ex);
        }
    }

    private void imprimirTermico() {
        Fatura f = atual();
        if (f == null) return;
        if (f.getStatus() != StatusFatura.EMITIDA && f.getStatus() != StatusFatura.PAGA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas documentos emitidos podem ser impressos.");
            return;
        }
        new Thread(() -> {
            try {
                Fatura completa = carregar();
                InvoiceReportData data = new InvoiceReportData(
                        completa.getNumero(),
                        completa.getDataEmissao() != null ? completa.getDataEmissao().atStartOfDay() : LocalDateTime.now(),
                        completa.getSystemEntryDate() != null ? completa.getSystemEntryDate() : LocalDateTime.now(),
                        completa.getUsuario() != null ? completa.getUsuario().getNome() : "",
                        completa.getCliente() != null ? completa.getCliente().getNome() : "Consumidor Final",
                        completa.getCliente() != null && completa.getCliente().getNif() != null ? completa.getCliente().getNif() : "999999999",
                        completa.getSubtotal() != null ? completa.getSubtotal() : BigDecimal.ZERO,
                        completa.getIva() != null ? completa.getIva() : BigDecimal.ZERO,
                        completa.getTotal() != null ? completa.getTotal() : BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        completa.getTotalRetencao() != null ? completa.getTotalRetencao() : BigDecimal.ZERO,
                        completa.getHash(),
                        completa.getSerie() != null ? completa.getSerie().getHashAnterior() : null
                );
                List<InvoiceItemData> items = completa.getItens().stream()
                        .map(it -> new InvoiceItemData(
                                it.getProduto() != null ? it.getProduto().getCodigo() : "",
                                it.getDescricao(),
                                it.getQuantidadeDecimal() != null ? it.getQuantidadeDecimal()
                                        : BigDecimal.valueOf(it.getQuantidade() != null ? it.getQuantidade() : 0),
                                "un",
                                it.getPrecoUnitario(),
                                it.getPercentualIva(),
                                it.getTotal(),
                                it.getCodigoIsencao(),
                                it.getMotivoIsencao()))
                        .toList();
                JasperPrint jp = invoiceReportService.createInvoiceThermalPrint(data, items);
                try {
                    invoiceReportService.printJasper(jp, null);
                } catch (Exception npf) {
                    Platform.runLater(() -> modalService.create()
                            .title("Impressora não encontrada")
                            .content(new Label("Nenhuma impressora encontrada. Deseja abrir o Visualizador de Relatório?"))
                            .autoSize()
                            .withConfirmButton("Abrir Visualizador", () -> {
                                jasperReportService.showReport(jp);
                                return true;
                            })
                            .withCancelButton("Cancelar")
                            .buildAndShow());
                }
            } catch (Exception ex) {
                Platform.runLater(() ->
                        AlertUtils.showExceptionAlert("Erro", "Não foi possível imprimir o talão.", ex));
            }
        }).start();
    }

    private void registarRecibo() {
        Fatura f = atual();
        if (f == null) return;
        if (f.getStatus() == StatusFatura.CANCELADA || f.getStatus() == StatusFatura.RASCUNHO) {
            AlertUtils.showWarningAlert("Aviso", "Apenas documentos emitidos aceitam recibos.");
            return;
        }

        BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
        BigDecimal emAberto = f.getTotal().subtract(recebido);
        if (emAberto.compareTo(BigDecimal.ZERO) <= 0) {
            AlertUtils.showInfoAlert("Informação", "Esta fatura já se encontra liquidada.");
            return;
        }

        TextField valorField = new TextField(emAberto.toPlainString());
        TextField obsField = new TextField();
        VBox content = new VBox(8,
                new Label("Saldo em aberto: " + moeda(emAberto)),
                new Label("Valor recebido"), valorField,
                new Label("Observações"), obsField);
        content.setPadding(new Insets(10));

        modalService.create()
                .title("Registar Recibo")
                .content(content)
                .autoSize()
                .withConfirmButton("Registar", () -> {
                    BigDecimal valor;
                    try {
                        valor = new BigDecimal(valorField.getText().replace(",", ".").trim());
                    } catch (NumberFormatException ex) {
                        AlertUtils.showWarningAlert("Valor inválido", "Introduza um valor numérico válido.");
                        return false;
                    }
                    if (valor.compareTo(BigDecimal.ZERO) <= 0 || valor.compareTo(emAberto) > 0) {
                        AlertUtils.showWarningAlert("Valor inválido",
                                "O valor deve estar entre 0 e o saldo em aberto (" + moeda(emAberto) + ").");
                        return false;
                    }
                    new Thread(() -> {
                        String loadingId = loadingService.showLoading("A registar recibo...", LoadingScreen.LoadingStyle.SPINNER);
                        try {
                            reciboService.registrar(f.getId(), valor, "Recebimento", obsField.getText());
                            Platform.runLater(() -> {
                                recarregar();
                                notificarAlteracao();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() ->
                                    AlertUtils.showExceptionAlert("Erro", "Não foi possível registar o recibo.", ex));
                        } finally {
                            Platform.runLater(() -> loadingService.hideLoading(loadingId));
                        }
                    }).start();
                    return true;
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void gerarCopia(EstadoDocumento tipoCopia) {
        Fatura f = atual();
        if (f == null) return;
        if (f.getEstadoDocumento() == tipoCopia) {
            AlertUtils.showWarningAlert("Aviso",
                    "Este documento já é " + tipoCopia.getTextoDocumento().toLowerCase() + ".");
            return;
        }

        TextField motivoField = new TextField();
        VBox content = new VBox(8, new Label("Motivo"), motivoField);
        content.setPadding(new Insets(10));

        modalService.create()
                .title(tipoCopia == EstadoDocumento.DUPLICADO ? "Gerar Duplicado" : "Gerar 2ª Via")
                .content(content)
                .autoSize()
                .withConfirmButton("Gerar", () -> {
                    new Thread(() -> {
                        String loadingId = loadingService.showLoading("A gerar cópia...", LoadingScreen.LoadingStyle.SPINNER);
                        try {
                            if (tipoCopia == EstadoDocumento.DUPLICADO) {
                                documentoEstadoService.gerarDuplicado(f, sessionManager.getCurrentUserId(), motivoField.getText());
                            } else {
                                documentoEstadoService.gerarSegundaVia(f, sessionManager.getCurrentUserId(), motivoField.getText());
                            }
                            Platform.runLater(() -> {
                                recarregar();
                                notificarAlteracao();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() ->
                                    AlertUtils.showExceptionAlert("Erro", "Não foi possível gerar a cópia.", ex));
                        } finally {
                            Platform.runLater(() -> loadingService.hideLoading(loadingId));
                        }
                    }).start();
                    return true;
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void abrirNcParcial() {
        Fatura f = atual();
        if (f == null || f.getItens() == null || f.getItens().isEmpty()) {
            AlertUtils.showInfoAlert("Sem itens", "Documento não tem itens para anular.");
            return;
        }
        javafx.collections.ObservableList<NcLinhaUI> rows = javafx.collections.FXCollections.observableArrayList();
        for (ao.allon.kubata.faturacao.domain.ItemFatura it : f.getItens()) {
            java.math.BigDecimal q = ao.allon.kubata.faturacao.util.Qty.qtyOrInt(it.getQuantidadeDecimal(), it.getQuantidade());
            rows.add(new NcLinhaUI(it.getId(), it.getDescricao(), q));
        }
        javafx.scene.control.TableView<NcLinhaUI> tv = new javafx.scene.control.TableView<>(rows);
        tv.setEditable(true);
        javafx.scene.control.TableColumn<NcLinhaUI, String> cDesc = new javafx.scene.control.TableColumn<>("Descrição");
        cDesc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().descricao));
        javafx.scene.control.TableColumn<NcLinhaUI, String> cQtd = new javafx.scene.control.TableColumn<>("Qtd");
        cQtd.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().qtd.get()));
        cQtd.setEditable(true);
        cQtd.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.TextField field = new javafx.scene.control.TextField();
            {
                field.setOnAction(e -> commit());
                field.focusedProperty().addListener((o, ov, nv) -> { if (!nv) commit(); });
            }
            private void commit() {
                NcLinhaUI row = getTableRow() != null ? getTableRow().getItem() : null;
                if (row != null) row.qtd.set(field.getText());
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); }
                else { field.setText(getTableRow().getItem().qtd.get()); setGraphic(field); }
            }
        });
        tv.getColumns().addAll(cDesc, cQtd);
        tv.setPrefHeight(260);
        javafx.scene.control.TextField txtMotivo = new javafx.scene.control.TextField();
        txtMotivo.setPromptText("Motivo da NC parcial");
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10,
                new javafx.scene.control.Label("Seleccione quantidades a anular (0 = ignorar linha)."),
                tv, txtMotivo);
        root.setPadding(new javafx.geometry.Insets(16));
        root.setPrefWidth(600);
        modalService.create()
                .title("Nota de Crédito parcial")
                .content(root)
                .autoSize()
                .withConfirmButton("Emitir NC", () -> {
                    try {
                        java.util.List<FaturaService.AlocacaoNcLinha> linhas = new java.util.ArrayList<>();
                        for (NcLinhaUI r : rows) {
                            java.math.BigDecimal v;
                            try { v = new java.math.BigDecimal(r.qtd.get().trim().replace(",", ".")); }
                            catch (Exception ex) { continue; }
                            if (v.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;
                            if (v.compareTo(r.maxQtd) > 0) throw new IllegalArgumentException("Qtd excede original em " + r.descricao);
                            linhas.add(new FaturaService.AlocacaoNcLinha(r.itemId, v));
                        }
                        if (linhas.isEmpty()) throw new IllegalArgumentException("Nenhuma linha seleccionada.");
                        Fatura nc = faturaService.emitirNotaCreditoParcial(f.getId(), linhas, txtMotivo.getText());
                        AlertUtils.showInfoAlert("Emitida", "NC " + nc.getNumero() + " registada.");
                        if (onChange != null) onChange.accept(faturaId);
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showErrorAlert("Erro", ex.getMessage());
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private static class NcLinhaUI {
        final Long itemId;
        final String descricao;
        final java.math.BigDecimal maxQtd;
        final javafx.beans.property.StringProperty qtd;
        NcLinhaUI(Long itemId, String descricao, java.math.BigDecimal maxQtd) {
            this.itemId = itemId;
            this.descricao = descricao;
            this.maxQtd = maxQtd;
            this.qtd = new javafx.beans.property.SimpleStringProperty("0");
        }
    }

    private void anular() {
        Fatura f = atual();
        if (f == null) return;
        if (!documentoEstadoService.podeAnular(f)) {
            AlertUtils.showWarningAlert("Aviso", "Este documento não pode ser anulado.");
            return;
        }

        TextField motivoField = new TextField();
        motivoField.setPromptText("Motivo da anulação");
        VBox content = new VBox(8, new Label("Informe o motivo:"), motivoField);
        content.setPadding(new Insets(10));

        modalService.create()
                .title("Anular Documento")
                .content(content)
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    if (motivoField.getText() == null || motivoField.getText().trim().isEmpty()) {
                        AlertUtils.showWarningAlert("Motivo obrigatório", "Informe o motivo da anulação.");
                        return false;
                    }
                    new Thread(() -> {
                        String loadingId = loadingService.showLoading("A anular documento...", LoadingScreen.LoadingStyle.SPINNER);
                        try {
                            faturaService.cancelarFatura(f.getId(), motivoField.getText());
                            Platform.runLater(() -> {
                                recarregar();
                                notificarAlteracao();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() ->
                                    AlertUtils.showExceptionAlert("Erro", "Não foi possível anular o documento.", ex));
                        } finally {
                            Platform.runLater(() -> loadingService.hideLoading(loadingId));
                        }
                    }).start();
                    return true;
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private Fatura carregar() {
        return faturaService.findByIdWithItens(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
    }

    private Fatura atual() {
        try {
            return faturaService.findById(faturaId)
                    .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível carregar o documento.", ex);
            return null;
        }
    }

    private void notificarAlteracao() {
        if (onChange != null) onChange.accept(faturaId);
    }

    private void fechar() {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    // ── Helpers visuais ──────────────────────────────────────────────────

    private Card card(String titulo, Node content) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMaxWidth(Double.MAX_VALUE);
        Label header = new Label(titulo);
        header.setFont(Font.font("System", FontWeight.BOLD, 14));
        VBox body = content instanceof VBox v ? v : new VBox(content);
        body.getChildren().add(0, header);
        card.setBody(body);
        return card;
    }

    private HBox linha(String rotulo, String valor) {
        Label r = new Label(rotulo + ": ");
        r.setStyle("-fx-font-weight: bold; -fx-opacity: 0.8;");
        Label v = new Label(valor != null && !valor.isBlank() ? valor : "-");
        return new HBox(r, v);
    }

    private HBox fiscalLinha(String rotulo, String valor) {
        Label r = new Label(rotulo);
        r.getStyleClass().add(Styles.TEXT_MUTED);
        r.setMinWidth(180);
        r.setPrefWidth(180);

        boolean longa = valor != null && valor.length() > 24;
        Label v = new Label(longa ? valor.substring(0, 20) + "… (" + valor.length() + " chars)" : (valor != null ? valor : "-"));
        v.setWrapText(true);
        v.setMaxWidth(240);

        if (longa) {
            ContextMenu cm = new ContextMenu();
            MenuItem copiar = new MenuItem("Copiar", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
            copiar.setOnAction(e -> copiar(valor));
            MenuItem ver = new MenuItem("Ver completo", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
            ver.setOnAction(e -> modalService.create()
                    .title(rotulo)
                    .content(new Label(valor))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow());
            cm.getItems().addAll(copiar, ver);
            v.setContextMenu(cm);
        }

        return new HBox(8, r, v);
    }

    private void copiar(String texto) {
        if (texto == null) return;
        ClipboardContent cc = new ClipboardContent();
        cc.putString(texto);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private Label rotuloTotais(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add(Styles.TEXT_MUTED);
        return l;
    }

    private Label valorTotais(String texto) {
        Label l = new Label(texto);
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private <T> TableColumn<T, String> coluna(String titulo, String propriedade, double largura) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        c.setPrefWidth(largura);
        return c;
    }

    private <T> TableColumn<T, String> colunaDecimal(String titulo, String propriedade, double largura) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        c.setPrefWidth(largura);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        return c;
    }

    private <T> TableColumn<T, String> colunaData(String titulo, String propriedade, double largura) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        c.setPrefWidth(largura);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        return c;
    }

    private String moeda(BigDecimal valor) {
        return String.format("Kz %,.2f", valor != null ? valor : BigDecimal.ZERO);
    }
}
