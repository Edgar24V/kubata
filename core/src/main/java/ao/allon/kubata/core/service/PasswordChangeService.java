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

    public PasswordChangeService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        validateNewPassword(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new SecurityException("A conta está inactiva.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new SecurityException("A palavra-passe actual está incorrecta.");
        }

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

    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("A nova palavra-passe é obrigatória.");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve ter pelo menos 8 caracteres."
            );
        }

        boolean upper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean lower = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean digit = newPassword.chars().anyMatch(Character::isDigit);

        if (!upper || !lower || !digit) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve conter maiúsculas, minúsculas e números."
            );
        }
    }
}
