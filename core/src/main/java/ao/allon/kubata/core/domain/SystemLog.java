package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para Logs do Sistema - registros técnicos de operação.
 */
@Entity
@Table(name = "system_log", indexes = {
    @Index(name = "idx_syslog_level", columnList = "log_level"),
    @Index(name = "idx_syslog_category", columnList = "category"),
    @Index(name = "idx_syslog_timestamp", columnList = "timestamp"),
    @Index(name = "idx_syslog_source", columnList = "source")
})
public class SystemLog extends BaseEntity {

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "log_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LogLevel logLevel;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "exception_class", length = 200)
    private String exceptionClass;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "thread_name", length = 100)
    private String threadName;

    @Column(name = "class_name", length = 200)
    private String className;

    @Column(name = "method_name", length = 100)
    private String methodName;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "memory_used_mb")
    private Long memoryUsedMb;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    public enum LogLevel {
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);

        private final int severity;

        LogLevel(int severity) {
            this.severity = severity;
        }

        public int getSeverity() {
            return severity;
        }

        public boolean isAtLeast(LogLevel level) {
            return this.severity >= level.severity;
        }
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
    }

    // Getters and Setters

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Long getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public void setMemoryUsedMb(Long memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
