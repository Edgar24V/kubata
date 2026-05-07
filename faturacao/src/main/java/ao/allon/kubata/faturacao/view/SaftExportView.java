package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.service.SaftAoExportService;
import ao.allon.kubata.faturacao.service.SaftValidatorService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface moderna de exportação SAF-T (AO) com pré-visualização,
 * validação em tempo real, filtros rápidos e feedback visual.
 */
public class SaftExportView extends BorderPane {

    private final SaftAoExportService saftService;
    private final SaftValidatorService validatorService;

    private DatePicker dpInicio;
    private DatePicker dpFim;
    private Button btnMesAtual;
    private Button btnMesAnterior;
    private Button btnUltimoTrimestre;
    private Label lblValidationStatus;
    private TextArea txtLog;
    private ProgressBar progressBar;
    private Label lblProgressStatus;
    private Button btnValidar;
    private Button btnExportar;
    private Button btnPreVisualizar;

    public SaftExportView(SaftAoExportService saftService, SaftValidatorService validatorService) {
        this.saftService = saftService;
        this.validatorService = validatorService;
        buildUI();
    }

    private void buildUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: -color-bg-default;");
        setTop(createHeader());
        setCenter(createMainContent());
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(0, 0, 20, 0));
        
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = IconUtils.icon(Feather.FILE_TEXT, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");
        
        VBox titleLabels = new VBox(5);
        Label title = new Label("Exportação SAF-T (AO)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 24px;");
        
        Label subtitle = new Label("Exporte dados fiscais no formato Standard Audit File for Tax Angola");
        subtitle.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 14px;");
        
        titleLabels.getChildren().addAll(title, subtitle);
        titleBox.getChildren().addAll(icon, titleLabels);
        header.getChildren().add(titleBox);
        return header;
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.65);
        splitPane.getItems().addAll(createLeftPanel(), createRightPanel());
        return splitPane;
    }

    private VBox createLeftPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(10));
        
        VBox periodCard = createCard("Período de Exportação", IconUtils.icon(Feather.CALENDAR, 18));
        periodCard.getChildren().add(createPeriodSelector());
        
        VBox validationCard = createCard("Status de Validação", IconUtils.icon(Feather.SHIELD, 18));
        validationCard.getChildren().add(createValidationPanel());
        
        panel.getChildren().addAll(periodCard, validationCard, createProgressPanel(), createActionButtons());
        return panel;
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 12px; -fx-border-radius: 12px;");
        
        Label logTitle = new Label("Log de Atividades");
        logTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setWrapText(true);
        txtLog.setPrefRowCount(20);
        txtLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-background-color: -color-bg-inset;");
        VBox.setVgrow(txtLog, Priority.ALWAYS);
        
        Button btnClearLog = new Button("Limpar Log", IconUtils.icon(Feather.TRASH_2, 14));
        btnClearLog.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        btnClearLog.setOnAction(e -> txtLog.clear());
        
        panel.getChildren().addAll(logTitle, txtLog, btnClearLog);
        return panel;
    }

    private VBox createCard(String title, Node icon) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: -color-border-muted; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        
        HBox titleBox = new HBox(10, icon, new Label(title));
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().get(1).setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        card.getChildren().add(titleBox);
        return card;
    }

    private VBox createPeriodSelector() {
        VBox container = new VBox(15);
        
        HBox quickFilters = new HBox(10);
        quickFilters.setAlignment(Pos.CENTER_LEFT);
        
        btnMesAtual = createFilterButton("Mês Atual", Feather.CALENDAR, () -> setPeriod(
            LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        btnMesAnterior = createFilterButton("Mês Anterior", Feather.CALENDAR, () -> {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            setPeriod(lastMonth.withDayOfMonth(1), lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));
        });
        btnUltimoTrimestre = createFilterButton("Último Trimestre", Feather.CALENDAR, () -> {
            LocalDate now = LocalDate.now();
            int quarter = (now.getMonthValue() - 1) / 3;
            int year = quarter == 0 ? now.getYear() - 1 : now.getYear();
            int startMonth = quarter == 0 ? 10 : (quarter - 1) * 3 + 1;
            LocalDate start = LocalDate.of(year, startMonth, 1);
            setPeriod(start, start.plusMonths(3).minusDays(1));
        });
        
        quickFilters.getChildren().addAll(btnMesAtual, btnMesAnterior, btnUltimoTrimestre);
        
        GridPane dateGrid = new GridPane();
        dateGrid.setHgap(15);
        dateGrid.setVgap(10);
        dateGrid.setAlignment(Pos.CENTER_LEFT);
        
        dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpFim = new DatePicker(LocalDate.now());
        
        dpInicio.valueProperty().addListener((obs, old, newVal) -> { if (newVal != null) logPeriodo(); });
        dpFim.valueProperty().addListener((obs, old, newVal) -> { if (newVal != null) logPeriodo(); });
        
        dateGrid.addRow(0, new Label("Data Início:"), dpInicio);
        dateGrid.addRow(1, new Label("Data Fim:"), dpFim);
        
        container.getChildren().addAll(quickFilters, dateGrid);
        return container;
    }

    private Button createFilterButton(String text, Feather icon, Runnable action) {
        Button btn = new Button(text, IconUtils.icon(icon, 14));
        btn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private HBox createValidationPanel() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: -color-bg-inset; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        
        lblValidationStatus = new Label("Clique em 'Validar' para verificar os dados");
        lblValidationStatus.setStyle("-fx-text-fill: -color-fg-muted;");
        
        Button btnValidarNow = new Button("Validar Agora", IconUtils.icon(Feather.CHECK_CIRCLE, 14));
        btnValidarNow.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnValidarNow.setOnAction(e -> validateData());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        panel.getChildren().addAll(lblValidationStatus, spacer, btnValidarNow);
        return panel;
    }

    private VBox createProgressPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        lblProgressStatus = new Label("");
        lblProgressStatus.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
        lblProgressStatus.setVisible(false);
        
        panel.getChildren().addAll(progressBar, lblProgressStatus);
        return panel;
    }

    private HBox createActionButtons() {
        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));
        
        btnPreVisualizar = createActionButton("Pré-visualizar", Feather.EYE, Styles.BUTTON_OUTLINED, this::previewData);
        btnValidar = createActionButton("Validar Dados", Feather.CHECK_SQUARE, Styles.BUTTON_OUTLINED, this::validateData);
        btnExportar = createActionButton("Exportar SAF-T", Feather.DOWNLOAD, Styles.ACCENT, this::exportar);
        btnExportar.setDefaultButton(true);
        
        buttons.getChildren().addAll(btnPreVisualizar, btnValidar, btnExportar);
        return buttons;
    }

    private Button createActionButton(String text, Feather icon, String styleClass, Runnable action) {
        Button btn = new Button(text, IconUtils.icon(icon, 16));
        btn.getStyleClass().addAll(styleClass, Styles.LARGE);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void setPeriod(LocalDate start, LocalDate end) {
        dpInicio.setValue(start);
        dpFim.setValue(end);
        log("Período selecionado: " + formatDate(start) + " a " + formatDate(end));
    }

    private void logPeriodo() {
        if (dpInicio.getValue() != null && dpFim.getValue() != null) {
            log("Período alterado: " + formatDate(dpInicio.getValue()) + " a " + formatDate(dpFim.getValue()));
        }
    }

    private void validateData() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        
        if (!validateDates(inicio, fim)) return;
        
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        lblProgressStatus.setVisible(true);
        lblProgressStatus.setText("Validando dados...");
        log("Iniciando validação dos dados...");
        
        CompletableFuture.supplyAsync(() -> validatorService.validateForExport(inicio, fim))
            .thenAccept(errors -> Platform.runLater(() -> {
                progressBar.setVisible(false);
                lblProgressStatus.setVisible(false);
                setControlsEnabled(true);
                
                if (errors.isEmpty()) {
                    lblValidationStatus.setText("✓ Dados validados com sucesso!");
                    lblValidationStatus.setStyle("-fx-text-fill: -color-success-emphasis;");
                    log("✓ Validação concluída: Nenhum erro encontrado");
                    AlertUtils.showSuccess("Validação", "Dados validados com sucesso!");
                } else {
                    lblValidationStatus.setText("✗ " + errors.size() + " erro(s) encontrado(s)");
                    lblValidationStatus.setStyle("-fx-text-fill: -color-danger-emphasis;");
                    log("✗ Validação concluída: " + errors.size() + " erro(s)");
                    errors.forEach(e -> log("  - " + e));
                    AlertUtils.showErrorWithCopy("Erros de Validação", 
                        "Encontrados " + errors.size() + " erro(s):\n\n• " + String.join("\n• ", errors));
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblProgressStatus.setVisible(false);
                    setControlsEnabled(true);
                    lblValidationStatus.setText("✗ Erro: " + ex.getMessage());
                    lblValidationStatus.setStyle("-fx-text-fill: -color-danger-emphasis;");
                    log("✗ Erro na validação: " + ex.getMessage());
                    AlertUtils.showExceptionAlert("Erro", "Falha ao validar dados", ex);
                });
                return null;
            });
    }

    private void previewData() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        
        if (!validateDates(inicio, fim)) return;
        
        log("Pré-visualização: " + formatDate(inicio) + " a " + formatDate(fim));
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Pré-visualização dos Dados");
        dialog.setHeaderText("Resumo dos dados a serem exportados");
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(Styles.class.getResource("/atlantafx/base/theme/cupertino-light.css").toExternalForm());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);
        
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(8);
        infoGrid.addRow(0, new Label("Período:"), new Label(formatDate(inicio) + " a " + formatDate(fim)));
        infoGrid.addRow(1, new Label("Versão SAF-T:"), new Label("1.01_01"));
        infoGrid.addRow(2, new Label("Tipo:"), new Label("F - Faturação"));
        
        Label note = new Label("A exportação incluirá todos os documentos fiscais do período: faturas, notas de crédito/débito, guias de remessa e transporte.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
        
        content.getChildren().addAll(infoGrid, new Separator(), note);
        dialogPane.setContent(content);
        
        ButtonType btnExport = new ButtonType("Exportar Agora", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnExport, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == btnExport) Platform.runLater(this::exportar);
            return null;
        });
        
        dialog.showAndWait();
    }

    private void exportar() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        
        if (!validateDates(inicio, fim)) return;
        
        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar Arquivo SAF-T (AO)");
        String defaultFileName = String.format("SAFT_AO_%s_%s_%s.xml", 
            inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            fim.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        fc.setInitialFileName(defaultFileName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) performExport(file, inicio, fim);
    }

    private void performExport(File file, LocalDate inicio, LocalDate fim) {
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        lblProgressStatus.setVisible(true);
        lblProgressStatus.setText("Exportando arquivo SAF-T...");
        log("Iniciando exportação para: " + file.getAbsolutePath());
        
        CompletableFuture.runAsync(() -> {
            try {
                saftService.exportFullSaft(file, inicio, fim);
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblProgressStatus.setVisible(false);
                    setControlsEnabled(true);
                    
                    lblValidationStatus.setText("✓ Exportado em " + formatDate(LocalDate.now()));
                    lblValidationStatus.setStyle("-fx-text-fill: -color-success-emphasis;");
                    
                    log("✓ Exportação concluída!");
                    log("  Arquivo: " + file.getAbsolutePath());
                    log("  Tamanho: " + (file.length() / 1024) + " KB");
                    
                    AlertUtils.showSuccess("Exportação Concluída", 
                        "Arquivo SAF-T exportado com sucesso!\n\n" + file.getAbsolutePath() + 
                        "\nTamanho: " + (file.length() / 1024) + " KB");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblProgressStatus.setVisible(false);
                    setControlsEnabled(true);
                    lblValidationStatus.setText("✗ Falha na exportação");
                    lblValidationStatus.setStyle("-fx-text-fill: -color-danger-emphasis;");

                    String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    log("✗ Erro na exportação: " + msg);

                    if (e instanceof IllegalStateException && msg.startsWith("Erros de validação SAF-T")) {
                        AlertUtils.showWarning("Validação SAF-T", msg +
                            "\n\nComo resolver:\n" +
                            "1) Abra os documentos listados (FT ...).\n" +
                            "2) Nos itens com IVA 0%/isento, preencha o 'Código de Isenção' (formato Mxx, ex: M00, M02...) e o motivo.\n" +
                            "3) Se usar um código diferente de M00, garanta que ele esteja cadastrado em 'Motivos de Isenção'.");
                    } else {
                        AlertUtils.showExceptionAlert("Erro na Exportação", "Falha ao gerar arquivo SAF-T", e);
                    }
                });
            }
        });
    }

    private boolean validateDates(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            AlertUtils.showWarning("Validação", "Selecione as datas de início e fim.");
            return false;
        }
        if (fim.isBefore(inicio)) {
            AlertUtils.showWarning("Validação", "A data final não pode ser anterior à data inicial.");
            return false;
        }
        return true;
    }

    private void setControlsEnabled(boolean enabled) {
        btnExportar.setDisable(!enabled);
        btnValidar.setDisable(!enabled);
        btnPreVisualizar.setDisable(!enabled);
        btnMesAtual.setDisable(!enabled);
        btnMesAnterior.setDisable(!enabled);
        btnUltimoTrimestre.setDisable(!enabled);
        dpInicio.setDisable(!enabled);
        dpFim.setDisable(!enabled);
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            txtLog.appendText("[" + timestamp + "] " + message + "\n");
            txtLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
