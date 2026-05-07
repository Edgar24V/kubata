package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Licenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LicencaRepository extends JpaRepository<Licenca, Long> {

    List<Licenca> findByColaboradorIdOrderByDataInicioDesc(Long colaboradorId);

    List<Licenca> findByTipoOrderByDataInicioDesc(Licenca.TipoLicenca tipo);

    List<Licenca> findByEstadoOrderByDataInicioDesc(Licenca.EstadoLicenca estado);

    List<Licenca> findByDataInicioBetweenOrderByDataInicio(LocalDate dataInicio, LocalDate dataFim);

    @Query("SELECT COUNT(l) FROM Licenca l WHERE l.estado = 'PENDENTE'")
    long countPendentesAprovacao();

    @Query("SELECT l FROM Licenca l WHERE " +
           "(:colaboradorId IS NULL OR l.colaborador.id = :colaboradorId) AND " +
           "(:tipo IS NULL OR l.tipo = :tipo) AND " +
           "(:estado IS NULL OR l.estado = :estado) AND " +
           "(:dataInicio IS NULL OR l.dataInicio >= :dataInicio) AND " +
           "(:dataFim IS NULL OR l.dataFim <= :dataFim) " +
           "ORDER BY l.dataInicio DESC")
    List<Licenca> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                 @Param("tipo") Licenca.TipoLicenca tipo,
                                 @Param("estado") Licenca.EstadoLicenca estado,
                                 @Param("dataInicio") LocalDate dataInicio,
                                 @Param("dataFim") LocalDate dataFim);

    @Query("SELECT l FROM Licenca l WHERE l.estado = 'PENDENTE' " +
           "ORDER BY l.dataInicio ASC")
    List<Licenca> findLicencasPendentes();

    @Query("SELECT l FROM Licenca l WHERE l.estado = 'APROVADA' AND " +
           "(:colaboradorId IS NULL OR l.colaborador.id = :colaboradorId) AND " +
           "((:data BETWEEN l.dataInicio AND l.dataFim) OR " +
           "(l.dataInicio BETWEEN :dataInicio AND :dataFim) OR " +
           "(l.dataFim BETWEEN :dataInicio AND :dataFim)) " +
           "ORDER BY l.dataInicio")
    List<Licenca> findLicencasConflitantes(@Param("data") LocalDate data,
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim,
                                           @Param("colaboradorId") Long colaboradorId);

    @Query("SELECT l FROM Licenca l WHERE l.estado = 'APROVADA' AND " +
           "l.colaborador.departamento.id = :departamentoId AND " +
           "l.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY l.dataInicio")
    List<Licenca> findLicencasPorDepartamento(@Param("departamentoId") Long departamentoId,
                                              @Param("dataInicio") LocalDate dataInicio,
                                              @Param("dataFim") LocalDate dataFim);

    @Query("SELECT l.tipo, COUNT(l) as total FROM Licenca l WHERE " +
           "l.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY l.tipo " +
           "ORDER BY total DESC")
    List<Object[]> countLicencasPorTipo(@Param("dataInicio") LocalDate dataInicio,
                                        @Param("dataFim") LocalDate dataFim);

    @Query("SELECT l.estado, COUNT(l) as total FROM Licenca l WHERE " +
           "l.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY l.estado " +
           "ORDER BY total DESC")
    List<Object[]> countLicencasPorEstado(@Param("dataInicio") LocalDate dataInicio,
                                          @Param("dataFim") LocalDate dataFim);

    @Query("SELECT l.colaborador.id, l.colaborador.nomeCompleto, COUNT(l) as total, SUM(l.quantidadeDias) as dias " +
           "FROM Licenca l WHERE l.estado = 'APROVADA' AND " +
           "l.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY l.colaborador.id, l.colaborador.nomeCompleto " +
           "ORDER BY dias DESC")
    List<Object[]> countLicencasPorColaborador(@Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    @Query("SELECT l FROM Licenca l WHERE l.dataFim IS NOT NULL AND " +
           "l.dataFim BETWEEN :dataInicio AND :dataFim AND l.estado = 'APROVADA' " +
           "ORDER BY l.dataFim ASC")
    List<Licenca> findLicencasAExpirar(@Param("dataInicio") LocalDate dataInicio,
                                        @Param("dataFim") LocalDate dataFim);
}
