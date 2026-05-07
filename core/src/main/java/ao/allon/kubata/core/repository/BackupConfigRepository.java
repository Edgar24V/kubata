package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupConfigRepository extends JpaRepository<BackupConfig, Long> {
    
    Optional<BackupConfig> findFirstByOrderByIdAsc();
}
