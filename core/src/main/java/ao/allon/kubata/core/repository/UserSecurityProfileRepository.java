package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.UserSecurityProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSecurityProfileRepository extends JpaRepository<UserSecurityProfile, Long> {
    Optional<UserSecurityProfile> findByUserId(Long userId);
}
