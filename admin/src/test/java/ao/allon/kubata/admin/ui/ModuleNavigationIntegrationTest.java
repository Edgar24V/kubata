package ao.allon.kubata.admin.ui;

import ao.allon.kubata.admin.ui.fxribbon.Ribbon;
import ao.allon.kubata.admin.ui.fxribbon.RibbonProgrammaticService;
import ao.allon.kubata.admin.ui.fxribbon.RibbonTab;
import ao.allon.kubata.admin.ui.fxribbon.RibbonButton;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
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
        List<String> openedIds = new ArrayList<>();
        List<String> openedTitles = new ArrayList<>();
        List<java.util.function.Supplier<Node>> openedContent = new ArrayList<>();

        ribbonService.buildRibbon(
                ribbon,
                administrator,
                (id, title, content, closable) -> {
                    openedIds.add(id);
                    openedTitles.add(title);
                    openedContent.add(content);
                },
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
            assertFalse(
                    module.getModuleViews().isEmpty(),
                    "O módulo '" + module.getModuleId()
                            + "' deve registar as suas funcionalidades antes de ser apresentado no acesso central."
            );
            assertTrue(
                    moduleGroups.contains(module.getModuleName()),
                    "O módulo registado '" + module.getModuleId()
                            + "' deve ter uma entrada no acesso central."
            );

            var group = modulesTab.getGroups().stream()
                    .filter(candidate -> candidate.getGroupTitle().equals(module.getModuleName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Não foi encontrado o grupo de Ribbon do módulo " + module.getModuleId()));

            RibbonButton openButton = group.getContentBox().getChildren().stream()
                    .filter(VBox.class::isInstance)
                    .map(VBox.class::cast)
                    .flatMap(column -> column.getChildren().stream())
                    .filter(RibbonButton.class::isInstance)
                    .map(RibbonButton.class::cast)
                    .filter(button -> ("open_module_" + module.getModuleId()).equals(button.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Não foi encontrado o comando de abertura de " + module.getModuleId()));

            openButton.fire();

            assertTrue(openedIds.contains("module_" + module.getModuleId()),
                    "O comando do módulo deve abrir o espaço de trabalho central.");
            assertTrue(openedTitles.contains(module.getModuleName()),
                    "O espaço de trabalho deve identificar o módulo aberto.");
        }

        assertEquals(moduleRegistry.getAllModules().size(), openedContent.size(),
                "Cada módulo registado deve accionar exactamente uma abertura central.");
    }
}
