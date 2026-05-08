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
