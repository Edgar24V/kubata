package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.view.ProdutoLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT new ao.allon.kubata.inventario.view.ProdutoLinha(
                p.id, p.codigoBarra, p.nome, c.nome, p.unidadeMedida,
                p.precoUnitario, p.stock, p.stockMinimo, p.percentualIva)
            FROM Produto p LEFT JOIN p.categoria c
            WHERE LOWER(p.nome) LIKE :termo OR LOWER(p.codigoBarra) LIKE :termo
            """)
    Page<ProdutoLinha> pesquisarLinhas(@Param("termo") String termo, Pageable pageable);
}
