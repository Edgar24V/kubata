package ao.allon.kubata.admin.module;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de carregamento dinâmico de módulos no admin.
 * Gerencia a inicialização e integração de módulos na interface.
 */
@Service
public class ModuleLoaderService {
    
    private final ModuleRegistry moduleRegistry;
    private ModuleLoadCallback loadCallback;
    
    public ModuleLoaderService(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }
    
    /**
     * Define o callback para notificar quando módulos são carregados.
     */
    public void setLoadCallback(ModuleLoadCallback callback) {
        this.loadCallback = callback;
    }
    
    /**
     * Carrega todos os módulos ativos e os integra na UI.
     */
    public void loadAllModules() {
        List<KubataModule> activeModules = moduleRegistry.getActiveModules();
        
        for (KubataModule module : activeModules) {
            loadModule(module);
        }
    }
    
    /**
     * Carrega um módulo específico na interface.
     */
    public void loadModule(KubataModule module) {
        try {
            module.initialize();
            
            Platform.runLater(() -> {
                if (loadCallback != null) {
                    loadCallback.onModuleLoaded(module);
                }
            });
            
            System.out.println("Module loaded: " + module.getModuleName());
        } catch (Exception e) {
            System.err.println("Error loading module " + module.getModuleId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Descarrega um módulo da interface.
     */
    public void unloadModule(String moduleId) {
        KubataModule module = moduleRegistry.getModule(moduleId).orElse(null);
        if (module != null) {
            Platform.runLater(() -> {
                if (loadCallback != null) {
                    loadCallback.onModuleUnloaded(module);
                }
            });
        }
    }
    
    /**
     * Recarrega um módulo (descarrega e carrega novamente).
     */
    public void reloadModule(String moduleId) {
        unloadModule(moduleId);
        moduleRegistry.getModule(moduleId).ifPresent(this::loadModule);
    }
    
    /**
     * Interface de callback para notificações de carregamento.
     */
    public interface ModuleLoadCallback {
        void onModuleLoaded(KubataModule module);
        void onModuleUnloaded(KubataModule module);
    }
}
