package ao.allon.kubata.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public final class PendingRestoreApplier {

    private static final Logger logger = LoggerFactory.getLogger(PendingRestoreApplier.class);

    private static final String PENDING_FILENAME = "kubata.restore.pending.db";

    private PendingRestoreApplier() {
    }

    public static void applyIfPresent() {
        try {
            Path pending = Paths.get(PENDING_FILENAME).toAbsolutePath();
            if (!Files.exists(pending)) {
                return;
            }

            String jdbcUrl = resolveJdbcUrl();
            Path dbFile = resolveSqliteDbFile(jdbcUrl);

            if (dbFile == null) {
                logger.error("Restauro pendente encontrado mas não foi possível resolver o ficheiro da BD. JDBC URL: {}", jdbcUrl);
                return;
            }

            Path dbAbs = dbFile.toAbsolutePath();
            logger.info("A aplicar restauro pendente. Pendente: {} -> BD: {}", pending, dbAbs);

            cleanupWalShm(dbAbs);

            if (Files.exists(dbAbs)) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path backup = dbAbs.getParent() != null
                        ? dbAbs.getParent().resolve("kubata.pre_restore." + ts + ".db")
                        : Paths.get("kubata.pre_restore." + ts + ".db");

                Files.move(dbAbs, backup, StandardCopyOption.REPLACE_EXISTING);
                cleanupWalShm(backup);
            }

            Files.move(pending, dbAbs, StandardCopyOption.REPLACE_EXISTING);
            cleanupWalShm(dbAbs);

            logger.info("Restauro aplicado com sucesso: {}", dbAbs);
        } catch (Exception ex) {
            logger.error("Falha ao aplicar restauro pendente", ex);
        }
    }

    private static void cleanupWalShm(Path dbFile) {
        try {
            Files.deleteIfExists(Paths.get(dbFile.toString() + "-wal"));
        } catch (Exception ignored) {
        }
        try {
            Files.deleteIfExists(Paths.get(dbFile.toString() + "-shm"));
        } catch (Exception ignored) {
        }
    }

    private static String resolveJdbcUrl() {
        String sysProp = System.getProperty("spring.datasource.url");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }

        String envProp = System.getenv("SPRING_DATASOURCE_URL");
        if (envProp != null && !envProp.isBlank()) {
            return envProp;
        }

        Properties p = new Properties();
        try (InputStream in = PendingRestoreApplier.class.getClassLoader().getResourceAsStream("application-sqlite.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                String url = p.getProperty("spring.datasource.url");
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        } catch (Exception ignored) {
        }

        return "jdbc:sqlite:kubata.db";
    }

    private static Path resolveSqliteDbFile(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            return null;
        }

        String raw = jdbcUrl.substring("jdbc:sqlite:".length());
        int q = raw.indexOf('?');
        if (q >= 0) {
            raw = raw.substring(0, q);
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return null;
        }

        return Paths.get(raw);
    }
}
