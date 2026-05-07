package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.service.AuditService;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Serviço avançado de Backup e Restauração.
 * Suporta PostgreSQL com compactação e criptografia básica.
 */
@Service
public class BackupRestoreService {

    private final BackupRecordRepository backupRecordRepository;
    private final BackupConfigService backupConfigService;
    private final SystemLogService logService;
    private final AuditService auditService;
    private final TaskScheduler taskScheduler;
    private final Environment env;
    private final DataSource dataSource;

    private ScheduledFuture<?> scheduledBackup;

    private static final String BACKUP_DIR = "backups";
    private static final String PG_DUMP = System.getenv().getOrDefault("PG_DUMP", "pg_dump");
    private static final String PG_RESTORE = System.getenv().getOrDefault("PG_RESTORE", "pg_restore");
    private static final String SQLITE3 = System.getenv().getOrDefault("SQLITE3", "sqlite3");
    
    private String dbUrl;
    private String dbUser;
    private String dbPass;
    
    private static final String APP_VERSION = "2.1.0";
    private static final String BACKUP_VERSION = "1.0";

    public BackupRestoreService(BackupRecordRepository backupRecordRepository,
                                 BackupConfigService backupConfigService,
                                 SystemLogService logService,
                                 AuditService auditService,
                                 TaskScheduler taskScheduler,
                                 Environment env,
                                 DataSource dataSource) {
        this.backupRecordRepository = backupRecordRepository;
        this.backupConfigService = backupConfigService;
        this.logService = logService;
        this.auditService = auditService;
        this.taskScheduler = taskScheduler;
        this.env = env;
        this.dataSource = dataSource;
        
        // Inicializar configurações do banco de dados a partir do ambiente Spring
        this.dbUrl = env.getProperty("spring.datasource.url", "");
        this.dbUser = env.getProperty("spring.datasource.username", "");
        this.dbPass = env.getProperty("spring.datasource.password", "");
        
        // Fallback para variáveis de ambiente se as propriedades do Spring estiverem vazias
        if (this.dbUrl.isEmpty()) this.dbUrl = System.getenv().getOrDefault("DB_URL", "");
        if (this.dbUser.isEmpty()) this.dbUser = System.getenv().getOrDefault("DB_USER", "");
        if (this.dbPass.isEmpty()) this.dbPass = System.getenv().getOrDefault("DB_PASS", "");
    }

    @PostConstruct
    public void init() {
        scheduleBackup();
    }

    public void scheduleBackup() {
        // Cancelar agendamento anterior se existir
        if (scheduledBackup != null && !scheduledBackup.isDone()) {
            scheduledBackup.cancel(false);
        }

        // Agendar novo backup baseado na configuração atual
        String cronExpression = backupConfigService.getCronExpression();
        logService.info("BACKUP", "BackupRestoreService", "Agendando backup automático com cron: " + cronExpression);
        
        scheduledBackup = taskScheduler.schedule(this::automaticBackupTask, new CronTrigger(cronExpression));
    }

    private void automaticBackupTask() {
        try {
            if (!backupConfigService.isBackupEnabled()) {
                logService.info("BACKUP", "BackupRestoreService", "Backup automático desabilitado");
                return;
            }
            logService.info("BACKUP", "BackupRestoreService", "Iniciando backup automático");
            performBackup("AUTOMATIC", "Sistema");
        } catch (Exception e) {
            logService.error("BACKUP", "BackupRestoreService", "Erro no backup automático", e);
        }
    }

    @Transactional
    public void automaticBackup() {
        automaticBackupTask();
    }

    @Transactional
    public BackupRecord performBackup(String type, String triggeredBy) throws Exception {
        Path backupDir = Paths.get(BACKUP_DIR);
        Files.createDirectories(backupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("kubata_backup_%s_%s", type.toLowerCase(), timestamp);
        Path sqlFile = backupDir.resolve(filename + ".sql");
        Path zipFile = backupDir.resolve(filename + ".zip");

        BackupRecord record = new BackupRecord();
        record.setFilename(filename + ".zip");
        record.setType(type);
        record.setStatus(BackupRecord.BackupStatus.IN_PROGRESS);
        record.setTriggeredBy(triggeredBy);
        record.setStartTime(LocalDateTime.now());
        record.setDbUrl(dbUrl);
        record.setCompressed(true);
        record = backupRecordRepository.save(record);

        try {
            DatabaseType dbType = detectDatabaseType();
            
            // Executar backup baseado no tipo de banco
            switch (dbType) {
                case POSTGRESQL -> performPostgreSQLBackup(sqlFile);
                case SQLITE -> performSQLiteBackup(sqlFile);
                case H2 -> performH2Backup(sqlFile);
                default -> throw new UnsupportedOperationException("Tipo de banco de dados não suportado: " + dbType);
            }

            // Adicionar metadados ao cabeçalho do arquivo
            prependBackupMetadata(sqlFile, dbType, record);

            // Compactar o arquivo SQL
            compressFile(sqlFile, zipFile);
            
            // Calcular checksum
            String checksum = calculateChecksum(zipFile);
            
            // Remover arquivo SQL original
            Files.deleteIfExists(sqlFile);
            
            // Atualizar registro
            record.setStatus(BackupRecord.BackupStatus.COMPLETED);
            record.setEndTime(LocalDateTime.now());
            record.setFileSize(Files.size(zipFile));
            record.setChecksum(checksum);
            record = backupRecordRepository.save(record);
            
            logService.info("BACKUP", "BackupRestoreService", 
                String.format("Backup concluído: %s (%d bytes)", filename, record.getFileSize()));
            
            auditService.logAction(null, triggeredBy, AuditLog.AuditActionType.BACKUP,
                "DATABASE", record.getId().toString(), filename, null, null,
                "BACKUP", null, null, null, false, AuditLog.AGTComplianceLevel.NORMAL);
            
            // Limpar backups antigos (manter últimos 30)
            cleanupOldBackups(30);
            
            return record;
            
        } catch (Exception e) {
            record.setStatus(BackupRecord.BackupStatus.FAILED);
            record.setEndTime(LocalDateTime.now());
            record.setErrorMessage(e.getMessage());
            backupRecordRepository.save(record);
            
            logService.error("BACKUP", "BackupRestoreService", "Falha no backup", e);
            throw e;
        }
    }

    public void restoreBackup(Long backupId, String restoreBy) throws Exception {
        // Obter o registro sem abrir transação longa
        BackupRecord record = backupRecordRepository.findById(backupId)
            .orElseThrow(() -> new IllegalArgumentException("Backup não encontrado"));
        
        Path zipFile = Paths.get(BACKUP_DIR, record.getFilename());
        
        if (!Files.exists(zipFile)) {
            throw new FileNotFoundException("Arquivo de backup não encontrado: " + record.getFilename());
        }
        
        // Usar logs que não dependem de banco de dados para evitar deadlock no pool de 1 conexão
        System.out.println("[RESTORE] Iniciando restauração do backup: " + record.getFilename());
        
        // Validação do checksum
        String currentChecksum = calculateChecksum(zipFile);
        if (!currentChecksum.equals(record.getChecksum())) {
            throw new SecurityException("Checksum inválido! O arquivo pode ter sido corrompido.");
        }
        
        // Descompactar
        Path restoredFile = unzipFile(zipFile);
        
        try {
            DatabaseType dbType = detectDatabaseType();
            
            switch (dbType) {
                case POSTGRESQL -> performPostgreSQLRestore(restoredFile);
                case SQLITE -> {
                    // Para SQLite, o restauro JDBC não pode rodar dentro de uma transação do Spring
                    // pois causaria deadlock no Hikari (que só tem 1 conexão no pool para SQLite).
                    performSQLiteRestore(restoredFile);
                }
                default -> throw new UnsupportedOperationException("Restauro não suportado para o tipo: " + dbType);
            }
            
            // Atualizar o registro do backup em uma transação curta e isolada
            updateBackupRecordAfterRestore(backupId, restoreBy);
            
            System.out.println("[RESTORE] Restauração concluída com sucesso.");
            
        } finally {
            Files.deleteIfExists(restoredFile);
        }
    }

    @Transactional
    public void updateBackupRecordAfterRestore(Long backupId, String restoreBy) {
        backupRecordRepository.findById(backupId).ifPresent(record -> {
            record.setRestoredAt(LocalDateTime.now());
            record.setRestoredBy(restoreBy);
            record.incrementRestoreCount(); // Requisito AGT: Registar número de reposições
            backupRecordRepository.save(record);
            
            auditService.logAction(null, restoreBy, AuditLog.AuditActionType.RESTORE,
                "DATABASE", record.getId().toString(), record.getFilename(), null, null,
                "RESTORE", null, null, null, false, AuditLog.AGTComplianceLevel.HIGH);
        });
    }

    private void performPostgreSQLRestore(Path sqlFile) throws Exception {
        logService.info("RESTORE", "BackupRestoreService", "Iniciando restauro PostgreSQL...");
        ProcessBuilder pb = new ProcessBuilder(
            PG_RESTORE, dbUrl, "-U", dbUser, "-w", "--clean", "--if-exists", "-d", dbUrl, sqlFile.toString()
        );
        pb.environment().put("PGPASSWORD", dbPass);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0 && exitCode != 1) { 
            throw new RuntimeException("pg_restore falhou com código: " + exitCode);
        }
    }

    private void performSQLiteRestore(Path restoredFile) throws Exception {
        System.out.println("[RESTORE] Iniciando restauro SQLite...");
        String dbPath = extractSQLitePath();
        Path targetDb = Paths.get(dbPath);
        
        if (restoredFile.getFileName().toString().endsWith(".sql")) {
            System.out.println("[RESTORE] Restaurando via dump SQL JDBC...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Desabilitar chaves estrangeiras temporariamente
                stmt.execute("PRAGMA foreign_keys = OFF;");
                
                // Ler o arquivo SQL linha por linha para evitar MalformedInputException com arquivos grandes ou codificação mista
                try (BufferedReader reader = Files.newBufferedReader(restoredFile, java.nio.charset.StandardCharsets.UTF_8)) {
                    StringBuilder command = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmedLine = line.trim();
                        if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) continue;
                        
                        command.append(line);
                        if (trimmedLine.endsWith(";")) {
                            try {
                                stmt.execute(command.toString());
                            } catch (Exception ex) {
                                System.err.println("[RESTORE] Erro em comando SQL: " + command.toString().substring(0, Math.min(50, command.length())));
                            }
                            command.setLength(0);
                        }
                    }
                } catch (MalformedInputException e) {
                    // Fallback para ISO-8859-1 se UTF-8 falhar
                    System.out.println("[RESTORE] Falha ao ler em UTF-8, tentando ISO-8859-1");
                    try (BufferedReader reader = Files.newBufferedReader(restoredFile, java.nio.charset.StandardCharsets.ISO_8859_1)) {
                        StringBuilder command = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmedLine = line.trim();
                            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) continue;
                            
                            command.append(line);
                            if (trimmedLine.endsWith(";")) {
                                try {
                                    stmt.execute(command.toString());
                                } catch (Exception ex) {
                                    System.err.println("[RESTORE] Erro em comando SQL (ISO): " + command.toString().substring(0, Math.min(50, command.length())));
                                }
                                command.setLength(0);
                            }
                        }
                    }
                }
                
                stmt.execute("PRAGMA foreign_keys = ON;");
                
            } catch (Exception e) {
                System.err.println("[RESTORE] Falha crítica ao restaurar dump SQL via JDBC: " + e.getMessage());
                throw e;
            }
        } else {
            // Se for arquivo .db (binário)
            System.out.println("[RESTORE] Tentando substituição binária");
            try {
                Files.copy(restoredFile, targetDb, StandardCopyOption.REPLACE_EXISTING);
            } catch (FileSystemException e) {
                System.err.println("[RESTORE] ERRO CRÍTICO: Não foi possível substituir o arquivo .db (travado pelo Windows).");
                throw new IOException("O arquivo de banco de dados está sendo usado pelo sistema e não pode ser substituído diretamente.", e);
            }
        }
        
        System.out.println("[RESTORE] Processo de restauro SQLite finalizado.");
    }

    public List<BackupRecord> listBackups() {
        return backupRecordRepository.findByOrderByStartTimeDesc();
    }

    public Optional<BackupRecord> getBackup(Long id) {
        return backupRecordRepository.findById(id);
    }

    @Transactional
    public void deleteBackup(Long id) throws IOException {
        Optional<BackupRecord> optRecord = backupRecordRepository.findById(id);
        if (optRecord.isPresent()) {
            BackupRecord record = optRecord.get();
            Path file = Paths.get(BACKUP_DIR, record.getFilename());
            Files.deleteIfExists(file);
            backupRecordRepository.delete(record);
            
            logService.info("BACKUP", "BackupRestoreService", "Backup excluído: " + record.getFilename());
        }
    }

    public Map<String, Object> getBackupStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = backupRecordRepository.count();
        long completed = backupRecordRepository.countByStatus(BackupRecord.BackupStatus.COMPLETED);
        long failed = backupRecordRepository.countByStatus(BackupRecord.BackupStatus.FAILED);
        
        Long totalSize = backupRecordRepository.sumFileSize();
        
        stats.put("totalBackups", total);
        stats.put("completed", completed);
        stats.put("failed", failed);
        stats.put("totalSizeBytes", totalSize != null ? totalSize : 0);
        stats.put("lastBackup", backupRecordRepository.findTopByStatusOrderByStartTimeDesc(BackupRecord.BackupStatus.COMPLETED));
        
        return stats;
    }

    private enum DatabaseType {
        POSTGRESQL, SQLITE, H2, UNKNOWN
    }

    private DatabaseType detectDatabaseType() {
        if (dbUrl == null || dbUrl.isBlank()) {
            // Tentar detectar pelo arquivo de configuração ou variáveis
            String dbFile = System.getenv("DB_FILE");
            if (dbFile != null && !dbFile.isBlank()) {
                return DatabaseType.SQLITE;
            }
        }
        
        String url = (dbUrl != null ? dbUrl : "").toLowerCase();
        if (url.contains("postgresql") || url.contains("postgres")) {
            return DatabaseType.POSTGRESQL;
        } else if (url.contains("sqlite")) {
            return DatabaseType.SQLITE;
        } else if (url.contains("h2")) {
            return DatabaseType.H2;
        }
        
        // Verificar arquivo SQLite no diretório
        Path sqliteDb = Paths.get("kubata.db");
        if (Files.exists(sqliteDb)) {
            return DatabaseType.SQLITE;
        }
        
        return DatabaseType.UNKNOWN;
    }

    private void performPostgreSQLBackup(Path sqlFile) throws Exception {
        logService.info("BACKUP", "BackupRestoreService", "Iniciando backup PostgreSQL...");
        
        ProcessBuilder pb = new ProcessBuilder(
            PG_DUMP, "-v", "--inserts", "--column-inserts", "--no-owner", "--no-acl",
            "-f", sqlFile.toString(),
            dbUrl
        );
        
        if (dbUser != null && !dbUser.isBlank()) {
            pb.environment().put("PGUSER", dbUser);
        }
        if (dbPass != null && !dbPass.isBlank()) {
            pb.environment().put("PGPASSWORD", dbPass);
        }
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logService.debug("BACKUP", "pg_dump", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump falhou com código " + exitCode + ": " + output.toString());
        }
        
        logService.info("BACKUP", "BackupRestoreService", "Backup PostgreSQL concluído com sucesso");
    }

    private void performSQLiteBackup(Path sqlFile) throws Exception {
        logService.info("BACKUP", "BackupRestoreService", "Iniciando backup SQLite...");
        
        // Extrair caminho do banco da URL ou usar padrão
        String dbPath = extractSQLitePath();
        
        Path sourceDb = Paths.get(dbPath);
        if (!Files.exists(sourceDb)) {
            throw new FileNotFoundException("Arquivo do banco SQLite não encontrado: " + dbPath);
        }
        
        try {
            // Método 1: Usar comando .dump do sqlite3 (preferido - cria SQL completo)
            ProcessBuilder pb = new ProcessBuilder(
                SQLITE3, dbPath, ".dump"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Capturar saída SQL
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = Files.newBufferedWriter(sqlFile)) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("sqlite3 .dump retornou código de erro: " + exitCode);
            }
            
            logService.info("BACKUP", "BackupRestoreService", 
                "Backup SQLite SQL concluído: " + Files.size(sqlFile) + " bytes");
                
        } catch (IOException | InterruptedException e) {
            // Fallback: copiar arquivo binário diretamente se sqlite3 não estiver instalado ou falhar
            logService.warn("BACKUP", "BackupRestoreService", 
                "Comando sqlite3 não disponível ou falhou, usando cópia binária como fallback: " + e.getMessage(), null);
            
            performSQLiteBinaryBackup(sourceDb, sqlFile);
        }
    }

    private void performSQLiteBinaryBackup(Path sourceDb, Path backupFile) throws IOException {
        // Criar backup binário do arquivo .db
        Path binaryBackup = Paths.get(backupFile.toString().replace(".sql", ".db"));
        Files.copy(sourceDb, binaryBackup, StandardCopyOption.REPLACE_EXISTING);
        
        // Renomear para manter consistência com o sistema
        Files.move(binaryBackup, backupFile, StandardCopyOption.REPLACE_EXISTING);
        
        logService.info("BACKUP", "BackupRestoreService", 
            "Backup SQLite binário concluído: " + Files.size(backupFile) + " bytes");
    }

    private void performH2Backup(Path sqlFile) throws Exception {
        logService.info("BACKUP", "BackupRestoreService", "Iniciando backup H2...");
        throw new UnsupportedOperationException("Backup H2 requer implementação JDBC específica");
    }

    private String extractSQLitePath() {
        if (dbUrl != null && dbUrl.contains(":sqlite:")) {
            int idx = dbUrl.indexOf(":sqlite:");
            String path = dbUrl.substring(idx + 8);
            
            // Remover parâmetros de consulta JDBC (ex: ?journal_mode=WAL...)
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }
            return path;
        }
        
        // Procurar arquivos .db comuns
        String[] candidates = {"kubata.db", "data/kubata.db", "database/kubata.db"};
        for (String candidate : candidates) {
            if (Files.exists(Paths.get(candidate))) {
                return candidate;
            }
        }
        
        return "kubata.db";
    }

    private void prependBackupMetadata(Path sqlFile, DatabaseType dbType, BackupRecord record) throws IOException {
        String metadata = String.format(
            "-- ============================================================================\n" +
            "-- Kubata Enterprise Database Backup\n" +
            "-- ============================================================================\n" +
            "-- Backup Version: %s\n" +
            "-- Application Version: %s\n" +
            "-- Database Type: %s\n" +
            "-- Backup ID: %s\n" +
            "-- Created At: %s\n" +
            "-- Created By: %s\n" +
            "-- Backup Type: %s\n" +
            "-- Database URL: %s\n" +
            "-- Checksum (SHA-256): [será calculado após compressão]\n" +
            "-- ============================================================================\n" +
            "-- WARNING: This is an automated backup file. Manual modification may corrupt\n" +
            "-- the backup and prevent successful restoration.\n" +
            "-- ============================================================================\n" +
            "\n",
            BACKUP_VERSION,
            APP_VERSION,
            dbType,
            record.getId(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            record.getTriggeredBy(),
            record.getType(),
            maskSensitiveInfo(dbUrl)
        );
        
        // Se for binário (SQLite fallback), não prependemos metadata como texto
        // pois corrompe o arquivo binário. O metadata será salvo em um arquivo separado no ZIP.
        if (sqlFile.getFileName().toString().endsWith(".db")) {
            Path metadataFile = sqlFile.resolveSibling(sqlFile.getFileName().toString() + ".metadata");
            Files.writeString(metadataFile, metadata);
            return;
        }

        // Para arquivos SQL (Texto), prependemos normalmente
        try {
            // Ler conteúdo existente
            String existingContent = Files.readString(sqlFile);
            
            // Escrever metadata + conteúdo existente
            Files.writeString(sqlFile, metadata + existingContent);
        } catch (MalformedInputException e) {
            // Se falhar ao ler como String, tratar como binário
            logService.warn("BACKUP", "BackupRestoreService", "Arquivo não é UTF-8 válido, salvando metadata separadamente", null);
            Path metadataFile = sqlFile.resolveSibling(sqlFile.getFileName().toString() + ".metadata");
            Files.writeString(metadataFile, metadata);
        }
    }

    private void compressFile(Path source, Path target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target.toFile()))) {
            // Adicionar arquivo principal
            ZipEntry entry = new ZipEntry(source.getFileName().toString());
            zos.putNextEntry(entry);
            Files.copy(source, zos);
            zos.closeEntry();

            // Adicionar metadata se existir
            Path metadataFile = source.resolveSibling(source.getFileName().toString() + ".metadata");
            if (Files.exists(metadataFile)) {
                ZipEntry metaEntry = new ZipEntry("backup_metadata.txt");
                zos.putNextEntry(metaEntry);
                Files.copy(metadataFile, zos);
                zos.closeEntry();
                Files.deleteIfExists(metadataFile);
            }
        }
    }

    private Path unzipFile(Path zipFile) throws IOException {
        Path target = null;
        
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".sql")) {
                    target = Paths.get(BACKUP_DIR, "restore_temp.sql");
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    break;
                } else if (entry.getName().endsWith(".db")) {
                    target = Paths.get(BACKUP_DIR, "restore_temp.db");
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
            }
        }
        
        if (target == null) {
            throw new FileNotFoundException("Nenhum arquivo de banco de dados (.sql ou .db) encontrado no backup.");
        }
        
        return target;
    }

    private String calculateChecksum(Path file) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private String maskSensitiveInfo(String url) {
        if (url == null || url.isBlank()) return "N/A";
        // Remover senha da URL se presente
        return url.replaceAll("(://[^:]+:)[^@]+(@)", "$1****$2");
    }

    private void cleanupOldBackups(int keepCount) {
        List<BackupRecord> backups = backupRecordRepository.findByOrderByStartTimeDesc();
        if (backups.size() > keepCount) {
            for (int i = keepCount; i < backups.size(); i++) {
                BackupRecord old = backups.get(i);
                try {
                    deleteBackup(old.getId());
                } catch (Exception e) {
                    logService.warn("BACKUP", "BackupRestoreService", 
                        "Erro ao limpar backup antigo: " + old.getFilename(), e);
                }
            }
        }
    }

    public InputStream downloadBackup(Long id) throws IOException {
        Optional<BackupRecord> optRecord = backupRecordRepository.findById(id);
        if (optRecord.isEmpty()) {
            throw new FileNotFoundException("Backup não encontrado");
        }
        
        Path file = Paths.get(BACKUP_DIR, optRecord.get().getFilename());
        return Files.newInputStream(file);
    }
}
