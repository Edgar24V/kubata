package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.InventarioFisicoLinha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventarioFisicoLinhaRepository extends JpaRepository<InventarioFisicoLinha, Long> {
    List<InventarioFisicoLinha> findByInventarioIdOrderByIdAsc(Long inventarioId);
}
