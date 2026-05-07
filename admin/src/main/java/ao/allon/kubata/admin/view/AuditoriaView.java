package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.repository.AuditLogRepository;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditoriaView extends VBox {

    private final AuditLogRepository auditLogRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;

    private AdvancedTableView<AuditLog> table;
    private ObservableList<AuditLog> logs;
    private TextField searchField;
    private ComboBox<String> cmbTipo;
    private ComboBox<String> cmbEntidade;
    private DatePicker dateFrom;
    private DatePicker dateTo;
    private Label lblTotal;

    public AuditoriaView(AuditLogRepository auditLogRepository, SessionManager sessionManager, ModalManager modalManager) {
        this.auditLogRepository = auditLogRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;

        logs = FXCollections.observableArrayList();
        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadLogs();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        HBox toolbar = buildToolbar();
        table = buildTable();
        HBox footer = buildFooter();

        getChildren().addAll(toolbar, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(10);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Logs de Auditoria");
        title.getStyleClass().add("h3");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterLogs());

        cmbTipo = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "PERMISSION_CHANGE", "CONFIG_CHANGE", "ACCESS_DENIED"
        ));
        cmbTipo.setValue("Todos");
        cmbTipo.setPrefWidth(140);
        cmbTipo.setOnAction(e -> filterLogs());

        cmbEntidade = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "USER", "EMPRESA", "PERMISSION", "AUTH", "CONFIG"
        ));
        cmbEntidade.setValue("Todos");
        cmbEntidade.setPrefWidth(120);
        cmbEntidade.setOnAction(e -> filterLogs());

        dateFrom = new DatePicker();
        dateFrom.setPromptText("De");
        dateFrom.setPrefWidth(130);
        dateFrom.valueProperty().addListener((obs, oldVal, newVal) -> filterLogs());

        dateTo = new DatePicker();
        dateTo.setPromptText("Até");
        dateTo.setPrefWidth(130);
        dateTo.valueProperty().addListener((obs, oldVal, newVal) -> filterLogs());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().add("button-danger");
        btnLimpar.setOnAction(e -> clearFilters());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().add("button-primary");
        btnAtualizar.setOnAction(e -> loadLogs());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, searchField, cmbTipo, cmbEntidade, dateFrom, dateTo, btnLimpar, btnAtualizar);
        return box;
    }

    private AdvancedTableView<AuditLog> buildTable() {
        AdvancedTableView<AuditLog> tv = new AdvancedTableView<>();
        tv.setData(logs);
        
        TableUtils.standardize(tv);

        TableColumn<AuditLog, LocalDateTime> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getTimestamp()));
        colData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            }
        });
        colData.setPrefWidth(180);

        TableColumn<AuditLog, String> colUtilizador = TableUtils.createTextColumn("Utilizador", col -> new SimpleStringProperty(col.getValue().getUsername()));
        colUtilizador.setPrefWidth(180);

        TableColumn<AuditLog, AuditLog.AuditActionType> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getActionType()));
        colTipo.setCellFactory(col -> new TableCell<AuditLog, AuditLog.AuditActionType>() {
            @Override
            protected void updateItem(AuditLog.AuditActionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String str = item.name();
                    setText(str);
                    String style = "-fx-font-weight: bold;";
                    if (str.contains("LOGIN") || str.contains("CREATE")) style += " -fx-text-fill: green;";
                    else if (str.contains("DELETE") || str.contains("ACCESS_DENIED")) style += " -fx-text-fill: red;";
                    else if (str.contains("UPDATE") || str.contains("CONFIG")) style += " -fx-text-fill: orange;";
                    setStyle(style);
                }
            }
        });
        colTipo.setPrefWidth(150);

        TableColumn<AuditLog, String> colEntidade = TableUtils.createTextColumn("Entidade", col -> new SimpleStringProperty(col.getValue().getEntityType()));
        colEntidade.setPrefWidth(150);

        TableColumn<AuditLog, String> colEntidadeId = TableUtils.createTextColumn("ID Entidade", col -> new SimpleStringProperty(col.getValue().getEntityId()));
        colEntidadeId.setPrefWidth(120);

        TableColumn<AuditLog, String> colIp = TableUtils.createTextColumn("IP", col -> new SimpleStringProperty(col.getValue().getIpAddress()));
        colIp.setPrefWidth(120);

        TableColumn<AuditLog, String> colDescricao = TableUtils.createTextColumn("Descrição", col -> new SimpleStringProperty(col.getValue().getEntityDescription()));
        colDescricao.setPrefWidth(400);

        TableColumn<AuditLog, String> colModulo = TableUtils.createTextColumn("Módulo", col -> new SimpleStringProperty(col.getValue().getModule()));
        colModulo.setPrefWidth(130);

        TableColumn<AuditLog, Boolean> colSucesso = new TableColumn<>("Sucesso");
        colSucesso.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getSuccess()));
        colSucesso.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "✓" : "✗");
                    setStyle(item ? "-fx-text-fill: green; -fx-font-size: 16px;" : "-fx-text-fill: red; -fx-font-size: 16px;");
                }
            }
        });
        colSucesso.setPrefWidth(70);

        TableColumn<AuditLog, String> colErro = TableUtils.createTextColumn("Erro", col -> new SimpleStringProperty(col.getValue().getErrorMessage()));
        colErro.setPrefWidth(250);

        TableColumn<AuditLog, String> colUserAgent = TableUtils.createTextColumn("User Agent", col -> new SimpleStringProperty(col.getValue().getUserAgent()));
        colUserAgent.setPrefWidth(250);

        TableColumn<AuditLog, String> colSession = TableUtils.createTextColumn("Sessão", col -> new SimpleStringProperty(col.getValue().getSessionId()));
        colSession.setPrefWidth(150);

        tv.getColumns().addAll(colData, colUtilizador, colTipo, colEntidade, colEntidadeId, colIp, colModulo, colDescricao, colSucesso, colErro, colUserAgent, colSession);
        return tv;
    }

    private HBox buildFooter() {
        HBox box = new HBox(15);
        box.getStyleClass().add("footer-box");
        box.setAlignment(Pos.CENTER_RIGHT);

        lblTotal = new Label("Total: 0 registos");
        lblTotal.setStyle("-fx-text-fill: #666666;");

        box.getChildren().add(lblTotal);
        return box;
    }

    private void loadLogs() {
        List<AuditLog> all = auditLogRepository.findAll(PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent();
        logs.setAll(all);
        lblTotal.setText("Total: " + logs.size() + " registos");
    }

    private void filterLogs() {
        String search = searchField.getText();
        String tipo = cmbTipo.getValue();
        String entidade = cmbEntidade.getValue();
        LocalDateTime from = dateFrom.getValue() != null ? dateFrom.getValue().atStartOfDay() : null;
        LocalDateTime to = dateTo.getValue() != null ? dateTo.getValue().plusDays(1).atStartOfDay() : null;

        table.setFilter(log -> {
            if (search != null && !search.isBlank()) {
                String lower = search.toLowerCase();
                boolean matchesSearch = (log.getUsername() != null && log.getUsername().toLowerCase().contains(lower)) ||
                                     (log.getEntityDescription() != null && log.getEntityDescription().toLowerCase().contains(lower));
                if (!matchesSearch) return false;
            }
            if (tipo != null && !tipo.equals("Todos")) {
                if (log.getActionType() == null || !log.getActionType().name().equals(tipo)) return false;
            }
            if (entidade != null && !entidade.equals("Todos")) {
                if (log.getEntityType() == null || !log.getEntityType().equals(entidade)) return false;
            }
            if (from != null && log.getTimestamp() != null && log.getTimestamp().isBefore(from)) return false;
            if (to != null && log.getTimestamp() != null && log.getTimestamp().isAfter(to)) return false;
            return true;
        });

        lblTotal.setText("Total: " + table.getItems().size() + " registos");
    }

    private void clearFilters() {
        searchField.clear();
        cmbTipo.setValue("Todos");
        cmbEntidade.setValue("Todos");
        dateFrom.setValue(null);
        dateTo.setValue(null);
        loadLogs();
    }
}
