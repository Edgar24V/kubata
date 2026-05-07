package ao.allon.kubata.admin.controller;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.ModuleView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST para gestão de módulos.
 * Permite ativar/desativar módulos e consultar estado via API.
 */
@RestController
@RequestMapping("/api/modules")
public class ModuleManagementController {
    
    private final ModuleRegistry moduleRegistry;
    
    public ModuleManagementController(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }
    
    /**
     * Lista todos os módulos registrados.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAllModules() {
        List<Map<String, Object>> modules = moduleRegistry.getAllModules().stream()
            .map(this::mapModuleToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(modules);
    }
    
    /**
     * Lista apenas módulos ativos.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> listActiveModules() {
        List<Map<String, Object>> modules = moduleRegistry.getActiveModules().stream()
            .map(this::mapModuleToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(modules);
    }
    
    /**
     * Obtém detalhes de um módulo específico.
     */
    @GetMapping("/{moduleId}")
    public ResponseEntity<Map<String, Object>> getModule(@PathVariable String moduleId) {
        return moduleRegistry.getModule(moduleId)
            .map(module -> ResponseEntity.ok(mapModuleToResponse(module)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Ativa ou desativa um módulo.
     */
    @PostMapping("/{moduleId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleModule(@PathVariable String moduleId) {
        return moduleRegistry.getModule(moduleId)
            .map(module -> {
                if (module instanceof ao.allon.kubata.core.module.AbstractKubataModule) {
                    ao.allon.kubata.core.module.AbstractKubataModule abstractModule = 
                        (ao.allon.kubata.core.module.AbstractKubataModule) module;
                    abstractModule.setActive(!module.isActive());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("moduleId", moduleId);
                    response.put("active", abstractModule.isActive());
                    response.put("message", "Module state updated successfully");
                    return ResponseEntity.ok(response);
                }
                return ResponseEntity.badRequest().<Map<String, Object>>body(
                    Map.of("error", "Module does not support toggle")
                );
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lista as views de um módulo.
     */
    @GetMapping("/{moduleId}/views")
    public ResponseEntity<List<Map<String, Object>>> getModuleViews(@PathVariable String moduleId) {
        return moduleRegistry.getModule(moduleId)
            .map(module -> {
                List<Map<String, Object>> views = module.getModuleViews().stream()
                    .map(this::mapViewToResponse)
                    .collect(Collectors.toList());
                return ResponseEntity.ok(views);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private Map<String, Object> mapModuleToResponse(KubataModule module) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", module.getModuleId());
        response.put("name", module.getModuleName());
        response.put("description", module.getModuleDescription());
        response.put("version", module.getVersion());
        response.put("active", module.isActive());
        response.put("viewCount", module.getModuleViews().size());
        response.put("requiredPermissions", module.getRequiredPermissions());
        return response;
    }
    
    private Map<String, Object> mapViewToResponse(ModuleView view) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", view.viewId());
        response.put("name", view.viewName());
        response.put("description", view.viewDescription());
        response.put("permission", view.permission());
        response.put("displayOrder", view.displayOrder());
        return response;
    }
}
