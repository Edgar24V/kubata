package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String RECOVERY_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gera um novo segredo TOTP, mas não activa a conta.
     * A activação só acontece depois da confirmação do primeiro código.
     */
    public String prepareActivation(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }

        GoogleAuthenticatorKey key =
                googleAuthenticator.createCredentials();

        user.setMfaSecret(key.getKey());
        user.setMfaEnabled(false);
        user.setMfaRecoveryCodes(null);

        return key.getKey();
    }

    /**
     * Confirma o primeiro código TOTP e activa MFA.
     * Os códigos de recuperação retornados só devem ser apresentados uma vez.
     */
    @Transactional
    public ActivationResult confirmActivation(User user, String code) {
        requireUser(user);

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException(
                    "Primeiro gere uma configuração MFA."
            );
        }

        String normalizedCode = normalizeTotpCode(code);
        if (normalizedCode == null
                || !googleAuthenticator.authorize(
                user.getMfaSecret(),
                Integer.parseInt(normalizedCode))) {
            throw new IllegalArgumentException(
                    "O código MFA está inválido ou expirado."
            );
        }

        List<String> recoveryCodes = generateRecoveryCodes();
        String hashedCodes = recoveryCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining("\n"));

        user.setMfaEnabled(true);
        user.setMfaRecoveryCodes(hashedCodes);

        if (user.getId() != null) {
            userRepository.save(user);
        }

        return new ActivationResult(
                recoveryCodes,
                true
        );
    }

    @Transactional
    public void disableMfa(User user) {
        requireUser(user);

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);

        if (user.getId() != null) {
            userRepository.save(user);
        }
    }

    /**
     * Gera novos códigos de recuperação. O código TOTP actual é obrigatório.
     * Os códigos anteriores deixam de ser válidos.
     */
    @Transactional
    public List<String> regenerateRecoveryCodes(User user, String totpCode) {
        requireUser(user);

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new IllegalStateException(
                    "O MFA não está activo nesta conta."
            );
        }

        String normalizedCode = normalizeTotpCode(totpCode);
        if (normalizedCode == null
                || !googleAuthenticator.authorize(
                user.getMfaSecret(),
                Integer.parseInt(normalizedCode))) {
            throw new IllegalArgumentException(
                    "O código MFA está inválido ou expirado."
            );
        }

        if (user.getId() == null) {
            throw new IllegalStateException(
                    "Guarde o utilizador antes de regenerar os códigos de recuperação."
            );
        }

        List<String> recoveryCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodes(
                recoveryCodes.stream()
                        .map(passwordEncoder::encode)
                        .collect(Collectors.joining("\n"))
        );

        userRepository.save(user);
        return recoveryCodes;
    }

    /**
     * Valida e consome um código de recuperação de uso único.
     * Retorna false quando não existe um código válido.
     */
    @Transactional
    public boolean verifyAndConsumeRecoveryCode(
            User user,
            String recoveryCode
    ) {
        requireUser(user);

        if (!user.isMfaEnabled()
                || user.getMfaRecoveryCodes() == null
                || user.getMfaRecoveryCodes().isBlank()) {
            return false;
        }

        String normalized = normalizeRecoveryCode(recoveryCode);
        if (normalized == null) {
            return false;
        }

        List<String> hashes = Arrays.stream(
                        user.getMfaRecoveryCodes().split("\\R")
                )
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < hashes.size(); i++) {
            String hash = hashes.get(i);
            if (passwordEncoder.matches(normalized, hash)) {
                hashes.remove(i);
                user.setMfaRecoveryCodes(
                        hashes.isEmpty()
                                ? null
                                : String.join("\n", hashes)
                );
                userRepository.save(user);
                return true;
            }
        }

        return false;
    }

    public int countRecoveryCodes(User user) {
        if (user == null
                || user.getMfaRecoveryCodes() == null
                || user.getMfaRecoveryCodes().isBlank()) {
            return 0;
        }

        return (int) Arrays.stream(
                        user.getMfaRecoveryCodes().split("\\R")
                )
                .filter(value -> !value.isBlank())
                .count();
    }

    public String buildProvisioningUri(User user) {
        requireUser(user);

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException(
                    "A conta ainda não possui um segredo MFA."
            );
        }

        String label = encodeComponent(
                "Kubata:" + safe(user.getEmail(), user.getNome())
        );
        String issuer = encodeComponent("Kubata");

        return "otpauth://totp/"
                + label
                + "?secret="
                + user.getMfaSecret()
                + "&issuer="
                + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);

        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String raw = randomToken(12);
            codes.add(
                    raw.substring(0, 4)
                            + "-"
                            + raw.substring(4, 8)
                            + "-"
                            + raw.substring(8, 12)
            );
        }

        return Collections.unmodifiableList(codes);
    }

    private String randomToken(int length) {
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(
                    RECOVERY_ALPHABET.charAt(
                            secureRandom.nextInt(RECOVERY_ALPHABET.length())
                    )
            );
        }

        return result.toString();
    }

    private String normalizeTotpCode(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code.replaceAll("\\s+", "");
        return normalized.matches("\\d{6}") ? normalized : null;
    }

    private String normalizeRecoveryCode(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code
                .replace("-", "")
                .replaceAll("\\s+", "")
                .toUpperCase();

        return normalized.matches("[A-Z2-9]{12}")
                ? normalized
                : null;
    }

    private String encodeComponent(String value) {
        try {
            return java.net.URLEncoder.encode(
                    value,
                    java.nio.charset.StandardCharsets.UTF_8
            ).replace("+", "%20");
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Não foi possível criar a configuração MFA.",
                    ex
            );
        }
    }

    private String safe(String first, String second) {
        return first == null || first.isBlank()
                ? (second == null || second.isBlank()
                ? "utilizador"
                : second)
                : first;
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilizador inválido.");
        }
    }

    public record ActivationResult(
            List<String> recoveryCodes,
            boolean enabled
    ) {
    }
}
