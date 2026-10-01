package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.FilialRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.service.UserAdministrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class UserAdministrationServiceIntegrationTest {

    @Autowired
    private UserAdministrationService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private FilialRepository filialRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void deveCriarContaComContextoEmpresarialEGerirEstado() {
        User actor = new User();
        actor.setNome("User Admin Integration");
        actor.setEmail("user-admin-" + System.nanoTime() + "@test.local");
        actor.setPassword("AdminHash1");
        actor.setRole(Role.ADMIN);
        actor.setTipoConta(TipoConta.ADMINISTRATIVA);
        actor.setActive(true);
        actor.setCodigo("IT-" + System.nanoTime());
        actor = userRepository.saveAndFlush(actor);

        Empresa empresa = new Empresa();
        empresa.setNome("Empresa Integration " + System.nanoTime());
        empresa.setNif("NIF-" + System.nanoTime());
        empresa.setAtiva(true);
        empresa = empresaRepository.saveAndFlush(empresa);

        Filial filial = new Filial();
        filial.setEmpresa(empresa);
        filial.setCodigo("LDA");
        filial.setNome("Luanda");
        filial.setActive(true);
        filial = filialRepository.saveAndFlush(filial);

        User draft = new User();
        draft.setNome("Utilizador Gerido");
        draft.setEmail("managed-" + System.nanoTime() + "@test.local");
        draft.setRole(Role.OPERATOR);
        draft.setTipoConta(TipoConta.PESSOAL);
        draft.setActive(true);
        draft.setPasswordProvisoria(true);
        draft.setLinhasPorPagina(50);

        User created = service.salvar(
                actor,
                draft,
                "SenhaForte1",
                java.util.Set.of(),
                empresa,
                filial,
                "127.0.0.1"
        );

        assertNotNull(created.getId());
        assertNotNull(created.getCodigo());
        assertEquals(empresa.getId(), created.getEmpresa().getId());
        assertEquals(filial.getId(), created.getFilial().getId());
        assertEquals(TipoConta.PESSOAL, created.getTipoConta());
        assertTrue(passwordEncoder.matches("SenhaForte1", created.getPassword()));

        User persisted = userRepository.findByIdWithPerfis(created.getId()).orElseThrow();
        assertEquals(created.getCodigo(), persisted.getCodigo());
        assertEquals(filial.getId(), persisted.getFilial().getId());

        service.alterarEstado(actor, created.getId(), false, "127.0.0.1");
        User disabled = userRepository.findById(created.getId()).orElseThrow();
        assertFalse(disabled.getActive());

        service.desbloquear(actor, created.getId(), "127.0.0.1");
        User unlocked = userRepository.findById(created.getId()).orElseThrow();
        assertEquals(0, unlocked.getFailedAttempts());
        assertNull(unlocked.getLockoutEnd());

        service.terminarSessoes(actor, created.getId(), "127.0.0.1");
        assertTrue(sessionRepository.findAllByUsernameOrderByLoginTimeDesc(created.getNome()).isEmpty());
    }

    @Test
    void deveDisponibilizarServicoNoContextoSpring() {
        assertNotNull(service);
    }
}
