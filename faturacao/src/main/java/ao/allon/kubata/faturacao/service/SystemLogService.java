package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.SystemLog;
import ao.allon.kubata.core.repository.SystemLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço de Logs do Sistema - registro técnico de operações.
 */
@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;

    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Transactional
    public SystemLog log(SystemLog.LogLevel level, String category, String source,
                         String message, Throwable exception, String correlationId) {
        
        SystemLog log = new SystemLog();
        log.setTimestamp(LocalDateTime.now());
        log.setLogLevel(level);
        log.setCategory(category);
        log.setSource(source);
        log.setMessage(message);
        log.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString().substring(0, 8));
        
        // Capturar informações de memória e CPU
        try {
            log.setMemoryUsedMb(memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024));
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                log.setCpuUsagePercent(((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad() * 100);
            }
        } catch (Exception ignored) {
        }
        
        // Capturar stack trace se houver exceção
        if (exception != null) {
            log.setExceptionClass(exception.getClass().getName());
            log.setStackTrace(getStackTraceAsString(exception));
            
            // Extrair informações do stack trace
            StackTraceElement[] stack = exception.getStackTrace();
            if (stack.length > 0) {
                log.setClassName(stack[0].getClassName());
                log.setMethodName(stack[0].getMethodName());
                log.setLineNumber(stack[0].getLineNumber());
            }
        }
        
        return systemLogRepository.save(log);
    }

    // Métodos convenientes

    @Transactional
    public void debug(String category, String source, String message) {
        log(SystemLog.LogLevel.DEBUG, category, source, message, null, null);
    }

    @Transactional
    public void info(String category, String source, String message) {
        log(SystemLog.LogLevel.INFO, category, source, message, null, null);
    }

    @Transactional
    public void warn(String category, String source, String message, Throwable exception) {
        log(SystemLog.LogLevel.WARN, category, source, message, exception, null);
    }

    @Transactional
    public void error(String category, String source, String message, Throwable exception) {
        log(SystemLog.LogLevel.ERROR, category, source, message, exception, null);
    }

    @Transactional
    public void fatal(String category, String source, String message, Throwable exception) {
        log(SystemLog.LogLevel.FATAL, category, source, message, exception, null);
    }

    // Consultas

    public Page<SystemLog> findAll(Pageable pageable) {
        return systemLogRepository.findByOrderByTimestampDesc(pageable);
    }

    public Page<SystemLog> search(SystemLog.LogLevel level, String category, String source,
                                   String message, LocalDateTime startDate, LocalDateTime endDate,
                                   Pageable pageable) {
        return systemLogRepository.search(level, category, source, message, startDate, endDate, pageable);
    }

    public List<SystemLog> findByLevel(SystemLog.LogLevel level) {
        return systemLogRepository.findByLogLevelOrderByTimestampDesc(level);
    }

    public Map<String, Long> getStatisticsLast24Hours() {
        LocalDateTime start = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        List<Object[]> counts = systemLogRepository.countByLogLevelSince(start);
        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : counts) {
            stats.put(((SystemLog.LogLevel) row[0]).name(), (Long) row[1]);
        }
        return stats;
    }

    public Map<String, Long> getCategoryStatistics() {
        LocalDateTime start = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<Object[]> counts = systemLogRepository.countByCategorySince(start);
        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : counts) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    @Transactional
    public void purgeOldLogs(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minus(daysToKeep, ChronoUnit.DAYS);
        // Implementação de purga seria feita com query de delete
    }

    private String getStackTraceAsString(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
