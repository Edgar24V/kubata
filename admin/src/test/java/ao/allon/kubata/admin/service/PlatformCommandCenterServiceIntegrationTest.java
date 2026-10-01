package ao.allon.kubata.admin.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("sqlite")
class PlatformCommandCenterServiceIntegrationTest {

    @BeforeAll
    static void setupJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit já inicializado.
        }
    }

    @Autowired
    private PlatformCommandCenterService commandCenter;

    @Test
    void deveDisponibilizarCommandCenterNoContextoSpring() {
        assertNotNull(commandCenter);
    }

    @Test
    void deveRecusarHealthCheckSemSessaoAutenticada() {
        assertThrows(SecurityException.class, () -> commandCenter.checkGlobal(null));
    }
}
