package ao.allon.kubata.core.module.communication;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.event.ModuleEvent;
import ao.allon.kubata.core.module.event.ModuleEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Serviço de comunicação entre módulos.
 * Permite solicitar dados e enviar mensagens entre módulos de forma assíncrona.
 */
@Service
public class ModuleCommunicationService {
    
    private final ModuleRegistry moduleRegistry;
    private final ModuleEventPublisher eventPublisher;
    private final Map<String, CompletableFuture<ModuleResponse>> pendingRequests = new ConcurrentHashMap<>();
    
    public ModuleCommunicationService(ModuleRegistry moduleRegistry, ModuleEventPublisher eventPublisher) {
        this.moduleRegistry = moduleRegistry;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Verifica se um módulo está ativo e disponível.
     */
    public boolean isModuleAvailable(String moduleId) {
        return moduleRegistry.getModule(moduleId)
            .map(KubataModule::isActive)
            .orElse(false);
    }
    
    /**
     * Solicita dados de outro módulo de forma assíncrona.
     */
    public CompletableFuture<ModuleResponse> requestData(String targetModuleId, String requestType, Map<String, Object> parameters) {
        if (!isModuleAvailable(targetModuleId)) {
            return CompletableFuture.completedFuture(
                ModuleResponse.error("Module not available: " + targetModuleId)
            );
        }
        
        String requestId = generateRequestId();
        CompletableFuture<ModuleResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        Map<String, Object> payload = Map.of(
            "requestId", requestId,
            "requestType", requestType,
            "parameters", parameters
        );
        
        eventPublisher.publishToModule(targetModuleId, ModuleEvent.EventType.REQUEST_DATA.name(), "system", payload);
        
        // Timeout após 30 segundos
        return future.orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(ex -> ModuleResponse.error("Request timeout: " + ex.getMessage()));
    }
    
    /**
     * Envia uma resposta para uma solicitação pendente.
     */
    public void sendResponse(String requestId, ModuleResponse response) {
        CompletableFuture<ModuleResponse> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }
    
    /**
     * Notifica outros módulos sobre mudança de dados.
     */
    public void notifyDataChanged(String sourceModuleId, String entityType, Long entityId, String operation) {
        Map<String, Object> payload = Map.of(
            "entityType", entityType,
            "entityId", entityId,
            "operation", operation,
            "sourceModule", sourceModuleId
        );
        
        eventPublisher.broadcast(ModuleEvent.EventType.DATA_CHANGED.name(), sourceModuleId, payload);
    }
    
    /**
     * Envia uma mensagem de broadcast para todos os módulos.
     */
    public void broadcast(String sourceModuleId, String messageType, Map<String, Object> data) {
        eventPublisher.broadcast(messageType, sourceModuleId, data);
    }
    
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }
}
