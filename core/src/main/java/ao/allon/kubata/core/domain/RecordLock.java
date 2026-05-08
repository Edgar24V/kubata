package ao.allon.kubata.core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "record_locks")
public class RecordLock extends BaseEntity {

    private String entityType;
    private String entityId;
    private String username;
    private LocalDateTime lockedSince;

    public RecordLock() {}

    public RecordLock(String entityType, String entityId, String username) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.username = username;
        this.lockedSince = LocalDateTime.now();
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getLockedSince() {
        return lockedSince;
    }

    public void setLockedSince(LocalDateTime lockedSince) {
        this.lockedSince = lockedSince;
    }
}
