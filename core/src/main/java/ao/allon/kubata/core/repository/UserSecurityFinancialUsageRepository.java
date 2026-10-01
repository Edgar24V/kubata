package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.UserSecurityFinancialUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserSecurityFinancialUsageRepository
        extends JpaRepository<UserSecurityFinancialUsage, Long> {

    Optional<UserSecurityFinancialUsage> findByUserIdAndUsageDateAndCurrency(
            Long userId,
            LocalDate usageDate,
            String currency
    );
}
