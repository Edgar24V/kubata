package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.LocalizacaoArmazem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizacaoArmazemRepository extends JpaRepository<LocalizacaoArmazem, Long> {
    List<LocalizacaoArmazem> findByArmazemIdOrderByCodigoAsc(Long armazemId);
    Optional<LocalizacaoArmazem> findByArmazemIdAndCodigo(Long armazemId, String codigo);
}
