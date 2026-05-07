package ao.allon.kubata.core.module.communication;

import java.util.Map;

/**
 * Resposta de uma comunicação entre módulos.
 */
public record ModuleResponse(
    boolean success,
    String message,
    Map<String, Object> data,
    String errorCode
) {
    
    public static ModuleResponse success(Map<String, Object> data) {
        return new ModuleResponse(true, "Success", data, null);
    }
    
    public static ModuleResponse success(String message, Map<String, Object> data) {
        return new ModuleResponse(true, message, data, null);
    }
    
    public static ModuleResponse error(String errorMessage) {
        return new ModuleResponse(false, errorMessage, null, "ERROR");
    }
    
    public static ModuleResponse error(String errorCode, String errorMessage) {
        return new ModuleResponse(false, errorMessage, null, errorCode);
    }
    
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return data != null ? (T) data.get(key) : null;
    }
}
