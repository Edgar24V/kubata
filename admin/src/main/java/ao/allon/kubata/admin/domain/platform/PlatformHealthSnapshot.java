package ao.allon.kubata.admin.domain.platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Snapshot imutável do estado global da plataforma.
 */
public record PlatformHealthSnapshot(
        String globalStatus,
        List<PlatformComponentHealth> components,
        long alertCount,
        List<PlatformOperationSummary> recentOperations,
        List<PlatformSessionSummary> sessions,
        LocalDateTime checkedAt) {

    public PlatformHealthSnapshot {
        components = List.copyOf(Objects.requireNonNull(components));
        recentOperations = List.copyOf(Objects.requireNonNull(recentOperations));
        sessions = List.copyOf(Objects.requireNonNull(sessions));
    }

    public PlatformComponentHealth component(String name) {
        return components.stream()
                .filter(c -> c.component().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public long activeSessionCount() {
        return sessions.size();
    }
}
