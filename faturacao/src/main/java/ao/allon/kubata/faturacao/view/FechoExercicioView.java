package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.service.FechoExercicioService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

/**
 * View para gestão do fecho de exercício fiscal.
 */
public class FechoExercicioView extends VBox {

    private final FechoExercicioService fechoExercicioService;
    private final ContabilidadeService contabilidadeService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private ComboBox<Integer> cbAno;
    private ComboBox<Integer> cbMes;
    private Label lblStatus;
    private Label lblResultado;

    public FechoExercicioView(FechoExercicioService fechoExercicioService,
                              ContabilidadeService contabilidadeService,
                              SessionManager sessionManager,
                              ModalService modalService) {
        this.fechoExercicioService = fechoExercicioService;
        this.contabilidadeService = contabilidadeService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupControles();
        setupInfoPanel();
        updateStatus();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Fecho de Exercício Fiscal");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Gestão do fecho mensal e anual conforme normas contabilísticas angolanas");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);
        getChildren().add(header);
    }

    private void setupControles() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 5;");

        int row = 0;

        // Ano
        cbAno = new ComboBox<>();
        int anoAtual = Year.now().getValue();
        for (int i = anoAtual - 2; i <= anoAtual + 1; i++) {
            cbAno.getItems().add(i);
        }
        cbAno.setValue(anoAtual);
        cbAno.setOnAction(e -> updateStatus());
        grid.add(new Label("Ano:"), 0, row);
        grid.add(cbAno, 1, row);

        row++;

        // Mês
        cbMes = new ComboBox<>();
        for (int i = 1; i <= 12; i++) {
            cbMes.getItems().add(i);
        }
        cbMes.setValue(LocalDate.now().getMonthValue());
        cbMes.setOnAction(e -> updateStatus());
        grid.add(new Label("Mês:"), 0, row);
        grid.add(cbMes, 1, row);

        row++;

        // Ações
        HBox botoes = new HBox(10);
        botoes.setAlignment(Pos.CENTER_LEFT);

        Button btnFecharMes = new Button("Fechar Mês", IconUtils.icon(Feather.LOCK, IconUtils.SIZE_SMALL));
        btnFecharMes.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.WARNING);
        btnFecharMes.setOnAction(e -> fecharMes());

        Button btnFecharAno = new Button("Fechar Exercício", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        btnFecharAno.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnFecharAno.setOnAction(e -> fecharExercicio());

        Button btnVerificar = new Button("Verificar Balancete", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnVerificar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnVerificar.setOnAction(e -> verificarBalancete());

        boolean canEdit = sessionManager.hasAccess("CONTABILIDADE", "Editar");
        btnFecharMes.setDisable(!canEdit);
        btnFecharAno.setDisable(!canEdit);

        botoes.getChildren().addAll(btnVerificar, btnFecharMes, btnFecharAno);
        grid.add(new Label("Ações:"), 0, row);
        grid.add(botoes, 1, row);

        getChildren().add(grid);
    }

    private void setupInfoPanel() {
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

        lblStatus = new Label("Status: Verificando...");
        lblStatus.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        lblResultado = new Label("Resultado do Exercício: -");
        lblResultado.setStyle("-fx-font-size: 13px;");

        // Instruções
        TitledPane instrucoes = new TitledPane("Instruções de Fecho", new VBox());
        instrucoes.setExpanded(false);

        TextArea txtInstrucoes = new TextArea(
            "PROCEDIMENTO DE FECHO DE EXERCÍCIO:\n\n" +
            "1. FECHO MENSAL:\n" +
            "   - Verifique se o balancete está equilibrado (Débitos = Créditos)\n" +
            "   - Realize a reconciliação bancária\n" +
            "   - Confirme todos os lançamentos do mês\n" +
            "   - Após o fecho, não será possível criar ou alterar lançamentos naquele mês\n\n" +
            "2. FECHO ANUAL:\n" +
            "   - Certifique-se de que todos os meses do ano estão fechados\n" +
            "   - O sistema calculará automaticamente o resultado do exercício\n" +
            "   - Serão criados lançamentos de apuração de resultados\n" +
            "   - As séries documentais serão fechadas\n" +
            "   - Após o fecho anual, o exercício não poderá ser reaberto\n\n" +
            "3. NOTAS IMPORTANTES:\n" +
            "   - Faça backup antes de realizar qualquer fecho\n" +
            "   - O fecho deve ser feito em sequência (mês 1, 2, 3, etc.)\n" +
            "   - Consulte o seu contabilista em caso de dúvidas"
        );
        txtInstrucoes.setEditable(false);
        txtInstrucoes.setPrefRowCount(10);
        txtInstrucoes.setWrapText(true);

        instrucoes.setContent(txtInstrucoes);

        infoBox.getChildren().addAll(lblStatus, lblResultado, instrucoes);
        getChildren().add(infoBox);
    }

    private void updateStatus() {
        int ano = cbAno.getValue() != null ? cbAno.getValue() : Year.now().getValue();
        int mes = cbMes.getValue() != null ? cbMes.getValue() : LocalDate.now().getMonthValue();

        boolean mesFechado = fechoExercicioService.isPeriodoFechado(ano, mes);
        boolean anoFechado = fechoExercicioService.isAnoFechado(ano);

        if (anoFechado) {
            lblStatus.setText("Status: ANO " + ano + " FECHADO");
            lblStatus.setTextFill(Color.BLUE);
        } else if (mesFechado) {
            lblStatus.setText("Status: Mês " + mes + "/" + ano + " FECHADO");
            lblStatus.setTextFill(Color.GREEN);
        } else {
            lblStatus.setText("Status: Mês " + mes + "/" + ano + " ABERTO");
            lblStatus.setTextFill(Color.ORANGE);
        }
    }

    private void fecharMes() {
        int ano = cbAno.getValue();
        int mes = cbMes.getValue();

        if (fechoExercicioService.isPeriodoFechado(ano, mes)) {
            AlertUtils.showWarningAlert("Aviso", "Este mês já está fechado.");
            return;
        }

        modalService.create()
            .title("Confirmar Fecho Mensal")
            .content(new Label("Tem certeza que deseja fechar o mês " + mes + "/" + ano + "?\n\n" +
                "Após o fecho, não será possível criar ou alterar lançamentos neste mês."))
            .autoSize()
            .withConfirmButton("Confirmar Fecho", () -> {
                try {
                    fechoExercicioService.fecharMes(ano, mes, 1L, "Usuário");
                    updateStatus();
                    AlertUtils.showInfoAlert("Sucesso", "Mês " + mes + "/" + ano + " fechado com sucesso!");
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível fechar o mês.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void fecharExercicio() {
        int ano = cbAno.getValue();

        if (fechoExercicioService.isAnoFechado(ano)) {
            AlertUtils.showWarningAlert("Aviso", "Este exercício já está fechado.");
            return;
        }

        if (!fechoExercicioService.isPeriodoFechado(ano, 12)) {
            AlertUtils.showWarningAlert("Mês 12 Aberto", "O mês 12 deve ser fechado antes de fechar o exercício.");
            return;
        }

        modalService.create()
            .title("Confirmar Fecho Anual")
            .content(new Label("TEM CERTEZA que deseja fechar o exercício " + ano + "?\n\n" +
                "Esta operação:\n" +
                "• Calculará o resultado do exercício\n" +
                "• Criará lançamentos de apuração\n" +
                "• Fechará as séries documentais\n" +
                "• NÃO PODERÁ SER DESFEITA\n\n" +
                "Recomenda-se fazer backup antes de continuar."))
            .autoSize()
            .withConfirmButton("CONFIRMAR FECHO ANUAL", () -> {
                try {
                    var resultado = fechoExercicioService.fecharExercicio(ano, 1L, "Usuário");

                    updateStatus();

                    String msgResultado = resultado.isLucro() ?
                        "LUCRO de Kz " + String.format("%.2f", resultado.getValorResultado()) :
                        "PREJUÍZO de Kz " + String.format("%.2f", resultado.getValorResultado());

                    lblResultado.setText("Resultado do Exercício " + ano + ": " + msgResultado);
                    lblResultado.setTextFill(resultado.isLucro() ? Color.GREEN : Color.RED);

                    AlertUtils.showInfoAlert("Sucesso",
                        "Exercício " + ano + " fechado com sucesso!\n\n" + msgResultado);
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível fechar o exercício.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void verificarBalancete() {
        int ano = cbAno.getValue();
        int mes = cbMes.getValue();
        LocalDate dataFim = LocalDate.of(ano, mes, 1).withDayOfMonth(
            LocalDate.of(ano, mes, 1).lengthOfMonth());

        try {
            var balancete = contabilidadeService.gerarBalancete(dataFim);

            BigDecimal totalDebitos = BigDecimal.ZERO;
            BigDecimal totalCreditos = BigDecimal.ZERO;

            for (var item : balancete) {
                totalDebitos = totalDebitos.add(item.getTotalDebito());
                totalCreditos = totalCreditos.add(item.getTotalCredito());
            }

            boolean equilibrado = totalDebitos.compareTo(totalCreditos) == 0;

            String mensagem = String.format(
                "Balancete de %02d/%d:\n\n" +
                "Total Débitos:  Kz %,.2f\n" +
                "Total Créditos: Kz %,.2f\n\n" +
                "Status: %s",
                mes, ano,
                totalDebitos,
                totalCreditos,
                equilibrado ? "EQUILIBRADO ✓" : "DESIQUILIBRADO ✗"
            );

            Alert.AlertType tipo = equilibrado ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
            Alert alert = new Alert(tipo);
            alert.setTitle("Verificação do Balancete");
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();

        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível gerar o balancete.", ex);
        }
    }
}
