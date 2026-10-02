package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PasswordHistory;
import ao.allon.kubata.core.domain.PasswordPolicy;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PasswordHistoryRepository;
import ao.allon.kubata.core.repository.PasswordPolicyRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSecurityProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PasswordPolicyService {

    public static final String MODULE = "ADMINISTRATOR";
    public static final String RESOURCE = "PASSWORD_POLICY";

    private final PasswordPolicyRepository policyRepository;
    private final PasswordHistoryRepository historyRepository;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final AuditService auditService;

    public PasswordPolicyService(
            PasswordPolicyRepository policyRepository,
            PasswordHistoryRepository historyRepository,
            UserSecurityProfileRepository userSecurityProfileRepository,
            UserRepository userRepository,
            EmpresaRepository empresaRepository,
            PerfilAcessoRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            SecurityService securityService,
            AuditService auditService) {
        this.policyRepository = policyRepository;
        this.historyRepository = historyRepository;
        this.userSecurityProfileRepository = userSecurityProfileRepository;
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PasswordPolicy getEffectivePolicy(User user) {
        PasswordPolicy explicit = findUserPolicy(user)
                .orElseGet(() -> findProfilePolicy(user)
                        .orElseGet(() -> findCompanyPolicy(user)
                                .orElseGet(this::findGlobalPolicyOrLegacy)));

        validatePolicyValues(explicit);
        return explicit;
    }

    @Transactional(readOnly = true)
    public boolean isPasswordExpired(User user, LocalDateTime now) {
        if (user == null) {
            return true;
        }

        LocalDateTime current = now == null ? LocalDateTime.now() : now;

        if (user.getDataExpiracaoPassword() != null
                && user.getDataExpiracaoPassword().isBefore(current.toLocalDate())) {
            return true;
        }

        if (user.getPasswordChangedAt() == null) {
            return false;
        }

        int expiryDays = getEffectivePolicy(user).getExpiryDays();
        return expiryDays > 0
                && current.isAfter(user.getPasswordChangedAt().plusDays(expiryDays));
    }

    @Transactional(readOnly = true)
    public void validateNewPassword(User user, String rawPassword) {
        PasswordPolicy policy = getEffectivePolicy(user);

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A palavra-passe é obrigatória.");
        }

        if (rawPassword.length() < policy.getMinLength()
                || rawPassword.length() > policy.getMaxLength()) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve ter entre "
                            + policy.getMinLength()
                            + " e "
                            + policy.getMaxLength()
                            + " caracteres."
            );
        }

        if (policy.isRequireUpper()
                && rawPassword.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos uma letra maiúscula."
            );
        }

        if (policy.isRequireLower()
                && rawPassword.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos uma letra minúscula."
            );
        }

        if (policy.isRequireDigit()
                && rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos um número."
            );
        }

        if (policy.isRequireSymbol()
                && rawPassword.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos um símbolo."
            );
        }

        if (policy.isProhibitIdentityFragments() && containsIdentityFragment(user, rawPassword)) {
            throw new IllegalArgumentException(
                    "A palavra-passe não deve conter fragmentos identificáveis da conta."
            );
        }

        if (user != null && user.getPassword() != null
                && passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve ser diferente da palavra-passe actual."
            );
        }

        int historyCount = policy.getHistoryCount();
        if (historyCount > 0 && user != null && user.getId() != null) {
            List<PasswordHistory> history =
                    historyRepository.findTop100ByUserIdOrderByChangedAtDesc(user.getId());

            history.stream()
                    .limit(historyCount)
                    .filter(entry -> entry.getPasswordHash() != null)
                    .filter(entry -> passwordEncoder.matches(rawPassword, entry.getPasswordHash()))
                    .findFirst()
                    .ifPresent(entry -> {
                        throw new IllegalArgumentException(
                                "A palavra-passe foi utilizada recentemente e não pode ser reutilizada."
                        );
                    });
        }
    }

    @Transactional(readOnly = true)
    public void validateMinimumPasswordAge(User user, LocalDateTime now) {
        if (user == null || user.getPasswordChangedAt() == null) {
            return;
        }

        int minimumAgeHours = getEffectivePolicy(user).getMinimumAgeHours();
        if (minimumAgeHours <= 0) {
            return;
        }

        LocalDateTime current = now == null ? LocalDateTime.now() : now;
        if (current.isBefore(user.getPasswordChangedAt().plusHours(minimumAgeHours))) {
            throw new IllegalArgumentException(
                    "A palavra-passe não pode ser alterada novamente antes de completar "
                            + minimumAgeHours + " hora(s)."
            );
        }
    }

    @Transactional
    public PasswordHistory recordPreviousPassword(
            User user,
            String previousPasswordHash,
            String changedBy,
            String reason) {

        if (user == null || user.getId() == null
                || previousPasswordHash == null || previousPasswordHash.isBlank()) {
            return null;
        }

        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(previousPasswordHash);
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy(truncate(changedBy, 120));
        history.setReason(truncate(reason, 60));

        return historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<PasswordPolicy> listPolicies() {
        return policyRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(PasswordPolicy::getScopeType,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PasswordPolicy::getScopeKey,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Transactional
    public PasswordPolicy savePolicy(
            User actor,
            PasswordPolicy requested,
            String sourceIp) {

        User managedActor = requirePolicyAdministrator(actor);
        validatePolicyScope(requested);

        PasswordPolicy target = requested.getId() == null
                ? new PasswordPolicy()
                : policyRepository.findById(requested.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Política de palavra-passe não encontrada."
                        ));

        PasswordPolicy before = requested.getId() == null ? null : target;
        target.setScopeType(requested.getScopeType());
        target.setScopeId(requested.getScopeId());
        target.setScopeKey(scopeKey(requested.getScopeType(), requested.getScopeId()));
        target.setNome(normalizeName(requested.getNome()));
        target.setMinLength(requested.getMinLength());
        target.setMaxLength(requested.getMaxLength());
        target.setRequireUpper(requested.isRequireUpper());
        target.setRequireLower(requested.isRequireLower());
        target.setRequireDigit(requested.isRequireDigit());
        target.setRequireSymbol(requested.isRequireSymbol());
        target.setHistoryCount(requested.getHistoryCount());
        target.setExpiryDays(requested.getExpiryDays());
        target.setMinimumAgeHours(requested.getMinimumAgeHours());
        target.setResetValidityHours(requested.getResetValidityHours());
        target.setForceChangeOnReset(requested.isForceChangeOnReset());
        target.setProhibitIdentityFragments(requested.isProhibitIdentityFragments());
        target.setActive(requested.getActive() == null || requested.getActive());

        validatePolicyValues(target);

        if (target.getScopeKey() != null) {
            policyRepository.findByScopeKeyAndActiveTrue(target.getScopeKey())
                    .filter(existing -> !existing.getId().equals(target.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "Já existe uma política activa para o âmbito: " + target.getScopeKey()
                        );
                    });
        }

        PasswordPolicy saved = policyRepository.save(target);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.CONFIG_CHANGE,
                RESOURCE,
                String.valueOf(saved.getId()),
                "Política de palavra-passe guardada para " + saved.getScopeKey(),
                before == null ? null : snapshot(before),
                snapshot(saved),
                MODULE,
                normalizeIp(sourceIp),
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.HIGH
        );

        return saved;
    }

    @Transactional
    public void deletePolicy(User actor, Long id, String sourceIp) {
        User managedActor = requirePolicyAdministrator(actor);
        PasswordPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Política de palavra-passe não encontrada."
                ));

        policyRepository.delete(policy);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.CONFIG_CHANGE,
                RESOURCE,
                String.valueOf(policy.getId()),
                "Política de palavra-passe removida: " + policy.getScopeKey(),
                snapshot(policy),
                null,
                MODULE,
                normalizeIp(sourceIp),
                null,
                null,
                false,
                AuditLog.AGTComplianceLevel.HIGH
        );
    }

    private Optional<PasswordPolicy> findUserPolicy(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return policyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                PasswordPolicy.ScopeType.UTILIZADOR,
                user.getId()
        );
    }

    private Optional<PasswordPolicy> findProfilePolicy(User user) {
        if (user == null || user.getPerfis() == null || user.getPerfis().isEmpty()) {
            return Optional.empty();
        }

        List<Long> ids = user.getPerfis().stream()
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()) && p.getId() != null)
                .map(PerfilAcesso::getId)
                .toList();

        if (ids.isEmpty()) {
            return Optional.empty();
        }

        List<PasswordPolicy> policies =
                policyRepository.findAllByScopeTypeAndScopeIdInAndActiveTrue(
                        PasswordPolicy.ScopeType.PERFIL,
                        ids
                );

        return policies.stream()
                .reduce((left, right) -> mergeStrongest(left, right));
    }

    private Optional<PasswordPolicy> findCompanyPolicy(User user) {
        if (user == null || user.getEmpresa() == null || user.getEmpresa().getId() == null) {
            return Optional.empty();
        }

        return policyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                PasswordPolicy.ScopeType.EMPRESA,
                user.getEmpresa().getId()
        );
    }

    private PasswordPolicy findGlobalPolicyOrLegacy() {
        return policyRepository.findAllByScopeTypeAndActiveTrue(PasswordPolicy.ScopeType.GLOBAL)
                .stream()
                .findFirst()
                .orElseGet(this::legacyDefaultPolicy);
    }

    private PasswordPolicy legacyDefaultPolicy() {
        PasswordPolicy fallback = new PasswordPolicy();

        Optional<UserSecurityProfile> legacy = userSecurityProfileRepository.findAll().stream()
                .findFirst();

        if (legacy.isPresent()) {
            UserSecurityProfile profile = legacy.get();
            fallback.setMinLength(profile.getPasswordMinLength());
            fallback.setMaxLength(Math.max(128, profile.getPasswordMinLength()));
            fallback.setRequireUpper(profile.isPasswordRequireUpper());
            fallback.setRequireLower(profile.isPasswordRequireLower());
            fallback.setRequireDigit(profile.isPasswordRequireDigit());
            fallback.setRequireSymbol(profile.isPasswordRequireSymbol());
            fallback.setExpiryDays(profile.getPasswordExpiryDays());
        }

        fallback.setScopeType(PasswordPolicy.ScopeType.GLOBAL);
        fallback.setScopeKey("LEGACY_DEFAULT");
        fallback.setNome("Política compatível com perfil de segurança");
        fallback.setHistoryCount(5);
        fallback.setMinimumAgeHours(0);
        fallback.setResetValidityHours(24);
        fallback.setForceChangeOnReset(true);
        fallback.setProhibitIdentityFragments(true);
        return fallback;
    }

    private PasswordPolicy mergeStrongest(PasswordPolicy left, PasswordPolicy right) {
        PasswordPolicy merged = new PasswordPolicy();
        merged.setScopeType(PasswordPolicy.ScopeType.PERFIL);
        merged.setScopeId(null);
        merged.setScopeKey("PROFILE-MERGED");
        merged.setNome("Política de perfis combinada");

        merged.setMinLength(Math.max(left.getMinLength(), right.getMinLength()));
        merged.setMaxLength(Math.min(left.getMaxLength(), right.getMaxLength()));
        if (merged.getMaxLength() < merged.getMinLength()) {
            merged.setMaxLength(merged.getMinLength());
        }

        merged.setRequireUpper(left.isRequireUpper() || right.isRequireUpper());
        merged.setRequireLower(left.isRequireLower() || right.isRequireLower());
        merged.setRequireDigit(left.isRequireDigit() || right.isRequireDigit());
        merged.setRequireSymbol(left.isRequireSymbol() || right.isRequireSymbol());
        merged.setHistoryCount(Math.max(left.getHistoryCount(), right.getHistoryCount()));
        merged.setExpiryDays(stricterPositive(left.getExpiryDays(), right.getExpiryDays()));
        merged.setMinimumAgeHours(Math.max(left.getMinimumAgeHours(), right.getMinimumAgeHours()));
        merged.setResetValidityHours(stricterPositive(
                left.getResetValidityHours(), right.getResetValidityHours()
        ));
        merged.setForceChangeOnReset(left.isForceChangeOnReset() || right.isForceChangeOnReset());
        merged.setProhibitIdentityFragments(
                left.isProhibitIdentityFragments() || right.isProhibitIdentityFragments()
        );
        merged.setActive(true);
        return merged;
    }

    private int stricterPositive(int left, int right) {
        if (left <= 0) return right;
        if (right <= 0) return left;
        return Math.min(left, right);
    }

    private void validatePolicyScope(PasswordPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Política de palavra-passe inválida.");
        }

        PasswordPolicy.ScopeType type = policy.getScopeType();
        if (type == null) {
            throw new IllegalArgumentException("O âmbito da política é obrigatório.");
        }

        if (type == PasswordPolicy.ScopeType.GLOBAL) {
            policy.setScopeId(null);
        } else if (policy.getScopeId() == null) {
            throw new IllegalArgumentException(
                    "O identificador do âmbito é obrigatório para " + type + "."
            );
        }

        switch (type) {
            case GLOBAL -> {
            }
            case EMPRESA -> {
                if (!empresaRepository.existsById(policy.getScopeId())) {
                    throw new IllegalArgumentException("A empresa do âmbito não existe.");
                }
            }
            case PERFIL -> {
                if (!perfilRepository.existsById(policy.getScopeId())) {
                    throw new IllegalArgumentException("O perfil do âmbito não existe.");
                }
            }
            case UTILIZADOR -> {
                if (!userRepository.existsById(policy.getScopeId())) {
                    throw new IllegalArgumentException("O utilizador do âmbito não existe.");
                }
            }
        }
    }

    private User requirePolicyAdministrator(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new SecurityException("Sessão administrativa inválida.");
        }

        User managed = userRepository.findById(actor.getId())
                .orElseThrow(() -> new SecurityException("Administrador da sessão não encontrado."));

        if (!Boolean.TRUE.equals(managed.getActive())) {
            throw new SecurityException("A conta administrativa está inactiva.");
        }

        if (managed.isSuperadmin() || managed.getRole() == Role.ADMIN) {
            return managed;
        }

        boolean allowed = securityService.hasPermission(
                managed,
                MODULE,
                RESOURCE,
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR
        );

        if (!allowed) {
            throw new SecurityException(
                    "Não possui permissão para gerir a política de palavras-passe."
            );
        }

        return managed;
    }

    private String scopeKey(PasswordPolicy.ScopeType type, Long id) {
        return type.name() + (id == null ? "" : ":" + id);
    }

    private void validatePolicyValues(PasswordPolicy policy) {
        if (policy.getMinLength() < 8 || policy.getMinLength() > 256) {
            throw new IllegalArgumentException("O comprimento mínimo deve estar entre 8 e 256.");
        }
        if (policy.getMaxLength() < policy.getMinLength() || policy.getMaxLength() > 256) {
            throw new IllegalArgumentException(
                    "O comprimento máximo deve estar entre o mínimo definido e 256."
            );
        }
        if (policy.getHistoryCount() < 0 || policy.getHistoryCount() > 50) {
            throw new IllegalArgumentException("O histórico deve estar entre 0 e 50 palavras-passe.");
        }
        if (policy.getExpiryDays() < 0 || policy.getExpiryDays() > 3650) {
            throw new IllegalArgumentException("A validade deve estar entre 0 e 3650 dias.");
        }
        if (policy.getMinimumAgeHours() < 0 || policy.getMinimumAgeHours() > 8760) {
            throw new IllegalArgumentException("A idade mínima deve estar entre 0 e 8760 horas.");
        }
        if (policy.getResetValidityHours() < 1 || policy.getResetValidityHours() > 720) {
            throw new IllegalArgumentException("A validade do reset deve estar entre 1 e 720 horas.");
        }
        if (policy.getNome() == null || policy.getNome().isBlank()
                || policy.getNome().trim().length() > 120) {
            throw new IllegalArgumentException("O nome da política é obrigatório e tem no máximo 120 caracteres.");
        }
    }

    private boolean containsIdentityFragment(User user, String rawPassword) {
        String password = rawPassword.toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();

        if (user != null) {
            addIdentityCandidates(candidates, user.getNome());
            addIdentityCandidates(candidates, user.getEmail());
            addIdentityCandidates(candidates, user.getCodigo());
        }

        return candidates.stream().anyMatch(password::contains);
    }

    private void addIdentityCandidates(List<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (normalized.length() >= 4) {
            target.add(normalized.replace(" ", ""));
        }

        for (String token : normalized.split("\s+")) {
            if (token.length() >= 4) {
                target.add(token);
            }
        }
    }

    private LinkedHashMap<String, Object> snapshot(PasswordPolicy policy) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("scope", policy.getScopeKey());
        map.put("nome", policy.getNome());
        map.put("minLength", policy.getMinLength());
        map.put("maxLength", policy.getMaxLength());
        map.put("requireUpper", policy.isRequireUpper());
        map.put("requireLower", policy.isRequireLower());
        map.put("requireDigit", policy.isRequireDigit());
        map.put("requireSymbol", policy.isRequireSymbol());
        map.put("historyCount", policy.getHistoryCount());
        map.put("expiryDays", policy.getExpiryDays());
        map.put("minimumAgeHours", policy.getMinimumAgeHours());
        map.put("resetValidityHours", policy.getResetValidityHours());
        map.put("forceChangeOnReset", policy.isForceChangeOnReset());
        map.put("prohibitIdentityFragments", policy.isProhibitIdentityFragments());
        return map;
    }

    private String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "Política de palavra-passe" : normalized;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String normalizeIp(String value) {
        return value == null || value.isBlank() ? "127.0.0.1" : value.trim();
    }
}
