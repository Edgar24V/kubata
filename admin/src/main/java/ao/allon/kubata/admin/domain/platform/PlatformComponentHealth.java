package ao.allon.kubata.admin.domain.platform;

import java.time.LocalDateTime;

/**
 * Estado calculado de um componente do Command Center.
 */
public record PlatformComponentHealth(
        String component,
        String status,
        String message,
        long responseTimeMs,
        LocalDateTime checkedAt) {

    public boolean isOk() {
        return "OK".equalsIgnoreCase(status);
    }

    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(status);
    }

    public boolean isError() {
        return "ERROR".equalsIgnoreCase(status);
    }
}
