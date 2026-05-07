package ao.allon.kubata.core.module.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento disparado quando ocorre uma mudança de estado em um módulo.
 */
public record ModuleEvent(
    String eventId,
    String eventType,
    String sourceModuleId,
    String targetModuleId,
    LocalDateTime timestamp,
    Map<String, Object> payload
) {
    
    public enum EventType {
        MODULE_STARTED,
        MODULE_STOPPED,
        MODULE_ERROR,
        DATA_CHANGED,
        REQUEST_DATA,
        PROVIDE_DATA,
        SYNC_REQUEST,
        SYNC_COMPLETE
    }
    
    public static ModuleEvent create(String eventType, String sourceModuleId, String targetModuleId, Map<String, Object> payload) {
        return new ModuleEvent(
            java.util.UUID.randomUUID().toString(),
            eventType,
            sourceModuleId,
            targetModuleId,
            LocalDateTime.now(),
            payload
        );
    }
}
