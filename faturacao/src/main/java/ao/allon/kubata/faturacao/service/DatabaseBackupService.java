package ao.allon.kubata.faturacao.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DatabaseBackupService {

    @Scheduled(cron = "0 0 2 * * *")
    public void automaticBackup() {
        try {
            performBackup();
        } catch (Exception ignored) {
        }
    }

    public void performBackup() throws IOException, InterruptedException {
        Path dir = Path.of("backups");
        Files.createDirectories(dir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File out = dir.resolve("backup_" + timestamp + ".sql").toFile();

        String pgDump = System.getenv().getOrDefault("PG_DUMP", "pg_dump");
        String url = System.getenv().getOrDefault("DB_URL", "");
        String user = System.getenv().getOrDefault("DB_USER", "");
        String pass = System.getenv().getOrDefault("DB_PASS", "");

        if (!url.isBlank() && url.contains("postgresql")) {
            ProcessBuilder pb = new ProcessBuilder(pgDump, url, "-U", user, "-w", "-f", out.getAbsolutePath());
            pb.environment().put("PGPASSWORD", pass);
            Process p = pb.start();
            p.waitFor();
        } else {
            Files.writeString(out.toPath(), "-- Backup não configurado. Configure variáveis DB_URL/DB_USER/DB_PASS.\n");
        }
    }
}
