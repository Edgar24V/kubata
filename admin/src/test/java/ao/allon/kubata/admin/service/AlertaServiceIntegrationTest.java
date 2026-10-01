package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Alerta;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AlertaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AlertaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class AlertaServiceIntegrationTest {

    @Autowired
    private AlertaService alertaService;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void devePersistirCicloCompletoDoAlertaNoContextoSpring() {
        User actor = new User();
        actor.setNome("Alerta Integration");
        actor.setEmail("alerta.integration." + System.nanoTime() + "@test.local");
        actor.setPassword("integration");
        actor.setRole(Role.ADMIN);
        actor.setActive(true);
        actor = userRepository.saveAndFlush(actor);

        Alerta criado = alertaService.criar(
                actor,
                "IT-" + System.nanoTime(),
                "Alerta de integração",
                "Teste do ciclo de vida do Alert Center.",
                Alerta.Severidade.MEDIUM,
                "INTEGRATION_TEST",
                "TEST-RUN",
                actor
        );

        assertEquals(Alerta.Estado.OPEN, criado.getEstado());
        assertNotNull(criado.getId());

        alertaService.reconhecer(actor, criado.getId(), "Reconhecido no teste.");
        alertaService.resolver(actor, criado.getId(), "Resolvido no teste.");

        Alerta resolved = alertaRepository.findById(criado.getId()).orElseThrow();
        assertEquals(Alerta.Estado.RESOLVED, resolved.getEstado());
        assertEquals(actor.getId(), resolved.getResolvidoPor().getId());

        alertaService.reabrir(actor, criado.getId(), "Regressão simulada.");
        Alerta reopened = alertaRepository.findById(criado.getId()).orElseThrow();
        assertEquals(Alerta.Estado.OPEN, reopened.getEstado());
        assertNull(reopened.getResolvedAt());
        assertNull(reopened.getResolvidoPor());
        assertTrue(alertaService.historico(actor, criado.getId()).size() >= 4);
    }

    @Test
    void deveDisponibilizarServicoNoContextoSpring() {
        assertNotNull(alertaService);
    }
}
