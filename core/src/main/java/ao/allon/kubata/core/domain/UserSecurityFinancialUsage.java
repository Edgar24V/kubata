package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "user_security_financial_usage",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_security_financial_usage_day",
                columnNames = {"user_id", "usage_date", "currency"}))
public class UserSecurityFinancialUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    public UserSecurityFinancialUsage() {
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
