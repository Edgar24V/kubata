package ao.allon.kubata.admin.service;

import ao.allon.kubata.admin.domain.platform.PlatformComponentHealth;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformCommandCenterServiceTest {

    @Test
    void deveConsolidarEstadoGlobalComoOperacional() {
        List<PlatformComponentHealth> components = List.of(
                component("Base de Dados","OK"),
                component("Flyway","OK"),
                component("Módulos","OK")
        );

        assertEquals("OPERACIONAL", PlatformCommandCenterService.globalStatus(components));
    }

    @Test
    void deveConsolidarEstadoGlobalComoDegradadoQuandoExisteAlerta() {
        List<PlatformComponentHealth> components = List.of(
                component("Base de Dados","OK"),
                component("Licenças","WARNING")
        );

        assertEquals("DEGRADADO", PlatformCommandCenterService.globalStatus(components));
    }

    @Test
    void deveConsolidarEstadoGlobalComoIndisponivelQuandoExisteErro() {
        List<PlatformComponentHealth> components = List.of(
                component("Base de Dados","ERROR"),
                component("API","OK")
        );

        assertEquals("INDISPONÍVEL", PlatformCommandCenterService.globalStatus(components));
    }

    private static PlatformComponentHealth component(String name, String status) {
        return new PlatformComponentHealth(
                name,
                status,
                "teste",
                3,
                LocalDateTime.now()
        );
    }
}
