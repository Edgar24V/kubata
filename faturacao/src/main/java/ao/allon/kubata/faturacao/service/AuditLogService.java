package ao.allon.kubata.faturacao.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

@Service
public class AuditLogService {
    private final SessionManager sessionManager;
    private final Path file;

    public AuditLogService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.file = Path.of(System.getProperty("user.home"), "kubata_audit.log");
        try {
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException ignored) {}
    }

    public void log(String action, String details) {
        String user = "desconhecido";
        try { user = sessionManager.getCurrentUser(); } catch (Exception ignored) {}
        String line = String.format("%s | user=%s | action=%s | %s%n", LocalDateTime.now(), user, action, details != null ? details : "");
        try {
            Files.writeString(file, line, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}
