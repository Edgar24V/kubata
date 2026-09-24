package ao.allon.kubata.core.config;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void shouldCreateDefaultAdminWhenNoUserExistsAndNoBootstrapPasswordIsConfigured() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("admin@dev.com")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hashed-admin-password");

        dataInitializer.initData().run();

        verify(userRepository).save(any(User.class));
    }
}
