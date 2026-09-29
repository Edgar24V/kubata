package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Consola do servidor de dados / runtime do Kubata.
 *
 * <p>Apresenta configuração não sensível, estado da configuração,
 * recursos básicos do runtime e diagnóstico copiável/exportável.</p>
 */
@Component
public class DataServidorView extends BorderPane {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String jdbcUrl;
    private final String username;
    private final String driver;

    private final Label configState = new Label();
    private final Label javaValue = new Label();
    private final Label osValue = new Label();
    private final Label processorsValue = new Label();
    private final Label memoryValue = new Label();
    private final Label timeValue = new Label();
    private final Label workingDirValue = new Label();
    private final Label driverValue = new Label();
    private final Label urlValue = new Label();
    private final Label userValue = new Label();

    public DataServidorView(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.driver-class-name:}") String driver) {

        this.jdbcUrl = jdbcUrl == null ? "" : jdbcUrl;
        this.username = username == null ? "" : username;
        this.driver = driver == null ? "" : driver;

        getStyleClass().add("kubata-server-page");
        buildUi();
        refreshRuntimeInfo();
    }

    private void buildUi() {
        VBox header = buildHeader();
        VBox content = buildContent();
        HBox footer = buildFooter();

        setTop(header);
        setCenter(content);
        setBottom(footer);
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(20, 22, 16, 22));
        header.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-server-title-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.SERVER, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Servidor");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Centro de administração do servidor de dados e do runtime do Kubata."
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
        refresh.setOnAction(e -> refreshRuntimeInfo());

        Button copy = new Button(
                "Copiar diagnóstico",
                IconUtils.icon(Feather.COPY, 13)
        );
        copy.getStyleClass().add("button-outlined");
        copy.setOnAction(e -> copyDiagnostics());

        Button export = new Button(
                "Exportar",
                IconUtils.icon(Feather.DOWNLOAD, 13)
        );
        export.getStyleClass().add("button-primary");
        export.setOnAction(e -> exportDiagnostics());

        line.getChildren().addAll(icon, titles, spacer, refresh, copy, export);

        HBox status = new HBox(9);
        status.setAlignment(Pos.CENTER_LEFT);

        Label statusDot = new Label("", IconUtils.icon(Feather.DATABASE, 13));
        statusDot.getStyleClass().add("kubata-server-status-icon");

        configState.getStyleClass().add("kubata-server-status-value");
        status.getStyleClass().add("kubata-server-status-bar");

        status.getChildren().addAll(
                statusDot,
                new Label("CONFIGURAÇÃO DA BD"),
                configState
        );

        header.getChildren().addAll(line, status);
        return header;
    }

    private VBox buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(0, 22, 18, 22));

        GridPane cards = new GridPane();
        cards.setHgap(12);
        cards.setVgap(12);

        cards.add(metricCard("Java", Feather.CPU, javaValue), 0, 0);
        cards.add(metricCard("Sistema operativo", Feather.MONITOR, osValue), 1, 0);
        cards.add(metricCard("Processadores", Feather.ACTIVITY, processorsValue), 2, 0);
        cards.add(metricCard("Memória JVM", Feather.HARD_DRIVE, memoryValue), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(Priority.ALWAYS);
            column.setPercentWidth(25);
            cards.getColumnConstraints().add(column);
        }

        VBox database = buildDatabaseCard();
        VBox runtime = buildRuntimeCard();

        HBox lower = new HBox(12, database, runtime);
        HBox.setHgrow(database, Priority.ALWAYS);
        HBox.setHgrow(runtime, Priority.ALWAYS);

        VBox diagnostic = buildDiagnosticCard();

        content.getChildren().addAll(cards, lower, diagnostic);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-server-scroll");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private VBox metricCard(String title, Feather icon, Label value) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(13, 15, 13, 15));
        card.getStyleClass().add("kubata-server-metric");

        HBox line = new HBox(7);
        line.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 14));
        i.getStyleClass().add("kubata-server-metric-icon");

        Label t = new Label(title.toUpperCase(Locale.ROOT));
        t.getStyleClass().add("kubata-server-metric-title");

        line.getChildren().addAll(i, t);
        value.getStyleClass().add("kubata-server-metric-value");

        card.getChildren().addAll(line, value);
        return card;
    }

    private VBox buildDatabaseCard() {
        VBox card = panel("Ligação principal", Feather.DATABASE);

        GridPane grid = compactGrid();

        driverValue.setWrapText(true);
        urlValue.setWrapText(true);
        userValue.setWrapText(true);

        setField(grid, 0, "Driver", driverValue);
        setField(grid, 1, "JDBC URL", urlValue);
        setField(grid, 2, "Utilizador", userValue);

        Button copyUrl = new Button(
                "Copiar ligação segura",
                IconUtils.icon(Feather.LINK, 12)
        );
        copyUrl.getStyleClass().add("button-outlined");
        copyUrl.setOnAction(e -> copySafeJdbcUrl());

        Label note = new Label(
                "Credenciais sensíveis não são apresentadas nesta consola. "
                        + "A URL é sanitizada antes de ser exibida ou copiada."
        );
        note.setWrapText(true);
        note.getStyleClass().add("kubata-server-note");

        card.getChildren().addAll(grid, copyUrl, note);
        return card;
    }

    private VBox buildRuntimeCard() {
        VBox card = panel("Runtime e ambiente", Feather.CPU);
        GridPane grid = compactGrid();

        setField(grid, 0, "Data/hora do servidor", timeValue);
        setField(grid, 1, "Directório de trabalho", workingDirValue);
        setField(grid, 2, "Processadores disponíveis", processorsValue);

        Label note = new Label(
                "Estes dados descrevem o processo Java onde o Kubata está a executar; "
                        + "não substituem monitorização do sistema operativo."
        );
        note.setWrapText(true);
        note.getStyleClass().add("kubata-server-note");

        card.getChildren().addAll(grid, note);
        return card;
    }

    private VBox buildDiagnosticCard() {
        VBox card = panel("Diagnóstico seguro", Feather.SHIELD);

        TextArea diagnosis = new TextArea();
        diagnosis.setEditable(false);
        diagnosis.setWrapText(true);
        diagnosis.setPrefRowCount(7);
        diagnosis.getStyleClass().add("kubata-server-diagnostic");

        Runnable refresh = () -> diagnosis.setText(buildDiagnosticsText());
        refresh.run();

        Button copy = new Button(
                "Copiar",
                IconUtils.icon(Feather.COPY, 12)
        );
        copy.getStyleClass().add("button-outlined");
        copy.setOnAction(e -> copyText(diagnosis.getText()));

        Button save = new Button(
                "Guardar ficheiro",
                IconUtils.icon(Feather.SAVE, 12)
        );
        save.getStyleClass().add("button-outlined");
        save.setOnAction(e -> saveTextFile(diagnosis.getText()));

        HBox actions = new HBox(8, copy, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(diagnosis, actions);
        return card;
    }

    private VBox panel(String title, Feather icon) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("kubata-server-panel");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 15));
        i.getStyleClass().add("kubata-server-panel-icon");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-server-panel-title");

        heading.getChildren().addAll(i, t);
        box.getChildren().add(heading);
        return box;
    }

    private GridPane compactGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(155);

        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labels, values);
        return grid;
    }

    private void setField(GridPane grid, int row, String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("kubata-server-field-label");
        value.getStyleClass().add("kubata-server-field-value");
        grid.add(l, 0, row);
        grid.add(value, 1, row);
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(8, 14, 8, 14));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-server-footer");

        Label safe = new Label(
                "Segurança: passwords e segredos não são mostrados."
        );
        safe.getStyleClass().add("kubata-server-footer-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label(
                "Servidor de dados · diagnóstico local"
        );
        hint.getStyleClass().add("kubata-server-footer-text");

        footer.getChildren().addAll(safe, spacer, hint);
        return footer;
    }

    private void refreshRuntimeInfo() {
        javaValue.setText(System.getProperty("java.version", "—"));
        osValue.setText(
                System.getProperty("os.name", "—")
                        + " · "
                        + System.getProperty("os.arch", "—")
        );
        processorsValue.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));

        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        memoryValue.setText(
                formatBytes(used) + " / " + formatBytes(max)
        );

        timeValue.setText(LocalDateTime.now().format(DATE_TIME));
        workingDirValue.setText(
                Paths.get(System.getProperty("user.dir", "."))
                        .toAbsolutePath()
                        .normalize()
                        .toString()
        );

        driverValue.setText(driver.isBlank() ? "Não definido" : driver);
        urlValue.setText(sanitizeJdbcUrl(jdbcUrl));
        userValue.setText(username.isBlank() ? "Não definido" : username);

        boolean configured = !jdbcUrl.isBlank() && !driver.isBlank();
        configState.setText(configured
                ? "Configurada · parâmetros presentes"
                : "Incompleta · reveja a configuração");

        configState.getStyleClass().removeAll(
                "kubata-server-status-ok",
                "kubata-server-status-warning"
        );
        configState.getStyleClass().add(
                configured
                        ? "kubata-server-status-ok"
                        : "kubata-server-status-warning"
        );
    }

    private String buildDiagnosticsText() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();

        return """
                KUBATA · DIAGNÓSTICO DO SERVIDOR
                ================================

                Data/Hora: %s

                RUNTIME
                Java: %s
                JVM: %s
                SO: %s %s
                Processadores: %d
                Memória JVM usada: %s
                Memória JVM reservada: %s
                Memória JVM máxima: %s

                CONFIGURAÇÃO DA BD
                Driver: %s
                JDBC URL: %s
                Utilizador: %s
                Estado da configuração: %s

                DIRECTÓRIO
                %s

                NOTA
                Diagnóstico sem passwords. A presença dos parâmetros não confirma
                conectividade com a base de dados.
                """.formatted(
                LocalDateTime.now().format(DATE_TIME),
                System.getProperty("java.version", "—"),
                System.getProperty("java.vm.name", "—"),
                System.getProperty("os.name", "—"),
                System.getProperty("os.arch", "—"),
                Runtime.getRuntime().availableProcessors(),
                formatBytes(used),
                formatBytes(runtime.totalMemory()),
                formatBytes(runtime.maxMemory()),
                driver.isBlank() ? "Não definido" : driver,
                sanitizeJdbcUrl(jdbcUrl),
                username.isBlank() ? "Não definido" : username,
                (!jdbcUrl.isBlank() && !driver.isBlank())
                        ? "Parâmetros presentes"
                        : "Incompleta",
                Paths.get(System.getProperty("user.dir", "."))
                        .toAbsolutePath()
                        .normalize()
        );
    }

    private void copyDiagnostics() {
        copyText(buildDiagnosticsText());
    }

    private void copySafeJdbcUrl() {
        copyText(sanitizeJdbcUrl(jdbcUrl));
    }

    private void copyText(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value == null ? "" : value);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void exportDiagnostics() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar diagnóstico do servidor");
        chooser.setInitialFileName("kubata-server-diagnostico.txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ficheiro de texto (*.txt)", "*.txt")
        );

        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target != null) {
            saveTextFile(target.toPath(), buildDiagnosticsText());
        }
    }

    private void saveTextFile(String text) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar diagnóstico");
        chooser.setInitialFileName("kubata-server-diagnostico.txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ficheiro de texto (*.txt)", "*.txt")
        );

        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target != null) {
            saveTextFile(target.toPath(), text);
        }
    }

    private void saveTextFile(Path target, String text) {
        try {
            Files.writeString(target, text);
        } catch (IOException ex) {
            // A view de infraestrutura não depende de uma camada de diálogo externa.
            // O erro fica observável no stdout, sem expor segredos.
            ex.printStackTrace();
        }
    }

    private String sanitizeJdbcUrl(String value) {
        if (value == null || value.isBlank()) {
            return "Não definido";
        }

        String result = value
                .replaceAll("(?i)(password|passwd|pwd)=([^;]+)", "$1=********")
                .replaceAll("(?i)(secret|token)=([^;]+)", "$1=********");

        int scheme = result.indexOf("://");
        if (scheme >= 0) {
            int start = scheme + 3;
            int at = result.indexOf('@', start);
            if (at > start) {
                int colon = result.indexOf(':', start);
                if (colon > start && colon < at) {
                    result = result.substring(0, colon + 1)
                            + "********"
                            + result.substring(at);
                }
            }
        }

        return result;
    }

    private String formatBytes(long value) {
        double bytes = Math.max(0, value);
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / (1024 * 1024 * 1024));
        }
        if (bytes >= 1024 * 1024) {
            return String.format(Locale.ROOT, "%.0f MB", bytes / (1024 * 1024));
        }
        if (bytes >= 1024) {
            return String.format(Locale.ROOT, "%.0f KB", bytes / 1024);
        }
        return String.format(Locale.ROOT, "%.0f B", bytes);
    }
}
