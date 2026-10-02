package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.MfaPolicy;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.MfaPolicyRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String RECOVERY_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcessoService acessoService;
    private final SecurityService securityService;
    private final MfaPolicyRepository mfaPolicyRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilAcessoRepository perfilAcessoRepository;
    private final UserSecurityProfileService userSecurityProfileService;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public MfaService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AcessoService acessoService,
                      SecurityService securityService,
                      MfaPolicyRepository mfaPolicyRepository,
                      EmpresaRepository empresaRepository,
                      PerfilAcessoRepository perfilAcessoRepository,
                      UserSecurityProfileService userSecurityProfileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
        this.securityService = securityService;
        this.mfaPolicyRepository = mfaPolicyRepository;
        this.empresaRepository = empresaRepository;
        this.perfilAcessoRepository = perfilAcessoRepository;
        this.userSecurityProfileService = userSecurityProfileService;
    }

    public MfaService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AcessoService acessoService,
                      SecurityService securityService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
        this.securityService = securityService;
        this.mfaPolicyRepository = null;
        this.empresaRepository = null;
        this.perfilAcessoRepository = null;
        this.userSecurityProfileService = null;
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
        return confirmActivationInternal(user, code, user, null, "MFA_ACTIVATED");
    }

    private ActivationResult confirmActivationInternal(
            User user,
            String code,
            User auditActor,
            String sourceIp,
            String auditOperation) {
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

        MfaPolicy policy = getEffectivePolicy(user);
        List<String> recoveryCodes = policy.isAllowRecoveryCodes()
                ? generateRecoveryCodes(policy.getRecoveryCodeCount())
                : List.of();

        String hashedCodes = recoveryCodes.isEmpty()
                ? null
                : recoveryCodes.stream()
                        .map(passwordEncoder::encode)
                        .collect(Collectors.joining("\n"));

        user.setMfaEnabled(true);
        user.setMfaRecoveryCodes(hashedCodes);

        if (user.getId() != null) {
            userRepository.save(user);
            acessoService.registrarAuditoria(
                    auditActor,
                    auditOperation,
                    sourceIp == null ? "AUTH" : "UTILIZADORES",
                    sourceIp,
                    (auditActor == user
                            ? "MFA TOTP activado"
                            : "MFA TOTP activado administrativamente para " + user.getEmail()),
                    true
            );
        }

        return new ActivationResult(recoveryCodes, true);
    }

    /**
     * Operações de MFA realizadas por um administrador sobre outra conta.
     * A autorização é sempre validada no servidor.
     */
    @Transactional
    public ActivationResult adminConfirmActivation(User actor, Long targetUserId, String code) {
        return adminConfirmActivation(actor, targetUserId, code, "127.0.0.1");
    }

    @Transactional
    public ActivationResult adminConfirmActivation(
            User actor,
            Long targetUserId,
            String code,
            String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);
        return confirmActivationInternal(
                target,
                code,
                actor,
                sourceIp,
                "MFA_ADMIN_ACTIVATED"
        );
    }

    @Transactional
    public void adminDisableMfa(User actor, Long targetUserId, String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);
        disableMfaInternal(
                target,
                actor,
                sourceIp,
                "MFA_ADMIN_DISABLED",
                true
        );
    }

    @Transactional
    public List<String> adminRegenerateRecoveryCodes(
            User actor,
            Long targetUserId,
            String code,
            String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);
        return regenerateRecoveryCodesInternal(
                target,
                code,
                actor,
                sourceIp,
                "MFA_RECOVERY_CODES_REGENERATED"
        );
    }

    private User authorizedAdminTarget(User actor, Long targetUserId) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }

        User managedActor = userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException("Administrador da sessão não encontrado."));

        if (!Boolean.TRUE.equals(managedActor.getActive())) {
            throw new SecurityException("A conta administrativa está inactiva.");
        }

        if (!managedActor.isSuperadmin()
                && managedActor.getRole() != ao.allon.kubata.core.domain.Role.ADMIN
                && !securityService.hasPermission(
                        managedActor,
                        "ADMINISTRATOR",
                        "UTILIZADORES",
                        ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR)) {
            throw new SecurityException("Não possui permissão para gerir o MFA de utilizadores.");
        }

        if (targetUserId == null) {
            throw new IllegalArgumentException("Utilizador de destino inválido.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException("Só um Superadministrador pode gerir o MFA de outro Superadministrador.");
        }

        return target;
    }

    @Transactional
    public void disableMfa(User user) {
        requireUser(user);
        MfaPolicy policy = getEffectivePolicy(user);
        if (policy.isRequired() || !policy.isAllowUserDisable()) {
            throw new SecurityException(
                    "A política MFA desta conta exige autenticação multifactor e não permite a sua desactivação voluntária."
            );
        }
        disableMfaInternal(user, user, null, "MFA_DISABLED", false);
    }

    private void disableMfaInternal(
            User target,
            User auditActor,
            String sourceIp,
            String auditOperation,
            boolean administrative) {
        target.setMfaEnabled(false);
        target.setMfaSecret(null);
        target.setMfaRecoveryCodes(null);

        if (target.getId() != null) {
            userRepository.save(target);
            acessoService.registrarAuditoria(
                    auditActor,
                    auditOperation,
                    administrative ? "UTILIZADORES" : "AUTH",
                    sourceIp,
                    administrative
                            ? "MFA desactivado administrativamente para " + target.getEmail()
                            : "MFA TOTP desactivado",
                    true
            );
        }
    }

    /**
     * Gera novos códigos de recuperação. O código TOTP actual é obrigatório.
     * Os códigos anteriores deixam de ser válidos.
     */
    @Transactional
    public List<String> regenerateRecoveryCodes(User user, String totpCode) {
        requireUser(user);
        return regenerateRecoveryCodesInternal(
                user,
                totpCode,
                user,
                null,
                "MFA_RECOVERY_REGENERATED"
        );
    }

    private List<String> regenerateRecoveryCodesInternal(
            User target,
            String totpCode,
            User auditActor,
            String sourceIp,
            String auditOperation) {
        if (!target.isMfaEnabled() || target.getMfaSecret() == null) {
            throw new IllegalStateException(
                    "O MFA não está activo nesta conta."
            );
        }

        if (!getEffectivePolicy(target).isAllowRecoveryCodes()) {
            throw new SecurityException("A política MFA não permite códigos de recuperação.");
        }

        String normalizedCode = normalizeTotpCode(totpCode);
        if (normalizedCode == null
                || !googleAuthenticator.authorize(
                target.getMfaSecret(),
                Integer.parseInt(normalizedCode))) {
            throw new IllegalArgumentException(
                    "O código MFA está inválido ou expirado."
            );
        }

        if (target.getId() == null) {
            throw new IllegalStateException(
                    "Guarde o utilizador antes de regenerar os códigos de recuperação."
            );
        }

        MfaPolicy policy = getEffectivePolicy(target);
        if (!policy.isAllowRecoveryCodes()) {
            throw new SecurityException("A política MFA não permite códigos de recuperação.");
        }

        List<String> recoveryCodes = generateRecoveryCodes(policy.getRecoveryCodeCount());
        target.setMfaRecoveryCodes(
                recoveryCodes.stream()
                        .map(passwordEncoder::encode)
                        .collect(Collectors.joining("\n"))
        );

        userRepository.save(target);
        acessoService.registrarAuditoria(
                auditActor,
                auditOperation,
                sourceIp == null ? "AUTH" : "UTILIZADORES",
                sourceIp,
                auditActor == target
                        ? "Códigos de recuperação MFA regenerados"
                        : "Códigos MFA regenerados administrativamente para " + target.getEmail(),
                true
        );
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

        if (!isRecoveryCodeAllowed(user)
                || !user.isMfaEnabled()
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

                acessoService.registrarAuditoria(
                        user,
                        "CONFIG_CHANGE",
                        "MFA_RECOVERY_CODE",
                        user.getUltimoIpLogin(),
                        "Código de recuperação MFA consumido; códigos restantes: " + hashes.size(),
                        true
                );
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
        String issuer = encodeComponent(getEffectivePolicy(user).getIssuer());

        return "otpauth://totp/"
                + label
                + "?secret="
                + user.getMfaSecret()
                + "&issuer="
                + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private List<String> generateRecoveryCodes(int requestedCount) {
        int count = Math.max(5, Math.min(20, requestedCount));
        List<String> codes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
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

    @Transactional(readOnly = true)
    public MfaPolicy getEffectivePolicy(User user) {
        if (mfaPolicyRepository == null || user == null || user.getId() == null) {
            return legacyPolicy(user);
        }

        return findUserPolicy(user)
                .orElseGet(() -> findProfilePolicy(user)
                        .orElseGet(() -> findCompanyPolicy(user)
                                .orElseGet(() -> mfaPolicyRepository
                                        .findByScopeTypeAndActiveTrue(MfaPolicy.ScopeType.GLOBAL)
                                        .orElseGet(() -> legacyPolicy(user)))));
    }

    @Transactional(readOnly = true)
    public boolean isMfaRequired(User user) {
        return getEffectivePolicy(user).isRequired()
                || (user != null && userSecurityProfileService != null
                && userSecurityProfileService.getEffectiveProfile(user).isRequireMfa());
    }

    @Transactional(readOnly = true)
    public boolean isRecoveryCodeAllowed(User user) {
        boolean policyAllowed = getEffectivePolicy(user).isAllowRecoveryCodes();
        boolean legacyAllowed = user == null || userSecurityProfileService == null
                || userSecurityProfileService.getEffectiveProfile(user).isAllowRecoveryCode();
        return policyAllowed && legacyAllowed;
    }

    @Transactional(readOnly = true)
    public MfaIndicators getIndicators(User actor) {
        authorizedAdminActor(actor);
        List<User> users = userRepository.findAll();
        long total = users.size();
        long active = users.stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count();
        long enabled = users.stream().filter(User::isMfaEnabled).count();
        long required = users.stream().filter(this::isMfaRequired).count();
        long requiredPending = users.stream()
                .filter(this::isMfaRequired)
                .filter(u -> !u.isMfaEnabled())
                .count();
        long lowRecovery = users.stream()
                .filter(User::isMfaEnabled)
                .filter(u -> countRecoveryCodes(u) <= 2)
                .count();

        return new MfaIndicators(
                total,
                active,
                enabled,
                required,
                requiredPending,
                lowRecovery
        );
    }

    @Transactional
    public String adminPrepareActivation(
            User actor,
            Long targetUserId,
            String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);
        if (target.isMfaEnabled()) {
            throw new IllegalStateException("O MFA já está activo nesta conta.");
        }

        String secret = prepareActivation(target);
        if (target.getId() != null) {
            userRepository.save(target);
        }

        acessoService.registrarAuditoria(
                actor,
                "CONFIG_CHANGE",
                "UTILIZADORES",
                sourceIp,
                "Preparação de activação MFA para " + target.getEmail(),
                true
        );
        return secret;
    }

    @Transactional
    public void adminForceMfa(
            User actor,
            Long targetUserId,
            String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);

        MfaPolicy policy = getOrCreateUserPolicy(target);
        boolean previous = policy.isRequired();
        policy.setRequired(true);
        policy.setAllowUserDisable(false);
        mfaPolicyRepository.save(policy);

        acessoService.registrarAuditoria(
                actor,
                "CONFIG_CHANGE",
                "MFA_POLICY",
                sourceIp,
                "MFA obrigatório definido para " + target.getEmail()
                        + " (anterior=" + previous + ")",
                true
        );
    }

    @Transactional
    public void adminAllowMfaDisable(
            User actor,
            Long targetUserId,
            String sourceIp) {
        User target = authorizedAdminTarget(actor, targetUserId);
        MfaPolicy policy = getOrCreateUserPolicy(target);
        policy.setAllowUserDisable(true);
        mfaPolicyRepository.save(policy);

        acessoService.registrarAuditoria(
                actor,
                "CONFIG_CHANGE",
                "MFA_POLICY",
                sourceIp,
                "Desactivação voluntária de MFA permitida para " + target.getEmail(),
                true
        );
    }

    @Transactional
    public MfaPolicy savePolicy(
            User actor,
            MfaPolicy requested,
            String sourceIp) {
        User managedActor = authorizedAdminActor(actor);

        if (requested == null || requested.getScopeType() == null) {
            throw new IllegalArgumentException("Política MFA inválida.");
        }

        validatePolicyScope(requested);

        MfaPolicy policy = requested.getId() == null
                ? new MfaPolicy()
                : mfaPolicyRepository.findById(requested.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Política MFA não encontrada."
                        ));

        Map<String, Object> before = requested.getId() == null
                ? null
                : policySnapshot(policy);

        policy.setScopeType(requested.getScopeType());
        policy.setScopeId(requested.getScopeId());
        policy.setScopeKey(scopeKey(requested.getScopeType(), requested.getScopeId()));
        policy.setNome(normalize(requested.getNome(), "Política MFA"));
        policy.setRequired(requested.isRequired());
        policy.setAllowUserDisable(requested.isAllowUserDisable());
        policy.setAllowRecoveryCodes(requested.isAllowRecoveryCodes());
        policy.setRecoveryCodeCount(requested.getRecoveryCodeCount());
        policy.setIssuer(normalize(requested.getIssuer(), "Kubata"));
        policy.setGracePeriodDays(requested.getGracePeriodDays());
        policy.setActive(requested.getActive() == null || requested.getActive());

        validatePolicy(policy);

        mfaPolicyRepository.findByScopeKeyAndActiveTrue(policy.getScopeKey())
                .filter(existing -> !existing.getId().equals(policy.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Já existe uma política MFA activa para este âmbito."
                    );
                });

        MfaPolicy saved = mfaPolicyRepository.save(policy);

        acessoService.registrarAuditoria(
                managedActor,
                "CONFIG_CHANGE",
                "MFA_POLICY",
                sourceIp,
                "Política MFA guardada: " + saved.getScopeKey(),
                true
        );
        return saved;
    }

    @Transactional
    public void deletePolicy(User actor, Long policyId, String sourceIp) {
        User managedActor = authorizedAdminActor(actor);

        if (policyId == null) {
            throw new IllegalArgumentException("Política MFA inválida.");
        }

        MfaPolicy policy = mfaPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Política MFA não encontrada."));

        mfaPolicyRepository.delete(policy);

        acessoService.registrarAuditoria(
                managedActor,
                "CONFIG_CHANGE",
                "MFA_POLICY",
                sourceIp,
                "Política MFA removida: " + policy.getScopeKey(),
                true
        );
    }

    private Optional<MfaPolicy> findUserPolicy(User user) {
        return mfaPolicyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                MfaPolicy.ScopeType.UTILIZADOR,
                user.getId()
        );
    }

    private Optional<MfaPolicy> findCompanyPolicy(User user) {
        return user.getEmpresa() == null || user.getEmpresa().getId() == null
                ? Optional.empty()
                : mfaPolicyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                        MfaPolicy.ScopeType.EMPRESA,
                        user.getEmpresa().getId()
                );
    }

    private Optional<MfaPolicy> findProfilePolicy(User user) {
        if (user.getPerfis() == null || user.getPerfis().isEmpty()) {
            return Optional.empty();
        }

        Set<Long> ids = user.getPerfis().stream()
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()))
                .map(PerfilAcesso::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (ids.isEmpty()) {
            return Optional.empty();
        }

        return mfaPolicyRepository
                .findAllByScopeTypeAndScopeIdInAndActiveTrue(
                        MfaPolicy.ScopeType.PERFIL,
                        ids
                )
                .stream()
                .reduce(this::mergePolicies);
    }

    private MfaPolicy mergePolicies(MfaPolicy left, MfaPolicy right) {
        MfaPolicy merged = new MfaPolicy();
        merged.setScopeType(MfaPolicy.ScopeType.PERFIL);
        merged.setScopeKey("PROFILE-MERGED");
        merged.setNome("Política MFA de perfis combinada");
        merged.setRequired(left.isRequired() || right.isRequired());
        merged.setAllowUserDisable(left.isAllowUserDisable() && right.isAllowUserDisable());
        merged.setAllowRecoveryCodes(left.isAllowRecoveryCodes() && right.isAllowRecoveryCodes());
        merged.setRecoveryCodeCount(Math.max(left.getRecoveryCodeCount(), right.getRecoveryCodeCount()));
        merged.setIssuer(left.getIssuer());
        merged.setGracePeriodDays(Math.max(left.getGracePeriodDays(), right.getGracePeriodDays()));
        merged.setActive(true);
        return merged;
    }

    private MfaPolicy getOrCreateUserPolicy(User target) {
        if (mfaPolicyRepository == null) {
            throw new IllegalStateException("O centro de políticas MFA não está disponível.");
        }

        return mfaPolicyRepository
                .findByScopeTypeAndScopeIdAndActiveTrue(
                        MfaPolicy.ScopeType.UTILIZADOR,
                        target.getId()
                )
                .orElseGet(() -> {
                    MfaPolicy policy = legacyPolicy(target);
                    policy.setScopeType(MfaPolicy.ScopeType.UTILIZADOR);
                    policy.setScopeId(target.getId());
                    policy.setScopeKey(scopeKey(MfaPolicy.ScopeType.UTILIZADOR, target.getId()));
                    return policy;
                });
    }

    private User authorizedAdminActor(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }

        User managed = userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException(
                        "Administrador da sessão não encontrado."
                ));

        if (!Boolean.TRUE.equals(managed.getActive())) {
            throw new SecurityException("A conta administrativa está inactiva.");
        }

        if (managed.isSuperadmin() || managed.getRole() == Role.ADMIN) {
            return managed;
        }

        if (!securityService.hasPermission(
                managed,
                "ADMINISTRATOR",
                "MFA_CENTER",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR)) {
            throw new SecurityException(
                    "Não possui permissão para gerir o MFA."
            );
        }

        return managed;
    }

    private void validatePolicyScope(MfaPolicy policy) {
        if (policy.getScopeType() == MfaPolicy.ScopeType.GLOBAL) {
            policy.setScopeId(null);
            return;
        }

        if (policy.getScopeId() == null) {
            throw new IllegalArgumentException("O identificador do âmbito MFA é obrigatório.");
        }

        switch (policy.getScopeType()) {
            case EMPRESA -> {
                if (empresaRepository == null
                        || !empresaRepository.existsById(policy.getScopeId())) {
                    throw new IllegalArgumentException("A empresa do âmbito MFA não existe.");
                }
            }
            case PERFIL, UTILIZADOR -> {
                if (policy.getScopeType() == MfaPolicy.ScopeType.PERFIL
                        && (perfilAcessoRepository == null
                        || !perfilAcessoRepository.existsById(policy.getScopeId()))) {
                    throw new IllegalArgumentException("O perfil do âmbito MFA não existe.");
                }
                if (policy.getScopeType() == MfaPolicy.ScopeType.UTILIZADOR
                        && !userRepository.existsById(policy.getScopeId())) {
                    throw new IllegalArgumentException("O utilizador do âmbito MFA não existe.");
                }
            }
            default -> {
            }
        }
    }

    private void validatePolicy(MfaPolicy policy) {
        if (policy.getRecoveryCodeCount() < 0 || policy.getRecoveryCodeCount() > 20) {
            throw new IllegalArgumentException("A quantidade de códigos de recuperação deve estar entre 0 e 20.");
        }
        if (policy.getRecoveryCodeCount() > 0 && policy.getRecoveryCodeCount() < 5) {
            throw new IllegalArgumentException("Use pelo menos 5 códigos de recuperação.");
        }
        if (policy.getGracePeriodDays() < 0 || policy.getGracePeriodDays() > 365) {
            throw new IllegalArgumentException("O período de tolerância deve estar entre 0 e 365 dias.");
        }
        if (policy.getIssuer() == null || policy.getIssuer().isBlank()
                || policy.getIssuer().length() > 80) {
            throw new IllegalArgumentException("O emissor MFA é obrigatório.");
        }
    }

    private MfaPolicy legacyPolicy(User user) {
        MfaPolicy policy = new MfaPolicy();
        policy.setScopeType(MfaPolicy.ScopeType.GLOBAL);
        policy.setScopeKey("LEGACY_DEFAULT");
        policy.setNome("Política MFA compatível");
        policy.setRequired(false);
        policy.setAllowUserDisable(true);
        policy.setAllowRecoveryCodes(true);
        policy.setRecoveryCodeCount(RECOVERY_CODE_COUNT);
        policy.setIssuer("Kubata");
        policy.setGracePeriodDays(0);

        return policy;
    }

    private String scopeKey(MfaPolicy.ScopeType type, Long id) {
        return type.name() + (id == null ? "" : ":" + id);
    }

    private Map<String, Object> policySnapshot(MfaPolicy policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scope", policy.getScopeKey());
        values.put("required", policy.isRequired());
        values.put("allowUserDisable", policy.isAllowUserDisable());
        values.put("allowRecoveryCodes", policy.isAllowRecoveryCodes());
        values.put("recoveryCodeCount", policy.getRecoveryCodeCount());
        values.put("issuer", policy.getIssuer());
        values.put("gracePeriodDays", policy.getGracePeriodDays());
        return values;
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record MfaIndicators(
            long totalUsers,
            long activeUsers,
            long mfaEnabledUsers,
            long requiredUsers,
            long requiredPendingUsers,
            long lowRecoveryUsers
    ) {
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
