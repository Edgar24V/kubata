package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.ExercicioFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExercicioFiscalRepository extends JpaRepository<ExercicioFiscal, Long> {
    Optional<ExercicioFiscal> findByEmpresaIdAndAno(Long empresaId, Integer ano);
    List<ExercicioFiscal> findByEmpresaIdOrderByAnoDesc(Long empresaId);
}
