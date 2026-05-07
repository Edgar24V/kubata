package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidade para configurações de backup automático.
 */
@Entity
@Table(name = "backup_config")
public class BackupConfig extends BaseEntity {

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "frequency", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupFrequency frequency = BackupFrequency.DAILY;

    @Column(name = "schedule_time", nullable = false, length = 8)
    private String scheduleTime = "02:00:00"; // Formato HH:mm:ss

    @Column(name = "retention_days")
    private Integer retentionDays = 30;

    @Column(name = "backup_location", length = 500)
    private String backupLocation = "backups";

    @Column(name = "compress_backup")
    private Boolean compressBackup = true;

    @Column(name = "include_attachments")
    private Boolean includeAttachments = true;

    @Column(name = "notify_on_success")
    private Boolean notifyOnSuccess = false;

    @Column(name = "notify_on_failure")
    private Boolean notifyOnFailure = true;

    @Column(name = "email_notifications", length = 500)
    private String emailNotifications;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(name = "next_execution")
    private LocalDateTime nextExecution;

    @Column(name = "last_status", length = 20)
    @Enumerated(EnumType.STRING)
    private LastStatus lastStatus;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    public enum BackupFrequency {
        // Formato Spring: segundos minutos horas dia-mês mês dia-semana
        HOURLY("Horária", "0 * * * *"),           // Minuto 0, toda hora, todo dia
        DAILY("Diária", "* * *"),                  // Hora/minuto definidos, todo dia
        WEEKLY("Semanal", "* * 0"),                // Hora/minuto definidos, domingos
        BIWEEKLY("Quinzenal", "1,15 * *"),         // Hora/minuto definidos, dias 1 e 15
        MONTHLY("Mensal", "1 * *"),                // Hora/minuto definidos, dia 1
        QUARTERLY("Trimestral", "1 1,4,7,10 *"),   // Hora/minuto definidos, dia 1 dos meses 1,4,7,10
        YEARLY("Anual", "1 1 *");                  // Hora/minuto definidos, dia 1 de janeiro

        private final String description;
        private final String cronExpression; // Parte variável: dia-mês mês dia-semana

        BackupFrequency(String description, String cronExpression) {
            this.description = description;
            this.cronExpression = cronExpression;
        }

        public String getDescription() {
            return description;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public String getCronExpression(int hour, int minute) {
            // Formato Spring: segundos(0) minutos horas dia-mês mês dia-semana
            if (this == HOURLY) {
                // Horária: a cada hora no minuto definido (ignora hora configurada)
                return String.format("0 %d * * * *", minute);
            }
            // Outras: usa hora e minuto configurados
            return String.format("0 %d %d %s", minute, hour, cronExpression);
        }
    }

    public enum LastStatus {
        SUCCESS, FAILED, NEVER_RUN
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (enabled == null) enabled = true;
        if (frequency == null) frequency = BackupFrequency.DAILY;
        if (scheduleTime == null) scheduleTime = "02:00:00";
        if (retentionDays == null) retentionDays = 30;
        if (compressBackup == null) compressBackup = true;
        if (includeAttachments == null) includeAttachments = true;
        if (notifyOnSuccess == null) notifyOnSuccess = false;
        if (notifyOnFailure == null) notifyOnFailure = true;
    }

    // Getters e Setters

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BackupFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(BackupFrequency frequency) {
        this.frequency = frequency;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public void setScheduleTime(int hour, int minute) {
        this.scheduleTime = String.format("%02d:%02d:00", hour, minute);
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    public Boolean getCompressBackup() {
        return compressBackup;
    }

    public void setCompressBackup(Boolean compressBackup) {
        this.compressBackup = compressBackup;
    }

    public Boolean getIncludeAttachments() {
        return includeAttachments;
    }

    public void setIncludeAttachments(Boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }

    public Boolean getNotifyOnSuccess() {
        return notifyOnSuccess;
    }

    public void setNotifyOnSuccess(Boolean notifyOnSuccess) {
        this.notifyOnSuccess = notifyOnSuccess;
    }

    public Boolean getNotifyOnFailure() {
        return notifyOnFailure;
    }

    public void setNotifyOnFailure(Boolean notifyOnFailure) {
        this.notifyOnFailure = notifyOnFailure;
    }

    public String getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(String emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(LocalDateTime lastExecution) {
        this.lastExecution = lastExecution;
    }

    public LocalDateTime getNextExecution() {
        return nextExecution;
    }

    public void setNextExecution(LocalDateTime nextExecution) {
        this.nextExecution = nextExecution;
    }

    public LastStatus getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(LastStatus lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    // Métodos auxiliares

    public String getCronExpression() {
        if (frequency == null || scheduleTime == null) {
            return "0 0 2 * * *"; // Default: 02:00 diário
        }
        // Parse scheduleTime (format: HH:mm:ss)
        String[] parts = scheduleTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return frequency.getCronExpression(hour, minute);
    }

    public String getFormattedTime() {
        return scheduleTime != null ? scheduleTime.substring(0, 5) : "02:00";
    }
}
