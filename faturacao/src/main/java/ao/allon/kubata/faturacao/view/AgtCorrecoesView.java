package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.TaxExemptionCode;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.SerieService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import atlantafx.base.theme.Styles;
import atlantafx.base.controls.Card;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class AgtCorrecoesView extends VBox {

    private final FaturaService faturaService;
    private final SerieService serieService;
    private final ModalService modalService;
    
    private Label lblStatusIsencao;
    private Label lblStatusSequencia;
    private TextArea txtLog;
    private ComboBox<Serie> cbSerie;
    private ComboBox<TaxExemptionCode> cbCodigoIsencao;

    public AgtCorrecoesView(FaturaService faturaService, SerieService serieService, ModalService modalService) {
        this.faturaService = faturaService;
        this.serieService = serieService;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("agt-correcoes-view");

        initializeUI();
    }

    private void initializeUI() {
        // Header
        VBox header = new VBox(10);
        Label title = new Label("Correções AGT / SAF-T AO");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Ferramentas para corrigir conformidade com a AGT.");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        header.getChildren().addAll(title, subtitle);

        // Cards container
        HBox cardsBox = new HBox(20);
        cardsBox.setAlignment(Pos.TOP_LEFT);

        // Card 1: Correção de Códigos de Isenção
        Card cardIsencao = criarCardCorrecaoIsencao();
        HBox.setHgrow(cardIsencao, Priority.ALWAYS);

        // Card 2: Correção de Sequência
        Card cardSequencia = criarCardCorrecaoSequencia();
        HBox.setHgrow(cardSequencia, Priority.ALWAYS);

        cardsBox.getChildren().addAll(cardIsencao, cardSequencia);

        // Log area
        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setPromptText("Log de operações...");
        txtLog.setPrefRowCount(10);
        VBox.setVgrow(txtLog, Priority.ALWAYS);

        getChildren().addAll(header, cardsBox, new Label("Log de Operações:"), txtLog);
    }

    private Card criarCardCorrecaoIsencao() {
        Card card = new Card();
        card.setHeader(new Label("Códigos de Isenção"));
        card.getStyleClass().add(Styles.ELEVATED_1);

        VBox body = new VBox(15);
        body.setPadding(new Insets(15));

        Label desc = new Label("Preencher o código fiscal em falta nos itens com IVA a 0%.\n" +
            "Seleccione o código de acordo com o enquadramento fiscal confirmado. Só rascunhos são alterados; documentos emitidos ou pagos permanecem imutáveis.");
        desc.setWrapText(true);
        desc.getStyleClass().add(Styles.TEXT_SMALL);

        TextField txtProduto = new TextField();
        txtProduto.setPromptText("Nome do produto (ex: Bolacha Lulu) - deixe vazio para todos");

        cbCodigoIsencao = new ComboBox<>();
        cbCodigoIsencao.getItems().setAll(TaxExemptionCode.values());
        cbCodigoIsencao.setPromptText("Código de isenção fiscal (obrigatório)");
        cbCodigoIsencao.setMaxWidth(Double.MAX_VALUE);

        lblStatusIsencao = new Label("Clique para verificar...");
        lblStatusIsencao.getStyleClass().add(Styles.TEXT_MUTED);

        Button btnVerificar = new Button("Verificar");
        btnVerificar.setGraphic(new FontIcon(Feather.SEARCH));
        btnVerificar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnVerificar.setOnAction(e -> verificarItensIsencao());

        Button btnCorrigir = new Button("Corrigir Todos");
        btnCorrigir.setGraphic(new FontIcon(Feather.CHECK));
        btnCorrigir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnCorrigir.setOnAction(e -> corrigirItensIsencao(txtProduto.getText()));

        HBox buttons = new HBox(10, btnVerificar, btnCorrigir);

        body.getChildren().addAll(desc, txtProduto, cbCodigoIsencao, lblStatusIsencao, buttons);
        card.setBody(body);

        return card;
    }

    private Card criarCardCorrecaoSequencia() {
        Card card = new Card();
        card.setHeader(new Label("Sequência de Documentos"));
        card.getStyleClass().add(Styles.ELEVATED_1);

        VBox body = new VBox(15);
        body.setPadding(new Insets(15));

        Label desc = new Label("Auditar lacunas na numeração fiscal da série.\n" +
            "O Kubata não cria documentos NULOS retroactivos automaticamente; as lacunas exigem reconciliação com registos e procedimento fiscal aplicável.");
        desc.setWrapText(true);
        desc.getStyleClass().add(Styles.TEXT_SMALL);

        cbSerie = new ComboBox<>();
        cbSerie.setPromptText("Selecione a série");
        cbSerie.setMaxWidth(Double.MAX_VALUE);
        loadSeries();

        lblStatusSequencia = new Label("Selecione uma série para verificar...");
        lblStatusSequencia.getStyleClass().add(Styles.TEXT_MUTED);

        Button btnVerificar = new Button("Verificar Gaps");
        btnVerificar.setGraphic(new FontIcon(Feather.SEARCH));
        btnVerificar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnVerificar.setOnAction(e -> verificarGaps());

        Button btnCorrigir = new Button("Ver Procedimento");
        btnCorrigir.setGraphic(new FontIcon(Feather.INFO));
        btnCorrigir.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnCorrigir.setOnAction(e -> mostrarProcedimentoGaps());

        HBox buttons = new HBox(10, btnVerificar, btnCorrigir);

        body.getChildren().addAll(desc, cbSerie, lblStatusSequencia, buttons);
        card.setBody(body);

        return card;
    }

    private void loadSeries() {
        try {
            List<Serie> series = serieService.findAll();
            cbSerie.getItems().setAll(series);
        } catch (Exception e) {
            log("Erro ao carregar séries: " + e.getMessage());
        }
    }

    private void verificarItensIsencao() {
        log("Verificando itens sem código de isenção...");
        
        Platform.runLater(() -> {
            try {
                // Usar método transacional do serviço
                int count = faturaService.contarItensSemCodigoIsencao();
                
                lblStatusIsencao.setText("Encontrados: " + count
                        + " itens sem código. Apenas rascunhos podem ser corrigidos.");
                if (count > 0) {
                    lblStatusIsencao.getStyleClass().removeAll(Styles.TEXT_MUTED);
                    lblStatusIsencao.getStyleClass().add(Styles.WARNING);
                }
                log("Verificação concluída. Itens afetados: " + count);
            } catch (Exception e) {
                log("Erro na verificação: " + e.getMessage());
                AlertUtils.showError("Erro ao verificar", e.getMessage());
            }
        });
    }

    private void corrigirItensIsencao(String descricaoProduto) {
        TaxExemptionCode codigo = cbCodigoIsencao.getValue();
        if (codigo == null) {
            AlertUtils.showWarning("Código obrigatório",
                    "Seleccione o código de isenção correcto após confirmar o enquadramento fiscal.");
            return;
        }

        String produto = descricaoProduto != null && !descricaoProduto.trim().isEmpty()
            ? descricaoProduto.trim()
            : null;

        String mensagem = produto != null
            ? "Corrigir rascunhos com itens '" + produto + "' sem código de isenção?"
            : "Corrigir rascunhos com itens sem código de isenção?";

        modalService.create()
            .title("Confirmar Correção Fiscal")
            .content(new Label(mensagem + "\n\nCódigo escolhido: " + codigo
                    + "\n\nSó os documentos em RASCUNHO serão alterados. Documentos emitidos ou pagos não serão modificados."))
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                Platform.runLater(() -> {
                    try {
                        log("Iniciando correção fiscal de rascunhos com código " + codigo.getCode() + "...");
                        int corrigidos = faturaService.corrigirItensSemCodigoIsencao(produto, codigo);
                        log("Correção concluída. Linhas corrigidas em rascunhos: " + corrigidos);
                        lblStatusIsencao.setText("Rascunhos corrigidos: " + corrigidos + " linhas");
                        lblStatusIsencao.getStyleClass().removeAll(Styles.WARNING);
                        lblStatusIsencao.getStyleClass().add(Styles.SUCCESS);
                        AlertUtils.showSuccess("Sucesso", corrigidos
                                + " linhas corrigidas em rascunhos. Confirme os códigos com o responsável fiscal.");
                    } catch (Exception e) {
                        log("Erro na correção: " + e.getMessage());
                        AlertUtils.showError("Erro", e.getMessage());
                    }
                });
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void verificarGaps() {
        Serie serie = cbSerie.getValue();
        if (serie == null) {
            AlertUtils.showWarning("Atenção", "Selecione uma série primeiro.");
            return;
        }

        log("Verificando gaps na série " + serie.getDesignacao() + "...");
        
        Platform.runLater(() -> {
            try {
                List<Long> gaps = serieService.verificarGaps(serie);
                if (gaps.isEmpty()) {
                    lblStatusSequencia.setText("✓ Nenhum gap encontrado. Sequência está correta.");
                    lblStatusSequencia.getStyleClass().removeAll(Styles.WARNING);
                    lblStatusSequencia.getStyleClass().add(Styles.SUCCESS);
                    log("Nenhum gap encontrado na série " + serie.getDesignacao());
                } else {
                    lblStatusSequencia.setText("⚠ Gaps encontrados: " + gaps.size() + " números ausentes");
                    lblStatusSequencia.getStyleClass().removeAll(Styles.TEXT_MUTED, Styles.SUCCESS);
                    lblStatusSequencia.getStyleClass().add(Styles.WARNING);
                    log("Gaps encontrados na série " + serie.getDesignacao() + ": " + gaps);
                }
            } catch (Exception e) {
                log("Erro na verificação: " + e.getMessage());
                AlertUtils.showError("Erro", e.getMessage());
            }
        });
    }

    private void mostrarProcedimentoGaps() {
        Serie serie = cbSerie.getValue();
        if (serie == null) {
            AlertUtils.showWarning("Atenção", "Selecione uma série primeiro.");
            return;
        }

        try {
            List<Long> gaps = serieService.verificarGaps(serie);
            if (gaps.isEmpty()) {
                AlertUtils.showSuccess("Sequência correcta", "Não foram encontradas lacunas até ao último número reservado.");
                return;
            }

            String numeros = gaps.size() > 100
                    ? gaps.subList(0, 100) + " ... (mais " + (gaps.size() - 100) + ")"
                    : gaps.toString();
            modalService.create()
                .title("Reconciliação de Numeração Fiscal")
                .content(new Label(
                    "Série: " + serie.getDesignacao() + "\n"
                    + "Lacunas detectadas: " + gaps.size() + "\n"
                    + "Números: " + numeros + "\n\n"
                    + "Não serão criadas facturas fictícias nem documentos NULOS retroactivos. "
                    + "Confirme os registos de emissão, auditoria, falhas de persistência e documentos anulados. "
                    + "Registe a causa e siga o procedimento fiscal aplicável com o responsável fiscal/AGT antes de alterar qualquer registo."
                ))
                .autoSize()
                .withConfirmButton("Entendi", () -> {
                    log("Lacunas de sequência detectadas na série " + serie.getDesignacao()
                            + ": " + gaps + ". Não foram criados documentos.");
                    lblStatusSequencia.setText("Reconciliação pendente: " + gaps.size() + " lacunas");
                    lblStatusSequencia.getStyleClass().removeAll(Styles.SUCCESS, Styles.TEXT_MUTED);
                    lblStatusSequencia.getStyleClass().add(Styles.WARNING);
                    return true;
                })
                .withCancelButton("Fechar")
                .buildAndShow();
        } catch (Exception e) {
            log("Erro ao auditar a sequência: " + e.getMessage());
            AlertUtils.showError("Erro ao auditar", e.getMessage());
        }
    }

    private void log(String mensagem) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtLog.appendText("[" + timestamp + "] " + mensagem + "\n");
    }
}
