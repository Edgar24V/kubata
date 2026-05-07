package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Optional<Produto> findByCodigoBarra(String codigoBarra);
    
    List<Produto> findByCategoriaId(Long categoriaId);
    
    @Query("SELECT p FROM Produto p WHERE p.stock <= p.stockMinimo")
    List<Produto> findProdutosComStockBaixo();
    
    boolean existsByCodigoBarra(String codigoBarra);
    
    @Query("SELECT p FROM Produto p WHERE p.codigoBarra LIKE CONCAT(:prefix, '%')")
    List<Produto> findByCodigoBarraStartingWith(String prefix);
}
