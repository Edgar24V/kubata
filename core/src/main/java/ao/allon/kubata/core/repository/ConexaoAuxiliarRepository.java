package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.ConexaoAuxiliar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConexaoAuxiliarRepository extends JpaRepository<ConexaoAuxiliar, Long> {
}