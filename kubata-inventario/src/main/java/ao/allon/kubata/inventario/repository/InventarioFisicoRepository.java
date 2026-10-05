package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.InventarioFisico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioFisicoRepository extends JpaRepository<InventarioFisico, Long> {
    List<InventarioFisico> findTop100ByOrderByDataInventarioDesc();
    Optional<InventarioFisico> findByNumero(String numero);
}
