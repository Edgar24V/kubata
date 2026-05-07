package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.Moeda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MoedaRepository extends JpaRepository<Moeda, Long> {
    Optional<Moeda> findByCodigoISO(String codigoISO);
}
