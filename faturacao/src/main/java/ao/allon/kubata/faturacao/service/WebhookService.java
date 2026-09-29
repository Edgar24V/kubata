package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import ao.allon.kubata.faturacao.api.WebhookSignatureService;
import ao.allon.kubata.faturacao.domain.WebhookDelivery;
import ao.allon.kubata.faturacao.repository.WebhookDeliveryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration STALE_SENDING_AFTER = Duration.ofMinutes(5);

    private final WebhookDeliveryRepository deliveryRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final ObjectMapper objectMapper;
    private final WebhookSignatureService signatureService;
    private final RestClient restClient;
    private final TaskExecutor taskExecutor;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public WebhookService(
            WebhookDeliveryRepository deliveryRepository,
            ParametroSistemaRepository parametroRepository,
            ObjectMapper objectMapper,
            @Qualifier("webhookTaskExecutor") TaskExecutor webhookTaskExecutor) {

        this.deliveryRepository = deliveryRepository;
        this.parametroRepository = parametroRepository;
        this.objectMapper = objectMapper;
        this.signatureService = new WebhookSignatureService();
        this.taskExecutor = webhookTaskExecutor;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Transactional
    public Optional<WebhookDelivery> enqueue(String eventType, Map<String, Object> data, Long empresaId) {
        String endpoint = read("INTEGRACAO_WEBHOOK_URL");
        String secret = read("INTEGRACAO_WEBHOOK_SECRET");

        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(secret)) {
            return Optional.empty();
        }

        validateEndpoint(endpoint);

        String eventId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", eventId);
        envelope.put("type", eventType);
        envelope.put("version", "1");
        envelope.put("timestamp", timestamp);
        envelope.put("empresaId", empresaId);
        envelope.put("data", data == null ? Map.of() : data);

        try {
            String payload = objectMapper.writeValueAsString(envelope);
            String signature = signatureService.sign(timestamp, payload, secret);

            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setEventId(eventId);
            delivery.setEventType(eventType);
            delivery.setEmpresaId(empresaId);
            delivery.setEndpointUrl(endpoint);
            delivery.setPayload(payload);
            delivery.setSignature(signature);
            delivery.setStatus(WebhookDelivery.Status.PENDING);
            delivery.setAttempts(0);
            delivery.setNextAttemptAt(LocalDateTime.now());

            WebhookDelivery saved = deliveryRepository.save(delivery);
            taskExecutor.execute(() -> deliver(saved.getId()));
            return Optional.of(saved);

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar o evento de webhook.", ex);
        }
    }

    @Scheduled(
            fixedDelayString = "${kubata.webhooks.retry-interval-ms:30000}",
            initialDelayString = "${kubata.webhooks.retry-initial-delay-ms:10000}"
    )
    public void retryPending() {
        LocalDateTime now = LocalDateTime.now();

        deliveryRepository
                .findTop50ByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
                        WebhookDelivery.Status.SENDING,
                        now.minus(STALE_SENDING_AFTER)
                )
                .forEach(delivery -> resetStale(delivery.getId()));

        deliveryRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        WebhookDelivery.Status.PENDING,
                        now
                )
                .forEach(delivery -> taskExecutor.execute(() -> deliver(delivery.getId())));
    }

    private void resetStale(Long deliveryId) {
        deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
            if (delivery.getStatus() == WebhookDelivery.Status.SENDING) {
                delivery.setStatus(WebhookDelivery.Status.PENDING);
                delivery.setNextAttemptAt(LocalDateTime.now());
                delivery.setLastError("Entrega recuperada após ficar em estado SENDING.");
                deliveryRepository.save(delivery);
            }
        });
    }

    public void deliver(Long deliveryId) {
        if (!inFlight.add(deliveryId)) {
            return;
        }

        try {
            WebhookDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
            if (delivery == null || delivery.getStatus() == WebhookDelivery.Status.DELIVERED) {
                return;
            }

            delivery.setStatus(WebhookDelivery.Status.SENDING);
            delivery.setLastAttemptAt(LocalDateTime.now());
            delivery.setAttempts(delivery.getAttempts() + 1);
            deliveryRepository.save(delivery);

            try {
                String timestamp = objectMapper.readTree(delivery.getPayload()).path("timestamp").asText();

                var response = restClient.post()
                        .uri(URI.create(delivery.getEndpointUrl()))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Kubata-Webhooks/1.0")
                        .header("X-Kubata-Webhook-Id", delivery.getEventId())
                        .header("X-Kubata-Webhook-Timestamp", timestamp)
                        .header("X-Kubata-Webhook-Signature", delivery.getSignature())
                        .body(delivery.getPayload())
                        .exchange((request, clientResponse) -> {
                            String body = clientResponse.getBody() == null
                                    ? ""
                                    : new String(
                                            clientResponse.getBody().readAllBytes(),
                                            java.nio.charset.StandardCharsets.UTF_8
                                    );
                            return new WebhookHttpResult(clientResponse.getStatusCode(), body);
                        });

                delivery.setResponseStatus(response.status().value());
                delivery.setResponseBody(truncate(response.body(), 2000));

                if (response.status().is2xxSuccessful()) {
                    delivery.setStatus(WebhookDelivery.Status.DELIVERED);
                    delivery.setDeliveredAt(LocalDateTime.now());
                    delivery.setNextAttemptAt(null);
                    delivery.setLastError(null);
                } else if (shouldRetry(response.status().value(), delivery.getAttempts())) {
                    scheduleRetry(delivery, "HTTP " + response.status().value());
                } else {
                    delivery.setStatus(WebhookDelivery.Status.FAILED);
                    delivery.setNextAttemptAt(null);
                    delivery.setLastError("Endpoint devolveu HTTP " + response.status().value());
                }

                deliveryRepository.save(delivery);

            } catch (Exception ex) {
                if (shouldRetry(0, delivery.getAttempts())) {
                    scheduleRetry(delivery, truncate(ex.getMessage(), 2000));
                } else {
                    delivery.setStatus(WebhookDelivery.Status.FAILED);
                    delivery.setNextAttemptAt(null);
                    delivery.setLastError(truncate(ex.getMessage(), 2000));
                }
                deliveryRepository.save(delivery);
            }
        } finally {
            inFlight.remove(deliveryId);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<WebhookDelivery> listRecent() {
        return deliveryRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public Optional<WebhookDelivery> retry(Long id) {
        return deliveryRepository.findById(id).map(delivery -> {
            delivery.setStatus(WebhookDelivery.Status.PENDING);
            delivery.setNextAttemptAt(LocalDateTime.now());
            delivery.setLastError(null);
            WebhookDelivery saved = deliveryRepository.save(delivery);
            taskExecutor.execute(() -> deliver(saved.getId()));
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public long count(WebhookDelivery.Status status) {
        return deliveryRepository.countByStatus(status);
    }

    private boolean shouldRetry(int status, int attempts) {
        if (attempts >= MAX_ATTEMPTS) {
            return false;
        }
        return status == 0 || status == 408 || status == 425 || status == 429 || status >= 500;
    }

    private void scheduleRetry(WebhookDelivery delivery, String error) {
        delivery.setStatus(WebhookDelivery.Status.PENDING);
        delivery.setLastError(error);
        delivery.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(delivery.getAttempts())));
    }

    private long backoffSeconds(int attempts) {
        return switch (attempts) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 300;
            case 4 -> 900;
            default -> 1800;
        };
    }

    private String read(String key) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(key)
                .map(ParametroSistema::getValor)
                .orElse("");
    }

    private void validateEndpoint(String endpoint) {
        URI uri = URI.create(endpoint);
        String scheme = uri.getScheme();

        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("A URL do webhook deve usar HTTP ou HTTPS.");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("A URL do webhook é inválida.");
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record WebhookHttpResult(HttpStatusCode status, String body) {}
}
