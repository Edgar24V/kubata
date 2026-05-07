package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.RegistoPonto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistoPontoRepository extends JpaRepository<RegistoPonto, Long> {

    List<RegistoPonto> findByColaboradorIdOrderByDataDesc(Long colaboradorId);

    List<RegistoPonto> findByDataBetweenOrderByData(LocalDate dataInicio, LocalDate dataFim);

    List<RegistoPonto> findByColaboradorIdAndDataBetweenOrderByData(Long colaboradorId, LocalDate dataInicio, LocalDate dataFim);

    Optional<RegistoPonto> findByColaboradorIdAndData(Long colaboradorId, LocalDate data);

    @Query("SELECT COUNT(r) FROM RegistoPonto r WHERE r.data BETWEEN :dataInicio AND :dataFim")
    long countByDataBetween(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);

    @Query("SELECT r FROM RegistoPonto r WHERE " +
           "(:colaboradorId IS NULL OR r.colaborador.id = :colaboradorId) AND " +
           "(:dataInicio IS NULL OR r.data >= :dataInicio) AND " +
           "(:dataFim IS NULL OR r.data <= :dataFim) AND " +
           "(:origem IS NULL OR r.origem = :origem) " +
           "ORDER BY r.data DESC, r.colaborador.nomeCompleto")
    List<RegistoPonto> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                     @Param("dataInicio") LocalDate dataInicio,
                                     @Param("dataFim") LocalDate dataFim,
                                     @Param("origem") RegistoPonto.OrigemPonto origem);

    @Query("SELECT r FROM RegistoPonto r WHERE r.horaEntrada IS NULL OR r.horaSaida IS NULL " +
           "AND r.data BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY r.data DESC")
    List<RegistoPonto> findRegistrosIncompletos(@Param("dataInicio") LocalDate dataInicio,
                                                @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(r) FROM RegistoPonto r WHERE r.colaborador.id = :colaboradorId AND " +
           "r.data BETWEEN :dataInicio AND :dataFim")
    long countByColaboradorAndDataBetween(@Param("colaboradorId") Long colaboradorId,
                                          @Param("dataInicio") LocalDate dataInicio,
                                          @Param("dataFim") LocalDate dataFim);

    @Query("SELECT r FROM RegistoPonto r WHERE r.aprovadoPor IS NULL " +
           "AND r.data BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY r.data DESC")
    List<RegistoPonto> findRegistrosPendentesAprovacao(@Param("dataInicio") LocalDate dataInicio,
                                                       @Param("dataFim") LocalDate dataFim);

    @Query(value = "SELECT r.data, COUNT(*) FROM rh_registos_ponto r " +
           "WHERE r.data BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY r.data ORDER BY r.data", nativeQuery = true)
    List<Object[]> countRegistrosPorDia(@Param("dataInicio") LocalDate dataInicio,
                                        @Param("dataFim") LocalDate dataFim);
}
