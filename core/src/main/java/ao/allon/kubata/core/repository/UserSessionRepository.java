package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findAllByUsernameOrderByLoginTimeDesc(String username);

    long deleteAllByUsername(String username);

}
