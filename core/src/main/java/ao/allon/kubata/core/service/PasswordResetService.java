package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.PasswordPolicy;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluxo administrativo seguro de redefinição de palavra-passe.
 *
 * A palavra-passe temporária só existe em memória até ser apresentada pela UI.
 * Nunca é incluída na auditoria.
 */
@Service
public class PasswordResetService {

    private static final String MODULE = "ADMINISTRATOR";
    private static final String RESOURCE = "UTILIZADORES";

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final AuditService auditService;
    private final PasswordPolicyService passwordPolicyService;
    private final UserSecurityProfileService legacyUserSecurityProfileService;
    private final SecureRandom secureRandom = new SecureRandom();

    private int resetValidityDays = 1;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                UserSessionRepository userSessionRepository,
                                PasswordEncoder passwordEncoder,
                                SecurityService securityService,
                                AuditService auditService,
                                PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
        this.auditService = auditService;
        this.passwordPolicyService = passwordPolicyService;
        this.legacyUserSecurityProfileService = null;
    }

    public PasswordResetService(UserRepository userRepository,
                                UserSessionRepository userSessionRepository,
                                PasswordEncoder passwordEncoder,
                                SecurityService securityService,
                                AuditService auditService,
                                UserSecurityProfileService userSecurityProfileService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
        this.auditService = auditService;
        this.passwordPolicyService = null;
        this.legacyUserSecurityProfileService = userSecurityProfileService;
    }

    @Transactional
    public ResetResult resetByAdministrator(User actor,
                                            Long targetUserId,
                                            String sourceIp,
                                            String reason) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }

        if (targetUserId == null) {
            throw new IllegalArgumentException("Utilizador de destino inválido.");
        }

        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 5) {
            throw new IllegalArgumentException(
                    "O motivo da redefinição deve ter pelo menos 5 caracteres."
            );
        }

        User managedActor = userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException(
                        "O utilizador administrador da sessão já não existe."
                ));

        boolean authorized =
                managedActor.isSuperadmin()
                        || managedActor.getRole() == Role.ADMIN
                        || securityService.hasPermission(
                                managedActor,
                                MODULE,
                                RESOURCE,
                                PermissaoPerfil.Operacao.EDITAR
                        );

        if (!authorized) {
            throw new SecurityException(
                    "Não possui permissão para redefinir palavras-passe de utilizadores."
            );
        }

        if (managedActor.getId().equals(targetUserId)) {
            throw new SecurityException(
                    "A redefinição administrativa não pode ser aplicada à própria conta."
            );
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilizador de destino não encontrado."
                ));

        LocalDateTime resetAt = LocalDateTime.now();
        PasswordPolicy policy;
        LocalDateTime resetExpiresAt;
        if (passwordPolicyService != null) {
            policy = passwordPolicyService.getEffectivePolicy(target);
            resetExpiresAt = resetAt.plusHours(policy.getResetValidityHours());
        } else {
            UserSecurityProfile legacy = legacyUserSecurityProfileService.getEffectiveProfile(target);
            policy = new PasswordPolicy();
            policy.setMinLength(legacy.getPasswordMinLength());
            policy.setMaxLength(Math.max(128, legacy.getPasswordMinLength()));
            policy.setRequireUpper(legacy.isPasswordRequireUpper());
            policy.setRequireLower(legacy.isPasswordRequireLower());
            policy.setRequireDigit(legacy.isPasswordRequireDigit());
            policy.setRequireSymbol(legacy.isPasswordRequireSymbol());
            policy.setHistoryCount(5);
            policy.setExpiryDays(legacy.getPasswordExpiryDays());
            policy.setResetValidityHours(Math.max(1, resetValidityDays * 24));
            policy.setForceChangeOnReset(true);
            policy.setProhibitIdentityFragments(true);
            resetExpiresAt = resetAt.plusDays(Math.max(1, resetValidityDays));
        }
        LocalDate expiresOn = resetExpiresAt.toLocalDate();

        int temporaryPasswordLength = Math.max(
                policy.getMinLength(),
                Math.min(16, policy.getMaxLength())
        );

        String temporaryPassword = generateTemporaryPassword(temporaryPasswordLength);
        if (passwordPolicyService != null) {
            passwordPolicyService.validateNewPassword(target, temporaryPassword);
            passwordPolicyService.recordPreviousPassword(
                    target,
                    target.getPassword(),
                    managedActor.getEmail(),
                    "RESET_ADMINISTRATIVO"
            );
        } else {
            legacyUserSecurityProfileService.validatePassword(target, temporaryPassword);
        }

        target.setPassword(passwordEncoder.encode(temporaryPassword));
        target.setPasswordChangedAt(resetAt);
        target.setPasswordProvisoria(policy.isForceChangeOnReset());
        target.setDataExpiracaoPassword(
                policy.isForceChangeOnReset() ? expiresOn : null
        );
        target.setPasswordResetExpiresAt(
                policy.isForceChangeOnReset() ? resetExpiresAt : null
        );
        target.setFailedAttempts(0);
        target.setLockoutEnd(null);

        userRepository.save(target);

        long revokedSessions = userSessionRepository.deleteAllByUsername(target.getNome());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("passwordProvisoria", target.isPasswordProvisoria());
        newValues.put("dataExpiracaoPassword", expiresOn.toString());
        newValues.put("sessoesRevogadas", revokedSessions);

        String targetLabel = target.getNome() == null || target.getNome().isBlank()
                ? target.getEmail()
                : target.getNome();

        String description = "Redefinição administrativa de palavra-passe para "
                + targetLabel
                + ". Motivo: "
                + normalizedReason
                + ". Sessões revogadas: "
                + revokedSessions
                + ".";

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.RESET_PASSWORD,
                "UTILIZADOR",
                String.valueOf(target.getId()),
                description,
                null,
                newValues,
                MODULE,
                normalizeIp(sourceIp),
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.HIGH
        );

        return new ResetResult(
                temporaryPassword,
                resetAt,
                expiresOn,
                revokedSessions
        );
    }

    private String generateTemporaryPassword(int length) {
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String lower = "abcdefghijkmnopqrstuvwxyz";
        final String digits = "23456789";
        final String symbols = "@#$%&*!";

        StringBuilder password = new StringBuilder(length);
        password.append(randomChar(upper));
        password.append(randomChar(lower));
        password.append(randomChar(digits));
        password.append(randomChar(symbols));

        String alphabet = upper + lower + digits + symbols;

        while (password.length() < length) {
            password.append(randomChar(alphabet));
        }

        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }

        return new String(chars);
    }

    private char randomChar(String alphabet) {
        return alphabet.charAt(secureRandom.nextInt(alphabet.length()));
    }

    private String normalizeIp(String sourceIp) {
        return sourceIp == null || sourceIp.isBlank()
                ? "127.0.0.1"
                : sourceIp.trim();
    }

    public record ResetResult(String temporaryPassword,
                              LocalDateTime resetAt,
                              LocalDate expiresOn,
                              long revokedSessions) {
    }
}
