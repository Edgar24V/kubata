package ao.allon.kubata.admin.service.job;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminJob {

    public enum Status {
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELED
    }

    private final String id = UUID.randomUUID().toString();

    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty statusText = new SimpleStringProperty();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.QUEUED);
    private final DoubleProperty progress = new SimpleDoubleProperty(-1.0);
    private final StringProperty message = new SimpleStringProperty();

    private final String actionType;
    private final String entityType;
    private final String description;

    private final LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private Throwable error;

    public AdminJob(String title, String actionType, String entityType, String description) {
        this.title.set(title);
        this.actionType = actionType;
        this.entityType = entityType;
        this.description = description;
        this.statusText.set("Em fila");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public Status getStatus() {
        return status.get();
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public String getStatusText() {
        return statusText.get();
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public Throwable getError() {
        return error;
    }

    public void markRunning() {
        this.startedAt = LocalDateTime.now();
        this.status.set(Status.RUNNING);
        this.statusText.set("Em execução");
        if (progress.get() < 0) {
            progress.set(-1.0);
        }
    }

    public void markSuccess() {
        this.finishedAt = LocalDateTime.now();
        this.status.set(Status.SUCCESS);
        this.statusText.set("Concluído");
        if (progress.get() < 0) {
            progress.set(1.0);
        }
    }

    public void markFailed(Throwable error) {
        this.finishedAt = LocalDateTime.now();
        this.status.set(Status.FAILED);
        this.statusText.set("Falhou");
        this.error = error;
        if (progress.get() < 0) {
            progress.set(1.0);
        }
    }

    public void markCanceled() {
        this.finishedAt = LocalDateTime.now();
        this.status.set(Status.CANCELED);
        this.statusText.set("Cancelado");
        if (progress.get() < 0) {
            progress.set(1.0);
        }
    }

    public void updateProgress(double value, String message) {
        if (value >= 0.0 && value <= 1.0) {
            this.progress.set(value);
        }
        if (message != null) {
            this.message.set(message);
        }
    }
}
