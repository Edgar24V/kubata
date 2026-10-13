package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class ModuleInstallationServiceIntegrationTest {

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Autowired
    private ModuleInstallationService installationService;

    @Autowired
    private ModuloSistemaRepository moduloRepository;

    @Test
    void deveSincronizarCatalogoApenasComModulosRegistadosEConservarVersoes() {
        var registeredModules = moduleRegistry.getAllModules();
        assertFalse(registeredModules.isEmpty(), "O runtime deve registar os módulos disponíveis.");

        var synced = installationService.synchronizeCatalog();
        Map<String, ModuloSistema> syncedByCode = synced.stream()
                .collect(Collectors.toMap(ModuloSistema::getCodigo, Function.identity()));

        assertEquals(registeredModules.size(), syncedByCode.size(),
                "A sincronização não deve produzir códigos de módulo duplicados.");

        for (KubataModule module : registeredModules) {
            ModuloSistema catalogEntry = moduloRepository.findByCodigo(module.getModuleId())
                    .orElseThrow(() -> new AssertionError(
                            "Módulo runtime ausente do catálogo: " + module.getModuleId()));

            assertEquals(module.getModuleName(), catalogEntry.getNome(),
                    "O nome do catálogo deve corresponder ao módulo registado.");
            assertEquals(module.getVersion(), catalogEntry.getVersao(),
                    "A versão do catálogo deve corresponder à versão do módulo no runtime.");
            assertNotNull(catalogEntry.getEstado(),
                    "Cada módulo registado deve ter um estado administrativo.");
        }
    }
}
