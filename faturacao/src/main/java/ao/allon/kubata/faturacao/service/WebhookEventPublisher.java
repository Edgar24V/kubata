package ao.allon.kubata.faturacao.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebhookEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public WebhookEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(String type, Map<String, Object> data, Long empresaId) {
        eventPublisher.publishEvent(new WebhookApplicationEvent(type, data, empresaId));
    }

    public record WebhookApplicationEvent(
            String type,
            Map<String, Object> data,
            Long empresaId
    ) {}
}
