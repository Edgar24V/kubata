package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.RecordLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecordLockRepository extends JpaRepository<RecordLock, Long> {
    Optional<RecordLock> findByEntityTypeAndEntityId(String entityType, String entityId);
    void deleteByEntityTypeAndEntityId(String entityType, String entityId);
}
