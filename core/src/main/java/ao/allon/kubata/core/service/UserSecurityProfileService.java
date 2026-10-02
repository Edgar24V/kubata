package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityFinancialUsage;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.UserAccessPermissionRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSecurityFinancialUsageRepository;
import ao.allon.kubata.core.repository.UserSecurityProfileRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ao.allon.kubata.core.exception.AuthenticationException;

@Service
public class UserSecurityProfileService {

    public static final String MODULE = "ADMINISTRATOR";
    public static final String RESOURCE = "PERFIL_SEGURANCA";

    private final UserSecurityProfileRepository profileRepository;
    private final UserSecurityFinancialUsageRepository financialUsageRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserAccessPermissionRepository userAccessPermissionRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditService auditService;

    public UserSecurityProfileService(
            UserSecurityProfileRepository profileRepository,
            UserSecurityFinancialUsageRepository financialUsageRepository,
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            UserAccessPermissionRepository userAccessPermissionRepository,
            EmpresaRepository empresaRepository,
            AuditService auditService) {
        this.profileRepository = profileRepository;
        this.financialUsageRepository = financialUsageRepository;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.userAccessPermissionRepository = userAccessPermissionRepository;
        this.empresaRepository = empresaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserSecurityProfile getEffectiveProfile(User user) {
        if (user == null || user.getId() == null) {
            return defaultProfile(null);
        }
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> defaultProfile(user));
    }

    @Transactional
    public UserSecurityProfile getOrCreateProfile(User actor, Long targetUserId) {
        User target = managedProfileTarget(actor, targetUserId);
        return profileRepository.findByUserId(target.getId())
                .orElseGet(() -> {
                    UserSecurityProfile profile = defaultProfile(target);
                    return profileRepository.save(profile);
                });
    }

    @Transactional(readOnly = true)
    public UserSecurityProfile loadForAdministration(User actor, Long targetUserId) {
        User target = managedTarget(actor, targetUserId);
        return profileRepository.findByUserId(target.getId())
                .orElseGet(() -> defaultProfile(target));
    }

    @Transactional
    public UserSecurityProfile saveProfile(
            User actor,
            Long targetUserId,
            UserSecurityProfile requested,
            String sourceIp) {

        User managedActor = managedActor(actor);
        User target = managedTarget(managedActor, targetUserId);

        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException(
                    "Só um Superadministrador pode alterar o perfil de segurança de outro Superadministrador."
            );
        }
        if (requested == null) {
            throw new IllegalArgumentException("Perfil de segurança inválido.");
        }
        if (managedActor.getId().equals(target.getId()) && !requested.isLoginEnabled()) {
            throw new SecurityException("A própria conta não pode ser desactivada pela sua política de login.");
        }
        if (requested.isRequireMfa() && !target.isMfaEnabled()) {
            throw new IllegalArgumentException(
                    "Não é possível exigir MFA antes de o segundo factor estar activo na conta."
            );
        }

        validateProfile(requested);

        UserSecurityProfile previous = profileRepository.findByUserId(target.getId())
                .orElseGet(() -> defaultProfile(target));

        UserSecurityProfile profile = profileRepository.findByUserId(target.getId())
                .orElseGet(UserSecurityProfile::new);
        profile.setUser(target);
        copyValues(requested, profile);

        UserSecurityProfile saved = profileRepository.save(profile);

        auditService.logAction(
                managedActor,
                null,
                AuditLog.AuditActionType.CONFIG_CHANGE,
                RESOURCE,
                String.valueOf(target.getId()),
                "Actualizado perfil de segurança de " + target.getEmail(),
                snapshot(previous),
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
    public void validateLoginPolicy(
            User user,
            String sourceIp,
            LocalDateTime now,
            boolean mfaSatisfied) {

        if (user == null || user.getId() == null) {
            throw new AuthenticationException("Conta inválida.");
        }

        UserSecurityProfile profile = getEffectiveProfile(user);

        if (!profile.isLoginEnabled()) {
            throw new AuthenticationException("O login desta conta está bloqueado pela política de segurança.");
        }

        if (!isIpAllowed(profile, sourceIp)) {
            throw new AuthenticationException("O endereço IP não está autorizado para esta conta.");
        }

        if (!isScheduleAllowed(profile, now)) {
            throw new AuthenticationException("O login não está autorizado neste horário para esta conta.");
        }

        if (!isWeekDayAllowed(profile, now.getDayOfWeek())) {
            throw new AuthenticationException("O login não está autorizado neste dia para esta conta.");
        }

        if (profile.isRequireMfa() && !mfaSatisfied) {
            throw new AuthenticationException("Esta conta exige autenticação multifactor.");
        }
    }

    @Transactional
    public void enforceConcurrentSessionLimit(User user, LocalDateTime now) {
        UserSecurityProfile profile = getEffectiveProfile(user);
        int timeoutMinutes = profile.getSessionTimeoutMinutes();

        List<UserSession> sessions =
                userSessionRepository.findAllByUsernameOrderByLoginTimeDesc(user.getNome());

        if (timeoutMinutes > 0) {
            LocalDateTime expiry = now.minusMinutes(timeoutMinutes);
            sessions.stream()
                    .filter(s -> s.getLoginTime() != null && s.getLoginTime().isBefore(expiry))
                    .forEach(userSessionRepository::delete);
            sessions = sessions.stream()
                    .filter(s -> s.getLoginTime() == null || !s.getLoginTime().isBefore(expiry))
                    .toList();
        }

        int maxSessions = profile.getMaxConcurrentSessions();
        if (maxSessions > 0 && sessions.size() >= maxSessions) {
            throw new AuthenticationException(
                    "O limite de sessões simultâneas desta conta foi atingido."
            );
        }
    }

    public boolean isRecoveryCodeAllowed(User user) {
        return getEffectiveProfile(user).isAllowRecoveryCode();
    }

    public int maxLoginAttempts(User user) {
        return getEffectiveProfile(user).getMaxLoginAttempts();
    }

    public int lockoutMinutes(User user) {
        return getEffectiveProfile(user).getLockoutMinutes();
    }

    public int passwordExpiryDays(User user) {
        return getEffectiveProfile(user).getPasswordExpiryDays();
    }

    public boolean isModuleAllowed(User user, String modulo) {
        if (user == null || modulo == null || modulo.isBlank()) {
            return false;
        }

        Set<String> allowed = getEffectiveProfile(user).getAllowedModules();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }

        return allowed.stream().anyMatch(m -> m.equalsIgnoreCase(modulo.trim()));
    }

    public boolean isCompanyAllowed(User user, Long empresaId) {
        if (user == null || empresaId == null) {
            return false;
        }

        Set<Long> allowed = getEffectiveProfile(user).getAllowedCompanyIds();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }

        return allowed.contains(empresaId);
    }

    public boolean isCriticalOperationAllowed(User user, String operationCode) {
        if (user == null || operationCode == null || operationCode.isBlank()) {
            return false;
        }

        Set<String> allowed = getEffectiveProfile(user).getAllowedCriticalOperations();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }

        return allowed.stream()
                .anyMatch(value -> value.equalsIgnoreCase(operationCode.trim()));
    }

    @Transactional
    public void requireFinancialAuthorization(
            User user,
            String operationCode,
            BigDecimal amount,
            String currency,
            Long empresaId,
            String modulo,
            String sourceIp) {

        User managedUser = user == null || user.getId() == null
                ? null
                : userRepository.findById(user.getId()).orElse(null);

        try {
            if (managedUser == null || !Boolean.TRUE.equals(managedUser.getActive())) {
                throw new SecurityException("Conta inválida ou inactiva.");
            }

            if (modulo != null && !modulo.isBlank()
                    && !isModuleAllowed(managedUser, modulo)) {
                throw new SecurityException("O módulo não está autorizado para esta conta.");
            }

            if (empresaId != null && !isCompanyAllowed(managedUser, empresaId)) {
                throw new SecurityException("A empresa não está autorizada para esta conta.");
            }

            if (operationCode != null && !operationCode.isBlank()
                    && !isCriticalOperationAllowed(managedUser, operationCode)) {
                throw new SecurityException("A operação crítica não está autorizada para esta conta.");
            }

            UserSecurityProfile profile = getEffectiveProfile(managedUser);
            BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
            if (normalizedAmount.signum() < 0) {
                throw new IllegalArgumentException("O valor financeiro não pode ser negativo.");
            }

            String effectiveCurrency = normalizeCurrency(
                    currency == null || currency.isBlank()
                            ? profile.getFinancialCurrency()
                            : currency
            );

            if (profile.getFinancialOperationLimit() != null
                    && normalizedAmount.compareTo(profile.getFinancialOperationLimit()) > 0) {
                throw new SecurityException(
                        "O valor da operação excede o limite financeiro individual."
                );
            }

            if (profile.getFinancialDailyLimit() != null) {
                LocalDate today = LocalDate.now();
                UserSecurityFinancialUsage usage =
                        financialUsageRepository.findByUserIdAndUsageDateAndCurrency(
                                managedUser.getId(), today, effectiveCurrency
                        ).orElseGet(() -> {
                            UserSecurityFinancialUsage created = new UserSecurityFinancialUsage();
                            created.setUser(managedUser);
                            created.setUsageDate(today);
                            created.setCurrency(effectiveCurrency);
                            created.setAmount(BigDecimal.ZERO);
                            return created;
                        });

                BigDecimal current = usage.getAmount() == null ? BigDecimal.ZERO : usage.getAmount();
                BigDecimal projected = current.add(normalizedAmount);
                if (projected.compareTo(profile.getFinancialDailyLimit()) > 0) {
                    throw new SecurityException(
                            "A operação excede o limite financeiro diário desta conta."
                    );
                }

                usage.setAmount(projected);
                financialUsageRepository.save(usage);
            }
        } catch (RuntimeException ex) {
            auditService.logError(
                    managedUser,
                    null,
                    AuditLog.AuditActionType.REJECT,
                    "FINANCIAL_SECURITY",
                    operationCode,
                    ex.getMessage(),
                    MODULE,
                    normalizeIp(sourceIp)
            );
            throw ex;
        }
    }

    public void validatePassword(User user, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A palavra-passe é obrigatória.");
        }

        UserSecurityProfile profile = getEffectiveProfile(user);

        if (rawPassword.length() < profile.getPasswordMinLength()) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve ter pelo menos "
                            + profile.getPasswordMinLength() + " caracteres."
            );
        }

        if (profile.isPasswordRequireUpper()
                && rawPassword.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos uma letra maiúscula."
            );
        }

        if (profile.isPasswordRequireLower()
                && rawPassword.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos uma letra minúscula."
            );
        }

        if (profile.isPasswordRequireDigit()
                && rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos um número."
            );
        }

        if (profile.isPasswordRequireSymbol()
                && rawPassword.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve conter pelo menos um símbolo."
            );
        }
    }

    private UserSecurityProfile managedProfileTarget(User actor, Long targetUserId) {
        User managedActor = managedActor(actor);
        return managedTarget(managedActor, targetUserId);
    }

    private User managedTarget(User managedActor, Long targetUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("Utilizador de destino inválido.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (target.isSuperadmin() && !managedActor.isSuperadmin()) {
            throw new SecurityException(
                    "Só um Superadministrador pode gerir o perfil de segurança de outro Superadministrador."
            );
        }
        return target;
    }

    private User managedActor(User actor) {
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

        boolean direct = userAccessPermissionRepository
                .existsByUserAndModuloIgnoreCaseAndOpcaoIgnoreCase(
                        managed, MODULE, "EDITAR"
                );

        boolean viaProfile = managed.getPerfis() != null
                && managed.getPerfis().stream()
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActivo()))
                .flatMap(p -> p.getPermissoes().stream())
                .anyMatch(pm ->
                        MODULE.equalsIgnoreCase(pm.getModulo())
                                && ("TODOS".equalsIgnoreCase(pm.getRecurso())
                                || RESOURCE.equalsIgnoreCase(pm.getRecurso()))
                                && pm.getOperacao() == PermissaoPerfil.Operacao.EDITAR
                                && Boolean.TRUE.equals(pm.getPermitido())
                );

        if (!direct && !viaProfile) {
            throw new SecurityException(
                    "Não possui permissão para administrar perfis de segurança."
            );
        }

        return managed;
    }

    private boolean isIpAllowed(UserSecurityProfile profile, String sourceIp) {
        Set<String> ranges = profile.getAllowedIpRanges();
        if (ranges == null || ranges.isEmpty()) {
            return true;
        }

        if (sourceIp == null || sourceIp.isBlank()) {
            return false;
        }

        return ranges.stream().anyMatch(range -> ipMatches(sourceIp.trim(), range));
    }

    private boolean ipMatches(String ip, String rule) {
        String value = rule == null ? "" : rule.trim();
        if (value.isBlank()) {
            return false;
        }

        try {
            if (!value.contains("/")) {
                return InetAddress.getByName(value).equals(InetAddress.getByName(ip));
            }

            String[] parts = value.split("/", 2);
            InetAddress network = InetAddress.getByName(parts[0].trim());
            InetAddress address = InetAddress.getByName(ip.trim());
            int prefix = Integer.parseInt(parts[1].trim());

            byte[] networkBytes = network.getAddress();
            byte[] addressBytes = address.getAddress();
            if (networkBytes.length != addressBytes.length) {
                return false;
            }

            int bits = networkBytes.length * 8;
            if (prefix < 0 || prefix > bits) {
                return false;
            }

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != addressBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (networkBytes[fullBytes] & mask)
                    == (addressBytes[fullBytes] & mask);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isScheduleAllowed(UserSecurityProfile profile, LocalDateTime now) {
        LocalTime start = profile.getLoginStart();
        LocalTime end = profile.getLoginEnd();

        if (start == null || end == null) {
            return true;
        }

        if (start.equals(end)) {
            return true;
        }

        LocalTime current = now.toLocalTime();
        if (start.isBefore(end)) {
            return !current.isBefore(start) && !current.isAfter(end);
        }

        return !current.isBefore(start) || !current.isAfter(end);
    }

    private boolean isWeekDayAllowed(UserSecurityProfile profile, DayOfWeek day) {
        Set<DayOfWeek> days = profile.getAllowedWeekDays();
        return days == null || days.isEmpty() || days.contains(day);
    }

    private void validateProfile(UserSecurityProfile profile) {
        if (profile.getMaxLoginAttempts() < 1 || profile.getMaxLoginAttempts() > 50) {
            throw new IllegalArgumentException("O máximo de tentativas de login deve estar entre 1 e 50.");
        }
        if (profile.getLockoutMinutes() < 1 || profile.getLockoutMinutes() > 1440) {
            throw new IllegalArgumentException("O bloqueio deve estar entre 1 e 1440 minutos.");
        }
        if (profile.getSessionTimeoutMinutes() < 5 || profile.getSessionTimeoutMinutes() > 10080) {
            throw new IllegalArgumentException("O timeout de sessão deve estar entre 5 e 10080 minutos.");
        }
        if (profile.getMaxConcurrentSessions() < 0 || profile.getMaxConcurrentSessions() > 100) {
            throw new IllegalArgumentException("O limite de sessões deve estar entre 0 e 100.");
        }
        if (profile.getPasswordMinLength() < 8 || profile.getPasswordMinLength() > 128) {
            throw new IllegalArgumentException("O comprimento mínimo da palavra-passe deve estar entre 8 e 128.");
        }
        if (profile.getPasswordExpiryDays() < 0 || profile.getPasswordExpiryDays() > 3650) {
            throw new IllegalArgumentException("A expiração da palavra-passe deve estar entre 0 e 3650 dias.");
        }

        BigDecimal operationLimit = profile.getFinancialOperationLimit();
        BigDecimal dailyLimit = profile.getFinancialDailyLimit();
        if (operationLimit != null && operationLimit.signum() < 0) {
            throw new IllegalArgumentException("O limite por operação não pode ser negativo.");
        }
        if (dailyLimit != null && dailyLimit.signum() < 0) {
            throw new IllegalArgumentException("O limite diário não pode ser negativo.");
        }
        if (operationLimit != null && dailyLimit != null
                && dailyLimit.compareTo(operationLimit) < 0) {
            throw new IllegalArgumentException(
                    "O limite diário não pode ser inferior ao limite por operação."
            );
        }

        profile.setFinancialCurrency(normalizeCurrency(profile.getFinancialCurrency()));
        normalizeSet(profile.getAllowedModules(), 80, "módulos");
        normalizeSet(profile.getAllowedIpRanges(), 64, "regras IP");
        normalizeSet(profile.getAllowedCriticalOperations(), 100, "operações críticas");

        if (profile.getAllowedCompanyIds() == null) {
            profile.setAllowedCompanyIds(new LinkedHashSet<>());
        }
        if (profile.getAllowedCompanyIds().size() > 100) {
            throw new IllegalArgumentException("É permitido configurar no máximo 100 empresas.");
        }
        if (!profile.getAllowedCompanyIds().isEmpty()) {
            long validCompanies = empresaRepository.findAllById(profile.getAllowedCompanyIds()).stream()
                    .filter(e -> Boolean.TRUE.equals(e.getAtiva()))
                    .count();
            if (validCompanies != profile.getAllowedCompanyIds().size()) {
                throw new IllegalArgumentException(
                        "Uma ou mais empresas seleccionadas não existem ou estão inactivas."
                );
            }
        }
        if (profile.getAllowedWeekDays() == null) {
            profile.setAllowedWeekDays(new LinkedHashSet<>());
        }

        for (String ip : profile.getAllowedIpRanges()) {
            if (!isValidIpRule(ip)) {
                throw new IllegalArgumentException("Regra IP inválida: " + ip);
            }
        }

        if ((profile.getLoginStart() == null) != (profile.getLoginEnd() == null)) {
            throw new IllegalArgumentException(
                    "O horário de início e fim do login deve ser preenchido em conjunto."
            );
        }
    }

    private boolean isValidIpRule(String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }
        if (!rule.contains("/")) {
            try {
                InetAddress.getByName(rule.trim());
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        String[] parts = rule.split("/", 2);
        try {
            InetAddress address = InetAddress.getByName(parts[0].trim());
            int prefix = Integer.parseInt(parts[1].trim());
            return prefix >= 0 && prefix <= address.getAddress().length * 8;
        } catch (Exception ex) {
            return false;
        }
    }

    private void normalizeSet(Set<String> values, int maxLength, String label) {
        if (values == null) {
            return;
        }

        if (values.size() > 100) {
            throw new IllegalArgumentException("É permitido configurar no máximo 100 " + label + ".");
        }

        for (String value : values) {
            if (value == null || value.isBlank() || value.trim().length() > maxLength) {
                throw new IllegalArgumentException("Valor inválido em " + label + ".");
            }
        }
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null || currency.isBlank()
                ? "AOA"
                : currency.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("A moeda financeira deve usar 3 letras ISO.");
        }
        return value;
    }

    private UserSecurityProfile defaultProfile(User user) {
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setUser(user);
        profile.setLoginEnabled(true);
        profile.setRequireMfa(false);
        profile.setAllowRecoveryCode(true);
        profile.setMaxLoginAttempts(5);
        profile.setLockoutMinutes(15);
        profile.setSessionTimeoutMinutes(480);
        profile.setMaxConcurrentSessions(3);
        profile.setPasswordMinLength(8);
        profile.setPasswordRequireUpper(true);
        profile.setPasswordRequireLower(true);
        profile.setPasswordRequireDigit(true);
        profile.setPasswordRequireSymbol(false);
        profile.setPasswordExpiryDays(90);
        profile.setLoginStart(LocalTime.of(0, 0));
        profile.setLoginEnd(LocalTime.of(23, 59));
        profile.setFinancialCurrency("AOA");
        profile.setFinancialOperationLimit(null);
        profile.setFinancialDailyLimit(null);
        profile.setAllowedCompanyIds(new LinkedHashSet<>());
        profile.setAllowedModules(new LinkedHashSet<>());
        profile.setAllowedIpRanges(new LinkedHashSet<>());
        profile.setAllowedWeekDays(new LinkedHashSet<>());
        profile.setAllowedCriticalOperations(new LinkedHashSet<>());
        return profile;
    }

    private void copyValues(UserSecurityProfile from, UserSecurityProfile to) {
        to.setLoginEnabled(from.isLoginEnabled());
        to.setRequireMfa(from.isRequireMfa());
        to.setAllowRecoveryCode(from.isAllowRecoveryCode());
        to.setMaxLoginAttempts(from.getMaxLoginAttempts());
        to.setLockoutMinutes(from.getLockoutMinutes());
        to.setSessionTimeoutMinutes(from.getSessionTimeoutMinutes());
        to.setMaxConcurrentSessions(from.getMaxConcurrentSessions());
        to.setPasswordMinLength(from.getPasswordMinLength());
        to.setPasswordRequireUpper(from.isPasswordRequireUpper());
        to.setPasswordRequireLower(from.isPasswordRequireLower());
        to.setPasswordRequireDigit(from.isPasswordRequireDigit());
        to.setPasswordRequireSymbol(from.isPasswordRequireSymbol());
        to.setPasswordExpiryDays(from.getPasswordExpiryDays());
        to.setLoginStart(from.getLoginStart());
        to.setLoginEnd(from.getLoginEnd());
        to.setFinancialOperationLimit(from.getFinancialOperationLimit());
        to.setFinancialDailyLimit(from.getFinancialDailyLimit());
        to.setFinancialCurrency(from.getFinancialCurrency());
        to.setAllowedCompanyIds(from.getAllowedCompanyIds());
        to.setAllowedModules(normalizeUpperSet(from.getAllowedModules()));
        to.setAllowedIpRanges(from.getAllowedIpRanges());
        to.setAllowedWeekDays(from.getAllowedWeekDays());
        to.setAllowedCriticalOperations(normalizeUpperSet(from.getAllowedCriticalOperations()));
    }

    private Set<String> normalizeUpperSet(Set<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.trim().toUpperCase(Locale.ROOT))
                    .forEach(result::add);
        }
        return result;
    }

    private LinkedHashMap<String, Object> snapshot(UserSecurityProfile profile) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("loginEnabled", profile.isLoginEnabled());
        map.put("requireMfa", profile.isRequireMfa());
        map.put("allowRecoveryCode", profile.isAllowRecoveryCode());
        map.put("maxLoginAttempts", profile.getMaxLoginAttempts());
        map.put("lockoutMinutes", profile.getLockoutMinutes());
        map.put("sessionTimeoutMinutes", profile.getSessionTimeoutMinutes());
        map.put("maxConcurrentSessions", profile.getMaxConcurrentSessions());
        map.put("passwordMinLength", profile.getPasswordMinLength());
        map.put("passwordRequireUpper", profile.isPasswordRequireUpper());
        map.put("passwordRequireLower", profile.isPasswordRequireLower());
        map.put("passwordRequireDigit", profile.isPasswordRequireDigit());
        map.put("passwordRequireSymbol", profile.isPasswordRequireSymbol());
        map.put("passwordExpiryDays", profile.getPasswordExpiryDays());
        map.put("loginStart", profile.getLoginStart());
        map.put("loginEnd", profile.getLoginEnd());
        map.put("financialOperationLimit", profile.getFinancialOperationLimit());
        map.put("financialDailyLimit", profile.getFinancialDailyLimit());
        map.put("financialCurrency", profile.getFinancialCurrency());
        map.put("allowedCompanyIds", profile.getAllowedCompanyIds());
        map.put("allowedModules", profile.getAllowedModules());
        map.put("allowedIpRanges", profile.getAllowedIpRanges());
        map.put("allowedWeekDays", profile.getAllowedWeekDays());
        map.put("allowedCriticalOperations", profile.getAllowedCriticalOperations());
        return map;
    }
}
