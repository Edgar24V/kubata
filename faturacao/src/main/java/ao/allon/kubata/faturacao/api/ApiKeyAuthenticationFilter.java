package ao.allon.kubata.faturacao.api;

import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_AUTHORIZATION_PREFIX = "ApiKey ";

    private final ParametroSistemaRepository parametroRepository;

    public ApiKeyAuthenticationFilter(ParametroSistemaRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = uri.startsWith(context) ? uri.substring(context.length()) : uri;

        return "/api/v1/health".equals(path)
                || "/api/v1/info".equals(path)
                || path.startsWith("/api/v1/openapi");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader("X-Request-Id");
        if (!StringUtils.hasText(requestId)) {
            requestId = java.util.UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        boolean enabled = readBoolean("INTEGRACAO_API_ENABLED", false);
        if (!enabled) {
            writeError(response, requestId, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API_DISABLED",
                    "A API REST está desactivada nas configurações de integração.",
                    request.getRequestURI());
            return;
        }

        String configuredKey = read("INTEGRACAO_API_KEY");
        if (!StringUtils.hasText(configuredKey)) {
            writeError(response, requestId, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API_KEY_NOT_CONFIGURED",
                    "A API está activa, mas não existe uma API Key configurada.",
                    request.getRequestURI());
            return;
        }

        String suppliedKey = request.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(suppliedKey)) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith(API_KEY_AUTHORIZATION_PREFIX)) {
                suppliedKey = authorization.substring(API_KEY_AUTHORIZATION_PREFIX.length()).trim();
            }
        }

        if (!StringUtils.hasText(suppliedKey) || !constantTimeEquals(configuredKey, suppliedKey)) {
            response.setHeader("WWW-Authenticate", "ApiKey realm=\"Kubata API\"");
            writeError(response, requestId, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_API_KEY",
                    "API Key inválida ou ausente.",
                    request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String read(String key) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(key)
                .map(ParametroSistema::getValor)
                .orElse("");
    }

    private boolean readBoolean(String key, boolean fallback) {
        String value = read(key);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : fallback;
    }

    private boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeError(
            HttpServletResponse response,
            String requestId,
            int status,
            String error,
            String message,
            String path) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String json = """
                {"requestId":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}
                """.formatted(
                escape(requestId),
                status,
                escape(error),
                escape(message),
                escape(path)
        ).trim();

        response.getWriter().write(json);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}