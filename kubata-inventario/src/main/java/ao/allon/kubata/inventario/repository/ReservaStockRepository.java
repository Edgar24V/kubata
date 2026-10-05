package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.ReservaStock;
import ao.allon.kubata.inventario.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {
    List<ReservaStock> findByEstadoOrderByDataReservaDesc(EstadoReserva estado);

    @Query("""
        select coalesce(sum(r.quantidade), 0)
        from ReservaStock r
        where r.produto.id = :produtoId
          and r.armazem.id = :armazemId
          and r.estado = ao.allon.kubata.inventario.enums.EstadoReserva.ATIVA
    """)
    BigDecimal sumAtivas(Long produtoId, Long armazemId);
}
