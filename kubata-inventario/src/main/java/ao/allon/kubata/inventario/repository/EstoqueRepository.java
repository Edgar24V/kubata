package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    List<Estoque> findByProdutoId(Long produtoId);

    List<Estoque> findByArmazemId(Long armazemId);

    Optional<Estoque> findByProdutoIdAndArmazemIdAndLote(Long produtoId, Long armazemId, String lote);

    @Query("SELECT SUM(e.quantidade) FROM Estoque e WHERE e.produto.id = :produtoId AND e.active = true")
    Integer getStockTotalByProdutoId(Long produtoId);

    @Query(value = "SELECT * FROM estoques e WHERE e.validade <= DATE('now') AND e.active = true", nativeQuery = true)
    List<Estoque> findProdutosVencidos();
}
