package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.DocumentoColaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentoColaboradorRepository extends JpaRepository<DocumentoColaborador, Long> {

    List<DocumentoColaborador> findByColaboradorIdOrderByDataUploadDesc(Long colaboradorId);

    List<DocumentoColaborador> findByTipoOrderByDataUploadDesc(DocumentoColaborador.TipoDocumento tipo);

    List<DocumentoColaborador> findByActiveTrueOrderByDataUploadDesc();

    List<DocumentoColaborador> findByActiveFalseOrderByDataUploadDesc();

    @Query("SELECT COUNT(d) FROM DocumentoColaborador d WHERE d.active = true")
    long countByActiveTrue();

    @Query("SELECT d FROM DocumentoColaborador d WHERE " +
           "(:colaboradorId IS NULL OR d.colaborador.id = :colaboradorId) AND " +
           "(:tipo IS NULL OR d.tipo = :tipo) AND " +
           "(:ativo IS NULL OR d.active = :ativo) AND " +
           "(:nomeDocumento IS NULL OR LOWER(d.nomeDocumento) LIKE LOWER(CONCAT('%', :nomeDocumento, '%'))) " +
           "ORDER BY d.dataUpload DESC")
    List<DocumentoColaborador> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                             @Param("tipo") DocumentoColaborador.TipoDocumento tipo,
                                             @Param("ativo") Boolean ativo,
                                             @Param("nomeDocumento") String nomeDocumento);

    @Query("SELECT d FROM DocumentoColaborador d WHERE d.dataValidade IS NOT NULL AND " +
           "d.dataValidade BETWEEN :dataInicio AND :dataFim AND d.active = true " +
           "ORDER BY d.dataValidade ASC")
    List<DocumentoColaborador> findDocumentosAExpirar(@Param("dataInicio") LocalDate dataInicio,
                                                      @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM DocumentoColaborador d WHERE d.dataValidade IS NOT NULL AND " +
           "d.dataValidade < :dataAtual AND d.active = true " +
           "ORDER BY d.dataValidade ASC")
    List<DocumentoColaborador> findDocumentosExpirados(@Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT d.tipo, COUNT(d) as total FROM DocumentoColaborador d WHERE d.active = true " +
           "GROUP BY d.tipo " +
           "ORDER BY total DESC")
    List<Object[]> countDocumentosPorTipo();

    @Query("SELECT d.colaborador.id, d.colaborador.nomeCompleto, COUNT(d) as total " +
           "FROM DocumentoColaborador d WHERE d.active = true " +
           "GROUP BY d.colaborador.id, d.colaborador.nomeCompleto " +
           "ORDER BY total DESC")
    List<Object[]> countDocumentosPorColaborador();

    @Query("SELECT d FROM DocumentoColaborador d WHERE d.colaborador.id = :colaboradorId AND " +
           "d.tipo = :tipo AND d.active = true " +
           "ORDER BY d.dataUpload DESC")
    List<DocumentoColaborador> findByColaboradorAndTipo(@Param("colaboradorId") Long colaboradorId,
                                                        @Param("tipo") DocumentoColaborador.TipoDocumento tipo);

    @Query("SELECT COUNT(d) FROM DocumentoColaborador d WHERE d.colaborador.id = :colaboradorId AND d.active = true")
    long countByColaboradorAndAtivo(@Param("colaboradorId") Long colaboradorId);

    @Query("SELECT d FROM DocumentoColaborador d WHERE d.active = true AND " +
           "(LOWER(d.nomeDocumento) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(d.descricao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(d.numeroDocumento) LIKE LOWER(CONCAT('%', :termo, '%'))) " +
           "ORDER BY d.dataUpload DESC")
    List<DocumentoColaborador> searchByTermo(@Param("termo") String termo);
}
