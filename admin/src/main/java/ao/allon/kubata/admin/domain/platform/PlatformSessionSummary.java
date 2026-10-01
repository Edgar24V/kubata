package ao.allon.kubata.admin.domain.platform;

import java.time.LocalDateTime;

/**
 * Resumo seguro de uma sessão persistida.
 */
public record PlatformSessionSummary(
        Long id,
        String username,
        String workstation,
        String ipAddress,
        String context,
        LocalDateTime loginTime) {
}
