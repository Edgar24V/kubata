package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.TaxaIVA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaxaIVARepository extends JpaRepository<TaxaIVA, Long> {
    Optional<TaxaIVA> findByCodigo(String codigo);
    Optional<TaxaIVA> findByPadraoTrue();
}
