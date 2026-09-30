package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PlataformaAutomationService;
import ao.allon.kubata.admin.service.PlataformaRuntimeService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.AdmPlataformaItem;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Studio operacional dos motores que alimentam o Centro da Plataforma.
 */
@Component
public class PlataformaMotoresView extends BorderPane {
    private final PlataformaRuntimeService runtime;
    private final PlataformaAutomationService automation;
    private final SessionManager sessions;
    private final Environment environment;
    private final ModalManager modalManager;
    private final TabPane tabs = new TabPane();
    private final Label engineState = new Label("Motores disponíveis · configuração carregada");

    public PlataformaMotoresView(
            PlataformaRuntimeService runtime,
            PlataformaAutomationService automation,
            SessionManager sessions,
            Environment environment,
            ModalManager modalManager) {
        this.runtime = runtime;
        this.automation = automation;
        this.sessions = sessions;
        this.environment = environment;
        this.modalManager = modalManager;
        build();
    }

    private void build() {
        getStyleClass().add("kubata-server-page");

        VBox header = buildHeader();

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("kubata-infra-tabs");
        tabs.getTabs().addAll(
                tab("Resumo", Feather.CPU, resumo()),
                tab("Extensibilidade", Feather.CPU, extensibility()),
                tab("Listagens", Feather.LIST, listagens()),
                tab("Mapas", Feather.MAP, mapas()),
                tab("Pesquisa", Feather.SEARCH, pesquisa()),
                tab("Eventos", Feather.ACTIVITY, eventos()),
                tab("Calendário", Feather.CALENDAR, calendario()),
                tab("Dashboard", Feather.BAR_CHART_2, dashboard()),
                tab("Anexos", Feather.PAPERCLIP, anexos()),
                tab("BD", Feather.DATABASE, basesDados()),
                tab("Licença & Layout", Feather.SHIELD, licencaLayout())
        );

        setTop(header);
        setCenter(tabs);
        setBottom(buildFooter());
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(20, 22, 16, 22));
        header.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-server-title-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.CPU, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Motores Runtime");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Centro de administração dos motores runtime da plataforma."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");

        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> {
            refreshCurrent();
            engineState.setText("Motores actualizados · " + java.time.LocalTime.now().withSecond(0).withNano(0));
        });

        Button summary = new Button(
                "Resumo",
                IconUtils.icon(Feather.HOME, 13)
        );
        summary.getStyleClass().add("button-outlined");
        summary.setOnAction(e -> tabs.getSelectionModel().selectFirst());

        line.getChildren().addAll(icon, titles, spacer, summary, refresh);

        HBox status = new HBox(9);
        status.setAlignment(Pos.CENTER_LEFT);

        Label statusIcon = new Label("", IconUtils.icon(Feather.CPU, 13));
        statusIcon.getStyleClass().add("kubata-server-status-icon");

        status.getStyleClass().add("kubata-server-status-bar");
        engineState.getStyleClass().add("kubata-server-status-value");
        status.getChildren().addAll(
                statusIcon,
                new Label("ESTADO DOS MOTORES"),
                engineState
        );

        header.getChildren().addAll(line, status);
        return header;
    }

    private GridPane engineMetrics() {
        GridPane metrics = new GridPane();
        metrics.setHgap(12);
        metrics.setVgap(12);

        metrics.add(metricCard("Personalização", "PERSONALIZACAO", Feather.CPU), 0, 0);
        metrics.add(metricCard("Listagens", "LISTAGEM", Feather.LIST), 1, 0);
        metrics.add(metricCard("Mapas", "MAPA", Feather.MAP), 2, 0);
        metrics.add(metricCard("Eventos", "EVENTO_REGRA", Feather.ACTIVITY), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(Priority.ALWAYS);
            column.setPercentWidth(25);
            metrics.getColumnConstraints().add(column);
        }

        return metrics;
    }

    private VBox metricCard(String title, String type, Feather icon) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(13, 15, 13, 15));
        card.getStyleClass().add("kubata-server-metric");

        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 14));
        i.getStyleClass().add("kubata-server-metric-icon");

        Label t = new Label(title.toUpperCase(Locale.ROOT));
        t.getStyleClass().add("kubata-server-metric-title");

        Label value = new Label(String.valueOf(automation.list(type).size()));
        value.getStyleClass().add("kubata-server-metric-value");

        row.getChildren().addAll(i, t);
        card.getChildren().addAll(row, value);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Tab tab(String text, Feather icon, Node node) {
        Tab t = new Tab(text, node instanceof ScrollPane ? node : scroll(node));
        t.setGraphic(IconUtils.icon(icon, 13));
        return t;
    }

    private VBox page() {
        VBox v = new VBox(14);
        v.setPadding(new Insets(0, 22, 18, 22));
        v.setFillWidth(true);
        v.getStyleClass().add("kubata-server-content");
        return v;
    }

    private ScrollPane scroll(Node node) {
        ScrollPane s = new ScrollPane(node);
        s.setFitToWidth(true);
        s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        s.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        s.setPannable(true);
        s.setFocusTraversable(false);
        s.getStyleClass().add("kubata-server-scroll");
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return s;
    }

    private Button button(String text, Feather icon, Runnable action) {
        Button b = new Button(text, IconUtils.icon(icon, 12));
        b.getStyleClass().add("button-outlined");
        b.setOnAction(e -> action.run());
        return b;
    }

    private VBox card(Node... nodes) {
        VBox v = new VBox(10);
        v.setPadding(new Insets(15));
        v.getStyleClass().add("kubata-server-panel");
        v.getChildren().addAll(nodes);
        return v;
    }

    private VBox section(String title, String description) {
        Label a = new Label(title);
        a.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");
        Label b = new Label(description);
        b.setWrapText(true);
        b.getStyleClass().add("text-muted");
        return new VBox(3, a, b);
    }

    private GridPane form() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(9);
        g.setPadding(new Insets(4));
        ColumnConstraints l = new ColumnConstraints(160);
        ColumnConstraints r = new ColumnConstraints();
        r.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(l, r);
        return g;
    }

    private void field(GridPane g, int row, String name, Node node) {
        g.add(new Label(name), 0, row);
        if (node instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
        g.add(node, 1, row);
    }

    private Node resumo() {
        VBox v = page();

        GridPane cards = new GridPane();
        cards.setHgap(10);
        cards.setVgap(10);
        cards.add(runtimeCard("Personalização", "Definições CDU/XDU/PDU/RDU/FDU/SDU/MDU.", Feather.CPU, "PERSONALIZACAO"), 0, 0);
        cards.add(runtimeCard("Listagens", "Consultas persistentes executáveis pelo runtime.", Feather.LIST, "LISTAGEM"), 1, 0);
        cards.add(runtimeCard("Mapas", "Fluxos definidos por nodes e edges.", Feather.MAP, "MAPA"), 0, 1);
        cards.add(runtimeCard("Eventos", "Regras de publicação e notificação.", Feather.ACTIVITY, "EVENTO_REGRA"), 1, 1);
        cards.add(runtimeCard("Dashboard", "Widgets e indicadores administrativos.", Feather.BAR_CHART_2, "DASHBOARD"), 0, 2);
        cards.add(runtimeCard("Calendário", "Eventos e recorrências administrativas.", Feather.CALENDAR, "CALENDARIO"), 1, 2);

        Label status = new Label("Motores disponíveis e prontos para operação.");
        status.getStyleClass().add("kubata-server-status-value");

        HBox shortcuts = new HBox(8,
                button("Extensibilidade", Feather.CPU, () -> selectTab("Extensibilidade")),
                button("Pesquisa", Feather.SEARCH, () -> selectTab("Pesquisa")),
                button("Eventos", Feather.ACTIVITY, () -> selectTab("Eventos")),
                button("Dashboard", Feather.BAR_CHART_2, () -> selectTab("Dashboard")),
                button("BD", Feather.DATABASE, () -> selectTab("BD"))
        );

        v.getChildren().addAll(
                section("Resumo dos motores", "Visão operacional dos principais motores e acesso direto às áreas mais utilizadas."),
                card(status, shortcuts),
                cards,
                info("Arquitectura runtime", "Os motores consomem as definições persistidas e executam apenas as operações expostas pelos serviços da plataforma.")
        );
        return scroll(v);
    }

    private VBox runtimeCard(String title, String description, Feather icon, String type) {
        VBox c = new VBox(7);
        c.setPadding(new Insets(12));
        c.setPrefHeight(118);
        c.setMaxWidth(Double.MAX_VALUE);
        c.getStyleClass().add("kubata-runtime-card");

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-runtime-card-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 15)));

        Label t = new Label(title);
        t.getStyleClass().add("kubata-runtime-card-title");

        Label count = new Label(String.valueOf(automation.list(type).size()));
        count.getStyleClass().add("kubata-runtime-card-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(iconBox, t, spacer, count);

        Label d = new Label(description);
        d.setWrapText(true);
        d.getStyleClass().add("kubata-runtime-card-text");
        c.getChildren().addAll(head, d);
        GridPane.setHgrow(c, Priority.ALWAYS);
        return c;
    }

    private void selectTab(String title) {
        for (Tab tab : tabs.getTabs()) {
            if (Objects.equals(tab.getText(), title)) {
                tabs.getSelectionModel().select(tab);
                return;
            }
        }
    }

    private Node extensibility() {
        VBox v = page();
        TableView<AdmPlataformaItem> table = platformTable("PERSONALIZACAO");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "TODOS", "CDU", "XDU", "PDU", "RDU", "FDU", "SDU", "MDU"));
        type.getSelectionModel().selectFirst();

        TextArea runtimeOutput = new TextArea();
        runtimeOutput.setEditable(false);
        runtimeOutput.setPrefRowCount(10);

        HBox actions = new HBox(8,
                button("Filtrar", Feather.FILTER, () -> loadExtensions(table, type.getValue())),
                button("Executar runtime", Feather.PLAY, () -> {
                    AdmPlataformaItem item = selected(table);
                    if (item == null) return;
                    String extType = runtime.extensionType(item);
                    String code = item.getCodigo().replaceFirst("^" + extType + "_", "");
                    runtimeOutput.setText(runtime.extensionRuntime(extType, code).toString());
                }),
                button("Actualizar", Feather.REFRESH_CW, () -> loadExtensions(table, type.getValue()))
        );

        v.getChildren().addAll(
                section("Motores de personalização",
                        "As definições CDU/XDU/PDU/RDU/FDU/SDU/MDU deixam de ser apenas catálogo: ficam expostas ao runtime como metadata validada."),
                actions,
                table,
                new Label("Runtime"),
                runtimeOutput
        );
        VBox.setVgrow(table, Priority.ALWAYS);
        return scroll(v);
    }

    private void loadExtensions(TableView<AdmPlataformaItem> table, String type) {
        List<AdmPlataformaItem> data = "TODOS".equalsIgnoreCase(type)
                ? runtime.extensibility(null)
                : runtime.extensibility(type);
        table.setItems(FXCollections.observableArrayList(data));
    }

    private Node listagens() {
        VBox v = page();
        TableView<AdmPlataformaItem> table = platformTable("LISTAGEM");
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setPrefRowCount(12);

        v.getChildren().addAll(
                section("Motor de Listagens",
                        "Executa definições persistentes com SELECT/WITH, limite de segurança e visualização tabular no centro administrativo."),
                new HBox(8,
                        button("Executar seleccionado", Feather.PLAY, () -> {
                            AdmPlataformaItem item = selected(table);
                            if (item == null) return;
                            try {
                                PlataformaRuntimeService.QueryResult r = runtime.executeListagem(item, 500);
                                StringBuilder sb = new StringBuilder();
                                sb.append(String.join("\t", r.columns())).append('\n');
                                for (List<String> row : r.rows()) sb.append(String.join("\t", row)).append('\n');
                                sb.append("\nLinhas: ").append(r.rowCount());
                                output.setText(sb.toString());
                            } catch (Exception ex) {
                                output.setText("Falha: " + ex.getMessage());
                            }
                        }),
                        button("Validar JSON", Feather.CHECK_CIRCLE, () -> {
                            AdmPlataformaItem item = selected(table);
                            if (item != null) showMessage("Validação", runtime.validateDefinition("LISTAGEM", item.getConfigJson()));
                        }),
                        button("Actualizar", Feather.REFRESH_CW, () -> table.setItems(FXCollections.observableArrayList(automation.list("LISTAGEM"))))
                ),
                table,
                new Label("Resultado"),
                output
        );
        VBox.setVgrow(table, Priority.ALWAYS);
        return scroll(v);
    }

    private Node mapas() {
        VBox v = page();
        TableView<AdmPlataformaItem> table = platformTable("MAPA");
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setPrefRowCount(12);

        v.getChildren().addAll(
                section("Motor de Mapas",
                        "Interpreta nodes/edges das definições e disponibiliza o fluxo lógico para os consumidores do ERP."),
                new HBox(8,
                        button("Renderizar", Feather.PLAY, () -> {
                            AdmPlataformaItem item = selected(table);
                            if (item == null) return;
                            try {
                                PlataformaRuntimeService.MapRuntime map = runtime.renderMap(item);
                                output.setText(
                                        "Nós: " + map.nodes() + "\n"
                                                + map.nodeLabels().stream().collect(Collectors.joining("\n- ", "- ", "\n\n"))
                                                + "Ligações: " + map.edges() + "\n"
                                                + map.edgeLabels().stream().collect(Collectors.joining("\n- ", "- ", ""))
                                );
                            } catch (Exception ex) {
                                output.setText("Falha: " + ex.getMessage());
                            }
                        }),
                        button("Actualizar", Feather.REFRESH_CW, () -> table.setItems(FXCollections.observableArrayList(automation.list("MAPA"))))
                ),
                table,
                new Label("Mapa runtime"),
                output
        );
        VBox.setVgrow(table, Priority.ALWAYS);
        return scroll(v);
    }

    private Node pesquisa() {
        VBox v = page();
        TextField term = new TextField();
        term.setPromptText("Nome, número, código, NIF, documento...");
        Spinner<Integer> max = new Spinner<>(10, 200, 50, 10);
        TextArea results = new TextArea();
        results.setEditable(false);
        results.setWrapText(true);

        v.getChildren().addAll(
                section("Pesquisa global + indexação leve",
                        "Procura automaticamente em tabelas de negócio e administrativas, ignorando tabelas internas do motor da BD."),
                new HBox(8, term, max, button("Pesquisar", Feather.SEARCH, () -> {
                    try {
                        List<PlataformaRuntimeService.SearchHit> hits = runtime.globalSearch(term.getText(), max.getValue());
                        if (hits.isEmpty()) results.setText("Nenhum resultado.");
                        else {
                            StringBuilder sb = new StringBuilder();
                            for (var hit : hits) {
                                sb.append('[').append(hit.table()).append("] PK=")
                                        .append(hit.primaryKey()).append('\n')
                                        .append(hit.summary()).append("\n\n");
                            }
                            results.setText(sb.toString());
                        }
                    } catch (Exception ex) {
                        results.setText("Falha na pesquisa: " + ex.getMessage());
                    }
                })),
                card(results)
        );
        HBox.setHgrow(term, Priority.ALWAYS);
        return scroll(v);
    }

    private Node eventos() {
        VBox v = page();
        TableView<AdmPlataformaItem> rules = platformTable("EVENTO_REGRA");
        TableView<AdmPlataformaItem> notificationsTable = platformTable("NOTIFICACAO");

        v.getChildren().addAll(
                section("Eventos e notificações",
                        "O publicador de eventos dos módulos passa a alimentar histórico persistente e regras de notificação."),
                new HBox(8,
                        button("Nova regra", Feather.PLUS, () -> newEventRule(rules)),
                        button("Emitir evento de teste", Feather.SEND, () -> emitTestEvent()),
                        button("Actualizar", Feather.REFRESH_CW, () -> {
                            rules.setItems(FXCollections.observableArrayList(automation.list("EVENTO_REGRA")));
                            notificationsTable.setItems(FXCollections.observableArrayList(runtime.notificationsList()));
                        })
                ),
                new Label("Regras"),
                rules,
                new Label("Notificações"),
                notificationsTable
        );
        rules.setPrefHeight(320);
        rules.setMinHeight(260);
        notificationsTable.setPrefHeight(420);
        notificationsTable.setMinHeight(320);
        VBox.setVgrow(rules, Priority.NEVER);
        VBox.setVgrow(notificationsTable, Priority.NEVER);
        return scroll(v);
    }

    private void newEventRule(TableView<AdmPlataformaItem> table) {
        TextField event = new TextField("DATA_CHANGED");
        TextField name = new TextField("Notificar alteração");
        TextField message = new TextField();
        GridPane g = form();
        field(g, 0, "Evento", event);
        field(g, 1, "Nome", name);
        field(g, 2, "Mensagem", message);
        modalManager.showConfirmModal(g, "Nova regra de evento", () -> {
            String json = "{\"event\":\"" + escape(event.getText().trim().toUpperCase(Locale.ROOT))
                    + "\",\"message\":\"" + escape(message.getText().trim()) + "\"}";
            automation.save("EVENTO_REGRA", "EVT_" + UUID.randomUUID(), name.getText(), "ACTIVO",
                    "Regra de notificação", json, null, user(), null);
            table.setItems(FXCollections.observableArrayList(automation.list("EVENTO_REGRA")));
        }, () -> {});
    }

    private void emitTestEvent() {
        runtime.emitEvent(
                "DATA_CHANGED",
                "ADMIN",
                Map.of("entity", "ADM_PLATAFORMA", "action", "TEST"),
                user());
    }

    private Node calendario() {
        VBox v = page();
        TableView<AdmPlataformaItem> table = platformTable("CALENDARIO");

        v.getChildren().addAll(
                section("Calendário administrativo",
                        "Eventos, duração e recorrência ficam persistidos no catálogo da plataforma."),
                new HBox(8,
                        button("Novo evento", Feather.PLUS, () -> newCalendarEvent(table)),
                        button("Actualizar", Feather.REFRESH_CW, () -> table.setItems(FXCollections.observableArrayList(runtime.calendarEvents())))
                ),
                table
        );
        VBox.setVgrow(table, Priority.ALWAYS);
        return scroll(v);
    }

    private void newCalendarEvent(TableView<AdmPlataformaItem> table) {
        TextField code = new TextField();
        TextField name = new TextField();
        DatePicker date = new DatePicker(LocalDate.now());
        TextField time = new TextField(LocalTime.now().withSecond(0).withNano(0).toString());
        TextField duration = new TextField("60");
        TextField recurrence = new TextField();
        TextArea description = new TextArea();

        GridPane g = form();
        field(g, 0, "Código", code);
        field(g, 1, "Nome", name);
        field(g, 2, "Data", date);
        field(g, 3, "Hora", time);
        field(g, 4, "Duração (min.)", duration);
        field(g, 5, "Recorrência", recurrence);
        field(g, 6, "Descrição", description);

        modalManager.showConfirmModal(g, "Novo evento", () -> {
            LocalTime parsedTime;
            try { parsedTime = LocalTime.parse(time.getText().trim()); }
            catch (Exception e) { parsedTime = LocalTime.MIDNIGHT; }

            runtime.saveCalendarEvent(
                    code.getText(), name.getText(), date.getValue(), parsedTime, duration.getText(),
                    recurrence.getText(), description.getText(), user());
            table.setItems(FXCollections.observableArrayList(runtime.calendarEvents()));
        }, () -> {});
    }

    private Node dashboard() {
        VBox v = page();
        TextArea stats = new TextArea();
        stats.setEditable(false);
        stats.setPrefRowCount(12);
        TableView<AdmPlataformaItem> widgets = platformTable("DASHBOARD");

        v.getChildren().addAll(
                section("Dashboard configurável",
                        "KPIs e widgets administrativos são persistidos por metadata e podem ser consumidos pela interface."),
                new HBox(8,
                        button("Actualizar KPIs", Feather.REFRESH_CW, () -> {
                            Map<String, Object> data = runtime.dashboardStats();
                            stats.setText(data.entrySet().stream()
                                    .map(e -> e.getKey() + " = " + e.getValue())
                                    .collect(Collectors.joining("\n")));
                        }),
                        button("Executar widget", Feather.PLAY, () -> {
                            AdmPlataformaItem item = selected(widgets);
                            if (item == null) return;
                            try {
                                stats.setText(item.getNome() + " → " + runtime.executeWidget(item));
                            } catch (Exception ex) {
                                stats.setText("Falha no widget: " + ex.getMessage());
                            }
                        }),
                        button("Novo widget", Feather.PLUS, () -> newWidget(widgets)),
                        button("Actualizar widgets", Feather.REFRESH_CW, () -> widgets.setItems(FXCollections.observableArrayList(runtime.dashboardWidgets())))
                ),
                card(stats),
                new Label("Widgets"),
                widgets
        );
        VBox.setVgrow(widgets, Priority.ALWAYS);
        return scroll(v);
    }

    private void newWidget(TableView<AdmPlataformaItem> table) {
        TextField code = new TextField();
        TextField title = new TextField();
        TextField metric = new TextField("documentos");
        TextArea query = new TextArea();
        query.setPromptText("SELECT COUNT(*) AS total FROM uma_tabela");
        GridPane g = form();
        field(g, 0, "Código", code);
        field(g, 1, "Título", title);
        field(g, 2, "Métrica", metric);
        field(g, 3, "Consulta opcional", query);
        modalManager.showConfirmModal(g, "Novo widget", () -> {
            if (query.getText() != null && !query.getText().isBlank() && !runtime.isReadOnlyQuery(query.getText())) {
                showMessage("Dashboard", "A consulta do widget deve ser somente SELECT/WITH.");
                return;
            }

            runtime.saveDashboardWidget(code.getText(), title.getText(), metric.getText(), query.getText(), user());
            table.setItems(FXCollections.observableArrayList(runtime.dashboardWidgets()));
        }, () -> {});
    }

    private Node anexos() {
        VBox v = page();
        TextField entityType = new TextField("CLIENTE");
        TextField entityId = new TextField();
        TableView<AdmPlataformaItem> table = platformTable("ANEXO");

        v.getChildren().addAll(
                section("Motor de anexos",
                        "Ficheiros são mantidos no repositório físico e relacionados a uma entidade por tipo + identificador."),
                new HBox(8, new Label("Entidade"), entityType, new Label("ID"), entityId,
                        button("Carregar", Feather.REFRESH_CW, () -> table.setItems(FXCollections.observableArrayList(runtime.attachments(entityType.getText().trim(), entityId.getText().trim()))))),
                new HBox(8,
                        button("Adicionar anexo", Feather.PAPERCLIP, () -> attachFile(table, entityType.getText(), entityId.getText())),
                        button("Eliminar", Feather.TRASH_2, () -> {
                            AdmPlataformaItem item = selected(table);
                            if (item == null) return;
                            try {
                                runtime.deleteAttachment(item);
                                table.setItems(FXCollections.observableArrayList(runtime.attachments(entityType.getText().trim(), entityId.getText().trim())));
                            } catch (Exception ex) {
                                showMessage("Anexos", ex.getMessage());
                            }
                        })
                ),
                table
        );
        HBox.setHgrow(entityType, Priority.ALWAYS);
        HBox.setHgrow(entityId, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);
        return scroll(v);
    }

    private void attachFile(TableView<AdmPlataformaItem> table, String type, String id) {
        if (id == null || id.isBlank()) {
            showMessage("Anexos", "Informe o identificador da entidade.");
            return;
        }
        FileChooser chooser = new FileChooser();
        java.io.File source = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (source == null) return;

        DirectoryChooser repositoryChooser = new DirectoryChooser();
        java.io.File dir = repositoryChooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (dir == null) return;

        try {
            runtime.attach(source.toPath(), dir.toPath(), type.trim(), id.trim(), user());
            table.setItems(FXCollections.observableArrayList(runtime.attachments(type.trim(), id.trim())));
            showMessage("Anexos", "Anexo adicionado.");
        } catch (Exception ex) {
            showMessage("Anexos", ex.getMessage());
        }
    }

    private Node basesDados() {
        VBox v = page();
        TextField cloneTarget = new TextField();
        TextField newDb = new TextField();
        TextField restoreFile = new TextField();
        TextField compareLeft = new TextField();
        TextField compareRight = new TextField();
        TextArea fingerprint = new TextArea();
        fingerprint.setEditable(false);
        fingerprint.setPrefRowCount(3);

        Button clone = button("Clonar SQLite", Feather.COPY, () -> {
            try {
                Path p = Paths.get(cloneTarget.getText().trim());
                showMessage("Base de Dados", runtime.cloneCurrentSqlite(p));
            } catch (Exception ex) {
                showMessage("Base de Dados", ex.getMessage());
            }
        });

        Button create = button("Criar SQLite vazio", Feather.DATABASE, () -> {
            try {
                showMessage("Base de Dados", runtime.createEmptySqlite(Paths.get(newDb.getText().trim())));
            } catch (Exception ex) {
                showMessage("Base de Dados", ex.getMessage());
            }
        });

        Button restore = button("Preparar restore", Feather.ROTATE_CW, () -> {
            try {
                showMessage("Base de Dados", runtime.prepareRestore(Paths.get(restoreFile.getText().trim())));
            } catch (Exception ex) {
                showMessage("Base de Dados", ex.getMessage());
            }
        });

        Button compare = button("Comparar SQLite", Feather.REFRESH_CW, () -> {
            try {
                showMessage("Base de Dados", runtime.compareSqlite(
                        Paths.get(compareLeft.getText().trim()),
                        Paths.get(compareRight.getText().trim())));
            } catch (Exception ex) {
                showMessage("Base de Dados", ex.getMessage());
            }
        });

        v.getChildren().addAll(
                section("Gestão avançada de BD",
                        "A plataforma cria bases SQLite vazias, clona a base actual e prepara restaurações sem substituir uma BD em uso."),
                card(
                        new Label("Clonagem da base actual"),
                        cloneTarget,
                        new HBox(8, clone, button("Escolher pasta", Feather.FOLDER, () -> chooseFileInto(cloneTarget)))
                ),
                card(
                        new Label("Nova base SQLite"),
                        newDb,
                        create
                ),
                card(
                        new Label("Restore seguro"),
                        restoreFile,
                        restore
                ),
                card(
                        new Label("Comparação de schemas SQLite"),
                        new HBox(8, compareLeft, button("Escolher A", Feather.FOLDER, () -> chooseOpenFileInto(compareLeft))),
                        new HBox(8, compareRight, button("Escolher B", Feather.FOLDER, () -> chooseOpenFileInto(compareRight))),
                        compare
                ),
                new HBox(8, button("Fingerprint do schema", Feather.HASH, () -> {
                    fingerprint.setText(runtime.schemaFingerprint());
                })),
                fingerprint,
                new Label("Nota: operações de servidor PostgreSQL/SQL Server continuam dependentes das credenciais e ferramentas do servidor; o centro não grava passwords de BD.")
        );
        return scroll(v);
    }

    private void chooseFileInto(TextField target) {
        FileChooser chooser = new FileChooser();
        java.io.File f = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (f != null) target.setText(f.getAbsolutePath());
    }

    private void chooseOpenFileInto(TextField target) {
        FileChooser chooser = new FileChooser();
        java.io.File f = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (f != null) target.setText(f.getAbsolutePath());
    }

    private Node licencaLayout() {
        VBox v = page();
        TextField code = new TextField();
        TextField holder = new TextField();
        DatePicker expiry = new DatePicker(LocalDate.now().plusYears(1));
        TextField modules = new TextField("faturacao,inventario,vendas,compras,financeiro,contabilidade,fiscal,rh");
        TextArea status = new TextArea();
        status.setEditable(false);
        TextArea layout = new TextArea(runtime.loadLayout(user()));
        layout.setPrefRowCount(7);

        v.getChildren().addAll(
                section("Licenciamento", "Estado e validade da licença administrativa são persistidos sem armazenar segredos."),
                new HBox(8,
                        button("Guardar licença", Feather.SAVE, () -> {
                            runtime.saveLicense(code.getText(), holder.getText(), expiry.getValue(), modules.getText(), user());
                            status.setText(runtime.licenseStatus());
                        }),
                        button("Consultar", Feather.SHIELD, () -> status.setText(runtime.licenseStatus()))
                ),
                card(formLicense(code, holder, expiry, modules)),
                status,
                section("Layout / Docking", "Layout de janelas pode ser persistido por utilizador para restauração futura."),
                layout,
                button("Guardar layout", Feather.LAYOUT, () -> {
                    runtime.saveLayout(user(), layout.getText());
                    showMessage("Layout", "Layout guardado para " + user() + ".");
                }),
                info(
                        "Instalação",
                        "Parâmetros de instalação existentes permanecem no Centro da Plataforma; esta área acrescenta licença e persistência de layout.")
        );
        return scroll(v);
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(8, 14, 8, 14));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-server-footer");

        Label safe = new Label("Motores runtime · operações administrativas");
        safe.getStyleClass().add("kubata-server-footer-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Kubata Administrator");
        hint.getStyleClass().add("kubata-server-footer-text");

        footer.getChildren().addAll(safe, spacer, hint);
        return footer;
    }

    private GridPane formLicense(TextField code, TextField holder, DatePicker expiry, TextField modules) {
        GridPane g = form();
        field(g, 0, "Código", code);
        field(g, 1, "Titular", holder);
        field(g, 2, "Validade", expiry);
        field(g, 3, "Módulos", modules);
        return g;
    }

    private VBox info(String title, String description) {
        return card(new Label(title), new Label(description));
    }

    private TableView<AdmPlataformaItem> platformTable(String type) {
        TableView<AdmPlataformaItem> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(automation.list(type)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(500);
        table.setMinHeight(350);
        table.setMaxHeight(900);
        table.getStyleClass().addAll("kubata-infra-table", "kubata-server-properties-table");
        table.setPlaceholder(new Label("Nenhum registo."));
        TableColumn<AdmPlataformaItem, String> code = new TableColumn<>("Código");
        code.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(safe(v.getValue().getCodigo())));
        TableColumn<AdmPlataformaItem, String> name = new TableColumn<>("Nome");
        name.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(safe(v.getValue().getNome())));
        TableColumn<AdmPlataformaItem, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(safe(v.getValue().getEstado())));
        TableColumn<AdmPlataformaItem, String> result = new TableColumn<>("Resultado");
        result.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(safe(v.getValue().getLastMessage())));
        table.getColumns().addAll(code, name, state, result);
        return table;
    }

    private AdmPlataformaItem selected(TableView<AdmPlataformaItem> table) {
        AdmPlataformaItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) showMessage("Plataforma", "Seleccione um registo.");
        return item;
    }

    private void refreshCurrent() {
        Tab tab = tabs.getSelectionModel().getSelectedItem();
        if (tab == null) return;
        // Reconstrói o conteúdo da aba através do builder correspondente.
        String title = tab.getText();
        Node node = switch (title) {
            case "Resumo" -> resumo();
            case "Extensibilidade" -> extensibility();
            case "Listagens" -> listagens();
            case "Mapas" -> mapas();
            case "Pesquisa" -> pesquisa();
            case "Eventos" -> eventos();
            case "Calendário" -> calendario();
            case "Dashboard" -> dashboard();
            case "Anexos" -> anexos();
            case "BD" -> basesDados();
            case "Licença & Layout" -> licencaLayout();
            default -> null;
        };
        if (node != null) tab.setContent(node);
    }

    private String user() {
        return sessions.getUser() == null ? "Sistema" : sessions.getUser().getNome();
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private String escape(String s) {
        return Objects.toString(s, "").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void showMessage(String title, String message) {
        modalManager.alert(title, Objects.toString(message, ""), "info", null);
    }
}
