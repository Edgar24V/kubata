package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrderByTimestampDesc(Pageable pageable);

    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    List<AuditLog> findByActionTypeOrderByTimestampDesc(AuditLog.AuditActionType actionType);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findBySaftRelevantTrueOrderByTimestampDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:saftRelevant IS NULL OR a.saftRelevant = :saftRelevant) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("actionType") AuditLog.AuditActionType actionType,
                          @Param("entityType") String entityType,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate,
                          @Param("saftRelevant") Boolean saftRelevant,
                          Pageable pageable);

    @Query("SELECT a.actionType, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :startDate GROUP BY a.actionType")
    List<Object[]> countByActionTypeSince(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT DATE(a.timestamp), COUNT(*) FROM audit_log a WHERE a.timestamp BETWEEN :start AND :end GROUP BY DATE(a.timestamp)", nativeQuery = true)
    List<Object[]> countByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
