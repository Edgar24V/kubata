package ao.allon.kubata.admin.service;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    public record BackupArtifact(Path file, long sizeBytes, String sha256, String dbUrl, boolean compressed) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public BackupService(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    public BackupArtifact createDatabaseBackup(Path backupDirectory, boolean compress) throws IOException {
        Objects.requireNonNull(backupDirectory, "backupDirectory");

        Files.createDirectories(backupDirectory);

        String dbUrl = environment.getProperty("spring.datasource.url", "");
        if (!dbUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalStateException("Backup integrado disponível apenas para SQLite. Datasource actual: " + dbUrl);
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path sqliteSnapshot = backupDirectory.resolve("kubata_snapshot_" + ts + ".db");

        String sql = "VACUUM INTO '" + sqliteSnapshot.toAbsolutePath().toString().replace("'", "''") + "'";
        jdbcTemplate.execute(sql);

        Path finalFile;
        if (compress) {
            finalFile = backupDirectory.resolve("bkp_" + ts + ".zip");
            zipSingleFile(sqliteSnapshot, finalFile, "kubata.db");
            Files.deleteIfExists(sqliteSnapshot);
        } else {
            finalFile = backupDirectory.resolve("bkp_" + ts + ".db");
            Files.move(sqliteSnapshot, finalFile);
        }

        long size = Files.size(finalFile);
        String sha256 = sha256(finalFile);

        return new BackupArtifact(finalFile, size, sha256, dbUrl, compress);
    }

    public Path prepareRestoreToPending(Path backupFile) throws IOException {
        Objects.requireNonNull(backupFile, "backupFile");

        if (!Files.exists(backupFile)) {
            throw new IOException("Ficheiro não encontrado: " + backupFile);
        }

        Path target = Paths.get("kubata.restore.pending.db").toAbsolutePath();

        if (backupFile.getFileName().toString().toLowerCase().endsWith(".zip")) {
            unzipFirstDbLike(backupFile, target);
        } else {
            Files.copy(backupFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    public String sha256Of(Path file) throws IOException {
        return sha256(file);
    }


    public record IntegrityCheck(
            boolean checksumMatches,
            boolean archiveReadable,
            boolean databaseHealthy,
            String expectedSha256,
            String calculatedSha256,
            String message
    ) {}

    /**
     * Valida um backup sem alterar o ficheiro nem o checksum guardado.
     * Para ZIPs, lê todos os entries (incluindo CRC) e, quando encontra uma
     * base SQLite, executa PRAGMA integrity_check sobre uma cópia temporária.
     */
    public IntegrityCheck verifyBackup(Path file, String expectedSha256) throws IOException {
        if (file == null || !Files.exists(file)) {
            return new IntegrityCheck(false, false, false, expectedSha256, "",
                    "Ficheiro de backup não encontrado.");
        }

        String calculated = sha256(file);
        boolean checksumMatches = expectedSha256 != null
                && !expectedSha256.isBlank()
                && expectedSha256.equalsIgnoreCase(calculated);

        boolean archiveReadable = true;
        boolean databaseHealthy = true;
        String message = checksumMatches
                ? "Checksum SHA-256 confirmado."
                : "O checksum actual não coincide com o checksum registado.";

        try {
            String lower = file.getFileName().toString().toLowerCase();
            if (lower.endsWith(".zip")) {
                Path extracted = Files.createTempFile("kubata-verify-", ".db");
                boolean foundDb = false;
                try (InputStream fis = Files.newInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis);
                     ZipInputStream zis = new ZipInputStream(bis)) {

                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        String name = entry.getName().toLowerCase();
                        if (!foundDb
                                && (name.endsWith(".db") || name.endsWith(".sqlite") || name.contains("kubata"))) {
                            Files.copy(zis, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            foundDb = true;
                        } else {
                            while (zis.read() != -1) {
                                // Consome a entrada para validar o CRC de toda a ZIP.
                            }
                        }
                    }
                } catch (Exception ex) {
                    archiveReadable = false;
                    message = "Arquivo ZIP inválido ou corrompido: " + ex.getMessage();
                }

                if (foundDb && archiveReadable) {
                    try {
                        databaseHealthy = sqliteIntegrityCheck(extracted);
                        if (!databaseHealthy) {
                            message = "A verificação SQLite PRAGMA integrity_check falhou.";
                        }
                    } finally {
                        Files.deleteIfExists(extracted);
                    }
                } else if (archiveReadable) {
                    archiveReadable = false;
                    databaseHealthy = false;
                    message = "O arquivo ZIP não contém uma base de dados SQLite reconhecível.";
                }
            } else if (lower.endsWith(".db") || lower.endsWith(".sqlite")) {
                databaseHealthy = sqliteIntegrityCheck(file);
                if (!databaseHealthy) {
                    message = "A verificação SQLite PRAGMA integrity_check falhou.";
                }
            }
        } catch (Exception ex) {
            archiveReadable = false;
            databaseHealthy = false;
            message = ex.getMessage() == null ? "Falha na validação do backup." : ex.getMessage();
        }

        return new IntegrityCheck(
                checksumMatches,
                archiveReadable,
                databaseHealthy,
                expectedSha256,
                calculated,
                message
        );
    }

    /**
     * Testa se o destino é gravável sem deixar ficheiros de teste.
     */
    public boolean testDestination(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Files.createDirectories(directory);
        Path probe = Files.createTempFile(directory, ".kubata-backup-test-", ".tmp");
        try {
            Files.writeString(probe, "Kubata backup destination test");
            return Files.size(probe) > 0;
        } finally {
            Files.deleteIfExists(probe);
        }
    }

    private boolean sqliteIntegrityCheck(Path dbFile) throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        try (java.sql.Connection connection = DriverManager.getConnection(jdbcUrl);
             java.sql.Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA integrity_check")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        }
    }


    private static void zipSingleFile(Path inputFile, Path zipFile, String entryName) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos);
             InputStream is = Files.newInputStream(inputFile);
             BufferedInputStream bis = new BufferedInputStream(is)) {

            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            bis.transferTo(zos);
            zos.closeEntry();
        }
    }

    private static void unzipFirstDbLike(Path zipFile, Path targetDbFile) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && (entry.getName().toLowerCase().endsWith(".db") || entry.getName().toLowerCase().endsWith(".sqlite") || entry.getName().toLowerCase().contains("kubata"))) {
                    Files.copy(zis, targetDbFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("ZIP não contém base de dados (.db/.sqlite).");
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }

        try (InputStream is = Files.newInputStream(file);
             DigestInputStream dis = new DigestInputStream(is, digest)) {
            dis.transferTo(OutputStream.nullOutputStream());
        }

        return HexFormat.of().formatHex(digest.digest()).toUpperCase();
    }
}
