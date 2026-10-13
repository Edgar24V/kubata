package ao.allon.kubata.admin.ui;

import ao.allon.kubata.admin.ui.fxribbon.Ribbon;
import ao.allon.kubata.admin.ui.fxribbon.RibbonProgrammaticService;
import ao.allon.kubata.admin.ui.fxribbon.RibbonTab;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class ModuleNavigationIntegrationTest {

    @BeforeAll
    static void initializeJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // O toolkit já foi inicializado por outro teste desta JVM.
        }
    }

    @Autowired
    private RibbonProgrammaticService ribbonService;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Test
    void deveExporOsModulosRegistadosNoRibbonCentral() {
        User administrator = new User();
        administrator.setNome("Administrator Integration");
        administrator.setRole(Role.ADMIN);

        Ribbon ribbon = new Ribbon();
        ribbonService.buildRibbon(
                ribbon,
                administrator,
                (id, title, content, closable) -> {},
                (Class<? extends Node> viewType) -> new Label("Ecrã de teste")
        );

        RibbonTab modulesTab = ribbon.getTabs().stream()
                .filter(tab -> "Módulos".equals(tab.getTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(modulesTab, "O Ribbon deve disponibilizar a aba Módulos.");

        Set<String> moduleGroups = modulesTab.getGroups().stream()
                .map(group -> group.getGroupTitle())
                .collect(Collectors.toSet());

        for (KubataModule module : moduleRegistry.getAllModules()) {
            assertTrue(
                    moduleGroups.contains(module.getModuleName()),
                    "O módulo registado '" + module.getModuleId()
                            + "' deve ter uma entrada no acesso central."
            );
        }
    }
}
