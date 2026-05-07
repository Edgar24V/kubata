package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.PedidoFerias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PedidoFeriasRepository extends JpaRepository<PedidoFerias, Long> {

    List<PedidoFerias> findByColaboradorIdOrderByDataInicioDesc(Long colaboradorId);

    List<PedidoFerias> findByEstadoOrderByDataInicioDesc(PedidoFerias.EstadoPedido estado);

    List<PedidoFerias> findByDataInicioBetweenOrderByDataInicio(LocalDate dataInicio, LocalDate dataFim);

    List<PedidoFerias> findByColaboradorIdAndEstadoOrderByDataInicioDesc(Long colaboradorId, PedidoFerias.EstadoPedido estado);

    @Query("SELECT COUNT(p) FROM PedidoFerias p WHERE p.estado = 'PENDENTE'")
    long countPendentesAprovacao();

    @Query("SELECT p FROM PedidoFerias p WHERE " +
           "(:colaboradorId IS NULL OR p.colaborador.id = :colaboradorId) AND " +
           "(:dataInicio IS NULL OR p.dataInicio >= :dataInicio) AND " +
           "(:dataFim IS NULL OR p.dataFim <= :dataFim) AND " +
           "(:estado IS NULL OR p.estado = :estado) " +
           "ORDER BY p.dataInicio DESC")
    List<PedidoFerias> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                     @Param("dataInicio") LocalDate dataInicio,
                                     @Param("dataFim") LocalDate dataFim,
                                     @Param("estado") PedidoFerias.EstadoPedido estado);

    @Query("SELECT p FROM PedidoFerias p WHERE p.estado = 'PENDENTE' " +
           "ORDER BY p.dataInicio ASC")
    List<PedidoFerias> findPedidosPendentes();

    @Query("SELECT p FROM PedidoFerias p WHERE p.estado = 'PENDENTE' AND " +
           "p.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY p.dataInicio ASC")
    List<PedidoFerias> findPedidosPendentesNoPeriodo(@Param("dataInicio") LocalDate dataInicio,
                                                     @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p FROM PedidoFerias p WHERE " +
           "p.estado = 'APROVADO' AND " +
           "((p.dataInicio <= :data AND p.dataFim >= :data) OR " +
           "(p.dataInicio >= :dataInicio AND p.dataFim <= :dataFim)) " +
           "ORDER BY p.dataInicio")
    List<PedidoFerias> findFeriasMarcadasNoPeriodo(@Param("data") LocalDate data,
                                                   @Param("dataInicio") LocalDate dataInicio,
                                                   @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p FROM PedidoFerias p WHERE " +
           "p.estado = 'APROVADO' AND " +
           "(:colaboradorId IS NULL OR p.colaborador.id = :colaboradorId) AND " +
           "((:data BETWEEN p.dataInicio AND p.dataFim) OR " +
           "(p.dataInicio BETWEEN :dataInicio AND :dataFim) OR " +
           "(p.dataFim BETWEEN :dataInicio AND :dataFim)) " +
           "ORDER BY p.dataInicio")
    List<PedidoFerias> findFeriasConflitantes(@Param("data") LocalDate data,
                                              @Param("dataInicio") LocalDate dataInicio,
                                              @Param("dataFim") LocalDate dataFim,
                                              @Param("colaboradorId") Long colaboradorId);

    @Query("SELECT p FROM PedidoFerias p WHERE p.estado = 'APROVADO' AND " +
           "p.colaborador.departamento.id = :departamentoId AND " +
           "p.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY p.dataInicio")
    List<PedidoFerias> findFeriasPorDepartamento(@Param("departamentoId") Long departamentoId,
                                                 @Param("dataInicio") LocalDate dataInicio,
                                                 @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p.estado, COUNT(p) as total FROM PedidoFerias p WHERE " +
           "p.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY p.estado " +
           "ORDER BY total DESC")
    List<Object[]> countPedidosPorEstado(@Param("dataInicio") LocalDate dataInicio,
                                         @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p.colaborador.id, p.colaborador.nomeCompleto, COUNT(p) as total, SUM(p.quantidadeDias) as dias " +
           "FROM PedidoFerias p WHERE p.estado = 'APROVADO' AND " +
           "p.dataInicio BETWEEN :dataInicio AND :dataFim " +
           "GROUP BY p.colaborador.id, p.colaborador.nomeCompleto " +
           "ORDER BY dias DESC")
    List<Object[]> countFeriasPorColaborador(@Param("dataInicio") LocalDate dataInicio,
                                             @Param("dataFim") LocalDate dataFim);
}
