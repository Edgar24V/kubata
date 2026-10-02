package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
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
    private final PasswordPolicyService passwordPolicyService;
    private final AuditService auditService;

    public PasswordChangeService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordPolicyService passwordPolicyService,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.auditService = auditService;
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
            auditService.logError(
                    user,
                    null,
                    AuditLog.AuditActionType.REJECT,
                    "USER_PASSWORD",
                    String.valueOf(user.getId()),
                    "Tentativa de alteração de palavra-passe com credencial actual inválida.",
                    "CORE",
                    null
            );
            throw new SecurityException("A palavra-passe actual está incorrecta.");
        }

        passwordPolicyService.validateMinimumPasswordAge(user, LocalDateTime.now());
        passwordPolicyService.validateNewPassword(user, newPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve ser diferente da palavra-passe actual."
            );
        }

        String previousPasswordHash = user.getPassword();
        passwordPolicyService.recordPreviousPassword(
                user,
                previousPasswordHash,
                user.getEmail(),
                "ALTERACAO"
        );

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordProvisoria(false);
        user.setDataExpiracaoPassword(null);
        user.setPasswordResetExpiresAt(null);
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);

        User saved = userRepository.save(user);

        auditService.logAction(
                saved,
                null,
                AuditLog.AuditActionType.UPDATE,
                "USER_PASSWORD",
                String.valueOf(saved.getId()),
                "Palavra-passe alterada pelo próprio utilizador.",
                null,
                java.util.Map.of(
                        "passwordChanged", true,
                        "passwordProvisoria", false
                ),
                "CORE",
                null,
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.HIGH
        );

        return saved;
    }

}
