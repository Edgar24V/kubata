package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("sqlite")
class PlatformCommandCenterServiceIntegrationTest {

    @BeforeAll
    static void setupJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit já inicializado.
        }
    }

    @Autowired
    private PlatformCommandCenterService commandCenter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void deveDisponibilizarCommandCenterNoContextoSpring() {
        assertNotNull(commandCenter);
    }

    @Test
    void deveRecusarHealthCheckSemSessaoAutenticada() {
        assertThrows(SecurityException.class, () -> commandCenter.checkGlobal(null));
    }

    @Test
    @Transactional
    void deveEncerrarSessaoEGuardarAuditoriaNaMesmaOperacao() {
        String suffix = java.util.UUID.randomUUID().toString();
        User actor = new User();
        actor.setCodigo("IT-ADM-" + suffix.substring(0, 8));
        actor.setNome("Admin Integration");
        actor.setEmail("admin-session-" + suffix + "@test.local");
        actor.setPassword("test-hash");
        actor.setRole(Role.ADMIN);
        actor.setActive(true);
        actor = userRepository.saveAndFlush(actor);

        UserSession target = new UserSession(
                "user-target-" + suffix, "POSTO-IT", "127.0.0.2", "INTEGRATION_TEST");
        target = sessionRepository.saveAndFlush(target);
        Long sessionId = target.getId();

        commandCenter.terminateSession(actor, sessionId);

        assertFalse(sessionRepository.existsById(sessionId),
                "A sessão seleccionada deve ser removida.");
        assertTrue(auditLogRepository
                        .findByEntityTypeAndEntityIdOrderByTimestampDesc("USER_SESSION", String.valueOf(sessionId))
                        .stream()
                        .anyMatch(log -> log.getActionType() == AuditLog.AuditActionType.CANCEL),
                "O encerramento deve ficar registado na auditoria.");
    }

    @Test
    @Transactional
    void deveRejeitarEncerramentoPorUtilizadorSemPermissaoAdministrativa() {
        String suffix = java.util.UUID.randomUUID().toString();
        User actor = new User();
        actor.setCodigo("IT-OP-" + suffix.substring(0, 8));
        actor.setNome("Operator Integration");
        actor.setEmail("operator-session-" + suffix + "@test.local");
        actor.setPassword("test-hash");
        actor.setRole(Role.OPERATOR);
        actor.setActive(true);
        actor = userRepository.saveAndFlush(actor);

        UserSession target = new UserSession(
                "user-protected-" + suffix, "POSTO-IT", "127.0.0.3", "INTEGRATION_TEST");
        target = sessionRepository.saveAndFlush(target);
        Long sessionId = target.getId();

        assertThrows(SecurityException.class, () -> commandCenter.terminateSession(actor, sessionId));
        assertTrue(sessionRepository.existsById(sessionId),
                "Uma sessão não pode ser removida quando a autorização falha.");
    }
}
