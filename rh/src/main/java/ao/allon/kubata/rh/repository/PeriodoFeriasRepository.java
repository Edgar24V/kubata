package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.PeriodoFerias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodoFeriasRepository extends JpaRepository<PeriodoFerias, Long> {

    List<PeriodoFerias> findByColaboradorIdOrderByAnoDesc(Long colaboradorId);

    List<PeriodoFerias> findByAnoOrderByColaboradorNomeCompleto(Integer ano);

    Optional<PeriodoFerias> findByColaboradorIdAndAno(Long colaboradorId, Integer ano);

    @Query("SELECT COUNT(p) FROM PeriodoFerias p WHERE p.ano = :ano")
    long countByAno(@Param("ano") Integer ano);

    @Query("SELECT p FROM PeriodoFerias p WHERE " +
           "(:colaboradorId IS NULL OR p.colaborador.id = :colaboradorId) AND " +
           "(:ano IS NULL OR p.ano = :ano) " +
           "ORDER BY p.ano DESC, p.colaborador.nomeCompleto")
    List<PeriodoFerias> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                      @Param("ano") Integer ano);

    @Query("SELECT p FROM PeriodoFerias p WHERE p.diasSaldo > 0 AND " +
           "p.ano = :ano " +
           "ORDER BY p.colaborador.nomeCompleto")
    List<PeriodoFerias> findComSaldoDisponivel(@Param("ano") Integer ano);

    @Query("SELECT p FROM PeriodoFerias p WHERE p.diasSaldo < 0 AND " +
           "p.ano = :ano " +
           "ORDER BY p.colaborador.nomeCompleto")
    List<PeriodoFerias> findComSaldoNegativo(@Param("ano") Integer ano);

    @Query("SELECT p FROM PeriodoFerias p WHERE p.ano = :ano AND " +
           "EXISTS (SELECT 1 FROM PedidoFerias pf WHERE pf.periodoFerias.id = p.id AND " +
           "pf.estado = 'APROVADO' AND " +
           "((pf.dataInicio BETWEEN :dataInicio AND :dataFim) OR " +
           "(pf.dataFim BETWEEN :dataInicio AND :dataFim))) " +
           "ORDER BY p.colaborador.nomeCompleto")
    List<PeriodoFerias> findComFeriasMarcadasNoPeriodo(@Param("ano") Integer ano,
                                                       @Param("dataInicio") LocalDate dataInicio,
                                                       @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p.ano, COUNT(p) as totalColaboradores, SUM(p.diasDireito) as totalDireito, " +
           "SUM(p.diasUsados) as totalUsados, SUM(p.diasSaldo) as totalSaldo " +
           "FROM PeriodoFerias p " +
           "GROUP BY p.ano " +
           "ORDER BY p.ano DESC")
    List<Object[]> getResumoPorAno();

    @Query("SELECT p FROM PeriodoFerias p WHERE p.ano = :ano AND " +
           "p.diasSaldo > 0 AND p.colaborador.estado = 'ATIVO' " +
           "ORDER BY p.diasSaldo DESC")
    List<PeriodoFerias> findAtivosComMaiorSaldo(@Param("ano") Integer ano);
}
