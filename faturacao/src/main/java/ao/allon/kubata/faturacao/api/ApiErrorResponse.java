package ao.allon.kubata.faturacao.api;

import java.time.Instant;

public record ApiErrorResponse(
        String requestId,
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
