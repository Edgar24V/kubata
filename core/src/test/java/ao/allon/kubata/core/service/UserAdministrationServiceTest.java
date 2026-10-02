package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.FilialRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock FilialRepository filialRepository;
    @Mock PerfilAcessoRepository perfilRepository;
    @Mock UserSessionRepository userSessionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SecurityService securityService;
    @Mock AuditService auditService;
    @Mock UserSecurityProfileService userSecurityProfileService;

    private UserAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new UserAdministrationService(
                userRepository,
                empresaRepository,
                filialRepository,
                perfilRepository,
                userSessionRepository,
                passwordEncoder,
                securityService,
                auditService,
                userSecurityProfileService
        );
    }

    @Test
    void deveRejeitarOperadorSemPermissaoAntesDeAlterarDados() {
        User actor = operator(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(securityService.hasPermission(eq(actor), eq("ADMINISTRATOR"), eq("UTILIZADORES"), any())).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.alterarEstado(actor, 20L, true, "127.0.0.1")
        );

        verify(userRepository, times(1)).findById(10L);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deveCriarContaComCodigoEmpresaFilialTipoEAuditoria() {
        User actor = admin(10L);

        Empresa empresa = new Empresa();
        empresa.setId(30L);
        empresa.setNome("Kubata Angola");
        empresa.setNif("500000000");
        empresa.setAtiva(true);

        Filial filial = new Filial();
        filial.setId(40L);
        filial.setEmpresa(empresa);
        filial.setCodigo("LUANDA");
        filial.setNome("Luanda");
        filial.setActive(true);

        User draft = new User();
        draft.setNome("Ana Administradora");
        draft.setEmail("ana@kubata.local");
        draft.setRole(Role.OPERATOR);
        draft.setTipoConta(TipoConta.ADMINISTRATIVA);
        draft.setActive(true);
        draft.setPasswordProvisoria(true);
        draft.setLinhasPorPagina(50);

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userRepository.existsByEmailIgnoreCase("ana@kubata.local")).thenReturn(false);
        when(userRepository.existsByCodigoIgnoreCase(anyString())).thenReturn(false);
        when(empresaRepository.findById(30L)).thenReturn(Optional.of(empresa));
        when(filialRepository.findById(40L)).thenReturn(Optional.of(filial));
        doNothing().when(userSecurityProfileService).validatePassword(any(User.class), eq("SenhaForte1"));
        when(passwordEncoder.encode("SenhaForte1")).thenReturn("HASH-SENHA");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        User result = service.salvar(
                actor,
                draft,
                "SenhaForte1",
                Set.of(),
                empresa,
                filial,
                "127.0.0.1"
        );

        assertEquals(50L, result.getId());
        assertNotNull(result.getCodigo());
        assertTrue(result.getCodigo().startsWith("USR-"));
        assertSame(empresa, result.getEmpresa());
        assertSame(filial, result.getFilial());
        assertEquals(TipoConta.ADMINISTRATIVA, result.getTipoConta());
        assertEquals("HASH-SENHA", result.getPassword());
        assertTrue(result.isPasswordProvisoria());

        verify(auditService).logAction(
                eq(actor), isNull(), any(), eq("USER"), eq("50"),
                contains("ana@kubata.local"), isNull(), any(),
                eq("ADMINISTRATOR"), eq("127.0.0.1"), isNull(), isNull(),
                eq(false), any()
        );
    }

    @Test
    void deveRejeitarCodigoDuplicadoNoServidor() {
        User actor = admin(10L);

        User draft = new User();
        draft.setCodigo("USR-DUP");
        draft.setNome("Utilizador Duplicado");
        draft.setEmail("duplicado@kubata.local");
        draft.setRole(Role.OPERATOR);
        draft.setTipoConta(TipoConta.PESSOAL);
        draft.setActive(true);
        draft.setPasswordProvisoria(true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userRepository.existsByCodigoIgnoreCase(anyString()))
                .thenAnswer(invocation ->
                        "USR-DUP".equalsIgnoreCase(invocation.getArgument(0)));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(
                        actor,
                        draft,
                        "SenhaForte1",
                        Set.of(),
                        null,
                        null,
                        "127.0.0.1"
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(empresaRepository, filialRepository, auditService);
    }

    @Test
    void deveTerminarTodasSessoesDosOutrosUtilizadoresPreservandoASessaoDoAdministrador() {
        User actor = admin(10L);
        UserSession own = new UserSession();
        own.setId(1L);
        own.setUsername(actor.getNome());
        UserSession otherA = new UserSession();
        otherA.setId(2L);
        otherA.setUsername("Operador A");
        UserSession otherB = new UserSession();
        otherB.setId(3L);
        otherB.setUsername("Operador B");

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userSessionRepository.findAll())
                .thenReturn(java.util.List.of(own, otherA, otherB));

        long terminated = service.terminarTodasSessoesDeUtilizadores(
                actor,
                "127.0.0.1"
        );

        assertEquals(2L, terminated);
        verify(userSessionRepository).deleteAll(
                argThat(iterable -> {
                    java.util.List<? extends UserSession> list = java.util.stream.StreamSupport
                            .stream(iterable.spliterator(), false)
                            .toList();
                    return list.size() == 2
                            && list.contains(otherA)
                            && list.contains(otherB)
                            && !list.contains(own);
                })
        );
        verify(auditService).logAction(
                eq(actor),
                isNull(),
                eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.LOGOUT),
                eq("USER_SESSION_BULK"),
                eq("ALL_USERS_EXCEPT_ACTOR"),
                contains("(2)"),
                isNull(),
                anyMap(),
                eq("ADMINISTRATOR"),
                eq("127.0.0.1"),
                isNull(),
                isNull(),
                eq(false),
                eq(ao.allon.kubata.core.domain.AuditLog.AGTComplianceLevel.HIGH)
        );
    }

    @Test
    void deveTerminarApenasASessaoSeleccionadaERegistarAuditoria() {
        User actor = admin(10L);

        User target = operator(20L);
        target.setNome("Operador A");
        target.setEmail("operador.a@kubata.local");

        UserSession session = new UserSession();
        session.setId(55L);
        session.setUsername(target.getNome());
        session.setWorkstation("POSTO-01");
        session.setIpAddress("10.0.0.20");

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userSessionRepository.findById(55L)).thenReturn(Optional.of(session));
        when(userRepository.findAllByNomeIgnoreCase("Operador A"))
                .thenReturn(java.util.List.of(target));

        service.terminarSessao(actor, 55L, "127.0.0.1");

        verify(userSessionRepository).delete(session);
        verify(auditService).logAction(
                eq(actor),
                isNull(),
                eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.LOGOUT),
                eq("USER_SESSION"),
                eq("55"),
                contains("operador.a@kubata.local"),
                isNull(),
                anyMap(),
                eq("ADMINISTRATOR"),
                eq("127.0.0.1"),
                isNull(),
                isNull(),
                eq(false),
                eq(ao.allon.kubata.core.domain.AuditLog.AGTComplianceLevel.NORMAL)
        );
    }

    @Test
    void deveTratarSessaoJaRemovidaComoOperacaoIdempotente() {
        User actor = admin(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userSessionRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = service.terminarSessao(actor, 99L, "127.0.0.1");

        assertFalse(result);
        verify(userSessionRepository, never()).delete(any(UserSession.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void deveImpedirAdministradorDeTerminarASuaPropriaSessaoPelaGestaoAdministrativa() {
        User actor = admin(10L);

        UserSession session = new UserSession();
        session.setId(66L);
        session.setUsername(actor.getNome());

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userSessionRepository.findById(66L)).thenReturn(Optional.of(session));

        assertThrows(
                SecurityException.class,
                () -> service.terminarSessao(actor, 66L, "127.0.0.1")
        );

        verify(userSessionRepository, never()).delete(any(UserSession.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void devePermitirTerminarOutraSessaoDoMesmoUtilizador() {
        User actor = admin(10L);
        actor.setSessionId(100L);

        UserSession anotherSession = new UserSession();
        anotherSession.setId(101L);
        anotherSession.setUsername(actor.getNome());
        anotherSession.setWorkstation("POSTO-02");
        anotherSession.setIpAddress("10.0.0.30");

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userSessionRepository.findById(101L)).thenReturn(Optional.of(anotherSession));
        when(userRepository.findAllByNomeIgnoreCase(actor.getNome()))
                .thenReturn(java.util.List.of(actor));

        boolean result = service.terminarSessao(actor, 101L, "127.0.0.1");

        assertTrue(result);
        verify(userSessionRepository).delete(anotherSession);
        verify(auditService).logAction(
                eq(actor),
                isNull(),
                eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.LOGOUT),
                eq("USER_SESSION"),
                eq("101"),
                contains(actor.getEmail()),
                isNull(),
                anyMap(),
                eq("ADMINISTRATOR"),
                eq("127.0.0.1"),
                isNull(),
                isNull(),
                eq(false),
                eq(ao.allon.kubata.core.domain.AuditLog.AGTComplianceLevel.NORMAL)
        );
    }

    @Test
    void deveImpedirTerminoGlobalDeSessoesPorContaNaoAdministrativa() {
        User actor = operator(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));

        assertThrows(
                SecurityException.class,
                () -> service.terminarTodasSessoesDeUtilizadores(actor, "127.0.0.1")
        );

        verify(userSessionRepository, never()).findAll();
        verify(userSessionRepository, never()).deleteAll(anyList());
        verifyNoInteractions(auditService);
    }

    @Test
    void deveImpedirQueSuperadministradorAltereOSeuProprioEstatuto() {
        User actor = admin(10L);
        actor.setSuperadmin(true);

        User target = admin(10L);
        target.setSuperadmin(true);

        User draft = admin(10L);
        draft.setSuperadmin(false);
        draft.setActive(true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(userRepository.findByIdWithPerfis(10L)).thenReturn(Optional.of(target));

        assertThrows(
                SecurityException.class,
                () -> service.salvar(
                        actor,
                        draft,
                        null,
                        Set.of(),
                        target.getEmpresa(),
                        target.getFilial(),
                        "127.0.0.1"
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deveImpedirDesactivarAPropriaConta() {
        User actor = admin(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        User managedTarget = admin(10L);
        when(userRepository.findByIdWithPerfis(10L)).thenReturn(Optional.of(managedTarget));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarEstado(actor, 10L, false, "127.0.0.1")
        );

        verify(userRepository, never()).save(any());
    }

    private static User admin(Long id) {
        User user = new User();
        user.setId(id);
        user.setCodigo("USR-ADMIN");
        user.setNome("Administrador");
        user.setEmail("admin@kubata.local");
        user.setPassword("HASH");
        user.setRole(Role.ADMIN);
        user.setTipoConta(TipoConta.ADMINISTRATIVA);
        user.setActive(true);
        return user;
    }

    private static User operator(Long id) {
        User user = admin(id);
        user.setRole(Role.OPERATOR);
        return user;
    }
}
