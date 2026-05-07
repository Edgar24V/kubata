package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.domain.AuditLog;

/**
 * DTO para relatório de auditoria.
 * Converte o enum AuditActionType para String para compatibilidade com JasperReports.
 */
public class AuditLogReportDTO {
    private final java.time.LocalDateTime timestamp;
    private final String username;
    private final String actionType;
    private final String entityType;
    private final String newValues;
    private final Long duracaoMs;

    public AuditLogReportDTO(AuditLog auditLog) {
        this.timestamp = auditLog.getTimestamp();
        this.username = auditLog.getUsername();
        this.actionType = auditLog.getActionType() != null ? auditLog.getActionType().getDescription() : "";
        this.entityType = auditLog.getEntityType();
        this.newValues = auditLog.getNewValues();
        this.duracaoMs = auditLog.getDuracaoMs();
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getNewValues() {
        return newValues;
    }

    public Long getDuracaoMs() {
        return duracaoMs;
    }
}
