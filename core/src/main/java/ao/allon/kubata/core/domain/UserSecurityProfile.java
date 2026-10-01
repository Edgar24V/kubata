package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "user_security_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_user", columnNames = "user_id"))
public class UserSecurityProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "login_enabled", nullable = false)
    private boolean loginEnabled = true;

    @Column(name = "require_mfa", nullable = false)
    private boolean requireMfa = false;

    @Column(name = "allow_recovery_code", nullable = false)
    private boolean allowRecoveryCode = true;

    @Column(name = "max_login_attempts", nullable = false)
    private int maxLoginAttempts = 5;

    @Column(name = "lockout_minutes", nullable = false)
    private int lockoutMinutes = 15;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes = 480;

    @Column(name = "max_concurrent_sessions", nullable = false)
    private int maxConcurrentSessions = 3;

    @Column(name = "password_min_length", nullable = false)
    private int passwordMinLength = 8;

    @Column(name = "password_require_upper", nullable = false)
    private boolean passwordRequireUpper = true;

    @Column(name = "password_require_lower", nullable = false)
    private boolean passwordRequireLower = true;

    @Column(name = "password_require_digit", nullable = false)
    private boolean passwordRequireDigit = true;

    @Column(name = "password_require_symbol", nullable = false)
    private boolean passwordRequireSymbol = false;

    @Column(name = "password_expiry_days", nullable = false)
    private int passwordExpiryDays = 90;

    @Column(name = "login_start")
    private LocalTime loginStart = LocalTime.of(0, 0);

    @Column(name = "login_end")
    private LocalTime loginEnd = LocalTime.of(23, 59);

    @Column(name = "financial_operation_limit", precision = 19, scale = 2)
    private BigDecimal financialOperationLimit;

    @Column(name = "financial_daily_limit", precision = 19, scale = 2)
    private BigDecimal financialDailyLimit;

    @Column(name = "financial_currency", length = 3, nullable = false)
    private String financialCurrency = "AOA";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_security_profile_companies",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_company",
                    columnNames = {"profile_id", "empresa_id"}))
    @Column(name = "empresa_id", nullable = false)
    private Set<Long> allowedCompanyIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_security_profile_modules",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_module",
                    columnNames = {"profile_id", "modulo"}))
    @Column(name = "modulo", length = 80, nullable = false)
    private Set<String> allowedModules = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_security_profile_ips",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_ip",
                    columnNames = {"profile_id", "ip_range"}))
    @Column(name = "ip_range", length = 64, nullable = false)
    private Set<String> allowedIpRanges = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_security_profile_weekdays",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_weekday",
                    columnNames = {"profile_id", "weekday"}))
    @Column(name = "weekday", length = 20, nullable = false)
    private Set<DayOfWeek> allowedWeekDays = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_security_profile_critical_ops",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_security_profile_critical_op",
                    columnNames = {"profile_id", "operation_code"}))
    @Column(name = "operation_code", length = 100, nullable = false)
    private Set<String> allowedCriticalOperations = new LinkedHashSet<>();

    public UserSecurityProfile() {
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isLoginEnabled() { return loginEnabled; }
    public void setLoginEnabled(boolean loginEnabled) { this.loginEnabled = loginEnabled; }

    public boolean isRequireMfa() { return requireMfa; }
    public void setRequireMfa(boolean requireMfa) { this.requireMfa = requireMfa; }

    public boolean isAllowRecoveryCode() { return allowRecoveryCode; }
    public void setAllowRecoveryCode(boolean allowRecoveryCode) { this.allowRecoveryCode = allowRecoveryCode; }

    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }

    public int getLockoutMinutes() { return lockoutMinutes; }
    public void setLockoutMinutes(int lockoutMinutes) { this.lockoutMinutes = lockoutMinutes; }

    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

    public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(int maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }

    public int getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }

    public boolean isPasswordRequireUpper() { return passwordRequireUpper; }
    public void setPasswordRequireUpper(boolean value) { this.passwordRequireUpper = value; }

    public boolean isPasswordRequireLower() { return passwordRequireLower; }
    public void setPasswordRequireLower(boolean value) { this.passwordRequireLower = value; }

    public boolean isPasswordRequireDigit() { return passwordRequireDigit; }
    public void setPasswordRequireDigit(boolean value) { this.passwordRequireDigit = value; }

    public boolean isPasswordRequireSymbol() { return passwordRequireSymbol; }
    public void setPasswordRequireSymbol(boolean value) { this.passwordRequireSymbol = value; }

    public int getPasswordExpiryDays() { return passwordExpiryDays; }
    public void setPasswordExpiryDays(int passwordExpiryDays) { this.passwordExpiryDays = passwordExpiryDays; }

    public LocalTime getLoginStart() { return loginStart; }
    public void setLoginStart(LocalTime loginStart) { this.loginStart = loginStart; }

    public LocalTime getLoginEnd() { return loginEnd; }
    public void setLoginEnd(LocalTime loginEnd) { this.loginEnd = loginEnd; }

    public BigDecimal getFinancialOperationLimit() { return financialOperationLimit; }
    public void setFinancialOperationLimit(BigDecimal value) { this.financialOperationLimit = value; }

    public BigDecimal getFinancialDailyLimit() { return financialDailyLimit; }
    public void setFinancialDailyLimit(BigDecimal value) { this.financialDailyLimit = value; }

    public String getFinancialCurrency() { return financialCurrency; }
    public void setFinancialCurrency(String value) { this.financialCurrency = value; }

    public Set<Long> getAllowedCompanyIds() { return allowedCompanyIds; }
    public void setAllowedCompanyIds(Set<Long> values) {
        this.allowedCompanyIds = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public Set<String> getAllowedModules() { return allowedModules; }
    public void setAllowedModules(Set<String> values) {
        this.allowedModules = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public Set<String> getAllowedIpRanges() { return allowedIpRanges; }
    public void setAllowedIpRanges(Set<String> values) {
        this.allowedIpRanges = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public Set<DayOfWeek> getAllowedWeekDays() { return allowedWeekDays; }
    public void setAllowedWeekDays(Set<DayOfWeek> values) {
        this.allowedWeekDays = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public Set<String> getAllowedCriticalOperations() { return allowedCriticalOperations; }
    public void setAllowedCriticalOperations(Set<String> values) {
        this.allowedCriticalOperations = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }
}
