package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MotivoIsencaoRepository extends JpaRepository<MotivoIsencao, Long> {
    Optional<MotivoIsencao> findByCodigo(String codigo);
}
