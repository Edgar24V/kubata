package ao.allon.kubata.contabilidade.repository;

import ao.allon.kubata.contabilidade.domain.MotivoIsencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotivoIsencaoRepository extends JpaRepository<MotivoIsencao, Long> {

    Optional<MotivoIsencao> findByCodigo(String codigo);

    List<MotivoIsencao> findByIsAtivoTrue();

    boolean existsByCodigo(String codigo);
}
