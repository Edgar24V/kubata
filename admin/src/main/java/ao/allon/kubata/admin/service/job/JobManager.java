package ao.allon.kubata.admin.service.job;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.service.AcessoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class JobManager {

    private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

    public interface JobWork {
        void run(AdminJob job) throws Exception;
    }

    private final ObservableList<AdminJob> jobs = FXCollections.observableArrayList();

    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final NotificationService notificationService;
    private final ModalManager modalManager;

    public JobManager(AcessoService acessoService,
                      SessionManager sessionManager,
                      NotificationService notificationService,
                      ModalManager modalManager) {
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.notificationService = notificationService;
        this.modalManager = modalManager;
    }

    public ObservableList<AdminJob> getJobs() {
        return jobs;
    }

    public AdminJob submit(String title, String actionType, String entityType, String description, JobWork work, Runnable onSuccess) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(work, "work");

        AdminJob job = new AdminJob(title, actionType, entityType, description);
        Platform.runLater(() -> jobs.add(0, job));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                job.markRunning();
                work.run(job);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            job.markSuccess();

            String userName = sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema";
            acessoService.registrarAuditoria(sessionManager.getUser(), actionType, entityType, "127.0.0.1", description, true);

            notificationService.showSuccess(
                    "Operação Concluída",
                    String.format("%s executado com sucesso.\nResponsável: %s\nDetalhes: %s", title, userName, description)
            );

            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            job.markFailed(ex);

            logger.error("Job falhou {}: {}", actionType, ex.getMessage(), ex);
            modalManager.alert("Falha na Operação",
                    "Não foi possível concluir a operação: " + title + "\nErro: " + ex.getMessage(),
                    "error", ex);

            acessoService.registrarAuditoria(sessionManager.getUser(), actionType + "_FAILED", entityType, "127.0.0.1",
                    "Falha em " + description + ". Erro: " + ex.getMessage(), false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        return job;
    }

    public void cancel(AdminJob job) {
        if (job == null) {
            return;
        }
        job.markCanceled();
    }
}
