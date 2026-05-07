package ao.allon.kubata.core.module.event;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publicador central de eventos de módulos.
 * Permite comunicação assíncrona entre módulos.
 */
@Component
public class ModuleEventPublisher {
    
    private final List<ModuleEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Registra um listener de eventos.
     */
    public void addListener(ModuleEventListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove um listener de eventos.
     */
    public void removeListener(ModuleEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Publica um evento para todos os listeners registrados.
     */
    public void publish(ModuleEvent event) {
        for (ModuleEventListener listener : listeners) {
            if (listener.accepts(event.eventType())) {
                executor.submit(() -> {
                    try {
                        listener.onModuleEvent(event);
                    } catch (Exception e) {
                        System.err.println("Error processing module event: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * Cria e publica um evento.
     */
    public void publishEvent(String eventType, String sourceModuleId, String targetModuleId, Map<String, Object> payload) {
        publish(ModuleEvent.create(eventType, sourceModuleId, targetModuleId, payload));
    }
    
    /**
     * Publica um evento para um módulo específico.
     */
    public void publishToModule(String targetModuleId, String eventType, String sourceModuleId, Map<String, Object> payload) {
        ModuleEvent event = ModuleEvent.create(eventType, sourceModuleId, targetModuleId, payload);
        publish(event);
    }
    
    /**
     * Publica um evento de broadcast para todos os módulos.
     */
    public void broadcast(String eventType, String sourceModuleId, Map<String, Object> payload) {
        publish(ModuleEvent.create(eventType, sourceModuleId, null, payload));
    }
}
