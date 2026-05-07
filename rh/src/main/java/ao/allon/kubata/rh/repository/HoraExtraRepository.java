package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.HoraExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HoraExtraRepository extends JpaRepository<HoraExtra, Long> {

    List<HoraExtra> findByColaboradorIdOrderByDataDesc(Long colaboradorId);

    List<HoraExtra> findByDataBetweenOrderByData(LocalDate dataInicio, LocalDate dataFim);

    List<HoraExtra> findByColaboradorIdAndDataBetweenOrderByData(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim);

    List<HoraExtra> findByEstadoOrderByDataDesc(HoraExtra.EstadoHoraExtra estado);

    List<HoraExtra> findByMotivoOrderByDataDesc(HoraExtra.MotivoHoraExtra motivo);

    @Query("SELECT COUNT(h) FROM HoraExtra h WHERE h.data BETWEEN :dataInicio AND :dataFim")
    long countByDataBetween(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);

    @Query("SELECT SUM(h.quantidadeHoras) FROM HoraExtra h WHERE h.colaborador.id = :colaboradorId AND " +
           "h.data BETWEEN :dataInicio AND :dataFim AND h.estado = 'APROVADO'")
    BigDecimal sumHorasAprovadasByColaboradorAndPeriodo(@Param("colaboradorId") Long colaboradorId,
                                                        @Param("dataInicio") LocalDate dataInicio,
                                                        @Param("dataFim") LocalDate dataFim);

    @Query("SELECT SUM(h.quantidadeHoras) FROM HoraExtra h WHERE " +
           "h.data BETWEEN :dataInicio AND :dataFim AND h.estado = 'APROVADO'")
    BigDecimal sumHorasAprovadasByPeriodo(@Param("dataInicio") LocalDate dataInicio,
                                          @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM HoraExtra h WHERE " +
           "(:colaboradorId IS NULL OR h.colaborador.id = :colaboradorId) AND " +
           "(:dataInicio IS NULL OR h.data >= :dataInicio) AND " +
           "(:dataFim IS NULL OR h.data <= :dataFim) AND " +
           "(:motivo IS NULL OR h.motivo = :motivo) AND " +
           "(:estado IS NULL OR h.estado = :estado) " +
           "ORDER BY h.data DESC")
    List<HoraExtra> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                  @Param("dataInicio") LocalDate dataInicio,
                                  @Param("dataFim") LocalDate dataFim,
                                  @Param("motivo") HoraExtra.MotivoHoraExtra motivo,
                                  @Param("estado") HoraExtra.EstadoHoraExtra estado);

    @Query("SELECT h FROM HoraExtra h WHERE h.estado = 'PENDENTE' " +
           "AND h.data BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY h.data DESC")
    List<HoraExtra> findHorasPendentesAprovacao(@Param("dataInicio") LocalDate dataInicio,
                                                @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h.motivo, COUNT(h) as total, SUM(h.quantidadeHoras) as horas FROM HoraExtra h WHERE " +
           "h.data BETWEEN :dataInicio AND :dataFim AND h.estado = 'APROVADO' " +
           "GROUP BY h.motivo " +
           "ORDER BY horas DESC")
    List<Object[]> countHorasPorMotivo(@Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h.colaborador.id, h.colaborador.nomeCompleto, COUNT(h) as total, SUM(h.quantidadeHoras) as horas " +
           "FROM HoraExtra h WHERE h.data BETWEEN :dataInicio AND :dataFim AND h.estado = 'APROVADO' " +
           "GROUP BY h.colaborador.id, h.colaborador.nomeCompleto " +
           "ORDER BY horas DESC")
    List<Object[]> countHorasPorColaborador(@Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(h) FROM HoraExtra h WHERE h.estado = 'PENDENTE'")
    long countPendentesAprovacao();

    @Query("SELECT h FROM HoraExtra h WHERE h.colaborador.id = :colaboradorId AND " +
           "FUNCTION('month', h.data) = :mes AND FUNCTION('year', h.data) = :ano AND h.estado = 'APROVADO'")
    List<HoraExtra> findByColaboradorAndMesAno(@Param("colaboradorId") Long colaboradorId,
                                              @Param("mes") int mes,
                                              @Param("ano") int ano);
}
