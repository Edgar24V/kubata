package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.UserSecurityFinancialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;

public interface UserSecurityFinancialUsageRepository extends JpaRepository<UserSecurityFinancialUsage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserSecurityFinancialUsage u where u.user.id = :userId and u.usageDate = :usageDate and u.currency = :currency")
    Optional<UserSecurityFinancialUsage> findForUpdate(Long userId, LocalDate usageDate, String currency);
}
