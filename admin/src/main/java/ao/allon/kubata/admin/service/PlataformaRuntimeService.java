package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.module.event.ModuleEvent;
import ao.allon.kubata.core.module.event.ModuleEventListener;
import ao.allon.kubata.core.module.event.ModuleEventPublisher;
import ao.allon.kubata.core.domain.Alerta;
import ao.allon.kubata.core.repository.AdmPlataformaItemRepository;
import ao.allon.kubata.core.repository.AlertaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motores runtime da plataforma administrativa.
 *
 * Não executa código vindo da configuração. As definições administrativas são
 * metadata e apenas operações SELECT/arquivos explicitamente suportados são
 * executadas.
 */
@Service
public class PlataformaRuntimeService implements ModuleEventListener {
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AdmPlataformaItemRepository repository;
    private final AlertaRepository alertaRepository;
    private final PlataformaAutomationService automation;
    private final PlataformaDocumentService documents;
    private final BackupService backupService;
    private final NotificationService notifications;
    private final ModuleEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlataformaRuntimeService(
            AdmPlataformaItemRepository repository,
            AlertaRepository alertaRepository,
            PlataformaAutomationService automation,
            PlataformaDocumentService documents,
            BackupService backupService,
            NotificationService notifications,
            ModuleEventPublisher eventPublisher,
            JdbcTemplate jdbcTemplate,
            Environment environment) {
        this.repository = repository;
        this.alertaRepository = alertaRepository;
        this.automation = automation;
        this.documents = documents;
        this.backupService = backupService;
        this.notifications = notifications;
        this.eventPublisher = eventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @PostConstruct
    public void registerEventListener() {
        eventPublisher.addListener(this);
    }

    @PreDestroy
    public void unregisterEventListener() {
        eventPublisher.removeListener(this);
    }

    // ---------------------------------------------------------------------
    // CDU / XDU / PDU / RDU / FDU / SDU / MDU
    // ---------------------------------------------------------------------

    public List<AdmPlataformaItem> extensibility(String type) {
        return automation.list("PERSONALIZACAO").stream()
                .filter(i -> type == null || type.equalsIgnoreCase(extensionType(i)))
                .toList();
    }

    public Optional<AdmPlataformaItem> activeExtension(String type, String code) {
        String prefix = type.toUpperCase(Locale.ROOT) + "_";
        return automation.list("PERSONALIZACAO").stream()
                .filter(i -> "ACTIVO".equalsIgnoreCase(i.getEstado()))
                .filter(i -> (prefix + code).equalsIgnoreCase(i.getCodigo()))
                .findFirst();
    }

    public String extensionType(AdmPlataformaItem item) {
        String code = item == null ? "" : Objects.toString(item.getCodigo(), "");
        int p = code.indexOf('_');
        return p > 0 ? code.substring(0, p).toUpperCase(Locale.ROOT) : "PERSONALIZACAO";
    }

    public Map<String, Object> extensionRuntime(String type, String code) {
        AdmPlataformaItem item = activeExtension(type, code).orElse(null);
        if (item == null) return Map.of("enabled", false, "type", type, "code", code);
        return Map.of(
                "enabled", true,
                "type", type.toUpperCase(Locale.ROOT),
                "code", code,
                "name", Objects.toString(item.getNome(), ""),
                "metadata", parse(item.getConfigJson())
        );
    }

    // ---------------------------------------------------------------------
    // Motor de LISTAGENS
    // ---------------------------------------------------------------------

    public record QueryResult(List<String> columns, List<List<String>> rows, int rowCount) {}

    public QueryResult executeListagem(AdmPlataformaItem definition, int maxRows) {
        Objects.requireNonNull(definition, "definition");
        if (!"LISTAGEM".equalsIgnoreCase(definition.getTipo())) {
            throw new IllegalArgumentException("A definição não é uma LISTAGEM.");
        }

        JsonNode cfg = parse(definition.getConfigJson());
        String sql = text(cfg, "query", "");
        if (sql.isBlank()) {
            throw new IllegalArgumentException("A listagem precisa de uma consulta SELECT.");
        }

        String normalized = sql.trim().replaceAll("\\s+", " ");
        if (!isReadOnlySql(normalized)) {
            throw new IllegalArgumentException("A listagem aceita apenas consultas SELECT/WITH sem múltiplos comandos.");
        }

        int limit = Math.max(1, Math.min(maxRows <= 0 ? 500 : maxRows, 1000));
        String executable = appendLimit(normalized, limit);
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(executable);

        if (raw.isEmpty()) return new QueryResult(List.of(), List.of(), 0);
        List<String> cols = new ArrayList<>(raw.get(0).keySet());
        List<List<String>> rows = raw.stream()
                .map(row -> cols.stream().map(k -> formatValue(row.get(k))).toList())
                .toList();
        return new QueryResult(cols, rows, rows.size());
    }

    public boolean isReadOnlyQuery(String sql) {
        if (sql == null) return false;
        return isReadOnlySql(sql.trim().replaceAll("\\s+", " "));
    }

    public String executeWidget(AdmPlataformaItem widget) {
        if (widget == null || !"DASHBOARD".equalsIgnoreCase(widget.getTipo())) {
            throw new IllegalArgumentException("O item não é um widget de dashboard.");
        }
        JsonNode cfg = parse(widget.getConfigJson());
        String query = text(cfg, "query", "");
        if (query.isBlank()) {
            return text(cfg, "metric", "Sem métrica");
        }
        if (!isReadOnlyQuery(query)) {
            throw new IllegalArgumentException("O widget contém uma consulta que não é somente leitura.");
        }
        String sql = appendLimit(query.trim().replaceAll("\\s+", " "), 1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) return "Sem dados.";
        return rows.get(0).entrySet().stream()
                .map(e -> e.getKey() + "=" + formatValue(e.getValue()))
                .collect(Collectors.joining(" | "));
    }

    public String validateDefinition(String type, String json) {
        try {
            JsonNode node = parse(json);
            if ("LISTAGEM".equalsIgnoreCase(type)) {
                String q = text(node, "query", "");
                if (q.isBlank()) return "A listagem requer o campo query.";
                if (!isReadOnlySql(q)) return "A consulta deve ser somente SELECT/WITH.";
            } else if ("MAPA".equalsIgnoreCase(type)) {
                if (!node.isObject()) return "O mapa deve ser um objecto JSON.";
                if (!node.has("nodes") || !node.has("edges")) return "O mapa deve possuir nodes e edges.";
            } else if ("PERSONALIZACAO".equalsIgnoreCase(type)) {
                if (!node.isObject()) return "A metadata deve ser um objecto JSON.";
            }
            return "Definição válida.";
        } catch (Exception e) {
            return "JSON inválido: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------------
    // Motor de MAPAS
    // ---------------------------------------------------------------------

    public record MapRuntime(int nodes, int edges, List<String> nodeLabels, List<String> edgeLabels) {}

    public MapRuntime renderMap(AdmPlataformaItem definition) {
        if (definition == null || !"MAPA".equalsIgnoreCase(definition.getTipo())) {
            throw new IllegalArgumentException("A definição não é um MAPA.");
        }
        JsonNode cfg = parse(definition.getConfigJson());
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();

        if (cfg.path("nodes").isArray()) {
            for (JsonNode n : cfg.path("nodes")) {
                nodes.add(n.isTextual() ? n.asText() : text(n, "label", text(n, "id", "Nó")));
            }
        }
        if (cfg.path("edges").isArray()) {
            for (JsonNode e : cfg.path("edges")) {
                String from = text(e, "from", "?");
                String to = text(e, "to", "?");
                edges.add(from + " → " + to);
            }
        }
        return new MapRuntime(nodes.size(), edges.size(), nodes, edges);
    }

    // ---------------------------------------------------------------------
    // Pesquisa global / indexação leve
    // ---------------------------------------------------------------------

    public record SearchHit(String table, String primaryKey, String summary) {}

    public List<SearchHit> globalSearch(String term, int maxHits) {
        String q = term == null ? "" : term.trim();
        if (q.length() < 2) return List.of();

        int max = Math.max(1, Math.min(maxHits <= 0 ? 50 : maxHits, 200));
        return jdbcTemplate.execute(
                (org.springframework.jdbc.core.ConnectionCallback<List<SearchHit>>)
                        connection -> searchConnection(connection, q, max)
        );
    }

    private List<SearchHit> searchConnection(Connection connection, String term, int maxHits) throws SQLException {
        DatabaseMetaData md = connection.getMetaData();
        List<SearchHit> hits = new ArrayList<>();
        String pattern = "%" + term + "%";

        try (ResultSet tables = md.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next() && hits.size() < maxHits) {
                String table = tables.getString("TABLE_NAME");
                if (table == null || isInternalTable(table)) continue;

                List<String> columns = new ArrayList<>();
                try (ResultSet rs = md.getColumns(connection.getCatalog(), null, table, "%")) {
                    while (rs.next() && columns.size() < 20) {
                        String name = rs.getString("COLUMN_NAME");
                        String type = rs.getString("TYPE_NAME");
                        if (name != null && searchableType(type)) columns.add(name);
                    }
                }
                if (columns.isEmpty()) continue;

                String qualified = quoteIdentifier(table, connection);
                String where = columns.stream()
                        .map(c -> "CAST(" + safeQuoteIdentifier(c, connection) + " AS VARCHAR(1000)) LIKE ?")
                        .collect(Collectors.joining(" OR "));
                String sql = "SELECT * FROM " + qualified + " WHERE " + where + limitClause(connection, Math.min(8, maxHits - hits.size()));

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    for (int i = 1; i <= columns.size(); i++) ps.setString(i, pattern);
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData rsm = rs.getMetaData();
                        while (rs.next() && hits.size() < maxHits) {
                            String pk = findPrimaryKey(connection, table, rs, rsm);
                            List<String> summary = new ArrayList<>();
                            for (int c = 1; c <= Math.min(rsm.getColumnCount(), 6); c++) {
                                Object value = rs.getObject(c);
                                if (value != null) summary.add(rsm.getColumnLabel(c) + "=" + formatValue(value));
                            }
                            hits.add(new SearchHit(table, pk, String.join(" | ", summary)));
                        }
                    } catch (SQLException ignored) {
                        // Algumas tabelas especiais não aceitam CAST/LIMIT dessa forma; são ignoradas.
                    }
                }
            }
        }
        return hits;
    }

    // ---------------------------------------------------------------------
    // Eventos + notificações
    // ---------------------------------------------------------------------

    @Override
    public boolean accepts(String eventType) {
        return true;
    }

    @Override
    @Transactional
    public void onModuleEvent(ModuleEvent event) {
        String code = "MOD_" + Objects.toString(event.eventType(), "EVENT")
                .replaceAll("[^A-Za-z0-9_]+", "_");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("eventId", event.eventId());
        payload.put("eventType", event.eventType());
        payload.put("sourceModule", event.sourceModuleId());
        payload.put("targetModule", event.targetModuleId());
        payload.put("timestamp", Objects.toString(event.timestamp(), ""));
        if (event.payload() != null) payload.set("payload", mapper.valueToTree(event.payload()));

        automation.save(
                "EVENTO",
                code + "_" + System.nanoTime(),
                "Evento " + event.eventType(),
                "PROCESSADO",
                "Evento recebido do módulo " + Objects.toString(event.sourceModuleId(), "desconhecido"),
                payload.toString(),
                null,
                "Sistema",
                null
        );
        processEventRules(event.eventType(), payload);
    }

    @Transactional
    public void emitEvent(String eventType, String sourceModule, Map<String, Object> payload, String actor) {
        eventPublisher.publishEvent(
                Objects.requireNonNull(eventType).trim().toUpperCase(Locale.ROOT),
                sourceModule,
                null,
                payload == null ? Map.of() : payload
        );
        automation.save(
                "EVENTO",
                "MANUAL_" + UUID.randomUUID(),
                "Evento " + eventType,
                "PROCESSADO",
                "Evento emitido manualmente por " + Objects.toString(actor, "Sistema"),
                mapper.valueToTree(payload == null ? Map.of() : payload).toString(),
                null,
                actor,
                null
        );
    }

    public List<AdmPlataformaItem> notificationsList() {
        return automation.list("NOTIFICACAO");
    }

    private void processEventRules(String eventType, ObjectNode payload) {
        automation.list("EVENTO_REGRA").stream()
                .filter(i -> "ACTIVO".equalsIgnoreCase(i.getEstado()))
                .forEach(rule -> {
                    JsonNode cfg = parse(rule.getConfigJson());
                    String expected = text(cfg, "event", "");
                    if (expected.isBlank() || !expected.equalsIgnoreCase(eventType)) return;
                    String message = text(cfg, "message", "Evento recebido: " + eventType);
                    String code = "NOTIF_" + UUID.randomUUID();
                    automation.save(
                            "NOTIFICACAO",
                            code,
                            rule.getNome(),
                            "NOVA",
                            message,
                            payload.toString(),
                            null,
                            rule.getOwnerUsername(),
                            null
                    );
                    try {
                        Platform.runLater(() -> notifications.showInfo(rule.getNome(), message));
                    } catch (IllegalStateException ignored) {
                        // Processo pode estar em modo headless/teste.
                    }
                });
    }

    // ---------------------------------------------------------------------
    // Calendário administrativo
    // ---------------------------------------------------------------------

    public List<AdmPlataformaItem> calendarEvents() {
        return automation.list("CALENDARIO");
    }

    @Transactional
    public AdmPlataformaItem saveCalendarEvent(String code, String name, LocalDate date, LocalTime time,
                                               String durationMinutes, String recurrence, String description, String owner) {
        if (date == null) throw new IllegalArgumentException("A data do evento é obrigatória.");
        int duration;
        try {
            duration = Math.max(0, Integer.parseInt(Objects.toString(durationMinutes, "0")));
        } catch (NumberFormatException e) {
            duration = 0;
        }
        ObjectNode cfg = mapper.createObjectNode();
        cfg.put("date", date.toString());
        cfg.put("time", (time == null ? LocalTime.MIDNIGHT : time).toString());
        cfg.put("durationMinutes", duration);
        cfg.put("recurrence", Objects.toString(recurrence, ""));
        cfg.put("description", Objects.toString(description, ""));
        return automation.save(
                "CALENDARIO",
                code == null || code.isBlank() ? "CAL_" + UUID.randomUUID() : code.trim().toUpperCase(Locale.ROOT),
                name == null || name.isBlank() ? "Evento" : name.trim(),
                "ACTIVO",
                description,
                cfg.toString(),
                null,
                owner,
                null
        );
    }

    // ---------------------------------------------------------------------
    // Dashboard configurável
    // ---------------------------------------------------------------------

    public List<AdmPlataformaItem> dashboardWidgets() {
        return automation.list("DASHBOARD");
    }

    @Transactional
    public AdmPlataformaItem saveDashboardWidget(String code, String title, String metric, String query, String owner) {
        ObjectNode cfg = mapper.createObjectNode();
        cfg.put("metric", Objects.toString(metric, ""));
        if (query != null && !query.isBlank()) cfg.put("query", query.trim());
        cfg.put("visible", true);
        return automation.save(
                "DASHBOARD",
                code == null || code.isBlank() ? "W_" + UUID.randomUUID() : code.trim().toUpperCase(Locale.ROOT),
                title == null || title.isBlank() ? "Widget" : title.trim(),
                "ACTIVO",
                "Widget configurável",
                cfg.toString(),
                null,
                owner,
                null
        );
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacoes", repository.countByTipo("OPERACAO"));
        m.put("alertasAbertos", alertaRepository.countByEstado(Alerta.Estado.OPEN)
                + alertaRepository.countByEstado(Alerta.Estado.ACKNOWLEDGED));
        m.put("documentos", repository.countByTipo("DOCUMENTO"));
        m.put("comunicacoes", repository.countByTipo("COMUNICACAO"));
        m.put("notificacoes", repository.countByTipo("NOTIFICACAO"));
        m.put("eventos", repository.countByTipo("EVENTO"));
        m.put("personalizacoes", repository.countByTipo("PERSONALIZACAO"));
        m.put("listagens", repository.countByTipo("LISTAGEM"));
        m.put("mapas", repository.countByTipo("MAPA"));
        m.put("anexos", repository.countByTipo("ANEXO"));
        m.put("tabelas", automation.databaseTables().size());
        Runtime runtime = Runtime.getRuntime();
        m.put("heapUsadaMB", (runtime.totalMemory() - runtime.freeMemory()) / 1048576L);
        m.put("memoriaLivreMB", runtime.freeMemory() / 1048576L);
        m.put("java", System.getProperty("java.version"));
        return m;
    }

    // ---------------------------------------------------------------------
    // Anexos
    // ---------------------------------------------------------------------

    @Transactional
    public AdmPlataformaItem attach(Path source, Path repositoryDir, String entityType, String entityId, String owner) throws IOException {
        AdmPlataformaItem document = documents.store(source, repositoryDir, owner);
        ObjectNode cfg = mapper.createObjectNode();
        cfg.put("entityType", Objects.toString(entityType, ""));
        cfg.put("entityId", Objects.toString(entityId, ""));
        cfg.put("fileName", document.getNome());
        cfg.put("documentCode", document.getCodigo());
        cfg.put("resourcePath", Objects.toString(document.getResourcePath(), ""));
        return automation.save(
                "ANEXO",
                "ANX_" + UUID.randomUUID(),
                document.getNome(),
                "ACTIVO",
                "Anexo " + entityType + "/" + entityId,
                cfg.toString(),
                null,
                owner,
                document.getResourcePath()
        );
    }

    public List<AdmPlataformaItem> attachments(String entityType, String entityId) {
        return automation.list("ANEXO").stream()
                .filter(i -> {
                    JsonNode cfg = parse(i.getConfigJson());
                    return Objects.equals(entityType, text(cfg, "entityType", ""))
                            && Objects.equals(entityId, text(cfg, "entityId", ""));
                })
                .toList();
    }

    @Transactional
    public void deleteAttachment(AdmPlataformaItem attachment) throws IOException {
        if (attachment == null) return;
        String path = attachment.getResourcePath();
        if (path != null && !path.isBlank()) Files.deleteIfExists(Paths.get(path));
        repository.delete(attachment);
    }

    // ---------------------------------------------------------------------
    // Licenciamento
    // ---------------------------------------------------------------------

    @Transactional
    public AdmPlataformaItem saveLicense(String code, String holder, LocalDate expiry, String modules, String owner) {
        ObjectNode cfg = mapper.createObjectNode();
        cfg.put("holder", Objects.toString(holder, ""));
        cfg.put("expiry", expiry == null ? "" : expiry.toString());
        ArrayNode arr = cfg.putArray("modules");
        for (String module : Objects.toString(modules, "").split(",")) {
            if (!module.isBlank()) arr.add(module.trim());
        }
        return automation.save(
                "LICENCA",
                code == null || code.isBlank() ? "LIC-" + UUID.randomUUID() : code.trim(),
                "Licença " + Objects.toString(holder, "Kubata"),
                expiry != null && expiry.isBefore(LocalDate.now()) ? "EXPIRADA" : "ACTIVO",
                "Licenciamento da plataforma",
                cfg.toString(),
                null,
                owner,
                null
        );
    }

    public String licenseStatus() {
        Optional<AdmPlataformaItem> current = automation.list("LICENCA").stream().findFirst();
        if (current.isEmpty()) return "Nenhuma licença administrativa configurada.";
        AdmPlataformaItem item = current.get();
        LocalDate expiry = parseDate(text(parse(item.getConfigJson()), "expiry", ""));
        if (expiry == null) return "Licença configurada sem data de expiração.";
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        if (expiry.isBefore(LocalDate.now())) return "Licença expirada em " + expiry + ".";
        return "Licença válida até " + expiry + " (" + days + " dia(s)).";
    }

    // ---------------------------------------------------------------------
    // Registry / docking / instalação
    // ---------------------------------------------------------------------

    public String readPreference(String key, String fallback) {
        return environment.getProperty(key, fallback);
    }

    @Transactional
    public AdmPlataformaItem saveLayout(String user, String json) {
        return automation.save(
                "LAYOUT",
                "DOCKING_" + sanitize(user),
                "Layout " + Objects.toString(user, "Sistema"),
                "ACTIVO",
                "Layout de janelas e painéis do utilizador",
                json == null || json.isBlank() ? "{}" : json,
                null,
                user,
                null
        );
    }

    public String loadLayout(String user) {
        return automation.list("LAYOUT").stream()
                .filter(i -> ("DOCKING_" + sanitize(user)).equalsIgnoreCase(i.getCodigo()))
                .findFirst()
                .map(i -> Objects.toString(i.getConfigJson(), "{}"))
                .orElse("{}");
    }

    // ---------------------------------------------------------------------
    // Gestão avançada de SQLite / schema
    // ---------------------------------------------------------------------

    public String schemaFingerprint() {
        String schema = automation.exportSchema();
        if (schema.startsWith("Falha")) return schema;
        return sha256(schema);
    }

    public String cloneCurrentSqlite(Path target) throws Exception {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.startsWith("jdbc:sqlite:")) {
            throw new IllegalStateException("A clonagem integrada exige um datasource SQLite.");
        }
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) throw new IOException("Destino sem pasta pai.");
        Files.createDirectories(parent);
        if (Files.exists(absolute)) Files.delete(absolute);

        String escaped = absolute.toString().replace("'", "''");
        jdbcTemplate.execute("VACUUM INTO '" + escaped + "'");
        return absolute.toString();
    }

    public String compareSqlite(Path left, Path right) throws Exception {
        Objects.requireNonNull(left, "Base esquerda");
        Objects.requireNonNull(right, "Base direita");
        if (!Files.isRegularFile(left) || !Files.isRegularFile(right)) {
            throw new IOException("As duas bases SQLite devem existir.");
        }

        Map<String, String> a = sqliteSchema(left);
        Map<String, String> b = sqliteSchema(right);
        Set<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        all.addAll(a.keySet());
        all.addAll(b.keySet());

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (String key : all) {
            if (!a.containsKey(key)) added.add(key);
            else if (!b.containsKey(key)) removed.add(key);
            else if (!Objects.equals(a.get(key), b.get(key))) changed.add(key);
        }

        StringBuilder out = new StringBuilder();
        out.append("Comparação de schema SQLite").append(System.lineSeparator())
                .append("Esquerda: ").append(left.toAbsolutePath()).append(System.lineSeparator())
                .append("Direita: ").append(right.toAbsolutePath()).append(System.lineSeparator())
                .append("Tabelas esquerda: ").append(a.size()).append(System.lineSeparator())
                .append("Tabelas direita: ").append(b.size()).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("ADICIONADAS: ").append(added.size()).append(System.lineSeparator());
        added.forEach(x -> out.append(" + ").append(x).append(System.lineSeparator()));
        out.append(System.lineSeparator()).append("REMOVIDAS: ").append(removed.size()).append(System.lineSeparator());
        removed.forEach(x -> out.append(" - ").append(x).append(System.lineSeparator()));
        out.append(System.lineSeparator()).append("ALTERADAS: ").append(changed.size()).append(System.lineSeparator());
        changed.forEach(x -> out.append(" * ").append(x).append(System.lineSeparator()));
        return out.toString();
    }

    private Map<String, String> sqliteSchema(Path file) throws Exception {
        Map<String, String> schema = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT name, sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (rs.next()) schema.put(rs.getString(1), Objects.toString(rs.getString(2), ""));
        }
        return schema;
    }

    public String createEmptySqlite(Path target) throws Exception {
        Objects.requireNonNull(target, "Destino");
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) throw new IOException("Destino sem pasta pai.");
        Files.createDirectories(parent);
        if (Files.exists(absolute)) throw new IOException("O ficheiro já existe: " + absolute);
        String url = "jdbc:sqlite:" + absolute;
        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
        }
        return absolute.toString();
    }

    public String prepareRestore(Path backupFile) throws IOException {
        Path target = Paths.get("kubata.restore.pending.db").toAbsolutePath();
        // O fluxo seguro de restore existente prepara o ficheiro para aplicação
        // no arranque, evitando substituir uma BD que está em uso.
        Path prepared = backupService.prepareRestoreToPending(backupFile);
        return prepared.toString();
    }

    // ---------------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------------

    private boolean isReadOnlySql(String sql) {
        String q = sql.trim().toLowerCase(Locale.ROOT);
        if (!(q.startsWith("select ") || q.startsWith("select\\n") || q.startsWith("with "))) return false;
        return !q.contains(";") && !q.matches("(?s).*\\b(insert|update|delete|drop|alter|truncate|attach|detach|pragma|vacuum|create|replace|merge|grant|revoke)\\b.*");
    }

    private String appendLimit(String sql, int limit) {
        if (sql.matches("(?is).*\\blimit\\s+\\d+\\s*$")) return sql;
        if (sql.matches("(?is).*\\bfetch\\s+first\\s+\\d+\\s+rows\\s+only\\s*$")) return sql;
        return sql + " LIMIT " + limit;
    }

    private String limitClause(Connection c, int limit) throws SQLException {
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        return product.contains("sql server") ? " OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY" : " LIMIT " + limit;
    }

    private String quoteIdentifier(String identifier, Connection connection) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString();
        if (quote == null || quote.isBlank()) quote = "\"";
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private String safeQuoteIdentifier(String identifier, Connection connection) {
        try {
            return quoteIdentifier(identifier, connection);
        } catch (SQLException ex) {
            // O identificador vem da metadata da BD; em caso de falha, usar aspas SQL padrão.
            String quote = "\"";
            return quote + identifier.replace(quote, quote + quote) + quote;
        }
    }


    private boolean searchableType(String type) {
        if (type == null) return true;
        String t = type.toUpperCase(Locale.ROOT);
        return t.contains("CHAR") || t.contains("TEXT") || t.contains("CLOB") || t.contains("UUID")
                || t.contains("DATE") || t.contains("TIME") || t.contains("JSON") || t.contains("NUMBER")
                || t.contains("DECIMAL") || t.contains("INT");
    }

    private boolean isInternalTable(String table) {
        String t = table.toUpperCase(Locale.ROOT);
        return t.startsWith("SQLITE_") || t.startsWith("PG_") || t.startsWith("FLYWAY_")
                || t.startsWith("INFORMATION_SCHEMA") || t.equals("DUAL");
    }

    private String findPrimaryKey(Connection c, String table, ResultSet rs, ResultSetMetaData md) {
        try (ResultSet pks = c.getMetaData().getPrimaryKeys(c.getCatalog(), null, table)) {
            if (pks.next()) {
                String pk = pks.getString("COLUMN_NAME");
                int index = findColumn(md, pk);
                return index > 0 ? formatValue(rs.getObject(index)) : pk;
            }
        } catch (Exception ignored) {}
        return "—";
    }

    private int findColumn(ResultSetMetaData md, String name) throws SQLException {
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(md.getColumnLabel(i)) || name.equalsIgnoreCase(md.getColumnName(i))) return i;
        }
        return -1;
    }

    private String formatValue(Object value) {
        if (value == null) return "—";
        if (value instanceof Timestamp t) return t.toLocalDateTime().format(EVENT_TIME);
        if (value instanceof java.sql.Date d) return d.toLocalDate().toString();
        return String.valueOf(value);
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String name, String fallback) {
        return node != null && node.has(name) && !node.get(name).isNull() ? node.get(name).asText(fallback) : fallback;
    }

    private LocalDate parseDate(String value) {
        try { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
        catch (Exception ignored) { return null; }
    }

    private String sanitize(String text) {
        return Objects.toString(text, "SISTEMA").replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
