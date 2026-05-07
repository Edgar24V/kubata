package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByNif(String nif);
    boolean existsByNif(String nif);
    Optional<Empresa> findFirstByAtivaTrue();
}
