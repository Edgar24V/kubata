package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Monitor mínimo da JVM e do processo (memória, runtime).
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
        HBox head = new HBox(12);
        head.setPadding(new Insets(12, 16, 12, 16));
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("header-box");
        Label title = new Label("Monitor do sistema", IconUtils.icon(Feather.ACTIVITY, 18));
        title.getStyleClass().add("h3");
        javafx.scene.layout.Pane sp = new javafx.scene.layout.Pane();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btn = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btn.setOnAction(e -> refresh());
        head.getChildren().addAll(title, sp, btn);

        GridPane g = new GridPane();
        g.setPadding(new Insets(20));
        g.setHgap(20);
        g.setVgap(10);
        g.getStyleClass().add("card");
        int r = 0;
        g.add(new Label("Memória heap usada (MB)"), 0, r);
        g.add(lblHeap, 1, r++);
        g.add(new Label("Memória heap máxima (MB)"), 0, r);
        g.add(lblMax, 1, r++);
        g.add(new Label("Memória livre aproximada (MB)"), 0, r);
        g.add(lblFree, 1, r++);
        g.add(new Label("Threads activas"), 0, r);
        g.add(lblThreads, 1, r++);
        g.add(new Label("Java"), 0, r);
        g.add(lblJava, 1, r++);
        g.add(new Label("Sistema operativo"), 0, r);
        g.add(lblOs, 1, r++);

        getChildren().addAll(head, g);
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
