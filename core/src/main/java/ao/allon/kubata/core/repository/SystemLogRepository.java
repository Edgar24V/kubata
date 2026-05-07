package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    Page<SystemLog> findByOrderByTimestampDesc(Pageable pageable);

    List<SystemLog> findByLogLevelOrderByTimestampDesc(SystemLog.LogLevel logLevel);

    List<SystemLog> findByCategoryOrderByTimestampDesc(String category);

    List<SystemLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM SystemLog s WHERE " +
           "(:logLevel IS NULL OR s.logLevel = :logLevel) AND " +
           "(:category IS NULL OR s.category = :category) AND " +
           "(:source IS NULL OR s.source LIKE %:source%) AND " +
           "(:message IS NULL OR s.message LIKE %:message%) AND " +
           "(:startDate IS NULL OR s.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR s.timestamp <= :endDate) " +
           "ORDER BY s.timestamp DESC")
    Page<SystemLog> search(@Param("logLevel") SystemLog.LogLevel logLevel,
                           @Param("category") String category,
                           @Param("source") String source,
                           @Param("message") String message,
                           @Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate,
                           Pageable pageable);

    @Query("SELECT s.logLevel, COUNT(s) FROM SystemLog s WHERE s.timestamp >= :startDate GROUP BY s.logLevel")
    List<Object[]> countByLogLevelSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT s.category, COUNT(s) FROM SystemLog s WHERE s.timestamp >= :startDate GROUP BY s.category")
    List<Object[]> countByCategorySince(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT DATE(s.timestamp), COUNT(*) FROM system_logs s WHERE s.timestamp BETWEEN :start AND :end GROUP BY DATE(s.timestamp)", nativeQuery = true)
    List<Object[]> countByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
