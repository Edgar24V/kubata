package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/**
 * Central operacional da instância local do Kubata.
 *
 * <p>É deliberadamente uma visão de observabilidade local: não afirma que
 * existe um cluster nem que uma instância está saudável externamente. As
 * métricas são obtidas directamente da JVM/SO e das propriedades carregadas.</p>
 */
@Component
public class InstanciasOverviewView extends VBox {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Environment environment;
    private final ModalManager modalManager;

    private Label statusBadge;
    private Label hostValue;
    private Label pidValue;
    private Label appValue;
    private Label profileValue;
    private Label uptimeValue;
    private Label javaValue;
    private Label memoryValue;
    private Label heapValue;
    private Label threadsValue;
    private Label diskValue;
    private Label lastRefreshValue;
    private TextArea diagnosticsArea;

    public InstanciasOverviewView(Environment environment, ModalManager modalManager) {
        this.environment = environment;
        this.modalManager = modalManager;

        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-instances-page");

        buildUi();
        refresh();
    }

    private void buildUi() {
        getChildren().addAll(
                buildHeader(),
                buildSummary(),
                buildTabs()
        );
        VBox.setVgrow(getChildren().get(2), Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-instances-header");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-instances-title-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.SERVER, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Instâncias");
        title.getStyleClass().add("kubata-instances-title");

        Label subtitle = new Label(
                "Observabilidade operacional da instância local do Kubata, ambiente, JVM, recursos e diagnóstico."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-instances-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> refresh());

        Button diagnostics = new Button("Diagnóstico", IconUtils.icon(Feather.ACTIVITY, 13));
        diagnostics.getStyleClass().add("button-outlined");
        diagnostics.setOnAction(e -> showDiagnosticsModal());

        Button export = new Button("Exportar diagnóstico", IconUtils.icon(Feather.DOWNLOAD, 13));
        export.getStyleClass().add("button-primary");
        export.setOnAction(e -> exportDiagnostics());

        line.getChildren().addAll(icon, titles, spacer, refresh, diagnostics, export);
        header.getChildren().add(line);

        return header;
    }

    private HBox buildSummary() {
        HBox summary = new HBox(10);
        summary.setPadding(new Insets(0, 22, 16, 22));

        VBox state = summaryCard("ESTADO", statusBadge = new Label("A validar"), Feather.SHIELD);
        VBox host = summaryCard("HOST", hostValue = new Label("—"), Feather.HOME);
        VBox pid = summaryCard("PROCESSO", pidValue = new Label("—"), Feather.CPU);
        VBox uptime = summaryCard("UPTIME", uptimeValue = new Label("—"), Feather.CLOCK);
        VBox refresh = summaryCard("ACTUALIZADO", lastRefreshValue = new Label("—"), Feather.REFRESH_CW);

        summary.getChildren().addAll(state, host, pid, uptime, refresh);
        return summary;
    }

    private VBox summaryCard(String title, Label value, Feather icon) {
        VBox box = new VBox(4);
        box.setMinWidth(145);
        box.getStyleClass().add("kubata-instance-summary-card");

        HBox line = new HBox(6);
        line.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label("", IconUtils.icon(icon, 13));
        ico.getStyleClass().add("kubata-instance-summary-icon");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-instance-summary-title");
        line.getChildren().addAll(ico, t);

        value.getStyleClass().add("kubata-instance-summary-value");
        box.getChildren().addAll(line, value);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("kubata-instances-tabs");

        Tab overview = new Tab("Visão geral", buildOverviewTab());
        Tab environmentTab = new Tab("Ambiente", buildEnvironmentTab());
        Tab resources = new Tab("Recursos", buildResourcesTab());
        Tab diagnostics = new Tab("Diagnóstico", buildDiagnosticsTab());
        Tab operations = new Tab("Operações", buildOperationsTab());

        tabs.getTabs().addAll(overview, environmentTab, resources, diagnostics, operations);
        return tabs;
    }

    private Node buildOverviewTab() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.SERVER,
                "Identidade da instância",
                "Identificação local usada para suporte e diagnóstico. Uma instância local não implica, por si só, uma arquitectura de alta disponibilidade."
        ));

        GridPane grid = infoGrid();
        addInfo(grid, 0, "Aplicação", appValue = new Label("—"));
        addInfo(grid, 1, "Hostname", hostValue);
        addInfo(grid, 2, "Processo / PID", pidValue);
        addInfo(grid, 3, "Perfis Spring activos", profileValue = new Label("—"));
        addInfo(grid, 4, "Java", javaValue = new Label("—"));
        addInfo(grid, 5, "Arranque", new Label(startTime()));
        root.getChildren().add(grid);

        root.getChildren().add(section(
                Feather.INFO,
                "Estado operacional",
                "Os indicadores desta página são leituras locais da JVM e do sistema operativo; não substituem monitorização externa, testes de conectividade ou verificações da base de dados."
        ));

        GridPane stateGrid = infoGrid();
        addInfo(stateGrid, 0, "Estado", statusBadge);
        addInfo(stateGrid, 1, "Memória JVM", memoryValue = new Label("—"));
        addInfo(stateGrid, 2, "Heap", heapValue = new Label("—"));
        addInfo(stateGrid, 3, "Threads", threadsValue = new Label("—"));
        addInfo(stateGrid, 4, "Disco da aplicação", diskValue = new Label("—"));
        root.getChildren().add(stateGrid);

        return wrapScroll(root);
    }

    private Node buildEnvironmentTab() {
        VBox root = contentRoot();
        root.getChildren().add(section(
                Feather.SETTINGS,
                "Configuração carregada",
                "Consulta apenas propriedades efectivamente presentes no ambiente Spring ou no sistema. Segredos não são exibidos deliberadamente."
        ));

        List<Map.Entry<String, String>> rows = List.of(
                entry("spring.application.name", property("spring.application.name", "kubata-admin")),
                entry("spring.profiles.active", profiles()),
                entry("server.port", property("server.port", "—")),
                entry("server.address", property("server.address", "—")),
                entry("spring.datasource.url", maskSensitive(property("spring.datasource.url", "—"))),
                entry("java.version", System.getProperty("java.version", "—")),
                entry("java.vendor", System.getProperty("java.vendor", "—")),
                entry("os.name", System.getProperty("os.name", "—")),
                entry("os.version", System.getProperty("os.version", "—")),
                entry("os.arch", System.getProperty("os.arch", "—")),
                entry("user.dir", System.getProperty("user.dir", "—")),
                entry("user.timezone", System.getProperty("user.timezone", "—"))
        );

        TableView<Map.Entry<String, String>> table = new TableView<>(
                FXCollections.observableArrayList(rows)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(390);
        table.getStyleClass().add("kubata-instance-properties-table");

        TableColumn<Map.Entry<String, String>, String> key = new TableColumn<>("Propriedade");
        key.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getKey()));

        TableColumn<Map.Entry<String, String>, String> value = new TableColumn<>("Valor");
        value.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getValue()));

        table.getColumns().addAll(key, value);
        root.getChildren().add(table);

        return wrapScroll(root);
    }

    private Node buildResourcesTab() {
        VBox root = contentRoot();
        root.getChildren().add(section(
                Feather.CPU,
                "Recursos em tempo real",
                "Leituras calculadas no momento da actualização para apoiar diagnóstico de capacidade e estabilidade da instância."
        ));

        GridPane metrics = infoGrid();
        addInfo(metrics, 0, "CPU / processadores", new Label(String.valueOf(Runtime.getRuntime().availableProcessors())));
        addInfo(metrics, 1, "Memória total JVM", new Label(formatBytes(Runtime.getRuntime().totalMemory())));
        addInfo(metrics, 2, "Memória livre JVM", new Label(formatBytes(Runtime.getRuntime().freeMemory())));
        addInfo(metrics, 3, "Memória máxima JVM", new Label(formatBytes(Runtime.getRuntime().maxMemory())));
        addInfo(metrics, 4, "Heap utilizada", heapValue);
        addInfo(metrics, 5, "Threads activas", threadsValue);
        addInfo(metrics, 6, "Threads daemon", new Label(String.valueOf(ManagementFactory.getThreadMXBean().getDaemonThreadCount())));
        addInfo(metrics, 7, "Classes carregadas", new Label(String.valueOf(ManagementFactory.getClassLoadingMXBean().getLoadedClassCount())));
        root.getChildren().add(metrics);

        VBox notes = new VBox(8);
        notes.getStyleClass().add("kubata-instance-callout");
        Label t = new Label("Leitura operacional");
        t.getStyleClass().add("kubata-instance-callout-title");
        Label b = new Label(
                "Memória e CPU não devem ser interpretadas isoladamente. Picos momentâneos podem ser normais; procure tendência persistente e correlacione com base de dados, backups, SAF-T, relatórios e carga documental."
        );
        b.setWrapText(true);
        b.getStyleClass().add("kubata-instance-callout-text");
        notes.getChildren().addAll(t, b);
        root.getChildren().add(notes);

        return wrapScroll(root);
    }

    private Node buildDiagnosticsTab() {
        VBox root = contentRoot();
        root.getChildren().add(section(
                Feather.ACTIVITY,
                "Diagnóstico técnico",
                "Gere uma fotografia técnica da instância para suporte, análise de incidentes e verificação operacional."
        ));

        diagnosticsArea = new TextArea();
        diagnosticsArea.setEditable(false);
        diagnosticsArea.setWrapText(false);
        diagnosticsArea.setPrefRowCount(26);
        diagnosticsArea.getStyleClass().add("kubata-instance-diagnostics");
        diagnosticsArea.setText(buildDiagnosticsText());

        HBox actions = new HBox(8);
        Button regenerate = new Button("Regenerar", IconUtils.icon(Feather.REFRESH_CW, 12));
        regenerate.getStyleClass().add("button-outlined");
        regenerate.setOnAction(e -> diagnosticsArea.setText(buildDiagnosticsText()));

        Button copy = new Button("Copiar", IconUtils.icon(Feather.COPY, 12));
        copy.getStyleClass().add("button-outlined");
        copy.setOnAction(e -> copyToClipboard(diagnosticsArea.getText()));

        Button export = new Button("Guardar .txt", IconUtils.icon(Feather.SAVE, 12));
        export.getStyleClass().add("button-primary");
        export.setOnAction(e -> exportDiagnostics());

        actions.getChildren().addAll(regenerate, copy, export);
        root.getChildren().addAll(actions, diagnosticsArea);
        VBox.setVgrow(diagnosticsArea, Priority.ALWAYS);

        return root;
    }

    private Node buildOperationsTab() {
        VBox root = contentRoot();
        root.getChildren().add(section(
                Feather.TOOL,
                "Operações seguras",
                "Ferramentas locais para manutenção e suporte. Nenhuma operação desta página remove dados fiscais ou reinicia o processo automaticamente."
        ));

        root.getChildren().add(operationCard(
                Feather.REFRESH_CW,
                "Actualizar estado",
                "Recarrega as métricas locais e actualiza o diagnóstico.",
                "Actualizar",
                this::refresh
        ));

        root.getChildren().add(operationCard(
                Feather.COPY,
                "Copiar relatório técnico",
                "Copia a fotografia actual da instância para a área de transferência.",
                "Copiar",
                () -> copyToClipboard(buildDiagnosticsText())
        ));

        root.getChildren().add(operationCard(
                Feather.DOWNLOAD,
                "Exportar relatório técnico",
                "Guarda um relatório .txt com ambiente, JVM, recursos e identificação da instância.",
                "Exportar",
                this::exportDiagnostics
        ));

        root.getChildren().add(operationCard(
                Feather.SHIELD,
                "Abrir diagnóstico em janela",
                "Apresenta o diagnóstico completo num modal para leitura e suporte.",
                "Abrir",
                this::showDiagnosticsModal
        ));

        VBox warning = new VBox(6);
        warning.getStyleClass().add("kubata-instance-warning");
        Label title = new Label("Protecção operacional");
        title.getStyleClass().add("kubata-instance-callout-title");
        Label text = new Label(
                "Reinício do serviço, alteração de parâmetros de produção, restauração de backups e operações de base de dados devem passar pelos fluxos próprios do Kubata e pelas políticas de mudança da organização."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-instance-callout-text");
        warning.getChildren().addAll(title, text);
        root.getChildren().add(warning);

        return wrapScroll(root);
    }

    private VBox operationCard(
            Feather icon,
            String title,
            String description,
            String actionText,
            Runnable action
    ) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kubata-instance-operation-card");

        HBox line = new HBox(10);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-instance-operation-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 15)));

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.getStyleClass().add("kubata-instance-operation-title");
        Label d = new Label(description);
        d.setWrapText(true);
        d.getStyleClass().add("kubata-instance-operation-text");
        text.getChildren().addAll(t, d);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button actionButton = new Button(actionText);
        actionButton.getStyleClass().add("button-outlined");
        actionButton.setOnAction(e -> action.run());

        line.getChildren().addAll(iconBox, text, actionButton);
        card.getChildren().add(line);
        return card;
    }

    private VBox contentRoot() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 22, 24, 22));
        root.getStyleClass().add("kubata-instances-content");
        return root;
    }

    private VBox section(Feather icon, String title, String description) {
        VBox box = new VBox(4);
        box.getStyleClass().add("kubata-instances-section");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 15));
        iconLabel.getStyleClass().add("kubata-instances-section-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-instances-section-title");
        heading.getChildren().addAll(iconLabel, titleLabel);

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("kubata-instances-section-text");

        box.getChildren().addAll(heading, desc);
        return box;
    }

    private GridPane infoGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.getStyleClass().add("kubata-instances-info-grid");

        ColumnConstraints labels = new ColumnConstraints(190);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, values);
        return grid;
    }

    private void addInfo(GridPane grid, int row, String title, Label value) {
        Label label = new Label(title);
        label.getStyleClass().add("kubata-instance-field-label");
        value.getStyleClass().add("kubata-instance-field-value");
        value.setWrapText(true);
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private ScrollPane wrapScroll(Node node) {
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-instances-scroll");
        return scroll;
    }

    private void refresh() {
        Platform.runLater(() -> {
            try {
                String host = hostname();
                String pid = pid();
                String profiles = profiles();

                hostValue.setText(host);
                pidValue.setText(pid);
                appValue.setText(property("spring.application.name", "kubata-admin"));
                profileValue.setText(profiles);
                javaValue.setText(System.getProperty("java.version", "—")
                        + " · " + System.getProperty("java.vendor", "—"));

                Duration uptime = Duration.ofMillis(
                        ManagementFactory.getRuntimeMXBean().getUptime()
                );
                uptimeValue.setText(formatDuration(uptime));

                memoryValue.setText(
                        formatBytes(Runtime.getRuntime().totalMemory())
                                + " / " + formatBytes(Runtime.getRuntime().maxMemory())
                );
                heapValue.setText(formatHeap());
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                threadsValue.setText(String.valueOf(threadBean.getThreadCount()));

                Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
                diskValue.setText(
                        formatBytes(Files.getFileStore(current).getUsableSpace())
                                + " livres de "
                                + formatBytes(Files.getFileStore(current).getTotalSpace())
                );

                long usable = Files.getFileStore(current).getUsableSpace();
                long total = Files.getFileStore(current).getTotalSpace();
                double diskUsage = total <= 0 ? 0 : 1.0 - ((double) usable / total);
                double heapUsage = heapUsageRatio();

                boolean attention = diskUsage >= 0.90 || heapUsage >= 0.85;
                statusBadge.setText(attention ? "ATENÇÃO" : "OPERACIONAL");
                statusBadge.getStyleClass().removeAll(
                        "kubata-instance-status-warning",
                        "kubata-instance-status-danger",
                        "kubata-instance-status-ok"
                );
                statusBadge.getStyleClass().add(
                        attention
                                ? "kubata-instance-status-warning"
                                : "kubata-instance-status-ok"
                );

                lastRefreshValue.setText(LocalDateTime.now().format(DATE_TIME));

                if (diagnosticsArea != null) {
                    diagnosticsArea.setText(buildDiagnosticsText());
                }
            } catch (Exception ex) {
                statusBadge.setText("ATENÇÃO");
                statusBadge.getStyleClass().add("kubata-instance-status-warning");
                if (lastRefreshValue != null) {
                    lastRefreshValue.setText(LocalDateTime.now().format(DATE_TIME));
                }
            }
        });
    }

    private String buildDiagnosticsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("KUBATA — DIAGNÓSTICO DA INSTÂNCIA").append(System.lineSeparator());
        sb.append("Gerado em: ").append(LocalDateTime.now().format(DATE_TIME)).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        append(sb, "Aplicação", property("spring.application.name", "kubata-admin"));
        append(sb, "Perfis", profiles());
        append(sb, "Hostname", hostname());
        append(sb, "PID", pid());
        append(sb, "Java", System.getProperty("java.version", "—"));
        append(sb, "Fornecedor Java", System.getProperty("java.vendor", "—"));
        append(sb, "SO", System.getProperty("os.name", "—"));
        append(sb, "Versão SO", System.getProperty("os.version", "—"));
        append(sb, "Arquitectura", System.getProperty("os.arch", "—"));
        append(sb, "Directório", System.getProperty("user.dir", "—"));
        append(sb, "Uptime", formatDuration(Duration.ofMillis(
                ManagementFactory.getRuntimeMXBean().getUptime()
        )));
        append(sb, "CPU disponíveis", String.valueOf(Runtime.getRuntime().availableProcessors()));
        append(sb, "Heap", formatHeap());
        append(sb, "Memória JVM", formatBytes(Runtime.getRuntime().totalMemory()));
        append(sb, "Threads", String.valueOf(ManagementFactory.getThreadMXBean().getThreadCount()));

        try {
            Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
            append(sb, "Disco livre", formatBytes(Files.getFileStore(current).getUsableSpace()));
            append(sb, "Disco total", formatBytes(Files.getFileStore(current).getTotalSpace()));
        } catch (Exception ex) {
            append(sb, "Disco", "Não disponível");
        }

        sb.append(System.lineSeparator());
        sb.append("CONFIGURAÇÃO RELEVANTE").append(System.lineSeparator());
        append(sb, "server.port", property("server.port", "—"));
        append(sb, "server.address", property("server.address", "—"));
        append(sb, "database.url", maskSensitive(property("spring.datasource.url", "—")));

        sb.append(System.lineSeparator());
        sb.append("NOTA").append(System.lineSeparator());
        sb.append("Este relatório não contém passwords, tokens ou chaves secretas e não substitui monitorização externa.")
                .append(System.lineSeparator());

        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        sb.append(String.format(Locale.ROOT, "%-24s : %s%n", key, value == null ? "—" : value));
    }

    private void exportDiagnostics() {
        Window owner = getScene() == null ? null : getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar diagnóstico da instância");
        chooser.setInitialFileName(
                "kubata-instance-diagnostic-"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        + ".txt"
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Relatório de texto (*.txt)", "*.txt")
        );

        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        try {
            Files.writeString(
                    file.toPath(),
                    buildDiagnosticsText(),
                    StandardCharsets.UTF_8
            );
            modalManager.alert(
                    "Exportação concluída",
                    "O relatório técnico foi guardado em:\n" + file.getAbsolutePath(),
                    "info",
                    null
            );
        } catch (IOException ex) {
            modalManager.alert(
                    "Erro ao exportar",
                    "Não foi possível guardar o relatório: " + ex.getMessage(),
                    "error",
                    ex
            );
        }
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
        modalManager.alert(
                "Copiado",
                "O relatório técnico foi copiado para a área de transferência.",
                "info",
                null
        );
    }

    private void showDiagnosticsModal() {
        TextArea area = new TextArea(buildDiagnosticsText());
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(26);
        area.setPrefColumnCount(90);
        area.getStyleClass().add("kubata-instance-diagnostics");

        modalManager.showModal(
                area,
                new ModalManager.ModalConfig()
                        .size(850, 610)
                        .minSize(700, 500)
                        .title("Diagnóstico da instância")
                        .icon(Feather.ACTIVITY)
        );
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "host-indisponível";
        }
    }

    private String pid() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int at = runtimeName.indexOf('@');
        return at > 0 ? runtimeName.substring(0, at) : runtimeName;
    }

    private String profiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0
                ? "(nenhum perfil explícito)"
                : Arrays.stream(profiles).collect(Collectors.joining(", "));
    }

    private String property(String key, String fallback) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String maskSensitive(String value) {
        if (value == null || value.isBlank()) return "—";
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("password=")) {
            int start = lower.indexOf("password=") + "password=".length();
            int end = value.indexOf(';', start);
            if (end < 0) end = value.length();
            return value.substring(0, start) + "********" + value.substring(end);
        }
        return value;
    }

    private double heapUsageRatio() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (heap.getMax() <= 0) return 0;
        return (double) heap.getUsed() / heap.getMax();
    }

    private String formatHeap() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return formatBytes(heap.getUsed()) + " / " + formatBytes(heap.getMax());
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "—";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private String formatDuration(Duration d) {
        long seconds = Math.max(0, d.getSeconds());
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        if (days > 0) return String.format(Locale.ROOT, "%dd %02dh %02dm", days, hours, minutes);
        if (hours > 0) return String.format(Locale.ROOT, "%dh %02dm %02ds", hours, minutes, seconds);
        return String.format(Locale.ROOT, "%dm %02ds", minutes, seconds);
    }

    private String startTime() {
        long start = ManagementFactory.getRuntimeMXBean().getStartTime();
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(start),
                java.time.ZoneId.systemDefault()
        ).format(DATE_TIME);
    }
}
