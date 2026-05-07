package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.service.AuditService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AuditoriaView extends VBox {

    private final AuditService auditService;
    private AdvancedTableView<AuditLog> table;
    private Label lblTotalRegistros;
    private Label lblSaftRelevantes;
    private Label lblUltimos30Dias;

    private ComboBox<AuditLog.AuditActionType> cbActionType;
    private ComboBox<String> cbEntityType;
    private DatePicker dpStartDate;
    private DatePicker dpEndDate;
    private CheckBox chkSaftRelevant;

    public AuditoriaView(AuditService auditService) {
        this.auditService = auditService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("auditoria-view");

        // Header
        HBox header = createHeader();

        // Estatísticas
        HBox stats = createStatsPanel();

        // Filtros
        HBox filters = createFiltersPanel();

        // Tabela
        VBox tableBox = createTablePanel();

        getChildren().addAll(header, stats, filters, tableBox);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        loadData();
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Auditoria do Sistema");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Rastreamento completo de operações - Compatível com normas AGT/SAFT-AO");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExport = new Button("Exportar SAFT-AO", IconUtils.icon(Feather.UPLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnExport.setOnAction(e -> showEmDesenvolvimento("Exportação SAFT-AO"));

        header.getChildren().addAll(titleBox, spacer, btnExport);
        return header;
    }

    private HBox createStatsPanel() {
        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);

        lblTotalRegistros = new Label("0");
        lblSaftRelevantes = new Label("0");
        lblUltimos30Dias = new Label("0");

        stats.getChildren().addAll(
            createStatCard("Total de Registros", lblTotalRegistros, Feather.LIST, Color.BLUE),
            createStatCard("SAFT Relevantes", lblSaftRelevantes, Feather.FILE_TEXT, Color.ORANGE),
            createStatCard("Últimos 30 Dias", lblUltimos30Dias, Feather.CALENDAR, Color.GREEN)
        );

        return stats;
    }

    private VBox createStatCard(String title, Label value, Feather icon, Color color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");
        card.setPrefWidth(150);

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

        cbActionType = new ComboBox<>();
        cbActionType.getItems().add(null);
        cbActionType.getItems().addAll(AuditLog.AuditActionType.values());
        cbActionType.setPromptText("Tipo de Ação");
        cbActionType.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(AuditLog.AuditActionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDescription());
            }
        });
        cbActionType.setButtonCell(cbActionType.getCellFactory().call(null));

        cbEntityType = new ComboBox<>();
        cbEntityType.getItems().addAll(null, "Fatura", "Cliente", "Produto", "Recibo", "Usuario", "Config");
        cbEntityType.setPromptText("Entidade");

        dpStartDate = new DatePicker();
        dpStartDate.setPromptText("Data Início");

        dpEndDate = new DatePicker();
        dpEndDate.setPromptText("Data Fim");

        chkSaftRelevant = new CheckBox("Apenas SAFT");

        Button btnFilter = new Button("Filtrar", IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        btnFilter.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnFilter.setOnAction(e -> applyFilters());

        Button btnClear = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnClear.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnClear.setOnAction(e -> clearFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filters.getChildren().addAll(
            new Label("Filtros:"), cbActionType, cbEntityType, dpStartDate, dpEndDate,
            chkSaftRelevant, btnFilter, btnClear, spacer
        );

        return filters;
    }

    private VBox createTablePanel() {
        VBox box = new VBox(10);

        table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AuditLog, String> colTimestamp = new TableColumn<>("Data/Hora");
        colTimestamp.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        ));

        TableColumn<AuditLog, String> colUser = new TableColumn<>("Usuário");
        colUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Ação");
        colAction.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getActionType() != null ? cell.getValue().getActionType().getDescription() : ""
        ));

        TableColumn<AuditLog, String> colEntity = new TableColumn<>("Entidade");
        colEntity.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getEntityType() + " - " + cell.getValue().getEntityDescription()
        ));

        TableColumn<AuditLog, String> colModule = new TableColumn<>("Módulo");
        colModule.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModule()));

        TableColumn<AuditLog, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            Boolean.TRUE.equals(cell.getValue().getSuccess()) ? "Sucesso" : "Erro"
        ));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    FontIcon icon = IconUtils.icon(
                        "Sucesso".equals(item) ? Feather.CHECK_CIRCLE : Feather.X_CIRCLE,
                        IconUtils.SIZE_SMALL
                    );
                    icon.setFill("Sucesso".equals(item) ? Color.GREEN : Color.RED);
                    setGraphic(icon);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        TableColumn<AuditLog, Void> colActions = new TableColumn<>("Ações");
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

        table.getColumns().addAll(colTimestamp, colUser, colAction, colEntity, colModule, colStatus, colActions);

        box.getChildren().add(table);
        return box;
    }

    private void loadData() {
        try {
            var page = auditService.findAll(org.springframework.data.domain.PageRequest.of(0, 100));
            table.setData(FXCollections.observableArrayList(page.getContent()));

            // Carregar estatísticas
            Map<String, Long> stats = auditService.getStatisticsLast30Days();
            lblUltimos30Dias.setText(String.valueOf(stats.values().stream().mapToLong(Long::longValue).sum()));
            lblTotalRegistros.setText(String.valueOf(page.getTotalElements()));
            lblSaftRelevantes.setText(String.valueOf(
                auditService.findSaftRelevant().size()
            ));
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao carregar dados de auditoria", e);
        }
    }

    private void applyFilters() {
        // Implementação básica - carregar todos e filtrar na UI
        loadData();
    }

    private void clearFilters() {
        cbActionType.setValue(null);
        cbEntityType.setValue(null);
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        chkSaftRelevant.setSelected(false);
        loadData();
    }

    private void showDetails(AuditLog log) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Detalhes da Auditoria");
            alert.setHeaderText(log.getActionType().getDescription() + " - " + log.getEntityType());

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            content.getChildren().addAll(
                new Label("Usuário: " + log.getUsername()),
                new Label("Data/Hora: " + log.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))),
                new Label("Entidade: " + log.getEntityType() + " (" + log.getEntityId() + ")"),
                new Label("Módulo: " + log.getModule()),
                new Label("Status: " + (Boolean.TRUE.equals(log.getSuccess()) ? "Sucesso" : "Falha")),
                new Label("IP: " + (log.getIpAddress() != null ? log.getIpAddress() : "N/A")),
                new Separator()
            );

            if (log.getOldValues() != null && !log.getOldValues().isEmpty()) {
                TitledPane oldPane = new TitledPane("Valores Anteriores", new TextArea(log.getOldValues()));
                oldPane.setExpanded(false);
                content.getChildren().add(oldPane);
            }

            if (log.getNewValues() != null && !log.getNewValues().isEmpty()) {
                TitledPane newPane = new TitledPane("Novos Valores", new TextArea(log.getNewValues()));
                newPane.setExpanded(false);
                content.getChildren().add(newPane);
            }

            alert.getDialogPane().setContent(content);
            alert.showAndWait();
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao mostrar detalhes da auditoria", e);
        }
    }

    private void showEmDesenvolvimento(String funcionalidade) {
        AlertUtils.showInfoAlert("Em Desenvolvimento", 
            "A funcionalidade '" + funcionalidade + "' está em desenvolvimento.");
    }
}
