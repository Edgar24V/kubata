package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Monitor mínimo da JVM e do processo (memória, runtime).
 *
 * <p>O ciclo de actualização e as métricas recolhidas permanecem os mesmos;
 * a alteração concentra-se na apresentação visual.</p>
 */
@Component
public class SystemMonitorView extends VBox {

    private final Label lblHeap = new Label();
    private final Label lblMax = new Label();
    private final Label lblFree = new Label();
    private final Label lblThreads = new Label();
    private final Label lblJava = new Label();
    private final Label lblOs = new Label();

    public SystemMonitorView() {
        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        refresh();
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(3), e -> refresh()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void buildUi() {
        VBox header = buildHeader();

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 20, 22, 20));

        content.getChildren().addAll(
                buildMetricGrid(),
                buildEnvironmentCards(),
                buildStatusNote()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("application-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(header, scroll);
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.getStyleClass().add("header-box");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(42, 42);
        iconBox.setPrefSize(42, 42);
        iconBox.setMaxSize(42, 42);
        iconBox.setStyle(
                "-fx-background-color: rgba(33,115,70,0.10);" +
                "-fx-background-radius: 12px;"
        );
        iconBox.getChildren().add(IconUtils.icon(Feather.ACTIVITY, 21));

        VBox titleBox = new VBox(2);

        Label title = new Label("Monitor do Sistema");
        title.setStyle(
                "-fx-font-size: 19px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #24292f;"
        );

        Label subtitle = new Label(
                "Visão rápida da JVM, memória, threads e ambiente onde o Kubata está a executar."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("text-muted");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar agora",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> refresh());

        line.getChildren().addAll(iconBox, titleBox, spacer, refresh);

        HBox status = new HBox(8);
        status.setAlignment(Pos.CENTER_LEFT);
        status.setPadding(new Insets(8, 10, 8, 10));
        status.setStyle(
                "-fx-background-color: #f6f8fa;" +
                "-fx-border-color: #eaeef2;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        Label statusIcon = new Label("", IconUtils.icon(Feather.ACTIVITY, 13));
        Label statusText = new Label("MONITORIZAÇÃO ACTIVA");
        statusText.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #217346;"
        );

        Label auto = new Label("Actualização automática a cada 3 segundos");
        auto.getStyleClass().add("text-muted");

        status.getChildren().addAll(statusIcon, statusText, auto);
        header.getChildren().addAll(line, status);

        return header;
    }

    private GridPane buildMetricGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(metricCard(
                "Memória heap usada",
                "MB",
                Feather.HARD_DRIVE,
                lblHeap
        ), 0, 0);

        grid.add(metricCard(
                "Memória heap máxima",
                "MB",
                Feather.SERVER,
                lblMax
        ), 1, 0);

        grid.add(metricCard(
                "Memória disponível",
                "MB",
                Feather.DATABASE,
                lblFree
        ), 2, 0);

        grid.add(metricCard(
                "Threads activas",
                "THREADS",
                Feather.ACTIVITY,
                lblThreads
        ), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        return grid;
    }

    private VBox metricCard(
            String title,
            String suffix,
            Feather iconType,
            Label value) {

        VBox card = new VBox(7);
        card.setPadding(new Insets(14));
        card.setMinHeight(104);
        card.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 10px;" +
                "-fx-border-color: #d0d7de;" +
                "-fx-border-radius: 10px;"
        );
        card.getStyleClass().add("card");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(30, 30);
        iconBox.setPrefSize(30, 30);
        iconBox.setMaxSize(30, 30);
        iconBox.setStyle(
                "-fx-background-color: #f6f8fa;" +
                "-fx-background-radius: 8px;"
        );
        iconBox.getChildren().add(IconUtils.icon(iconType, 14));

        Label name = new Label(title.toUpperCase());
        name.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #6e7781;" +
                "-fx-letter-spacing: 0.5px;"
        );

        heading.getChildren().addAll(iconBox, name);

        HBox valueLine = new HBox(5);
        valueLine.setAlignment(Pos.BASELINE_LEFT);

        value.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #24292f;"
        );

        Label unit = new Label(suffix);
        unit.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: #8c959f;"
        );

        valueLine.getChildren().addAll(value, unit);
        card.getChildren().addAll(heading, valueLine);

        return card;
    }

    private HBox buildEnvironmentCards() {
        HBox row = new HBox(12);

        VBox javaCard = environmentCard(
                "Runtime Java",
                Feather.CPU,
                "Versão e fornecedor da JVM",
                lblJava
        );

        VBox osCard = environmentCard(
                "Sistema operativo",
                Feather.MONITOR,
                "Plataforma onde o processo está activo",
                lblOs
        );

        HBox.setHgrow(javaCard, Priority.ALWAYS);
        HBox.setHgrow(osCard, Priority.ALWAYS);

        row.getChildren().addAll(javaCard, osCard);
        return row;
    }

    private VBox environmentCard(
            String title,
            Feather iconType,
            String description,
            Label value) {

        VBox card = new VBox(9);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(iconType, 15));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800;");

        heading.getChildren().addAll(icon, titleLabel);

        Label desc = new Label(description);
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        value.setWrapText(true);
        value.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: #24292f;"
        );

        card.getChildren().addAll(heading, desc, value);
        return card;
    }

    private HBox buildStatusNote() {
        HBox note = new HBox(10);
        note.setPadding(new Insets(12, 14, 12, 14));
        note.setAlignment(Pos.CENTER_LEFT);
        note.getStyleClass().add("card");

        Label icon = new Label("", IconUtils.icon(Feather.INFO, 14));

        VBox text = new VBox(2);

        Label title = new Label("Monitorização local");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 800;");

        Label desc = new Label(
                "Os valores apresentados representam o processo Java do Kubata. " +
                "A memória livre é uma estimativa baseada na heap máxima disponível."
        );
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        text.getChildren().addAll(title, desc);
        HBox.setHgrow(text, Priority.ALWAYS);

        note.getChildren().addAll(icon, text);
        return note;
    }

    private void refresh() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long used = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long max = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long free = (mem.getHeapMemoryUsage().getMax() - mem.getHeapMemoryUsage().getUsed()) / (1024 * 1024);

        lblHeap.setText(Long.toString(used));
        lblMax.setText(max > 0 ? Long.toString(max) : "n/d");
        lblFree.setText(max > 0 ? Long.toString(Math.max(free, 0)) : "n/d");
        lblThreads.setText(Integer.toString(ManagementFactory.getThreadMXBean().getThreadCount()));
        lblJava.setText(System.getProperty("java.version") + " — " + System.getProperty("java.vendor"));
        lblOs.setText(System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }
}
