package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_device_key", columnNames = {"user_id", "device_key"})
}, indexes = {
        @Index(name = "idx_user_device_user", columnList = "user_id"),
        @Index(name = "idx_user_device_last_seen", columnList = "last_seen"),
        @Index(name = "idx_user_device_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_key", nullable = false, length = 128)
    private String deviceKey;

    @Column(name = "device_name", nullable = false, length = 120)
    private String deviceName;

    @Column(name = "device_type", length = 40)
    private String deviceType;

    @Column(length = 120)
    private String platform;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(nullable = false)
    private boolean trusted = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(length = 500)
    private String notes;

    @PrePersist
    void initDevice() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeen == null) firstSeen = now;
        if (lastSeen == null) lastSeen = now;
        if (deviceName == null || deviceName.isBlank()) deviceName = "Dispositivo";
    }
}
