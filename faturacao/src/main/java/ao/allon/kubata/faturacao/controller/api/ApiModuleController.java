package ao.allon.kubata.faturacao.controller.api;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.ModuleView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/modules")
@SecurityRequirement(name = "kubataApiKey")
public class ApiModuleController {

    private final ModuleRegistry moduleRegistry;

    public ApiModuleController(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        return ResponseEntity.ok(moduleRegistry.getAllModules().stream()
                .map(this::mapModule)
                .toList());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> listActive() {
        return ResponseEntity.ok(moduleRegistry.getActiveModules().stream()
                .map(this::mapModule)
                .toList());
    }

    @GetMapping("/{moduleId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable("moduleId") String moduleId) {
        return moduleRegistry.getModule(moduleId)
                .map(module -> ResponseEntity.ok(mapModule(module)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> mapModule(KubataModule module) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", module.getModuleId());
        response.put("name", module.getModuleName());
        response.put("description", module.getModuleDescription());
        response.put("version", module.getVersion());
        response.put("active", module.isActive());
        response.put("viewCount", module.getModuleViews().size());
        response.put("requiredPermissions", module.getRequiredPermissions());
        response.put("views", module.getModuleViews().stream()
                .map(this::mapView)
                .toList());
        return response;
    }

    private Map<String, Object> mapView(ModuleView view) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", view.viewId());
        response.put("name", view.viewName());
        response.put("description", view.viewDescription());
        response.put("permission", view.permission());
        response.put("displayOrder", view.displayOrder());
        return response;
    }
}
