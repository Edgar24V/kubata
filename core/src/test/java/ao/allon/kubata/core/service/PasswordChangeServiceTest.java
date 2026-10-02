package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSecurityProfileService userSecurityProfileService;

    @Mock
    private AuditService auditService;

    private PasswordChangeService service;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(
                userRepository,
                passwordEncoder,
                userSecurityProfileService,
                auditService
        );
    }

    @Test
    void deveAlterarSenhaEEliminarEstadoProvisorio() {
        User user = new User();
        user.setId(10L);
        user.setActive(true);
        user.setPassword("HASH-ANTIGA");
        user.setPasswordProvisoria(true);
        user.setDataExpiracaoPassword(LocalDate.now().plusDays(1));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Temporaria1", "HASH-ANTIGA")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha1", "HASH-ANTIGA")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("HASH-NOVA");
        doNothing().when(userSecurityProfileService).validatePassword(user, "NovaSenha1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.changeOwnPassword(10L, "Temporaria1", "NovaSenha1");

        assertSame(user, result);
        assertEquals("HASH-NOVA", user.getPassword());
        assertFalse(user.isPasswordProvisoria());
        assertNull(user.getDataExpiracaoPassword());
        assertNotNull(user.getPasswordChangedAt());
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockoutEnd());

        verify(userRepository).save(user);
        verify(auditService).logAction(
                eq(user),
                isNull(),
                eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.UPDATE),
                eq("USER_PASSWORD"),
                eq("10"),
                eq("Palavra-passe alterada pelo próprio utilizador."),
                isNull(),
                anyMap(),
                eq("CORE"),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(ao.allon.kubata.core.domain.AuditLog.AGTComplianceLevel.HIGH)
        );
    }

    @Test
    void deveRejeitarSenhaAtualIncorreta() {
        User user = new User();
        user.setId(10L);
        user.setActive(true);
        user.setPassword("HASH-ANTIGA");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "HASH-ANTIGA")).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.changeOwnPassword(10L, "errada", "NovaSenha1")
        );

        verify(userRepository, never()).save(any(User.class));
        verify(auditService).logError(
                eq(user),
                isNull(),
                eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.REJECT),
                eq("USER_PASSWORD"),
                eq("10"),
                contains("credencial actual inválida"),
                eq("CORE"),
                isNull()
        );
    }

    @Test
    void deveRejeitarSenhaFraca() {
        User user = new User();
        user.setId(10L);
        user.setActive(true);
        user.setPassword("HASH-ANTIGA");
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setPasswordMinLength(8);
        profile.setPasswordRequireUpper(true);
        profile.setPasswordRequireLower(true);
        profile.setPasswordRequireDigit(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userSecurityProfileService.getEffectiveProfile(user)).thenReturn(profile);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeOwnPassword(10L, "Temporaria1", "1234567")
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }
}
