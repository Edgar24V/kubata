package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ao.allon.kubata.faturacao.domain.Serie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    Optional<Fatura> findByNumero(String numero);
    
    @EntityGraph(attributePaths = {"pagamentos", "cliente", "serie", "usuario"})
    Optional<Fatura> findWithDetailsByNumero(String numero);

    List<Fatura> findByClienteId(Long clienteId);
    List<Fatura> findByStatus(StatusFatura status);
    List<Fatura> findByTipoDocumento(TipoDocumento tipo);
    
    @Query("SELECT f.numeroSequencial FROM Fatura f WHERE f.serie = :serie ORDER BY f.numeroSequencial ASC")
    List<Long> findAllNumerosSequenciaisBySerie(@Param("serie") Serie serie);
    
    @Query("SELECT f FROM Fatura f WHERE f.dataEmissao BETWEEN :startDate AND :endDate")
    List<Fatura> findByDataEmissaoBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT f FROM Fatura f LEFT JOIN FETCH f.itens WHERE f.dataEmissao BETWEEN :startDate AND :endDate")
    List<Fatura> findByDataEmissaoBetweenWithItens(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(f.total) FROM Fatura f WHERE f.status = :status AND f.dataEmissao BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByStatusAndDataEmissaoBetween(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT f.dataEmissao, SUM(f.total) FROM Fatura f WHERE f.status = :status AND f.dataEmissao >= :startDate GROUP BY f.dataEmissao ORDER BY f.dataEmissao ASC")
    List<Object[]> findDailySales(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate);
    
    @Query("SELECT SUM(f.total) FROM Fatura f WHERE f.status = :status AND f.dataEmissao BETWEEN :startDate AND :endDate AND f.cliente.nome = :clienteNome")
    BigDecimal sumTotalByStatusAndDataEmissaoBetweenAndClienteNome(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("clienteNome") String clienteNome);

    @Query("SELECT SUM(f.total) FROM Fatura f WHERE f.status = :status AND f.dataEmissao BETWEEN :startDate AND :endDate AND f.cliente.nome <> :clienteNome")
    BigDecimal sumTotalByStatusAndDataEmissaoBetweenAndClienteNomeNot(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("clienteNome") String clienteNome);

    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.status = :status AND f.dataEmissao BETWEEN :startDate AND :endDate AND f.cliente.nome = :clienteNome")
    Long countByStatusAndDataEmissaoBetweenAndClienteNome(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("clienteNome") String clienteNome);

    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.status = :status AND f.dataEmissao BETWEEN :startDate AND :endDate AND f.cliente.nome <> :clienteNome")
    Long countByStatusAndDataEmissaoBetweenAndClienteNomeNot(@Param("status") StatusFatura status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("clienteNome") String clienteNome);
    
    long countByStatus(StatusFatura status);

    @Query("SELECT f FROM Fatura f JOIN FETCH f.itens WHERE f.id = :id")
    Optional<Fatura> findByIdWithItens(@Param("id") Long id);

    long countByFaturaReferenciaIdAndTipoDocumentoIn(Long faturaReferenciaId, Collection<TipoDocumento> tipos);

    List<Fatura> findByFaturaReferenciaIdAndTipoDocumentoInOrderByDataEmissaoAsc(Long faturaReferenciaId, Collection<TipoDocumento> tipos);

    boolean existsByClienteIdAndStatusIn(Long clienteId, List<StatusFatura> statuses);

    @Query("SELECT i.descricao, SUM(i.quantidade), SUM(i.total) " +
           "FROM ItemFatura i " +
           "JOIN i.fatura f " +
           "WHERE f.status = 'EMITIDA' AND f.dataEmissao BETWEEN :startDate AND :endDate " +
           "GROUP BY i.descricao " +
           "ORDER BY SUM(i.total) DESC")
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT f.dataEmissao, SUM(f.total), SUM(f.iva) " +
           "FROM Fatura f " +
           "WHERE f.status = 'EMITIDA' AND f.dataEmissao BETWEEN :startDate AND :endDate " +
           "GROUP BY f.dataEmissao " +
           "ORDER BY f.dataEmissao ASC")
    List<Object[]> findTaxReportData(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT f.serie.designacao, COUNT(f), SUM(f.total) " +
           "FROM Fatura f " +
           "WHERE f.status = 'EMITIDA' AND f.dataEmissao BETWEEN :startDate AND :endDate " +
           "GROUP BY f.serie.designacao " +
           "ORDER BY SUM(f.total) DESC")
    List<Object[]> findPerformanceByCashier(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(f.total) FROM Fatura f WHERE f.dataEmissao BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByDataEmissaoBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT f FROM Fatura f
            WHERE f.tipoDocumento = :tipo
              AND (:start IS NULL OR f.dataEmissao >= :start)
              AND (:end IS NULL OR f.dataEmissao <= :end)
              AND (:clienteId IS NULL OR f.cliente.id = :clienteId)
              AND (:status IS NULL OR f.status = :status)
            ORDER BY f.dataEmissao DESC
            """)
    List<Fatura> searchByTipoAndFilters(@Param("tipo") TipoDocumento tipo,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end,
                                        @Param("clienteId") Long clienteId,
                                        @Param("status") StatusFatura status);

    /**
     * Busca todas as cópias (duplicados, segundas vias) de um documento original.
     * 
     * @param faturaOriginal A fatura original
     * @return Lista de cópias do documento
     */
    List<Fatura> findByFaturaOriginal(Fatura faturaOriginal);
}
