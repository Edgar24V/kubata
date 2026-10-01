package ao.allon.kubata.admin.domain.platform;

import java.time.LocalDateTime;

/**
 * Resumo seguro de uma operação recente para o Command Center.
 */
public record PlatformOperationSummary(
        String username,
        String action,
        String module,
        LocalDateTime timestamp,
        boolean success) {
}
