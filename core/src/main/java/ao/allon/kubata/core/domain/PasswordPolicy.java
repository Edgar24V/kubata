package ao.allon.kubata.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "password_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_password_policy_scope_key",
                columnNames = "scope_key"
        )
)
public class PasswordPolicy extends BaseEntity {

    public enum ScopeType {
        GLOBAL,
        EMPRESA,
        PERFIL,
        UTILIZADOR
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private ScopeType scopeType = ScopeType.GLOBAL;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "scope_key", nullable = false, length = 80, unique = true)
    private String scopeKey;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome = "Política de palavra-passe";

    @Column(name = "min_length", nullable = false)
    private int minLength = 8;

    @Column(name = "max_length", nullable = false)
    private int maxLength = 128;

    @Column(name = "require_upper", nullable = false)
    private boolean requireUpper = true;

    @Column(name = "require_lower", nullable = false)
    private boolean requireLower = true;

    @Column(name = "require_digit", nullable = false)
    private boolean requireDigit = true;

    @Column(name = "require_symbol", nullable = false)
    private boolean requireSymbol = false;

    @Column(name = "history_count", nullable = false)
    private int historyCount = 5;

    @Column(name = "expiry_days", nullable = false)
    private int expiryDays = 90;

    @Column(name = "minimum_age_hours", nullable = false)
    private int minimumAgeHours = 0;

    @Column(name = "reset_validity_hours", nullable = false)
    private int resetValidityHours = 24;

    @Column(name = "force_change_on_reset", nullable = false)
    private boolean forceChangeOnReset = true;

    @Column(name = "prohibit_identity_fragments", nullable = false)
    private boolean prohibitIdentityFragments = true;

    public ScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(ScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean isRequireUpper() {
        return requireUpper;
    }

    public void setRequireUpper(boolean requireUpper) {
        this.requireUpper = requireUpper;
    }

    public boolean isRequireLower() {
        return requireLower;
    }

    public void setRequireLower(boolean requireLower) {
        this.requireLower = requireLower;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public boolean isRequireSymbol() {
        return requireSymbol;
    }

    public void setRequireSymbol(boolean requireSymbol) {
        this.requireSymbol = requireSymbol;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int historyCount) {
        this.historyCount = historyCount;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(int expiryDays) {
        this.expiryDays = expiryDays;
    }

    public int getMinimumAgeHours() {
        return minimumAgeHours;
    }

    public void setMinimumAgeHours(int minimumAgeHours) {
        this.minimumAgeHours = minimumAgeHours;
    }

    public int getResetValidityHours() {
        return resetValidityHours;
    }

    public void setResetValidityHours(int resetValidityHours) {
        this.resetValidityHours = resetValidityHours;
    }

    public boolean isForceChangeOnReset() {
        return forceChangeOnReset;
    }

    public void setForceChangeOnReset(boolean forceChangeOnReset) {
        this.forceChangeOnReset = forceChangeOnReset;
    }

    public boolean isProhibitIdentityFragments() {
        return prohibitIdentityFragments;
    }

    public void setProhibitIdentityFragments(boolean prohibitIdentityFragments) {
        this.prohibitIdentityFragments = prohibitIdentityFragments;
    }
}
