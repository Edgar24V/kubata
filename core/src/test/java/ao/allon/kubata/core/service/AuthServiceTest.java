package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @Test
    void authenticate_ShouldRequirePasswordChange_WhenPasswordIsExpired() {
        user.setDataExpiracaoPassword(LocalDate.now().minusDays(1));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        PasswordChangeRequiredException thrown = assertThrows(
                PasswordChangeRequiredException.class,
                () -> authService.authenticate(
                        "test@example.com",
                        "password",
                        null,
                        "127.0.0.1"
                )
        );

        assertTrue(thrown.isExpired());
        verifyNoInteractions(userSessionRepository);
    }

    @Test
    void logout_ShouldCloseOnlyTheMostRecentSession_EvenWithSeveralSessions() {
        user.setNome("Administrador");
        UserSession recente = new UserSession("Administrador", "POSTO", "127.0.0.1", "KUBATA ERP");
        UserSession antiga = new UserSession("Administrador", "POSTO", "127.0.0.1", "KUBATA ERP");
        when(userSessionRepository.findAllByUsernameOrderByLoginTimeDesc("Administrador"))
                .thenReturn(List.of(recente, antiga));

        authService.logout(user, "127.0.0.1");

        verify(userSessionRepository).delete(recente);
        verify(userSessionRepository, never()).delete(antiga);
    }

}
