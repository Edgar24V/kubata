package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.MfaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MfaPolicyRepository extends JpaRepository<MfaPolicy, Long> {

    Optional<MfaPolicy> findByScopeTypeAndScopeIdAndActiveTrue(
            MfaPolicy.ScopeType scopeType,
            Long scopeId
    );

    Optional<MfaPolicy> findByScopeTypeAndActiveTrue(
            MfaPolicy.ScopeType scopeType
    );

    Optional<MfaPolicy> findByScopeKeyAndActiveTrue(String scopeKey);

    List<MfaPolicy> findAllByScopeTypeAndScopeIdInAndActiveTrue(
            MfaPolicy.ScopeType scopeType,
            Collection<Long> scopeIds
    );
}
