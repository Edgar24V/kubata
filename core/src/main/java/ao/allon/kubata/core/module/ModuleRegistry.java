package ao.allon.kubata.core.module;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro central de módulos do Kubata.
 * Gerencia o carregamento e acesso a todos os módulos do sistema.
 */
@Component
public class ModuleRegistry {
    
    private final Map<String, KubataModule> modules = new ConcurrentHashMap<>();
    private final List<ModuleRegistrationListener> listeners = new ArrayList<>();
    
    /**
     * Registra um módulo no sistema
     */
    public void registerModule(KubataModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        
        String moduleId = module.getModuleId();
        if (modules.containsKey(moduleId)) {
            throw new IllegalStateException("Module already registered: " + moduleId);
        }
        
        modules.put(moduleId, module);
        
        // Notifica listeners
        listeners.forEach(listener -> listener.onModuleRegistered(module));
    }
    
    /**
     * Remove um módulo do sistema
     */
    public void unregisterModule(String moduleId) {
        KubataModule removed = modules.remove(moduleId);
        if (removed != null) {
            listeners.forEach(listener -> listener.onModuleUnregistered(removed));
        }
    }
    
    /**
     * Obtém um módulo pelo ID
     */
    public Optional<KubataModule> getModule(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId));
    }
    
    /**
     * Lista todos os módulos registrados
     */
    public Collection<KubataModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }
    
    /**
     * Lista módulos ativos ordenados por nome
     */
    public List<KubataModule> getActiveModules() {
        return modules.values().stream()
            .filter(KubataModule::isActive)
            .sorted(Comparator.comparing(KubataModule::getModuleName))
            .toList();
    }
    
    /**
     * Verifica se um módulo está registrado
     */
    public boolean isModuleRegistered(String moduleId) {
        return modules.containsKey(moduleId);
    }
    
    /**
     * Adiciona um listener para eventos de registro
     */
    public void addRegistrationListener(ModuleRegistrationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove um listener
     */
    public void removeRegistrationListener(ModuleRegistrationListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Interface para listeners de registro de módulos
     */
    public interface ModuleRegistrationListener {
        void onModuleRegistered(KubataModule module);
        void onModuleUnregistered(KubataModule module);
    }
}
