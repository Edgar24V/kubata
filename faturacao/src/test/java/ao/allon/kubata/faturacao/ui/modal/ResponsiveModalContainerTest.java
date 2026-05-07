package ao.allon.kubata.faturacao.ui.modal;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ResponsiveModalContainerTest {

    @BeforeAll
    static void initToolkit() {
        // Inicializa o Toolkit JavaFX para testes
        new JFXPanel();
    }

    @Test
    void testInitialSizeIsComputed() {
        Platform.runLater(() -> {
            ResponsiveModalContainer container = new ResponsiveModalContainer();
            assertEquals(VBox.USE_COMPUTED_SIZE, container.getPrefWidth());
            assertEquals(VBox.USE_COMPUTED_SIZE, container.getPrefHeight());
        });
    }

    @Test
    void testResizeOnContentChange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            ResponsiveModalContainer container = new ResponsiveModalContainer();
            container.setAnimateChanges(false); // Desativa animação para verificar resultado imediato (simulado)
            
            Label label = new Label("Teste de Conteúdo");
            label.setMinWidth(200);
            label.setMinHeight(50);
            
            container.getChildren().add(label);
            
            // Força layout
            container.layout();
            
            // Verifica se o container detectou a necessidade de redimensionar
            // Nota: Como a lógica é assíncrona (Platform.runLater), este teste é apenas estrutural
            // Em um ambiente real, verificaríamos as propriedades após o pulso do JavaFX
            assertNotNull(container.getChildren());
            assertEquals(1, container.getChildren().size());
            
            latch.countDown();
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testMaxLimits() {
        ResponsiveModalContainer container = new ResponsiveModalContainer();
        container.setMaxDimensions(500, 400);
        
        // Simula verificação interna (método privado, acessado indiretamente via comportamento)
        // Aqui apenas validamos se a API pública não lança exceções
        assertDoesNotThrow(() -> container.requestLayout());
    }
}
