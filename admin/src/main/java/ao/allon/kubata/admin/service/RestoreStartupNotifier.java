package ao.allon.kubata.admin.service;

import ao.allon.kubata.admin.ui.StageReadyEvent;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.service.job.JobManager;
import ao.allon.kubata.core.domain.SystemLog;
import ao.allon.kubata.core.repository.SystemLogRepository;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestoreStartupNotifier {

    private static final Logger logger = LoggerFactory.getLogger(RestoreStartupNotifier.class);

    private final SystemLogRepository systemLogRepository;
    private final NotificationService notificationService;
    private final ModalManager modalManager;
    private final JobManager jobManager;

    public RestoreStartupNotifier(SystemLogRepository systemLogRepository,
                                 NotificationService notificationService,
                                 ModalManager modalManager,
                                 JobManager jobManager) {
        this.systemLogRepository = systemLogRepository;
        this.notificationService = notificationService;
        this.modalManager = modalManager;
        this.jobManager = jobManager;
    }

    @EventListener
    public void onStageReadyEvent(StageReadyEvent event) {
        Platform.runLater(() -> {
            try {
                checkAndNotify();
            } catch (Exception ex) {
                logger.error("Erro ao verificar notificações de restauro", ex);
            }
        });
    }

    private void checkAndNotify() {
        String marker = "kubata.restore.pending.db";
        Path markerFile = Paths.get(marker).toAbsolutePath();
        if (Files.exists(markerFile)) {
            logger.warn("Ficheiro de restauro pendente ainda existe após arranque. O restauro pode não ter sido aplicado.");
            notificationService.showWarning("Restauro Não Aplicado", "Ficheiro pendente encontrado: " + markerFile);
            return;
        }

        List<Path> backups = findRecentBackups();
        if (backups.isEmpty()) {
            return;
        }

        for (Path backup : backups) {
            String name = backup.getFileName().toString();
            if (!name.startsWith("kubata.pre_restore.") || !name.endsWith(".db")) {
                continue;
            }

            String ts = name.replace("kubata.pre_restore.", "").replace(".db", "");
            LocalDateTime parsed;
            try {
                parsed = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            } catch (Exception ignored) {
                continue;
            }

            String msg = String.format("Restauro aplicado com sucesso no arranque.%nBackup anterior: %s%nData/Hora: %s",
                    backup.getFileName(),
                    parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            notificationService.showSuccess("Restauro Aplicado", msg);

            SystemLog log = new SystemLog();
            log.setLogLevel(SystemLog.LogLevel.INFO);
            log.setCategory("RESTORE");
            log.setSource("RestoreStartupNotifier");
            log.setMessage("Restauro aplicado no arranque. Backup anterior: " + backup.getFileName());
            log.setTimestamp(LocalDateTime.now());
            try {
                systemLogRepository.save(log);
            } catch (Exception ex) {
                logger.warn("Não foi possível gravar log de restauro", ex);
            }

            if (jobManager != null) {
                jobManager.submit("Notificação de Restauro", "RESTORE_NOTIFY", "SYSTEM", "Registrar log de restauro", job -> {
                }, null);
            }

            break;
        }
    }

    private List<Path> findRecentBackups() {
        try {
            Path cwd = Paths.get("").toAbsolutePath();
            return Files.list(cwd)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("kubata.pre_restore.") && n.endsWith(".db");
                    })
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .limit(1)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            logger.warn("Não foi possível procurar backups de pré-restauro", ex);
            return List.of();
        }
    }
}
