package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.MovimentoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {
    
    @Query("SELECT m FROM MovimentoStock m LEFT JOIN FETCH m.produto LEFT JOIN FETCH m.armazem LEFT JOIN FETCH m.fornecedor WHERE m.produto.id = :produtoId ORDER BY m.dataMovimento DESC")
    List<MovimentoStock> findByProdutoIdOrderByDataMovimentoDesc(@Param("produtoId") Long produtoId);

    @Query("SELECT m FROM MovimentoStock m LEFT JOIN FETCH m.produto LEFT JOIN FETCH m.armazem LEFT JOIN FETCH m.fornecedor WHERE m.dataMovimento BETWEEN :inicio AND :fim ORDER BY m.dataMovimento DESC")
    List<MovimentoStock> findByDataMovimentoBetween(@Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);

    @Query("SELECT m FROM MovimentoStock m LEFT JOIN FETCH m.produto LEFT JOIN FETCH m.armazem LEFT JOIN FETCH m.fornecedor WHERE m.produto.id = :produtoId AND m.dataMovimento BETWEEN :inicio AND :fim ORDER BY m.dataMovimento DESC")
    List<MovimentoStock> findByProdutoIdAndDataMovimentoBetween(@Param("produtoId") Long produtoId, @Param("inicio") java.time.LocalDateTime inicio, @Param("fim") java.time.LocalDateTime fim);

    @Query("SELECT m FROM MovimentoStock m LEFT JOIN FETCH m.produto LEFT JOIN FETCH m.armazem LEFT JOIN FETCH m.fornecedor ORDER BY m.dataMovimento DESC")
    List<MovimentoStock> findAllByOrderByDataMovimentoDesc();

    @Query("""
           SELECT m.produto.id, SUM(m.quantidade) 
           FROM MovimentoStock m 
           WHERE m.tipoMovimento = ao.allon.kubata.faturacao.enums.TipoMovimento.SAIDA 
             AND m.dataMovimento >= :inicio
           GROUP BY m.produto.id
           ORDER BY SUM(m.quantidade) DESC
           """)
    List<Object[]> findTopVendidosDesde(@Param("inicio") java.time.LocalDateTime inicio);
}
