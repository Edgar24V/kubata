package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.TransferenciaStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferenciaStockRepository extends JpaRepository<TransferenciaStock, Long> {
    List<TransferenciaStock> findTop100ByOrderByCreatedAtDesc();
    Optional<TransferenciaStock> findByNumero(String numero);
}
