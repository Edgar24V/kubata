package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.BackupConfig;
import ao.allon.kubata.core.repository.BackupConfigRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço para gerenciar configurações de backup automático.
 */
@Service
public class BackupConfigService {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupRestoreService backupRestoreService;

    public BackupConfigService(BackupConfigRepository backupConfigRepository,
                                @Lazy BackupRestoreService backupRestoreService) {
        this.backupConfigRepository = backupConfigRepository;
        this.backupRestoreService = backupRestoreService;
    }

    @Transactional(readOnly = true)
    public BackupConfig getConfig() {
        return backupConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    // Criar configuração padrão se não existir
                    BackupConfig config = new BackupConfig();
                    config.setEnabled(true);
                    config.setFrequency(BackupConfig.BackupFrequency.DAILY);
                    config.setRetentionDays(30);
                    return backupConfigRepository.save(config);
                });
    }

    @Transactional
    public BackupConfig saveConfig(BackupConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        BackupConfig saved = backupConfigRepository.save(config);
        // Reagendar backup após salvar nova configuração
        backupRestoreService.scheduleBackup();
        return saved;
    }

    @Transactional
    public void updateLastExecution(BackupConfig.LastStatus status, String errorMessage) {
        BackupConfig config = getConfig();
        config.setLastExecution(LocalDateTime.now());
        config.setLastStatus(status);
        if (errorMessage != null) {
            config.setLastErrorMessage(errorMessage);
        }
        backupConfigRepository.save(config);
    }

    public boolean isBackupEnabled() {
        return getConfig().getEnabled();
    }

    public String getCronExpression() {
        return getConfig().getCronExpression();
    }
}
