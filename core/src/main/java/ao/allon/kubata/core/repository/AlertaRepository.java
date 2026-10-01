package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.Alerta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    @EntityGraph(attributePaths = {"responsavel", "reconhecidoPor", "resolvidoPor", "ignoradoPor"})
    List<Alerta> findAllByOrderByOpenedAtDescIdDesc();

    @EntityGraph(attributePaths = {"responsavel", "reconhecidoPor", "resolvidoPor", "ignoradoPor"})
    List<Alerta> findByEstadoOrderByOpenedAtDescIdDesc(Alerta.Estado estado);

    List<Alerta> findBySeveridadeOrderByOpenedAtDescIdDesc(Alerta.Severidade severidade);

    Optional<Alerta> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    long countByEstado(Alerta.Estado estado);
}
