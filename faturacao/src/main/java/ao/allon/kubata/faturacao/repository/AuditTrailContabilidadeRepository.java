package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.AuditTrailContabilidade;
import ao.allon.kubata.core.domain.LancamentoContabil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para trilha de auditoria contábil.
 */
@Repository
public interface AuditTrailContabilidadeRepository extends JpaRepository<AuditTrailContabilidade, Long> {

    /**
     * Busca todo o histórico de auditoria de um lançamento específico.
     */
    List<AuditTrailContabilidade> findByLancamentoOrderByDataOperacaoDesc(LancamentoContabil lancamento);

    /**
     * Busca todo o histórico de auditoria de um lançamento pelo ID.
     */
    List<AuditTrailContabilidade> findByLancamentoIdOrderByDataOperacaoDesc(Long lancamentoId);

    /**
     * Busca auditoria por tipo de operação.
     */
    List<AuditTrailContabilidade> findByTipoOperacaoOrderByDataOperacaoDesc(
            AuditTrailContabilidade.TipoOperacao tipoOperacao);

    /**
     * Busca auditoria por utilizador.
     */
    List<AuditTrailContabilidade> findByUsuarioIdOrderByDataOperacaoDesc(Long usuarioId);

    /**
     * Busca auditoria por período.
     */
    @Query("SELECT a FROM AuditTrailContabilidade a WHERE a.dataOperacao BETWEEN :inicio AND :fim ORDER BY a.dataOperacao DESC")
    List<AuditTrailContabilidade> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /**
     * Busca lançamentos que foram alterados (não apenas criados).
     */
    @Query("SELECT DISTINCT a.lancamento.id FROM AuditTrailContabilidade a WHERE a.tipoOperacao = 'UPDATE'")
    List<Long> findLancamentosAlterados();

    /**
     * Busca lançamentos anulados.
     */
    @Query("SELECT a FROM AuditTrailContabilidade a WHERE a.anulado = true ORDER BY a.dataAnulacao DESC")
    List<AuditTrailContabilidade> findLancamentosAnulados();

    /**
     * Conta operações por tipo em um período.
     */
    @Query("SELECT a.tipoOperacao, COUNT(a) FROM AuditTrailContabilidade a WHERE a.dataOperacao BETWEEN :inicio AND :fim GROUP BY a.tipoOperacao")
    List<Object[]> countOperacoesPorTipo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
