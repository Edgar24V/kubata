package ao.allon.kubata.admin.service;

import ao.allon.kubata.admin.domain.platform.PlatformComponentHealth;
import ao.allon.kubata.admin.domain.platform.PlatformHealthSnapshot;
import ao.allon.kubata.admin.domain.platform.PlatformOperationSummary;
import ao.allon.kubata.admin.domain.platform.PlatformSessionSummary;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.AdmPlataformaItemRepository;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.AuditService;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Backend do Command Center do Kubata Administrator.
 *
 * <p>Centraliza os health checks e as operações privilegiadas. A UI consome
 * somente snapshots e comandos validados; nenhuma autorização depende de
 * botões visíveis ou estados da interface.</p>
 */
@Service
public class PlatformCommandCenterService {

    public static final String MODULE = "ADMINISTRATOR";
    public static final String VIEW_PERMISSION = "DASHBOARD";
    public static final String SESSION_PERMISSION = "UTILIZADORES";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<Flyway> flywayProvider;
    private final ModuleRegistry moduleRegistry;
    private final ModuloSistemaRepository moduloRepository;
    private final AdmPlataformaItemRepository itemRepository;
    private final BackupRecordRepository backupRepository;
    private final UserSessionRepository sessionRepository;
    private final AuditLogRepository auditRepository;
    private final AuditService auditService;
    private final AcessoService acessoService;
    private final Environment environment;

    public PlatformCommandCenterService(
            JdbcTemplate jdbcTemplate,
            ObjectProvider<Flyway> flywayProvider,
            ModuleRegistry moduleRegistry,
            ModuloSistemaRepository moduloRepository,
            AdmPlataformaItemRepository itemRepository,
            BackupRecordRepository backupRepository,
            UserSessionRepository sessionRepository,
            AuditLogRepository auditRepository,
            AuditService auditService,
            AcessoService acessoService,
            Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.flywayProvider = flywayProvider;
        this.moduleRegistry = moduleRegistry;
        this.moduloRepository = moduloRepository;
        this.itemRepository = itemRepository;
        this.backupRepository = backupRepository;
        this.sessionRepository = sessionRepository;
        this.auditRepository = auditRepository;
        this.auditService = auditService;
        this.acessoService = acessoService;
        this.environment = environment;
    }

    public PlatformHealthSnapshot checkGlobal(User actor) {
        requirePermission(actor, VIEW_PERMISSION, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER);

        List<PlatformComponentHealth> components = List.of(
                timed("Base de Dados", this::checkDatabase),
                timed("Flyway", this::checkFlyway),
                timed("Módulos", this::checkModules),
                timed("Integrações", this::checkIntegrations),
                timed("Scheduler", this::checkScheduler),
                timed("Backups", this::checkBackups),
                timed("Licenças", this::checkLicenses),
                timed("API", this::checkApi),
                timed("Sessões", this::checkSessions),
                timed("Alertas", this::checkAlerts)
        );

        PlatformHealthSnapshot snapshot = new PlatformHealthSnapshot(
                globalStatus(components),
                components,
                safeAlertCount(),
                recentOperationsInternal(),
                sessionsInternal(),
                LocalDateTime.now()
        );

        auditHealthCheck(actor, snapshot);
        return snapshot;
    }

    public List<PlatformSessionSummary> sessions(User actor) {
        requirePermission(actor, SESSION_PERMISSION, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER);
        return sessionsInternal();
    }

    @Transactional
    public void terminateSession(User actor, Long sessionId) {
        requirePermission(actor, SESSION_PERMISSION, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR);

        if (sessionId == null) {
            throw new IllegalArgumentException("Sessão inválida.");
        }

        UserSession target = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada."));

        if (actor != null && actor.getNome() != null
                && actor.getNome().equalsIgnoreCase(target.getUsername())) {
            throw new IllegalArgumentException("A sessão actual não pode ser encerrada a partir do próprio Command Center.");
        }

        sessionRepository.delete(target);

        try {
            auditService.logAction(
                    actor,
                    actor == null ? "SYSTEM" : actor.getNome(),
                    AuditLog.AuditActionType.CANCEL,
                    "USER_SESSION",
                    String.valueOf(sessionId),
                    "Sessão encerrada pelo Command Center: " + safe(target.getUsername()),
                    target.getUsername(),
                    null,
                    MODULE,
                    null,
                    "Kubata Administrator",
                    null,
                    false,
                    AuditLog.AGTComplianceLevel.HIGH
            );
        } catch (Exception ignored) {
            // Auditoria não deve impedir a conclusão segura do encerramento.
        }
    }

    public List<PlatformOperationSummary> recentOperations(User actor) {
        requirePermission(actor, VIEW_PERMISSION, ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER);
        return recentOperationsInternal();
    }

    private PlatformComponentHealth checkDatabase() {
        try {
            jdbcTemplate.execute("SELECT 1");
            String driver = "—";
            String product = "—";
            if (jdbcTemplate.getDataSource() != null) {
                try (var connection = jdbcTemplate.getDataSource().getConnection()) {
                    product = safe(connection.getMetaData().getDatabaseProductName());
                    driver = safe(connection.getMetaData().getDriverName());
                }
            }
            return ok("Conexão operacional · " + product + " · " + driver);
        } catch (Exception ex) {
            return error("Falha na conexão: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkFlyway() {
        try {
            Flyway flyway = flywayProvider.getIfAvailable();
            if (flyway == null) {
                return warn("Flyway não está disponível no contexto actual.");
            }
            int pending = flyway.info().pending().length;
            if (pending > 0) {
                return warn(pending + " migration(ões) pendente(s).");
            }
            return ok("Schema validado; nenhuma migration pendente.");
        } catch (Exception ex) {
            return error("Falha na verificação Flyway: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkModules() {
        try {
            int registered = moduleRegistry.getAllModules().size();
            long database = moduloRepository.count();
            long runtimeActive = moduleRegistry.getActiveModules().size();

            if (registered == 0 && database > 0) {
                return warn(database + " módulo(s) no catálogo, mas nenhum registado no runtime.");
            }
            return ok("Runtime " + registered + " · catálogo " + database + " · activos " + runtimeActive);
        } catch (Exception ex) {
            return error("Falha no catálogo de módulos: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkIntegrations() {
        try {
            long integrationItems = countItems("INTEGRACAO") + countItems("INTEGRATION");
            long communicationItems = countItems("COMUNICACAO");
            long errors = itemRepository.countByTipoAndEstado("COMUNICACAO", "ERRO")
                    + itemRepository.countByTipoAndEstado("INTEGRACAO", "ERRO")
                    + itemRepository.countByTipoAndEstado("INTEGRATION", "ERRO");

            if (errors > 0) {
                return warn(errors + " integração(ões)/comunicação(ões) com erro.");
            }
            return ok("Integrações " + integrationItems + " · comunicações " + communicationItems);
        } catch (Exception ex) {
            return error("Falha ao consultar integrações: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkScheduler() {
        try {
            List<ao.allon.kubata.core.domain.AdmPlataformaItem> items = itemRepository.findByTipoOrderByUpdatedAtDesc("OPERACAO");
            long active = items.stream()
                    .filter(i -> Boolean.TRUE.equals(i.getActive()) && !"PAUSADO".equalsIgnoreCase(i.getEstado()))
                    .count();
            long errors = items.stream().filter(i -> "ERRO".equalsIgnoreCase(i.getEstado())).count();

            if (errors > 0) {
                return warn(errors + " tarefa(s) em erro · " + active + " activa(s).");
            }
            return ok(active + " tarefa(s) activa(s).");
        } catch (Exception ex) {
            return error("Falha ao verificar scheduler: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkBackups() {
        try {
            BackupRecord latest = backupRepository.findByOrderByStartTimeDesc()
                    .stream()
                    .findFirst()
                    .orElse(null);
            BackupRecord latestCompleted = backupRepository
                    .findTopByStatusOrderByStartTimeDesc(BackupRecord.BackupStatus.COMPLETED)
                    .orElse(null);

            if (latest == null || latestCompleted == null) {
                return warn("Nenhum backup concluído encontrado.");
            }

            if (latest.getStatus() == BackupRecord.BackupStatus.FAILED) {
                return warn("Último backup falhou em " + format(latest.getStartTime()) + ".");
            }

            return ok("Último concluído: " + format(latestCompleted.getStartTime()));
        } catch (Exception ex) {
            return error("Falha ao consultar backups: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkLicenses() {
        try {
            List<ModuloSistema> modules = moduloRepository.findAll();
            long without = modules.stream().filter(m -> m.getLicencaChave() == null || m.getLicencaChave().isBlank()).count();
            long expired = modules.stream().filter(this::isLicenseExpired).count();
            long expiring = modules.stream().filter(this::isLicenseExpiring).count();

            if (expired > 0) {
                return warn(expired + " licença(s) expirada(s).");
            }
            if (expiring > 0) {
                return warn(expiring + " licença(s) a expirar.");
            }
            if (without > 0) {
                return warn(without + " módulo(s) sem licença registada.");
            }
            return ok("Licenças verificadas · " + modules.size() + " módulo(s).");
        } catch (Exception ex) {
            return error("Falha ao consultar licenças: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkApi() {
        try {
            String exposure = environment.getProperty("management.endpoints.web.exposure.include", "");
            String basePath = environment.getProperty("management.endpoints.web.base-path", "/actuator");
            String port = environment.getProperty("management.server.port", "");
            boolean enabled = environment.containsProperty("management.endpoints.web.exposure.include")
                    || environment.containsProperty("management.server.port");

            if (!enabled) {
                return warn("Actuator/API não está explicitamente exposto; base-path " + basePath + ".");
            }

            String endpoint = exposure == null || exposure.isBlank() ? "configurado" : exposure;
            return ok("Actuator configurado · " + endpoint + (port.isBlank() ? "" : " · porta " + port));
        } catch (Exception ex) {
            return error("Falha ao avaliar a configuração da API: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkSessions() {
        try {
            long count = sessionRepository.count();
            return ok(count + " sessão(ões) persistida(s) no contexto actual.");
        } catch (Exception ex) {
            return error("Falha ao consultar sessões: " + safe(ex.getMessage()));
        }
    }

    private PlatformComponentHealth checkAlerts() {
        try {
            long count = safeAlertCount();
            if (count > 0) {
                return warn(count + " ocorrência(s) técnica(s) activa(s).");
            }
            return ok("Nenhum alerta técnico activo.");
        } catch (Exception ex) {
            return error("Falha ao consultar alertas: " + safe(ex.getMessage()));
        }
    }

    private List<PlatformOperationSummary> recentOperationsInternal() {
        try {
            return auditRepository.findByOrderByTimestampDesc(PageRequest.of(0, 12)).getContent().stream()
                    .map(a -> new PlatformOperationSummary(
                            safe(a.getUsername()),
                            a.getActionType() == null ? "—" : a.getActionType().getDescription(),
                            safe(a.getModule()),
                            a.getTimestamp(),
                            Boolean.TRUE.equals(a.getSuccess())
                    ))
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<PlatformSessionSummary> sessionsInternal() {
        try {
            return sessionRepository.findAll().stream()
                    .sorted(Comparator.comparing(
                            UserSession::getLoginTime,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(25)
                    .map(s -> new PlatformSessionSummary(
                            s.getId(),
                            safe(s.getUsername()),
                            safe(s.getWorkstation()),
                            safe(s.getIpAddress()),
                            safe(s.getContext()),
                            s.getLoginTime()
                    ))
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private long safeAlertCount() {
        try {
            return itemRepository.countByTipo("ALERTA");
        } catch (Exception ex) {
            return 0;
        }
    }

    private long countItems(String type) {
        try {
            return itemRepository.countByTipo(type);
        } catch (Exception ex) {
            return 0;
        }
    }

    private void requirePermission(User actor, String resource,
                                   ao.allon.kubata.core.domain.PermissaoPerfil.Operacao operation) {
        if (actor == null) {
            throw new SecurityException("Sessão administrativa não autenticada.");
        }
        if (actor.isSuperadmin() || actor.getRole() == ao.allon.kubata.core.domain.Role.ADMIN) {
            return;
        }
        if (!acessoService.temAcesso(actor, MODULE, resource, operation)) {
            throw new SecurityException("Permissão insuficiente para " + resource + ".");
        }
    }

    private void auditHealthCheck(User actor, PlatformHealthSnapshot snapshot) {
        try {
            auditService.logAction(
                    actor,
                    actor.getNome(),
                    AuditLog.AuditActionType.VIEW,
                    "PLATFORM_HEALTH",
                    "GLOBAL",
                    "Health Check global do Command Center: " + snapshot.globalStatus(),
                    null,
                    snapshot.globalStatus(),
                    MODULE,
                    null,
                    "Kubata Administrator",
                    null,
                    false,
                    snapshot.components().stream().anyMatch(PlatformComponentHealth::isError)
                            ? AuditLog.AGTComplianceLevel.HIGH
                            : AuditLog.AGTComplianceLevel.NORMAL
            );
        } catch (Exception ignored) {
            // A observabilidade não deve derrubar o Command Center se a auditoria falhar.
        }
    }

    static String globalStatus(List<PlatformComponentHealth> components) {
        if (components.stream().anyMatch(PlatformComponentHealth::isError)) {
            return "INDISPONÍVEL";
        }
        if (components.stream().anyMatch(PlatformComponentHealth::isWarning)) {
            return "DEGRADADO";
        }
        return "OPERACIONAL";
    }

    private PlatformComponentHealth timed(String name, Supplier<PlatformComponentHealth> check) {
        long start = System.nanoTime();
        try {
            PlatformComponentHealth result = Objects.requireNonNull(check.get());
            return new PlatformComponentHealth(
                    name,
                    result.status(),
                    result.message(),
                    (System.nanoTime() - start) / 1_000_000,
                    LocalDateTime.now()
            );
        } catch (Exception ex) {
            return new PlatformComponentHealth(
                    name,
                    "ERROR",
                    "Falha inesperada: " + safe(ex.getMessage()),
                    (System.nanoTime() - start) / 1_000_000,
                    LocalDateTime.now()
            );
        }
    }

    private PlatformComponentHealth ok(String message) {
        return new PlatformComponentHealth("", "OK", message, 0, LocalDateTime.now());
    }

    private PlatformComponentHealth warn(String message) {
        return new PlatformComponentHealth("", "WARNING", message, 0, LocalDateTime.now());
    }

    private PlatformComponentHealth error(String message) {
        return new PlatformComponentHealth("", "ERROR", message, 0, LocalDateTime.now());
    }

    private boolean isLicenseExpired(ModuloSistema module) {
        return module != null
                && module.getLicencaValidade() != null
                && module.getLicencaValidade().isBefore(LocalDateTime.now());
    }

    private boolean isLicenseExpiring(ModuloSistema module) {
        if (module == null || module.getLicencaValidade() == null) {
            return false;
        }
        LocalDate date = module.getLicencaValidade().toLocalDate();
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && !date.isAfter(today.plusDays(30));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String format(LocalDateTime value) {
        return value == null ? "—" : value.toLocalDate() + " " + value.toLocalTime().withNano(0);
    }
}
