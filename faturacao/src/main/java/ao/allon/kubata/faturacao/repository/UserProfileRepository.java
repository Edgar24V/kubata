package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.UserProfileEntity;
import ao.allon.kubata.faturacao.profile.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    long countByTipo(UserType tipo);
}
