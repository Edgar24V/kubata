package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, Long> {

    Optional<PasswordPolicy> findByScopeKeyAndActiveTrue(String scopeKey);

    Optional<PasswordPolicy> findByScopeTypeAndScopeIdAndActiveTrue(
            PasswordPolicy.ScopeType scopeType,
            Long scopeId
    );

    List<PasswordPolicy> findAllByScopeTypeAndScopeIdInAndActiveTrue(
            PasswordPolicy.ScopeType scopeType,
            java.util.Collection<Long> scopeIds
    );

    List<PasswordPolicy> findAllByScopeTypeAndActiveTrue(
            PasswordPolicy.ScopeType scopeType
    );

    Optional<PasswordPolicy> findByScopeTypeAndActiveTrue(
            PasswordPolicy.ScopeType scopeType
    );
}
