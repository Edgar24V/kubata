package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.User;
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
                auditService
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
