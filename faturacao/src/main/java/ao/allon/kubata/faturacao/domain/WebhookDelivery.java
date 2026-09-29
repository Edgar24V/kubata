package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kubata_webhook_delivery", indexes = {
        @Index(name = "idx_kubata_wh_status_next", columnList = "status,next_attempt_at"),
        @Index(name = "idx_kubata_wh_created", columnList = "created_at")
})
public class WebhookDelivery extends BaseEntity {

    public enum Status {
        PENDING,
        SENDING,
        DELIVERED,
        FAILED
    }

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "endpoint_url", nullable = false, length = 1000)
    private String endpointUrl;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "signature", nullable = false, length = 200)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
