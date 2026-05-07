package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByCodigoBarra(String codigoBarra);

    List<Produto> findByNomeContainingIgnoreCase(String nome);

    List<Produto> findByCategoriaId(Long categoriaId);

    @Query("SELECT p FROM Produto p WHERE p.stock <= p.stockMinimo AND p.active = true")
    List<Produto> findProdutosStockBaixo();

    boolean existsByCodigoBarra(String codigoBarra);
}
