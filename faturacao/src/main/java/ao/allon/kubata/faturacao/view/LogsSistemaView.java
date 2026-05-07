package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.SystemLog;
import ao.allon.kubata.faturacao.service.SystemLogService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LogsSistemaView extends VBox {

    private final SystemLogService logService;
    private TableView<SystemLog> table;
    private Label lblTotalLogs;
    private Label lblErros24h;
    private Label lblWarns24h;

    private ComboBox<SystemLog.LogLevel> cbLogLevel;
    private ComboBox<String> cbCategory;
    private TextField txtSearch;

    public LogsSistemaView(SystemLogService logService) {
        this.logService = logService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("logs-sistema-view");

        HBox header = createHeader();
        HBox stats = createStatsPanel();
        HBox filters = createFiltersPanel();
        VBox tableBox = createTablePanel();

        getChildren().addAll(header, stats, filters, tableBox);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        loadData();
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Logs do Sistema");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Registros técnicos de operação e diagnóstico");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClear = new Button("Limpar Logs Antigos", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
        btnClear.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnClear.setOnAction(e -> showEmDesenvolvimento("Limpar Logs"));

        header.getChildren().addAll(titleBox, spacer, btnClear);
        return header;
    }

    private HBox createStatsPanel() {
        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);

        lblTotalLogs = new Label("0");
        lblErros24h = new Label("0");
        lblWarns24h = new Label("0");

        stats.getChildren().addAll(
            createStatCard("Total", lblTotalLogs, Feather.LIST, Color.BLUE),
            createStatCard("Erros 24h", lblErros24h, Feather.ALERT_TRIANGLE, Color.RED),
            createStatCard("Avisos 24h", lblWarns24h, Feather.ALERT_CIRCLE, Color.ORANGE)
        );

        return stats;
    }

    private VBox createStatCard(String title, Label value, Feather icon, Color color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");
        card.setPrefWidth(120);

        FontIcon ico = IconUtils.icon(icon, 24);
        ico.setFill(color);

        value.getStyleClass().add(Styles.TITLE_3);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add(Styles.TEXT_SMALL);

        card.getChildren().addAll(ico, value, lblTitle);
        return card;
    }

    private HBox createFiltersPanel() {
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(5, 0, 5, 0));

        cbLogLevel = new ComboBox<>();
        cbLogLevel.getItems().addAll(null, SystemLog.LogLevel.DEBUG, SystemLog.LogLevel.INFO,
            SystemLog.LogLevel.WARN, SystemLog.LogLevel.ERROR, SystemLog.LogLevel.FATAL);
        cbLogLevel.setPromptText("Nível de Log");
        cbLogLevel.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(SystemLog.LogLevel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        cbLogLevel.setButtonCell(cbLogLevel.getCellFactory().call(null));

        cbCategory = new ComboBox<>();
        cbCategory.getItems().addAll(null, "BACKUP", "RESTORE", "AUTH", "DATABASE", "SAFT", "SYSTEM", "API");
        cbCategory.setPromptText("Categoria");

        txtSearch = new TextField();
        txtSearch.setPromptText("Buscar na mensagem...");
        txtSearch.setPrefWidth(250);

        Button btnFilter = new Button("Filtrar", IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        btnFilter.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnFilter.setOnAction(e -> loadData());

        Button btnClear = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnClear.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnClear.setOnAction(e -> {
            cbLogLevel.setValue(null);
            cbCategory.setValue(null);
            txtSearch.clear();
            loadData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filters.getChildren().addAll(
            new Label("Filtros:"), cbLogLevel, cbCategory, txtSearch, btnFilter, btnClear, spacer
        );

        return filters;
    }

    private VBox createTablePanel() {
        VBox box = new VBox(10);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SystemLog, String> colLevel = new TableColumn<>("Nível");
        colLevel.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getLogLevel() != null ? cell.getValue().getLogLevel().name() : ""
        ));
        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Color color = switch (item) {
                        case "DEBUG" -> Color.GRAY;
                        case "INFO" -> Color.BLUE;
                        case "WARN" -> Color.ORANGE;
                        case "ERROR", "FATAL" -> Color.RED;
                        default -> Color.BLACK;
                    };
                    FontIcon icon = IconUtils.icon(
                        item.equals("ERROR") || item.equals("FATAL") ? Feather.ALERT_CIRCLE :
                        item.equals("WARN") ? Feather.ALERT_TRIANGLE : Feather.INFO, IconUtils.SIZE_SMALL
                    );
                    icon.setFill(color);
                    setGraphic(icon);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        TableColumn<SystemLog, String> colTimestamp = new TableColumn<>("Data/Hora");
        colTimestamp.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"))
        ));

        TableColumn<SystemLog, String> colCategory = new TableColumn<>("Categoria");
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory()));

        TableColumn<SystemLog, String> colSource = new TableColumn<>("Fonte");
        colSource.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSource()));

        TableColumn<SystemLog, String> colMessage = new TableColumn<>("Mensagem");
        colMessage.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMessage()));

        TableColumn<SystemLog, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
            {
                btnView.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnView.setTooltip(new Tooltip("Ver Detalhes"));
                btnView.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
            }
        });

        table.getColumns().addAll(colLevel, colTimestamp, colCategory, colSource, colMessage, colActions);

        box.getChildren().add(table);
        return box;
    }

    private void loadData() {
        try {
            var page = logService.findAll(org.springframework.data.domain.PageRequest.of(0, 100));
            table.getItems().setAll(page.getContent());

            lblTotalLogs.setText(String.valueOf(page.getTotalElements()));

            Map<String, Long> stats = logService.getStatisticsLast24Hours();
            lblErros24h.setText(String.valueOf(stats.getOrDefault("ERROR", 0L) + stats.getOrDefault("FATAL", 0L)));
            lblWarns24h.setText(String.valueOf(stats.getOrDefault("WARN", 0L)));
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao carregar logs do sistema", e);
        }
    }

    private void showDetails(SystemLog log) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Detalhes do Log");
            alert.setHeaderText(log.getLogLevel().name() + " - " + log.getCategory());

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            content.getChildren().addAll(
                new Label("Data/Hora: " + log.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))),
                new Label("Categoria: " + log.getCategory()),
                new Label("Fonte: " + log.getSource()),
                new Label("Thread: " + log.getThreadName()),
                new Label("Memória: " + log.getMemoryUsedMb() + " MB"),
                new Label("Mensagem: " + log.getMessage()),
                new Separator()
            );

            if (log.getExceptionClass() != null) {
                TitledPane exceptionPane = new TitledPane("Exceção: " + log.getExceptionClass(),
                    new TextArea(log.getStackTrace()));
                exceptionPane.setExpanded(false);
                content.getChildren().add(exceptionPane);
            }

            alert.getDialogPane().setContent(content);
            alert.getDialogPane().setPrefSize(600, 400);
            alert.showAndWait();
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao mostrar detalhes do log", e);
        }
    }

    private void showEmDesenvolvimento(String funcionalidade) {
        AlertUtils.showInfoAlert("Em Desenvolvimento",
            "A funcionalidade '" + funcionalidade + "' está em desenvolvimento.");
    }
}
