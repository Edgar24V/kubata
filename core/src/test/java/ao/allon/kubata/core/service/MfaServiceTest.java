package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AcessoService acessoService;

    @Mock
    private SecurityService securityService;

    @Test
    void confirmActivationShouldEnableMfaAndCreateRecoveryCodes() {
        MfaService service = new MfaService(
                userRepository,
                passwordEncoder,
                acessoService,
                securityService
        );

        User user = new User();
        user.setEmail("admin@kubata.ao");

        String secret = service.prepareActivation(user);
        assertNotNull(secret);
        assertFalse(user.isMfaEnabled());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-recovery-code");

        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        String code = String.format(
                "%06d",
                authenticator.getTotpPassword(secret)
        );

        MfaService.ActivationResult result =
                service.confirmActivation(user, code);

        assertTrue(result.enabled());
        assertEquals(10, result.recoveryCodes().size());
        assertTrue(user.isMfaEnabled());
        assertNotNull(user.getMfaRecoveryCodes());
        assertEquals(10, user.getMfaRecoveryCodes().split("\\R").length);
        verify(passwordEncoder, times(10)).encode(anyString());
        verifyNoInteractions(userRepository);
    }


    @Test
    void recoveryCodeShouldBeConsumedAfterSuccessfulValidation() {
        MfaService service = new MfaService(
                userRepository,
                passwordEncoder,
                acessoService,
                securityService
        );

        User user = new User();
        user.setMfaEnabled(true);
        user.setMfaRecoveryCodes("hash-1\nhash-2");

        when(passwordEncoder.matches("ABCDEFGHJKLM", "hash-1"))
                .thenReturn(true);

        assertTrue(
                service.verifyAndConsumeRecoveryCode(
                        user,
                        "ABCD-EFGH-JKLM"
                )
        );

        assertEquals("hash-2", user.getMfaRecoveryCodes());
        verify(userRepository).save(user);
    }

    @Test
    void provisioningUriShouldUseTotpScheme() {
        MfaService service = new MfaService(
                userRepository,
                passwordEncoder,
                acessoService,
                securityService
        );

        User user = new User();
        user.setEmail("admin@kubata.ao");

        service.prepareActivation(user);

        String uri = service.buildProvisioningUri(user);

        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + user.getMfaSecret()));
        assertTrue(uri.contains("issuer=Kubata"));
    }
}
