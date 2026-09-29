package ao.allon.kubata.faturacao.controller.api;

import ao.allon.kubata.faturacao.domain.WebhookDelivery;
import ao.allon.kubata.faturacao.service.WebhookService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@SecurityRequirement(name = "kubataApiKey")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<Map<String, Object>>> deliveries() {
        return ResponseEntity.ok(webhookService.listRecent().stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        WebhookDelivery delivery = webhookService
                .enqueue(
                        "webhook.test",
                        Map.of(
                                "message", "Teste de integração do Kubata",
                                "requestedAt", LocalDateTime.now()
                        ),
                        null
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Webhook não configurado. Defina URL e segredo em API & Webhooks."
                ));

        return ResponseEntity.accepted().body(toResponse(delivery));
    }

    @PostMapping("/deliveries/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable("id") Long id) {
        return webhookService.retry(id)
                .map(delivery -> ResponseEntity.accepted().body(toResponse(delivery)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pending", webhookService.count(WebhookDelivery.Status.PENDING));
        stats.put("sending", webhookService.count(WebhookDelivery.Status.SENDING));
        stats.put("delivered", webhookService.count(WebhookDelivery.Status.DELIVERED));
        stats.put("failed", webhookService.count(WebhookDelivery.Status.FAILED));
        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> toResponse(WebhookDelivery delivery) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", delivery.getId());
        response.put("eventId", delivery.getEventId());
        response.put("eventType", delivery.getEventType());
        response.put("empresaId", delivery.getEmpresaId());
        response.put("endpointUrl", delivery.getEndpointUrl());
        response.put("status", delivery.getStatus().name());
        response.put("attempts", delivery.getAttempts());
        response.put("nextAttemptAt", delivery.getNextAttemptAt());
        response.put("lastAttemptAt", delivery.getLastAttemptAt());
        response.put("deliveredAt", delivery.getDeliveredAt());
        response.put("responseStatus", delivery.getResponseStatus());
        response.put("lastError", delivery.getLastError());
        return response;
    }
}
