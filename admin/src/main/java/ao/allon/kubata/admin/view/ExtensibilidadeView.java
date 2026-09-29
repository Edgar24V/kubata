package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.extensibilidade.AplicacaoAdministrador;
import ao.allon.kubata.admin.extensibilidade.AplicacaoConfiguravel;
import ao.allon.kubata.admin.extensibilidade.AdministradorExtensibilidadeRegistry;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Central profissional de extensibilidade do Kubata Administrator.
 *
 * <p>Permite gerir o catálogo de aplicações externas através do contrato de
 * extensibilidade já existente, visualizar capacidades de segurança/operação/
 * serviços e remover integrações registadas.</p>
 */
@Component
public class ExtensibilidadeView extends VBox {

    private final AdministradorExtensibilidadeRegistry registry;
    private final ModalManager modalManager;

    private final ObservableList<AplicacaoAdministrador> apps =
            FXCollections.observableArrayList();

    private AdvancedTableView<AplicacaoAdministrador> table;
    private TextField searchField;

    private Label totalValue;
    private Label securityValue;
    private Label servicesValue;
    private Label operationsValue;

    private Label detailName;
    private Label detailCode;
    private Label detailSecurity;
    private Label detailOperations;
    private Label detailServices;
    private Label detailAuditLog;
    private Label detailStatus;

    private Button btnDetails;
    private Button btnRemove;

    public ExtensibilidadeView(
            AdministradorExtensibilidadeRegistry registry,
            ModalManager modalManager
    ) {
        this.registry = registry;
        this.modalManager = modalManager;

        setSpacing(0);
        getStyleClass().add("kubata-extensibility-page");

        buildUI();
        refreshList();
    }

    private void buildUI() {
        VBox header = buildHeader();

        BorderPane workspace = new BorderPane();
        workspace.setTop(buildToolbar());

        SplitPane split = new SplitPane(
                new StackPane(buildTable()),
                buildDetailsPane()
        );
        split.setDividerPositions(0.68);
        workspace.setCenter(split);

        getChildren().addAll(header, workspace, buildStatusBar());
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(18, 22, 15, 22));
        header.getStyleClass().add("kubata-extensibility-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-extensibility-title-icon");
        iconBox.getChildren().add(
                new Label("", IconUtils.icon(Feather.LAYERS, 22))
        );

        VBox titleBox = new VBox(2);
        Label title = new Label("Extensibilidade");
        title.getStyleClass().add("kubata-extensibility-title");

        Label subtitle = new Label(
                "Centro de integração para aplicações externas, contratos, segurança e serviços do Administrator."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-extensibility-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button add = new Button(
                "Adicionar aplicação",
                IconUtils.icon(Feather.PLUS, 13)
        );
        add.getStyleClass().add("button-primary");
        add.setOnAction(e -> showRegisterDialog());

        Button refresh = new Button(
                "",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.setTooltip(new Tooltip("Actualizar catálogo"));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> refreshList());

        titleLine.getChildren().addAll(iconBox, titleBox, spacer, add, refresh);

        totalValue = new Label("0");
        securityValue = new Label("0");
        servicesValue = new Label("0");
        operationsValue = new Label("0");

        HBox kpis = new HBox(
                10,
                kpi("APLICAÇÕES", Feather.GRID, totalValue),
                kpi("SEGURANÇA", Feather.SHIELD, securityValue),
                kpi("SERVIÇOS", Feather.ZAP, servicesValue),
                kpi("OPERAÇÕES", Feather.ACTIVITY, operationsValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.setMinWidth(155);
        card.setPadding(new Insets(9, 13, 9, 13));
        card.getStyleClass().add("kubata-extensibility-kpi");

        HBox line = new HBox(7);
        line.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 13));
        iconLabel.getStyleClass().add("kubata-extensibility-kpi-icon");

        Label caption = new Label(title);
        caption.getStyleClass().add("kubata-extensibility-kpi-title");

        line.getChildren().addAll(iconLabel, caption);

        value.getStyleClass().add("kubata-extensibility-kpi-value");
        card.getChildren().addAll(line, value);
        return card;
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(9);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(9, 14, 9, 14));
        toolbar.getStyleClass().add("kubata-extensibility-toolbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome ou código...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, value) -> applyFilter());

        Button clear = new Button(
                "Limpar",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> searchField.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Duplo clique para ver os detalhes");
        hint.getStyleClass().add("kubata-extensibility-toolbar-hint");

        toolbar.getChildren().addAll(searchField, clear, spacer, hint);
        return toolbar;
    }

    private AdvancedTableView<AplicacaoAdministrador> buildTable() {
        table = new AdvancedTableView<>(apps);
        table.setEntityName("Aplicação");
        table.setPlaceholder(new Label("Nenhuma aplicação externa registada."));
        TableUtils.standardize(table);

        TableColumn<AplicacaoAdministrador, String> code =
                TableUtils.createTextColumn(
                        "Código",
                        c -> new SimpleStringProperty(safe(c.getValue().getAbreviatura()))
                );
        code.setPrefWidth(80);

        TableColumn<AplicacaoAdministrador, String> name =
                TableUtils.createTextColumn(
                        "Aplicação",
                        c -> new SimpleStringProperty(safe(c.getValue().getNome()))
                );
        name.setPrefWidth(260);

        TableColumn<AplicacaoAdministrador, String> security =
                TableUtils.createTextColumn(
                        "Segurança",
                        c -> new SimpleStringProperty(
                                c.getValue().getAudit() == null ? "N/D" : "Disponível"
                        )
                );
        security.setPrefWidth(105);

        TableColumn<AplicacaoAdministrador, String> operations =
                TableUtils.createTextColumn(
                        "Operações",
                        c -> new SimpleStringProperty(
                                operationCount(c.getValue())
                        )
                );
        operations.setPrefWidth(95);

        TableColumn<AplicacaoAdministrador, String> services =
                TableUtils.createTextColumn(
                        "Serviços",
                        c -> new SimpleStringProperty(
                                c.getValue().getServicos() == null ? "N/D" : "Disponível"
                        )
                );
        services.setPrefWidth(105);

        TableColumn<AplicacaoAdministrador, String> auditLog =
                TableUtils.createTextColumn(
                        "Auditoria",
                        c -> new SimpleStringProperty(
                                c.getValue().getOperacoesLog() == null ? "N/D" : "Disponível"
                        )
                );
        auditLog.setPrefWidth(100);

        table.getColumns().addAll(
                code, name, security, operations, services, auditLog
        );

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateDetails(selected));

        table.setOnViewDetails(selected -> {
            if (selected != null) {
                table.getSelectionModel().select(selected);
                showSelectedDetails(selected);
            }
        });
        table.setOnDelete(selected -> removeSelected());
        table.setRowFactory(view -> {
            TableRow<AplicacaoAdministrador> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showSelectedDetails();
                }
            });
            return row;
        });

        return table;
    }

    private ScrollPane buildDetailsPane() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("kubata-extensibility-details");

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-extensibility-detail-icon");
        icon.getChildren().add(
                new Label("", IconUtils.icon(Feather.LAYERS, 18))
        );

        VBox identityText = new VBox(2);
        detailName = new Label("Nenhuma aplicação seleccionada");
        detailName.getStyleClass().add("kubata-extensibility-detail-title");

        detailCode = new Label("Seleccione uma aplicação no catálogo.");
        detailCode.getStyleClass().add("kubata-extensibility-detail-subtitle");

        identityText.getChildren().addAll(detailName, detailCode);
        identity.getChildren().addAll(icon, identityText);

        detailSecurity = detailItem(root, "SEGURANÇA", "—");
        detailOperations = detailItem(root, "OPERAÇÕES", "—");
        detailServices = detailItem(root, "SERVIÇOS", "—");
        detailAuditLog = detailItem(root, "AUDITORIA", "—");
        detailStatus = detailItem(root, "ESTADO", "—");

        Separator separator = new Separator();

        Label capabilities = new Label("Capacidades expostas");
        capabilities.getStyleClass().add("kubata-extensibility-section-title");

        VBox governance = new VBox(7);
        governance.getStyleClass().add("kubata-extensibility-info-card");

        Label governanceTitle = new Label("Contrato de integração");
        governanceTitle.getStyleClass().add("kubata-extensibility-info-title");

        Label governanceText = new Label(
                "A aplicação externa é integrada através das interfaces do Administrator. "
                        + "As capacidades apresentadas aqui correspondem ao contrato fornecido "
                        + "pela integração, e não a uma certificação ou validação externa."
        );
        governanceText.setWrapText(true);
        governanceText.getStyleClass().add("kubata-extensibility-info-text");

        governance.getChildren().addAll(governanceTitle, governanceText);

        btnDetails = new Button(
                "Abrir detalhes completos",
                IconUtils.icon(Feather.INFO, 12)
        );
        btnDetails.getStyleClass().add("button-outlined");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> showSelectedDetails());

        btnRemove = new Button(
                "Remover aplicação",
                IconUtils.icon(Feather.TRASH_2, 12)
        );
        btnRemove.getStyleClass().add("button-danger");
        btnRemove.setMaxWidth(Double.MAX_VALUE);
        btnRemove.setOnAction(e -> removeSelected());

        root.getChildren().addAll(
                identity,
                new Separator(),
                detailSecurity,
                detailOperations,
                detailServices,
                detailAuditLog,
                detailStatus,
                separator,
                capabilities,
                governance,
                btnDetails,
                btnRemove
        );

        updateDetails(null);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-extensibility-details-scroll");
        return scroll;
    }

    private Label detailItem(VBox root, String caption, String initial) {
        VBox row = new VBox(2);
        row.getStyleClass().add("kubata-extensibility-detail-row");

        Label title = new Label(caption);
        title.getStyleClass().add("kubata-extensibility-detail-label");

        Label value = new Label(initial);
        value.setWrapText(true);
        value.getStyleClass().add("kubata-extensibility-detail-value");

        row.getChildren().addAll(title, value);
        root.getChildren().add(row);
        return value;
    }

    private void updateDetails(AplicacaoAdministrador app) {
        if (detailName == null) return;

        if (app == null) {
            detailName.setText("Nenhuma aplicação seleccionada");
            detailCode.setText("Seleccione uma aplicação no catálogo.");
            detailSecurity.setText("—");
            detailOperations.setText("—");
            detailServices.setText("—");
            detailAuditLog.setText("—");
            detailStatus.setText("A aguardar selecção");

            if (btnDetails != null) btnDetails.setDisable(true);
            if (btnRemove != null) btnRemove.setDisable(true);
            return;
        }

        detailName.setText(safe(app.getNome(), "Aplicação"));
        detailCode.setText("Código · " + safe(app.getAbreviatura(), "—"));
        detailSecurity.setText(
                app.getAudit() == null ? "Não disponibilizada" : "Disponibilizada"
        );
        detailOperations.setText(
                app.getOperacoesAplicacao() == null
                        ? "Não disponibilizadas"
                        : operationList(app)
        );
        detailServices.setText(
                app.getServicos() == null
                        ? "Não disponibilizados"
                        : "Contrato de serviços disponível"
        );
        detailAuditLog.setText(
                app.getOperacoesLog() == null
                        ? "Não disponibilizada"
                        : "Entidades de auditoria disponíveis"
        );
        detailStatus.setText("Registada no catálogo local");

        if (btnDetails != null) btnDetails.setDisable(false);
        if (btnRemove != null) btnRemove.setDisable(false);
    }

    private void showRegisterDialog() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(11);
        grid.setPadding(new Insets(8));

        TextField nome = new TextField();
        nome.setPromptText("Ex.: Gestão de Activos Fixos");
        nome.setPrefWidth(360);

        TextField code = new TextField();
        code.setPromptText("Ex.: AFX");
        code.setPrefColumnCount(7);

        Label guidance = new Label(
                "Use exactamente 3 caracteres alfanuméricos. O código identifica a integração "
                        + "dentro do Administrator e não deve coincidir com abreviaturas reservadas."
        );
        guidance.setWrapText(true);
        guidance.getStyleClass().add("kubata-extensibility-dialog-hint");

        grid.add(label("Nome da aplicação"), 0, 0);
        grid.add(nome, 1, 0);
        grid.add(label("Código"), 0, 1);
        grid.add(code, 1, 1);
        grid.add(guidance, 1, 2);

        modalManager.showConfirmModal(
                grid,
                "Nova aplicação externa",
                () -> {
                    try {
                        String appName = safe(nome.getText()).trim();
                        String appCode = safe(code.getText()).trim().toUpperCase(Locale.ROOT);

                        if (appName.isBlank()) {
                            throw new IllegalArgumentException(
                                    "O nome da aplicação é obrigatório."
                            );
                        }

                        if (!appCode.matches("[A-Z0-9]{3}")) {
                            throw new IllegalArgumentException(
                                    "O código deve ter exactamente 3 caracteres alfanuméricos."
                            );
                        }

                        AplicacaoAdministrador app =
                                new AplicacaoConfiguravel(appName, appCode);

                        registry.registarAplicacao(app);
                        refreshList();

                        modalManager.alert(
                                "Aplicação registada",
                                "A integração «" + appName
                                        + "» foi adicionada ao catálogo de extensibilidade.",
                                "success",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Não foi possível registar",
                                message(ex, "Verifique os dados da aplicação."),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private void showSelectedDetails(AplicacaoAdministrador selected) {
        AplicacaoAdministrador app = selected != null ? selected : getSelected();
        if (app == null) {
            modalManager.alert(
                    "Extensibilidade",
                    "Seleccione uma aplicação para consultar os detalhes.",
                    "warning",
                    null
            );
            return;
        }
        VBox content = new VBox(12);
        content.setPadding(new Insets(4));

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-extensibility-detail-icon");
        icon.getChildren().add(
                new Label("", IconUtils.icon(Feather.LAYERS, 18))
        );

        VBox titleBox = new VBox(2);
        Label title = new Label(app.getNome());
        title.getStyleClass().add("kubata-extensibility-detail-title");

        Label code = new Label("Código · " + safe(app.getAbreviatura(), "—"));
        code.getStyleClass().add("kubata-extensibility-detail-subtitle");

        titleBox.getChildren().addAll(title, code);
        identity.getChildren().addAll(icon, titleBox);

        content.getChildren().addAll(
                identity,
                capabilityCard(
                        Feather.SHIELD,
                        "Segurança",
                        app.getAudit() == null
                                ? "A integração não disponibiliza políticas de segurança."
                                : "Políticas de segurança disponíveis: "
                                + String.join(", ", app.getAudit().getApplicationRoles())
                ),
                capabilityCard(
                        Feather.ACTIVITY,
                        "Operações",
                        app.getOperacoesAplicacao() == null
                                ? "Nenhuma operação declarada."
                                : String.join(
                                        ", ",
                                        app.getOperacoesAplicacao().getOperacoesDisponiveis()
                                )
                ),
                capabilityCard(
                        Feather.ZAP,
                        "Serviços",
                        app.getServicos() == null
                                ? "Contrato de serviços não disponibilizado."
                                : "Inicialização e encerramento de serviços suportados."
                ),
                capabilityCard(
                        Feather.FILE_TEXT,
                        "Auditoria",
                        app.getOperacoesLog() == null
                                ? "Entidades de auditoria não declaradas."
                                : String.join(
                                        ", ",
                                        app.getOperacoesLog().getEntidadesLog()
                                )
                )
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-extensibility-details-scroll");

        modalManager.showModal(
                scroll,
                new ModalManager.ModalConfig()
                        .size(700, 560)
                        .minSize(600, 480)
                        .title("Detalhes da integração")
                        .icon(Feather.LAYERS)
        );
    }

    private VBox capabilityCard(Feather icon, String title, String text) {
        VBox card = new VBox(5);
        card.getStyleClass().add("kubata-extensibility-capability-card");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 13));
        iconLabel.getStyleClass().add("kubata-extensibility-capability-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-extensibility-capability-title");

        heading.getChildren().addAll(iconLabel, titleLabel);

        Label body = new Label(text);
        body.setWrapText(true);
        body.getStyleClass().add("kubata-extensibility-capability-text");

        card.getChildren().addAll(heading, body);
        return card;
    }

    private void removeSelected() {
        AplicacaoAdministrador app = getSelected();
        if (app == null) {
            modalManager.alert(
                    "Extensibilidade",
                    "Seleccione uma aplicação para remover.",
                    "warning",
                    null
            );
            return;
        }

        modalManager.showConfirmModal(
                new Label(
                        "Confirma a remoção de «" + app.getNome()
                                + "» (" + app.getAbreviatura() + ")?"
                ),
                "Remover aplicação",
                () -> {
                    try {
                        registry.removerAplicacao(app.getAbreviatura());
                        refreshList();

                        modalManager.alert(
                                "Aplicação removida",
                                "A integração foi removida do catálogo local.",
                                "success",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Erro ao remover",
                                message(ex, "Não foi possível remover a aplicação."),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private AplicacaoAdministrador getSelected() {
        return table == null
                ? null
                : table.getSelectionModel().getSelectedItem();
    }

    private void refreshList() {
        apps.setAll(registry.getAplicacoesRegistadas());
        applyFilter();
        updateKpis();
        updateDetails(getSelected());
    }

    private void applyFilter() {
        if (table == null || searchField == null) return;

        String q = safe(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        table.setFilter(app -> app != null
                && (q.isBlank()
                || safe(app.getNome()).toLowerCase(Locale.ROOT).contains(q)
                || safe(app.getAbreviatura()).toLowerCase(Locale.ROOT).contains(q)));
    }

    private void updateKpis() {
        totalValue.setText(String.valueOf(apps.size()));
        securityValue.setText(String.valueOf(
                apps.stream().filter(app -> app.getAudit() != null).count()
        ));
        servicesValue.setText(String.valueOf(
                apps.stream().filter(app -> app.getServicos() != null).count()
        ));
        operationsValue.setText(String.valueOf(
                apps.stream()
                        .filter(app -> app.getOperacoesAplicacao() != null)
                        .mapToLong(app -> app.getOperacoesAplicacao()
                                .getOperacoesDisponiveis().size())
                        .sum()
        ));
    }

    private String operationCount(AplicacaoAdministrador app) {
        return app.getOperacoesAplicacao() == null
                ? "0"
                : String.valueOf(
                        app.getOperacoesAplicacao()
                                .getOperacoesDisponiveis()
                                .size()
                );
    }

    private String operationList(AplicacaoAdministrador app) {
        if (app.getOperacoesAplicacao() == null) {
            return "Nenhuma";
        }
        List<String> values = app.getOperacoesAplicacao().getOperacoesDisponiveis();
        return values.isEmpty() ? "Nenhuma declarada" : String.join(", ", values);
    }

    private Label label(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("kubata-extensibility-field-label");
        return label;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        String result = safe(value);
        return result.isBlank() ? fallback : result;
    }

    private String message(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? fallback
                : ex.getMessage();
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.getStyleClass().add("kubata-extensibility-statusbar");

        Label left = new Label();
        left.getStyleClass().add("kubata-extensibility-status-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label(
                "Integrações externas são mantidas pelo catálogo de extensibilidade."
        );
        right.getStyleClass().add("kubata-extensibility-status-hint");

        bar.getChildren().addAll(left, spacer, right);

        apps.addListener((javafx.collections.ListChangeListener<AplicacaoAdministrador>) change ->
                left.setText(apps.size() + " aplicação(ões) registada(s)")
        );

        left.setText(apps.size() + " aplicação(ões) registada(s)");
        return bar;
    }
}
