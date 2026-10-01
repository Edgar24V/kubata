package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.AlertaHistorico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaHistoricoRepository extends JpaRepository<AlertaHistorico, Long> {

    @EntityGraph(attributePaths = {"utilizador"})
    List<AlertaHistorico> findByAlertaIdOrderByEventAtAscIdAsc(Long alertaId);
}
