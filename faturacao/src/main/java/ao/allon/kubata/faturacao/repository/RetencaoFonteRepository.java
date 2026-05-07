package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.RetencaoFonte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetencaoFonteRepository extends JpaRepository<RetencaoFonte, Long> {
    Optional<RetencaoFonte> findByCodigo(String codigo);
    
    @Query("SELECT r FROM RetencaoFonte r WHERE r.active = true")
    List<RetencaoFonte> findActive();
}
