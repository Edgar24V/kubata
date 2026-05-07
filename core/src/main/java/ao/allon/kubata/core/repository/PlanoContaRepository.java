package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.PlanoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoContaRepository extends JpaRepository<PlanoConta, Long> {
    Optional<PlanoConta> findByCodigo(String codigo);
    List<PlanoConta> findByContaPaiIsNullOrderByCodigoAsc(); // Raízes (Classes)
    List<PlanoConta> findByContaPaiOrderByCodigoAsc(PlanoConta contaPai);
    boolean existsByCodigo(String codigo);
}
