package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.MovimentoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {
    List<MovimentoStock> findTop250ByOrderByDataMovimentoDesc();
    List<MovimentoStock> findByProdutoIdOrderByDataMovimentoDesc(Long produtoId);
    List<MovimentoStock> findByArmazemIdOrderByDataMovimentoDesc(Long armazemId);
}
