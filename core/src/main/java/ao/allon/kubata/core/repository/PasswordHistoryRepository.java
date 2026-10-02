package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findTop100ByUserIdOrderByChangedAtDesc(Long userId);
}
