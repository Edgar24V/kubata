package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.domain.Alerta;
import ao.allon.kubata.core.domain.AlertaHistorico;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AlertaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Lazy
public class AlertCenterView extends BorderPane {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AlertaService alertaService;
    private final SessionManager sessions;
    private final ModalManager modalManager;

    private final TableView<Alerta> table = new TableView<>();
    private final ComboBox<String> estadoFiltro = new ComboBox<>();
    private final ComboBox<String> severidadeFiltro = new ComboBox<>();

    private final Label openCount = metricValue();
    private final Label acknowledgedCount = metricValue();
    private final Label resolvedCount = metricValue();
    private final Label ignoredCount = metricValue();
    private boolean built;

    public AlertCenterView(
            AlertaService alertaService,
            SessionManager sessions,
            ModalManager modalManager) {
        this.alertaService = alertaService;
        this.sessions = sessions;
        this.modalManager = modalManager;
        build();
        refresh();
    }

    private void build() {
        if (built) {
            return;
        }
        built = true;

        getStyleClass().addAll("kubata-server-page", "kubata-platform-center-page");

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 20, 22));
        content.setFillWidth(true);

        content.getChildren().addAll(
                header(),
                summary(),
                filters(),
                actions(),
                tablePanel()
        );

        setCenter(new ScrollPane(content));
        ((ScrollPane) getCenter()).setFitToWidth(true);
        ((ScrollPane) getCenter()).setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ((ScrollPane) getCenter()).setPannable(true);
        ((ScrollPane) getCenter()).getStyleClass().add("kubata-center-content-scroll");
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private VBox header() {
        VBox box = new VBox(4);
        box.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(10);
        line.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", ao.allon.kubata.admin.ui.util.IconUtils.icon(
                Feather.ALERT_TRIANGLE, 22));
        icon.getStyleClass().add("kubata-server-title-icon");

        VBox titles = new VBox(2);
        Label eyebrow = new Label("KUBATA ADMINISTRATOR");
        eyebrow.getStyleClass().add("kubata-center-hero-eyebrow");

        Label title = new Label("Alert Center");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Centralize alertas técnicos e administrativos por severidade, origem, responsável e ciclo de vida.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");

        titles.getChildren().addAll(eyebrow, title, subtitle);
        line.getChildren().addAll(icon, titles);
        box.getChildren().add(line);
        return box;
    }

    private GridPane summary() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(summaryCard("OPEN", openCount, "Alertas que exigem tratamento imediato.", Feather.ALERT_TRIANGLE), 0, 0);
        grid.add(summaryCard("ACKNOWLEDGED", acknowledgedCount, "Ocorrências reconhecidas por um operador.", Feather.EYE), 1, 0);
        grid.add(summaryCard("RESOLVED", resolvedCount, "Alertas encerrados com resolução registada.", Feather.CHECK_CIRCLE), 2, 0);
        grid.add(summaryCard("IGNORED", ignoredCount, "Alertas deliberadamente ignorados.", Feather.MINUS_CIRCLE), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        return grid;
    }

    private VBox summaryCard(String title, Label value, String description, Feather icon) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("kubata-center-overview-card");

        HBox head = new HBox(7);
        head.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", ao.allon.kubata.admin.ui.util.IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("kubata-server-metric-icon");

        Label caption = new Label(title);
        caption.getStyleClass().add("kubata-server-metric-title");

        head.getChildren().addAll(iconLabel, caption);
        value.getStyleClass().add("kubata-center-overview-value");

        Label text = new Label(description);
        text.setWrapText(true);
        text.getStyleClass().add("kubata-center-overview-text");

        card.getChildren().addAll(head, value, text);
        return card;
    }

    private static Label metricValue() {
        return new Label("0");
    }

    private VBox filters() {
        VBox box = new VBox(8);
        box.getStyleClass().add("kubata-center-dashboard-panel");

        Label title = new Label("Filtros e pesquisa");
        title.getStyleClass().add("kubata-server-panel-title");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        estadoFiltro.setItems(FXCollections.observableArrayList(
                "Todos", "OPEN", "ACKNOWLEDGED", "RESOLVED", "IGNORED"));
        estadoFiltro.getSelectionModel().selectFirst();

        severidadeFiltro.setItems(FXCollections.observableArrayList(
                "Todas", "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"));
        severidadeFiltro.getSelectionModel().selectFirst();

        estadoFiltro.setOnAction(e -> refresh());
        severidadeFiltro.setOnAction(e -> refresh());

        row.getChildren().addAll(
                fieldBox("Estado", estadoFiltro),
                fieldBox("Severidade", severidadeFiltro)
        );

        box.getChildren().addAll(title, row);
        return box;
    }

    private VBox fieldBox(String label, Node node) {
        VBox box = new VBox(4);
        Label caption = new Label(label);
        caption.getStyleClass().add("kubata-center-field-label");
        if (node instanceof Region region) {
            region.setPrefWidth(180);
            region.setMaxWidth(220);
        }
        box.getChildren().addAll(caption, node);
        return box;
    }

    private HBox actions() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kubata-center-actionbar");

        Button add = button("Novo alerta", Feather.PLUS, this::createAlert);
        add.getStyleClass().add("button-primary");

        row.getChildren().addAll(
                add,
                button("Reconhecer", Feather.EYE, () -> transition("Reconhecimento",
                        table.getSelectionModel().getSelectedItem(), Action.ACKNOWLEDGE)),
                button("Resolver", Feather.CHECK_CIRCLE, () -> transition("Resolução",
                        table.getSelectionModel().getSelectedItem(), Action.RESOLVE)),
                button("Ignorar", Feather.MINUS_CIRCLE, () -> transition("Ignorar",
                        table.getSelectionModel().getSelectedItem(), Action.IGNORE)),
                button("Reabrir", Feather.REFRESH_CW, () -> transition("Reabertura",
                        table.getSelectionModel().getSelectedItem(), Action.REOPEN)),
                button("Atribuir responsável", Feather.USER_CHECK, () -> assignResponsible()),
                button("Histórico", Feather.LIST, this::showHistory),
                button("Actualizar", Feather.REFRESH_CW, this::refresh)
        );
        return row;
    }

    private VBox tablePanel() {
        VBox box = new VBox(8);
        box.getStyleClass().add("kubata-center-dashboard-panel");

        Label title = new Label("Ocorrências");
        title.getStyleClass().add("kubata-server-panel-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(540);
        table.setMinHeight(360);
        table.setPlaceholder(new Label("Nenhum alerta encontrado."));
        table.getStyleClass().addAll(
                "kubata-infra-table", "kubata-server-properties-table", "kubata-center-table");

        table.getColumns().setAll(
                col("Código", Alerta::getCodigo),
                col("Título", Alerta::getTitulo),
                col("Severidade", a -> a.getSeveridade() == null ? "—" : a.getSeveridade().name()),
                col("Estado", a -> a.getEstado() == null ? "—" : a.getEstado().name()),
                col("Origem", Alerta::getOrigem),
                col("Responsável", a -> userName(a.getResponsavel())),
                col("Abertura", a -> fmt(a.getOpenedAt())),
                col("Resolver", a -> userName(a.getResolvidoPor()))
        );

        table.setRowFactory(tv -> {
            TableRow<Alerta> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showHistory();
                }
            });
            return row;
        });

        box.getChildren().addAll(title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private <T> TableColumn<Alerta, String> col(
            String title,
            java.util.function.Function<Alerta, String> mapper) {
        TableColumn<Alerta, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data ->
                new SimpleStringProperty(safe(mapper.apply(data.getValue()))));
        return column;
    }

    private void createAlert() {
        User actor = actorOrReport();
        if (actor == null) return;

        List<User> responsaveis;
        try {
            responsaveis = alertaService.responsaveis(actor);
        } catch (Exception ex) {
            showError("Alert Center", ex);
            return;
        }

        TextField codigo = new TextField();
        codigo.setPromptText("Ex.: SEC_LOGIN_LOCKOUT");

        TextField titulo = new TextField();
        titulo.setPromptText("Título curto e identificável");

        TextArea descricao = new TextArea();
        descricao.setPromptText("Descreva o problema, impacto e contexto.");
        descricao.setPrefRowCount(5);
        descricao.setWrapText(true);

        ComboBox<Alerta.Severidade> severidade =
                new ComboBox<>(FXCollections.observableArrayList(Alerta.Severidade.values()));
        severidade.getSelectionModel().select(Alerta.Severidade.MEDIUM);

        TextField origem = new TextField("ADMINISTRATOR");

        TextField referencia = new TextField();
        referencia.setPromptText("ID, operação, módulo ou referência externa (opcional)");

        ComboBox<User> responsavel =
                new ComboBox<>(FXCollections.observableArrayList(responsaveis));
        responsavel.setPromptText("Opcional");

        GridPane grid = form();
        field(grid, 0, "Código", codigo);
        field(grid, 1, "Título", titulo);
        field(grid, 2, "Descrição", descricao);
        field(grid, 3, "Severidade", severidade);
        field(grid, 4, "Origem", origem);
        field(grid, 5, "Referência", referencia);
        field(grid, 6, "Responsável", responsavel);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
                .title("Novo alerta")
                .subtitle("Registar ocorrência no Alert Center")
                .icon(Feather.ALERT_TRIANGLE)
                .scrollable(true)
                .maximizable(false)
                .withConfirmButtons("Criar", "Cancelar")
                .onConfirm(() -> {
                    try {
                        alertaService.criar(actor,
                                codigo.getText(),
                                titulo.getText(),
                                descricao.getText(),
                                severidade.getValue(),
                                origem.getText(),
                                referencia.getText(),
                                responsavel.getValue());
                        show("Alert Center", "Alerta criado com sucesso.");
                        refresh();
                    } catch (Exception ex) {
                        showError("Novo alerta", ex);
                    }
                }));
    }

    private void transition(String title, Alerta alert, Action action) {
        User actor = actorOrReport();
        if (actor == null) return;

        if (alert == null) {
            show("Alert Center", "Seleccione um alerta.");
            return;
        }

        TextArea noteField = new TextArea();
        noteField.setPromptText("Observação (opcional)");
        noteField.setPrefRowCount(3);
        noteField.setWrapText(true);

        VBox box = new VBox(9, new Label("Observação (opcional):"), noteField);

        modalManager.showModal(box, new ModalManager.ModalConfig()
                .title(title)
                .subtitle(alert.getCodigo())
                .icon(Feather.EDIT_3)
                .maximizable(false)
                .withConfirmButtons("Confirmar", "Cancelar")
                .onConfirm(() -> {
                    String note = noteField.getText();
                    try {
                        switch (action) {
                            case ACKNOWLEDGE -> alertaService.reconhecer(actor, alert.getId(), note);
                            case RESOLVE -> alertaService.resolver(actor, alert.getId(), note);
                            case IGNORE -> alertaService.ignorar(actor, alert.getId(), note);
                            case REOPEN -> alertaService.reabrir(actor, alert.getId(), note);
                        }
                        show("Alert Center", title + " concluída.");
                        refresh();
                    } catch (Exception ex) {
                        showError(title, ex);
                    }
                }));
    }

    private void assignResponsible() {
        User actor = actorOrReport();
        if (actor == null) return;

        Alerta alert = table.getSelectionModel().getSelectedItem();
        if (alert == null) {
            show("Alert Center", "Seleccione um alerta.");
            return;
        }

        List<User> responsaveis;
        try {
            responsaveis = alertaService.responsaveis(actor);
        } catch (Exception ex) {
            showError("Responsáveis", ex);
            return;
        }

        ComboBox<User> combo =
                new ComboBox<>(FXCollections.observableArrayList(responsaveis));
        combo.setPromptText("Seleccione o responsável");
        combo.setMaxWidth(Double.MAX_VALUE);
        if (alert.getResponsavel() != null) {
            responsaveis.stream()
                    .filter(u -> u.getId() != null && u.getId().equals(alert.getResponsavel().getId()))
                    .findFirst()
                    .ifPresent(combo.getSelectionModel()::select);
        }

        TextArea note = new TextArea();
        note.setPromptText("Motivo/observação");
        note.setPrefRowCount(4);
        note.setWrapText(true);

        VBox box = new VBox(9,
                new Label("Responsável"), combo,
                new Label("Observação"), note);
        box.setPadding(new Insets(6));

        modalManager.showModal(box, new ModalManager.ModalConfig()
                .title("Atribuir responsável")
                .subtitle(alert.getCodigo())
                .icon(Feather.USER_CHECK)
                .maximizable(false)
                .withConfirmButtons("Atribuir", "Cancelar")
                .onConfirm(() -> {
                    try {
                        alertaService.atribuirResponsavel(actor, alert.getId(), combo.getValue(), note.getText());
                        show("Alert Center", "Responsável actualizado.");
                        refresh();
                    } catch (Exception ex) {
                        showError("Responsável", ex);
                    }
                }));
    }

    private void showHistory() {
        User actor = actorOrReport();
        if (actor == null) return;

        Alerta alert = table.getSelectionModel().getSelectedItem();
        if (alert == null) {
            show("Histórico", "Seleccione um alerta.");
            return;
        }

        try {
            List<AlertaHistorico> history = alertaService.historico(actor, alert.getId());
            ListView<AlertaHistorico> historyView = new ListView<>(
                    FXCollections.observableArrayList(history));
            historyView.setPrefHeight(420);
            historyView.setMinHeight(300);
            historyView.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(AlertaHistorico item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    String actorName = item.getUtilizador() == null
                            ? "SYSTEM"
                            : safe(item.getUtilizador().getNome());
                    String state = stateText(item.getEstadoAnterior()) + " → "
                            + stateText(item.getEstadoNovo());
                    setText(fmt(item.getEventAt()) + " · "
                            + actionText(item.getAcao()) + " · "
                            + state + " · " + actorName + " · "
                            + safe(item.getObservacao()));
                }
            });

            modalManager.showModal(
                    historyView,
                    new ModalManager.ModalConfig()
                            .title("Histórico · " + alert.getCodigo())
                            .subtitle("Ciclo de vida e intervenções registadas no servidor.")
                            .icon(Feather.LIST)
                            .size(760, 560)
                            .minSize(680, 460)
                            .scrollable(true)
                            .singleButton("Fechar")
            );
        } catch (Exception ex) {
            showError("Histórico", ex);
        }
    }

    private void refresh() {
        User actor = actorOrReport();
        if (actor == null) return;

        try {
            List<Alerta> all = alertaService.listar(actor);
            String estado = estadoFiltro.getValue();
            String severidade = severidadeFiltro.getValue();

            List<Alerta> filtered = new ArrayList<>();
            for (Alerta alert : all) {
                boolean stateOk = estado == null || "Todos".equals(estado)
                        || alert.getEstado() == Alerta.Estado.valueOf(estado);
                boolean severityOk = severidade == null || "Todas".equals(severidade)
                        || alert.getSeveridade() == Alerta.Severidade.valueOf(severidade);
                if (stateOk && severityOk) {
                    filtered.add(alert);
                }
            }

            table.setItems(FXCollections.observableArrayList(filtered));

            Map<Alerta.Estado, Long> summary = alertaService.resumo(actor);
            openCount.setText(String.valueOf(summary.getOrDefault(Alerta.Estado.OPEN, 0L)));
            acknowledgedCount.setText(String.valueOf(
                    summary.getOrDefault(Alerta.Estado.ACKNOWLEDGED, 0L)));
            resolvedCount.setText(String.valueOf(
                    summary.getOrDefault(Alerta.Estado.RESOLVED, 0L)));
            ignoredCount.setText(String.valueOf(
                    summary.getOrDefault(Alerta.Estado.IGNORED, 0L)));
        } catch (Exception ex) {
            table.setItems(FXCollections.observableArrayList());
            showError("Alert Center", ex);
        }
    }

    private User actorOrReport() {
        User actor = sessions.getUser();
        if (actor == null) {
            show("Alert Center", "Sessão administrativa não autenticada.");
            return null;
        }
        return actor;
    }

    private void showError(String title, Exception ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        modalManager.alert(title, message, "error", null);
    }

    private void show(String title, String message) {
        modalManager.alert(title, message == null ? "" : message, "info", null);
    }

    private Button button(String title, Feather icon, Runnable action) {
        Button button = new Button(title, ao.allon.kubata.admin.ui.util.IconUtils.icon(icon, 12));
        button.getStyleClass().add("button-outlined");
        button.setOnAction(e -> action.run());
        return button;
    }

    private GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(6));
        grid.getStyleClass().add("kubata-center-form");

        ColumnConstraints label = new ColumnConstraints(160);
        ColumnConstraints value = new ColumnConstraints();
        value.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, value);
        return grid;
    }

    private void field(GridPane grid, int row, String title, Node value) {
        Label label = new Label(title);
        label.getStyleClass().add("kubata-center-field-label");
        if (value instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String actionText(AlertaHistorico.Acao action) {
        if (action == null) return "—";
        return switch (action) {
            case CREATED -> "Criado";
            case UPDATED -> "Actualização";
            case ACKNOWLEDGED -> "Reconhecido";
            case RESOLVED -> "Resolvido";
            case IGNORED -> "Ignorado";
            case REOPENED -> "Reaberto";
            case ASSIGNED -> "Responsável alterado";
            case MIGRATED -> "Migrado";
        };
    }

    private String stateText(Alerta.Estado state) {
        return state == null ? "—" : state.name();
    }

    private String userName(User user) {
        return user == null ? "—" : safe(user.getNome());
    }

    private String fmt(LocalDateTime value) {
        return value == null ? "—" : DT.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private enum Action {
        ACKNOWLEDGE,
        RESOLVE,
        IGNORE,
        REOPEN
    }
}
