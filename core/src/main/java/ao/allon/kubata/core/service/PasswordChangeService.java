package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço único para alteração da palavra-passe pelo próprio utilizador,
 * incluindo o primeiro acesso com credencial provisória.
 */
@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityProfileService userSecurityProfileService;

    public PasswordChangeService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 UserSecurityProfileService userSecurityProfileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSecurityProfileService = userSecurityProfileService;
    }

    @Transactional
    public User changeOwnPassword(Long userId,
                                  String currentPassword,
                                  String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("A palavra-passe actual é obrigatória.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new SecurityException("A conta está inactiva.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new SecurityException("A palavra-passe actual está incorrecta.");
        }

        userSecurityProfileService.validatePassword(user, newPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve ser diferente da palavra-passe actual."
            );
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordProvisoria(false);
        user.setDataExpiracaoPassword(null);
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);

        return userRepository.save(user);
    }

}
