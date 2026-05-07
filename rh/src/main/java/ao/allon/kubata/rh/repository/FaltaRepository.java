package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Falta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FaltaRepository extends JpaRepository<Falta, Long> {

    List<Falta> findByColaboradorIdOrderByDataDesc(Long colaboradorId);

    List<Falta> findByDataBetweenOrderByData(LocalDate dataInicio, LocalDate dataFim);

    List<Falta> findByColaboradorIdAndDataBetweenOrderByData(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim);

    List<Falta> findByJustificadaTrueOrderByDataDesc();

    List<Falta> findByJustificadaFalseOrderByDataDesc();

    List<Falta> findByTipoOrderByDataDesc(Falta.TipoFalta tipo);

    @Query("SELECT COUNT(f) FROM Falta f WHERE f.data BETWEEN :dataInicio AND :dataFim")
    long countByDataBetween(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(f) FROM Falta f WHERE f.colaborador.id = :colaboradorId AND " +
           "f.data BETWEEN :dataInicio AND :dataFim")
    long countByColaboradorAndDataBetween(@Param("colaboradorId") Long colaboradorId,
                                          @Param("dataInicio") LocalDate dataInicio,
                                          @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(f) FROM Falta f WHERE f.justificada = false AND " +
           "f.data BETWEEN :dataInicio AND :dataFim")
    long countFaltasNaoJustificadas(@Param("dataInicio") LocalDate dataInicio,
                                    @Param("dataFim") LocalDate dataFim);

    @Query("SELECT f FROM Falta f WHERE " +
           "(:colaboradorId IS NULL OR f.colaborador.id = :colaboradorId) AND " +
           "(:dataInicio IS NULL OR f.data >= :dataInicio) AND " +
           "(:dataFim IS NULL OR f.data <= :dataFim) AND " +
           "(:tipo IS NULL OR f.tipo = :tipo) AND " +
           "(:justificada IS NULL OR f.justificada = :justificada) " +
           "ORDER BY f.data DESC")
    List<Falta> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                              @Param("dataInicio") LocalDate dataInicio,
                              @Param("dataFim") LocalDate dataFim,
                              @Param("tipo") Falta.TipoFalta tipo,
                              @Param("justificada") Boolean justificada);

    @Query("SELECT f.tipo, COUNT(f) as total FROM Falta f WHERE " +
           "f.data BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY f.tipo " +
           "ORDER BY total DESC")
    List<Object[]> countFaltasPorTipo(@Param("dataInicio") LocalDate dataInicio,
                                      @Param("dataFim") LocalDate dataFim);

    @Query("SELECT f.colaborador.id, f.colaborador.nomeCompleto, COUNT(f) as total FROM Falta f WHERE " +
           "f.data BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY f.colaborador.id, f.colaborador.nomeCompleto " +
           "ORDER BY total DESC")
    List<Object[]> countFaltasPorColaborador(@Param("dataInicio") LocalDate dataInicio,
                                             @Param("dataFim") LocalDate dataFim);

    @Query("SELECT f FROM Falta f WHERE f.aprovadoPor IS NULL " +
           "AND f.data BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY f.data DESC")
    List<Falta> findFaltasPendentesAprovacao(@Param("dataInicio") LocalDate dataInicio,
                                             @Param("dataFim") LocalDate dataFim);
}
