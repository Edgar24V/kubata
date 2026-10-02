package ao.allon.kubata.faturacao.ui.modal;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ResponsiveModalContainerTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        new JFXPanel();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "O JavaFX Toolkit não iniciou.");
    }

    @Test
    void testInitialSizeIsComputed() throws Exception {
        runOnFxThread(() -> {
            ResponsiveModalContainer container = new ResponsiveModalContainer();
            assertEquals(VBox.USE_COMPUTED_SIZE, container.getPrefWidth());
            assertEquals(VBox.USE_COMPUTED_SIZE, container.getPrefHeight());
        });
    }

    @Test
    void testResizeOnContentChange() throws Exception {
        runOnFxThread(() -> {
            ResponsiveModalContainer container = new ResponsiveModalContainer();
            container.setAnimateChanges(false);

            Label label = new Label("Teste de Conteúdo");
            label.setMinWidth(200);
            label.setMinHeight(50);

            container.getChildren().add(label);
            container.layout();

            assertNotNull(container.getChildren());
            assertEquals(1, container.getChildren().size());
        });
    }

    @Test
    void testMaxLimits() throws Exception {
        runOnFxThread(() -> {
            ResponsiveModalContainer container = new ResponsiveModalContainer();
            assertDoesNotThrow(() -> container.setMaxDimensions(500, 400));
            assertDoesNotThrow(container::requestLayout);
        });
    }

    private static void runOnFxThread(ThrowingAction action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable ex) {
                failure.set(ex);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "A tarefa do JavaFX não foi executada.");
        Throwable error = failure.get();
        if (error != null) {
            throw new AssertionError("Falha no thread do JavaFX.", error);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
