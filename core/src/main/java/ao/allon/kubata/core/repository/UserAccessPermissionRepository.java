package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserAccessPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAccessPermissionRepository extends JpaRepository<UserAccessPermission, Long> {
    List<UserAccessPermission> findByUser(User user);
    void deleteByUser(User user);
    void deleteByUser_Id(Long userId);
    boolean existsByUserAndModuloIgnoreCaseAndOpcaoIgnoreCase(User user, String modulo, String opcao);
}
