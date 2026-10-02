package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ao.allon.kubata.core.domain.UserSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AcessoService acessoService;

    @Mock
    private ao.allon.kubata.core.repository.UserSessionRepository userSessionRepository;

    @Mock
    private MfaService mfaService;

    @Mock
    private UserSecurityProfileService userSecurityProfileService;

    @Mock
    private UserDeviceService userDeviceService;

    @InjectMocks
    private AuthService authService;


    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setActive(true);
        user.setPasswordProvisoria(false);
    }

    @Test
    void authenticate_ShouldReturnUser_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        User result = authService.authenticate("test@example.com", "password", null, "127.0.0.1");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void authenticate_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> 
            authService.authenticate("wrong@example.com", "password", null, "127.0.0.1"));
    }

    @Test
    void authenticate_ShouldThrowException_WhenPasswordIsInvalid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> 
            authService.authenticate("test@example.com", "wrongPassword", null, "127.0.0.1"));
    }

    @Test
    void terminateOldestSessionForLogin_ShouldRejectNonAdministrativeAccount() {
        user.setRole(ao.allon.kubata.core.domain.Role.USER);
        user.setSuperadmin(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(true);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.terminateOldestSessionForLogin(
                        "test@example.com",
                        "password",
                        "127.0.0.1"
                )
        );

        assertEquals(
                "Apenas Administradores e Superadministradores podem terminar sessões a partir do ecrã de login.",
                exception.getMessage()
        );
        verify(userSessionRepository, never()).delete(any(UserSession.class));
        verify(userSessionRepository, never())
                .findAllByUsernameOrderByLoginTimeDesc(anyString());
        verify(acessoService).registrarAuditoria(
                eq(user),
                eq("LOGIN_SESSION_TERMINATE"),
                eq("AUTH"),
                eq("127.0.0.1"),
                contains("conta não administrativa"),
                eq(false)
        );
    }

    @Test
    void terminateOldestSessionForLogin_ShouldDeleteOnlyOldestSession() {
        UserSession newer = new UserSession();
        newer.setId(2L);
        newer.setUsername(user.getNome());
        newer.setLoginTime(LocalDateTime.of(2026, 10, 2, 10, 30));

        UserSession older = new UserSession();
        older.setId(1L);
        older.setUsername(user.getNome());
        older.setLoginTime(LocalDateTime.of(2026, 10, 2, 9, 30));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(true);
        when(userSessionRepository.findAllByUsernameOrderByLoginTimeDesc(user.getNome()))
                .thenReturn(List.of(newer, older));

        int deleted = authService.terminateOldestSessionForLogin(
                "test@example.com",
                "password",
                "127.0.0.1"
        );

        assertEquals(1, deleted);
        verify(userSessionRepository).delete(older);
        verify(userSessionRepository, never()).delete(newer);
        verify(acessoService).registrarAuditoria(
                eq(user),
                eq("LOGOUT"),
                eq("AUTH"),
                eq("127.0.0.1"),
                contains("Sessão antiga terminada"),
                eq(true)
        );
    }

    @Test
    void authenticate_ShouldThrowException_WhenUserIsInactive() {
        user.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(AuthenticationException.class, () -> 
            authService.authenticate("test@example.com", "password", null, "127.0.0.1"));
    }

    @Test
    void authenticate_ShouldRequirePasswordChange_WhenPasswordIsProvisional() {
        user.setPasswordProvisoria(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(
                PasswordChangeRequiredException.class,
                () -> authService.authenticate(
                        "test@example.com",
                        "password",
                        null,
                        "127.0.0.1"
                )
        );

        verifyNoInteractions(userSessionRepository);
    }

}
