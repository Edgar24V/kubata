package ao.allon.kubata.core.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "mfa_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mfa_policy_scope_key",
                columnNames = "scope_key"
        )
)
public class MfaPolicy extends BaseEntity {

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
    private String nome = "Política MFA";

    @Column(name = "required", nullable = false)
    private boolean required = false;

    @Column(name = "allow_user_disable", nullable = false)
    private boolean allowUserDisable = true;

    @Column(name = "allow_recovery_codes", nullable = false)
    private boolean allowRecoveryCodes = true;

    @Column(name = "recovery_code_count", nullable = false)
    private int recoveryCodeCount = 10;

    @Column(name = "issuer", nullable = false, length = 80)
    private String issuer = "Kubata";

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays = 0;

    public ScopeType getScopeType() { return scopeType; }
    public void setScopeType(ScopeType scopeType) { this.scopeType = scopeType; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }

    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public boolean isAllowUserDisable() { return allowUserDisable; }
    public void setAllowUserDisable(boolean allowUserDisable) { this.allowUserDisable = allowUserDisable; }

    public boolean isAllowRecoveryCodes() { return allowRecoveryCodes; }
    public void setAllowRecoveryCodes(boolean allowRecoveryCodes) { this.allowRecoveryCodes = allowRecoveryCodes; }

    public int getRecoveryCodeCount() { return recoveryCodeCount; }
    public void setRecoveryCodeCount(int recoveryCodeCount) { this.recoveryCodeCount = recoveryCodeCount; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
}
