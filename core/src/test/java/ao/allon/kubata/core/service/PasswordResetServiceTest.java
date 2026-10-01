package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserSecurityProfileService userSecurityProfileService;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                userSessionRepository,
                passwordEncoder,
                securityService,
                auditService,
                userSecurityProfileService
        );
        ReflectionTestUtils.setField(service, "resetValidityDays", 1);
    }

    @Test
    void deveRedefinirSenhaRevogarSessoesEAuditarSemGuardarSenha() {
        User admin = user(1L, "Administrador", "admin@kubata.ao", Role.ADMIN);
        User target = user(2L, "Operador", "operador@kubata.ao", Role.OPERATOR);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setPasswordMinLength(8);
        when(userSecurityProfileService.getEffectiveProfile(target)).thenReturn(profile);
        doNothing().when(userSecurityProfileService).validatePassword(eq(target), anyString());

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "HASH:" + invocation.getArgument(0));
        when(userSessionRepository.deleteAllByUsername("Operador")).thenReturn(2L);

        PasswordResetService.ResetResult result = service.resetByAdministrator(
                admin,
                2L,
                "192.168.1.20",
                "Solicitação administrativa do responsável"
        );

        assertNotNull(result);
        assertNotNull(result.temporaryPassword());
        assertEquals(16, result.temporaryPassword().length());
        assertEquals(2L, result.revokedSessions());
        assertNotNull(result.expiresOn());

        assertEquals("HASH:" + result.temporaryPassword(), target.getPassword());
        assertTrue(target.isPasswordProvisoria());
        assertEquals(0, target.getFailedAttempts());
        assertNull(target.getLockoutEnd());
        assertEquals(result.expiresOn(), target.getDataExpiracaoPassword());

        verify(userRepository).save(target);
        verify(userSessionRepository).deleteAllByUsername("Operador");

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logAction(
                eq(admin),
                isNull(),
                eq(AuditLog.AuditActionType.RESET_PASSWORD),
                eq("UTILIZADOR"),
                eq("2"),
                descriptionCaptor.capture(),
                isNull(),
                anyMap(),
                eq("ADMINISTRATOR"),
                eq("192.168.1.20"),
                isNull(),
                isNull(),
                eq(false),
                eq(AuditLog.AGTComplianceLevel.HIGH)
        );

        assertTrue(descriptionCaptor.getValue().contains("Solicitação administrativa"));
        assertFalse(descriptionCaptor.getValue().contains(result.temporaryPassword()));
    }

    @Test
    void deveRejeitarRedefinicaoDaPropriaConta() {
        User admin = user(1L, "Administrador", "admin@kubata.ao", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> service.resetByAdministrator(
                        admin,
                        1L,
                        "127.0.0.1",
                        "Teste de segurança"
                )
        );

        assertTrue(exception.getMessage().contains("própria conta"));
        verifyNoInteractions(passwordEncoder, userSessionRepository, auditService);
    }

    @Test
    void deveRejeitarOperadorSemPermissao() {
        User operator = user(1L, "Operador", "operator@kubata.ao", Role.OPERATOR);
        User target = user(2L, "Utilizador", "user@kubata.ao", Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(securityService.hasPermission(
                eq(operator),
                eq("ADMINISTRATOR"),
                eq("UTILIZADORES"),
                any()
        )).thenReturn(false);

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> service.resetByAdministrator(
                        operator,
                        2L,
                        "127.0.0.1",
                        "Tentativa sem autorização"
                )
        );

        assertTrue(exception.getMessage().toLowerCase().contains("permiss"));
        verifyNoInteractions(passwordEncoder, userSessionRepository, auditService);
    }

    private User user(Long id, String nome, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
