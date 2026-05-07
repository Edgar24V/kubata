package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.ModuloSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ModuloSistemaRepository extends JpaRepository<ModuloSistema, Long> {
    Optional<ModuloSistema> findByCodigo(String codigo);
}
