package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade de Auditoria para rastreamento de operações no sistema.
 * Compatível com requisitos de normas angolanas (SAFT-AO, AGT).
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_saft", columnList = "saft_relevant")
})
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "action_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditActionType actionType;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "entity_description", length = 500)
    private String entityDescription;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "saft_relevant")
    private Boolean saftRelevant = false;

    @Column(name = "agt_compliance_level", length = 20)
    @Enumerated(EnumType.STRING)
    private AGTComplianceLevel agtComplianceLevel = AGTComplianceLevel.NORMAL;

    @Column(name = "duracao_ms")
    private Long duracaoMs;

    @Column(name = "hash_integrity", length = 64)
    private String hashIntegrity;

    public Long getDuracaoMs() {
        return duracaoMs;
    }

    public void setDuracaoMs(Long duracaoMs) {
        this.duracaoMs = duracaoMs;
    }

    public String getHashIntegrity() {
        return hashIntegrity;
    }

    public void setHashIntegrity(String hashIntegrity) {
        this.hashIntegrity = hashIntegrity;
    }

    public enum AuditActionType {
        CREATE("Criação"),
        UPDATE("Atualização"),
        DELETE("Exclusão"),
        VIEW("Visualização"),
        EXPORT("Exportação"),
        IMPORT("Importação"),
        LOGIN("Login"),
        LOGOUT("Logout"),
        PRINT("Impressão"),
        APPROVE("Aprovação"),
        REJECT("Rejeição"),
        CANCEL("Cancelamento"),
        BACKUP("Backup"),
        RESTORE("Restauração"),
        CONFIG_CHANGE("Alteração de Configuração"),
        PERMISSION_CHANGE("Alteração de Permissão"),
        SAFT_EXPORT("Exportação SAFT"),
        AGT_COMMUNICATION("Comunicação AGT");

        private final String description;

        AuditActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AGTComplianceLevel {
        CRITICAL,
        HIGH,
        NORMAL,
        LOW
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
        if (saftRelevant == null) {
            saftRelevant = false;
        }
        if (agtComplianceLevel == null) {
            agtComplianceLevel = AGTComplianceLevel.NORMAL;
        }
    }

    // Getters and Setters

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public void setActionType(AuditActionType actionType) {
        this.actionType = actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(String entityDescription) {
        this.entityDescription = entityDescription;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Boolean getSaftRelevant() {
        return saftRelevant;
    }

    public void setSaftRelevant(Boolean saftRelevant) {
        this.saftRelevant = saftRelevant;
    }

    public AGTComplianceLevel getAgtComplianceLevel() {
        return agtComplianceLevel;
    }

    public void setAgtComplianceLevel(AGTComplianceLevel agtComplianceLevel) {
        this.agtComplianceLevel = agtComplianceLevel;
    }
}
