package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para registro de backups do sistema.
 */
@Entity
@Table(name = "backup_record", indexes = {
    @Index(name = "idx_backup_status", columnList = "status"),
    @Index(name = "idx_backup_time", columnList = "start_time")
})
public class BackupRecord extends BaseEntity {

    @Column(name = "filename", nullable = false, length = 200)
    private String filename;

    @Column(name = "type", length = 50)
    private String type; // AUTOMATIC, MANUAL, SCHEDULED

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "db_url", length = 500)
    private String dbUrl;

    @Column(name = "compressed")
    private Boolean compressed = true;

    @Column(name = "encrypted")
    private Boolean encrypted = false;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "restore_count")
    private Integer restoreCount = 0;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "restored_by", length = 100)
    private String restoredBy;

    @Column(name = "notes", length = 500)
    private String notes;

    public enum BackupStatus {
        IN_PROGRESS("Em Progresso"),
        COMPLETED("Concluído"),
        FAILED("Falhou"),
        VERIFIED("Verificado");

        private final String description;

        BackupStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    public void prePersist() {
        if (compressed == null) {
            compressed = true;
        }
        if (encrypted == null) {
            encrypted = false;
        }
    }

    // Getters and Setters

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public Boolean getCompressed() {
        return compressed;
    }

    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRestoreCount() {
        return restoreCount;
    }

    public void setRestoreCount(Integer restoreCount) {
        this.restoreCount = restoreCount;
    }

    public void incrementRestoreCount() {
        if (this.restoreCount == null) this.restoreCount = 0;
        this.restoreCount++;
    }

    public LocalDateTime getRestoredAt() {
        return restoredAt;
    }

    public void setRestoredAt(LocalDateTime restoredAt) {
        this.restoredAt = restoredAt;
    }

    public String getRestoredBy() {
        return restoredBy;
    }

    public void setRestoredBy(String restoredBy) {
        this.restoredBy = restoredBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper methods

    public long getDurationSeconds() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        return 0;
    }

    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}
