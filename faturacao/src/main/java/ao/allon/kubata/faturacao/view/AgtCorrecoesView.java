package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
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

        Label desc = new Label("Corrigir itens isentos (IVA 0%) sem código de isenção.\n" +
            "Atribui M00 (Regime Simplificado) aos itens afetados.");
        desc.setWrapText(true);
        desc.getStyleClass().add(Styles.TEXT_SMALL);

        TextField txtProduto = new TextField();
        txtProduto.setPromptText("Nome do produto (ex: Bolacha Lulu) - deixe vazio para todos");

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

        body.getChildren().addAll(desc, txtProduto, lblStatusIsencao, buttons);
        card.setBody(body);

        return card;
    }

    private Card criarCardCorrecaoSequencia() {
        Card card = new Card();
        card.setHeader(new Label("Sequência de Documentos"));
        card.getStyleClass().add(Styles.ELEVATED_1);

        VBox body = new VBox(15);
        body.setPadding(new Insets(15));

        Label desc = new Label("Verificar e corrigir gaps na numeração de documentos.\n" +
            "Gera documentos 'NULOS' para preencher quebras de sequência.");
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

        Button btnCorrigir = new Button("Gerar Fillers");
        btnCorrigir.setGraphic(new FontIcon(Feather.PLUS));
        btnCorrigir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnCorrigir.setOnAction(e -> gerarFillers());

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
                
                lblStatusIsencao.setText("Encontrados: " + count + " itens sem código de isenção");
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
        String produto = descricaoProduto != null && !descricaoProduto.trim().isEmpty() 
            ? descricaoProduto.trim() 
            : null;
        
        String mensagem = produto != null 
            ? "Corrigir itens '" + produto + "' sem código de isenção?"
            : "Corrigir TODOS os itens sem código de isenção?";

        modalService.create()
            .title("Confirmar Correção")
            .content(new Label(mensagem + "\n\nSerá atribuído o código M00 (Regime Simplificado)."))
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                Platform.runLater(() -> {
                    try {
                        log("Iniciando correção...");
                        int corrigidos = faturaService.corrigirItensSemCodigoIsencao(produto);
                        log("Correção concluída. Itens corrigidos: " + corrigidos);
                        lblStatusIsencao.setText("Corrigidos: " + corrigidos + " itens");
                        lblStatusIsencao.getStyleClass().removeAll(Styles.WARNING);
                        lblStatusIsencao.getStyleClass().add(Styles.SUCCESS);
                        AlertUtils.showSuccess("Sucesso", corrigidos + " itens corrigidos!");
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

    private void gerarFillers() {
        Serie serie = cbSerie.getValue();
        if (serie == null) {
            AlertUtils.showWarning("Atenção", "Selecione uma série primeiro.");
            return;
        }

        List<Long> gaps = serieService.verificarGaps(serie);
        if (gaps.isEmpty()) {
            AlertUtils.showSuccess("Informação", "Não há gaps para corrigir nesta série.");
            return;
        }

        modalService.create()
            .title("Confirmar Geração de Fillers")
            .content(new Label(
                "Serão gerados " + gaps.size() + " documentos 'NULOS' para preencher gaps.\n" +
                "Série: " + serie.getDesignacao() + "\n" +
                "Números: " + gaps + "\n\n" +
                "Documentos nulos mantêm a integridade da sequência sem afetar valores."
            ))
            .autoSize()
            .withConfirmButton("Gerar Fillers", () -> {
                Platform.runLater(() -> {
                    try {
                        log("Gerando fillers para série " + serie.getDesignacao() + "...");
                        List<Fatura> fillers = serieService.gerarDocumentosFiller(serie, gaps);
                        log("Gerados " + fillers.size() + " documentos filler:");
                        for (Fatura f : fillers) {
                            log("  - " + f.getNumero() + " (Status: " + f.getStatus() + ")");
                        }
                        lblStatusSequencia.setText("✓ " + fillers.size() + " fillers gerados com sucesso");
                        lblStatusSequencia.getStyleClass().removeAll(Styles.WARNING);
                        lblStatusSequencia.getStyleClass().add(Styles.SUCCESS);
                        AlertUtils.showSuccess("Sucesso", fillers.size() + " documentos filler gerados!");
                    } catch (Exception e) {
                        log("Erro ao gerar fillers: " + e.getMessage());
                        AlertUtils.showError("Erro", e.getMessage());
                    }
                });
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void log(String mensagem) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtLog.appendText("[" + timestamp + "] " + mensagem + "\n");
    }
}
