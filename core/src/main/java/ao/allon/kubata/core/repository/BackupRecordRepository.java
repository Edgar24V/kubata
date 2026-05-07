package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {

    List<BackupRecord> findByOrderByStartTimeDesc();

    List<BackupRecord> findByStatusOrderByStartTimeDesc(BackupRecord.BackupStatus status);

    Optional<BackupRecord> findTopByStatusOrderByStartTimeDesc(BackupRecord.BackupStatus status);

    long countByStatus(BackupRecord.BackupStatus status);

    @Query("SELECT COALESCE(SUM(b.fileSize), 0) FROM BackupRecord b WHERE b.status = 'COMPLETED'")
    Long sumFileSize();
}
