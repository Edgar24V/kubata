package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.ArtigoArmazem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtigoArmazemRepository extends JpaRepository<ArtigoArmazem, Long> {
    Optional<ArtigoArmazem> findByProdutoIdAndArmazemId(Long produtoId, Long armazemId);
}
