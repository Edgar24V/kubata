package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserDevice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserAndDeviceKey(User user, String deviceKey);

    @EntityGraph(attributePaths = {"user"})
    List<UserDevice> findByUserOrderByLastSeenDesc(User user);

    long countByUserAndActiveTrue(User user);

    long countByUserAndTrustedTrueAndActiveTrue(User user);
}
