package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.NumeroSerieStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NumeroSerieStockRepository extends JpaRepository<NumeroSerieStock, Long> {
    List<NumeroSerieStock> findByProdutoIdAndArmazemId(Long produtoId, Long armazemId);
    Optional<NumeroSerieStock> findByProdutoIdAndNumero(Long produtoId, String numero);
}
