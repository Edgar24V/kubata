package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findTop50ByOrderByCreatedAtDesc();

    List<WebhookDelivery> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            WebhookDelivery.Status status,
            LocalDateTime nextAttemptAt
    );

    List<WebhookDelivery> findTop50ByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
            WebhookDelivery.Status status,
            LocalDateTime lastAttemptAt
    );

    @Query("select count(w) from WebhookDelivery w where w.status = :status")
    long countByStatus(@Param("status") WebhookDelivery.Status status);
}
