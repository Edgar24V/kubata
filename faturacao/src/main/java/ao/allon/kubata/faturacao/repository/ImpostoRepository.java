package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Imposto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImpostoRepository extends JpaRepository<Imposto, Long> {
    Optional<Imposto> findByCodigo(String codigo);
    List<Imposto> findByActiveTrue();
}
