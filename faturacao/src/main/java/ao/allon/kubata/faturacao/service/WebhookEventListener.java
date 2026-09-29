package ao.allon.kubata.faturacao.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WebhookEventListener {

    private final WebhookService webhookService;

    public WebhookEventListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WebhookEventPublisher.WebhookApplicationEvent event) {
        webhookService.enqueue(event.type(), event.data(), event.empresaId());
    }
}
