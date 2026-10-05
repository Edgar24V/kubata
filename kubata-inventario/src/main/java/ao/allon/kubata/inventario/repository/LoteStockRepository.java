package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.LoteStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteStockRepository extends JpaRepository<LoteStock, Long> {
    List<LoteStock> findByProdutoIdOrderByDataValidadeAsc(Long produtoId);
    List<LoteStock> findByDataValidadeBeforeAndActiveTrue(LocalDate data);
    Optional<LoteStock> findByProdutoIdAndCodigo(Long produtoId, String codigo);
}
