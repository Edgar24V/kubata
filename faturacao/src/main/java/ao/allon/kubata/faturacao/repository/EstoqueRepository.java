package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.Armazem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    
    Optional<Estoque> findByProdutoAndArmazemAndLote(Produto produto, Armazem armazem, String lote);
    
    List<Estoque> findByProduto(Produto produto);
    
    List<Estoque> findByArmazem(Armazem armazem);

    @Query("SELECT SUM(e.quantidade) FROM Estoque e WHERE e.produto = :produto AND e.dataEntrada IS NOT NULL")
    Integer sumQuantidadeByProduto(@Param("produto") Produto produto);
    
    @Query("SELECT COUNT(e) > 0 FROM Estoque e WHERE e.produto = :produto AND (e.validade IS NULL OR e.validade >= :hoje) AND e.quantidade > 0")
    boolean existsValidoByProduto(@Param("produto") Produto produto, @Param("hoje") java.time.LocalDate hoje);
}
