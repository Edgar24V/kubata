package ao.allon.kubata.core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession extends BaseEntity {

    private String username;
    private String workstation;
    private String ipAddress;
    private String context;
    private LocalDateTime loginTime;
    private String memoryUsage;

    public UserSession() {}

    public UserSession(String username, String workstation, String ipAddress, String context) {
        this.username = username;
        this.workstation = workstation;
        this.ipAddress = ipAddress;
        this.context = context;
        this.loginTime = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkstation() {
        return workstation;
    }

    public void setWorkstation(String workstation) {
        this.workstation = workstation;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(String memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
}
