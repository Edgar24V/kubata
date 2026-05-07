package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.MovimentoExtrato;
import ao.allon.kubata.faturacao.domain.ReconciliacaoBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para movimentos de extrato bancário.
 */
@Repository
public interface MovimentoExtratoRepository extends JpaRepository<MovimentoExtrato, Long> {

    /**
     * Busca todos os movimentos de uma reconciliação.
     */
    List<MovimentoExtrato> findByReconciliacaoOrderByDataMovimentoAsc(ReconciliacaoBancaria reconciliacao);

    /**
     * Busca movimentos não conciliados de uma reconciliação.
     */
    @Query("SELECT m FROM MovimentoExtrato m WHERE m.reconciliacao = :reconciliacao AND m.conciliado = false ORDER BY m.dataMovimento")
    List<MovimentoExtrato> findNaoConciliadosByReconciliacao(@Param("reconciliacao") ReconciliacaoBancaria reconciliacao);

    /**
     * Busca movimentos conciliados de uma reconciliação.
     */
    List<MovimentoExtrato> findByReconciliacaoAndConciliadoTrueOrderByDataConciliacaoDesc(ReconciliacaoBancaria reconciliacao);

    /**
     * Soma o valor total dos movimentos de débito.
     */
    @Query("SELECT SUM(m.valor) FROM MovimentoExtrato m WHERE m.reconciliacao = :reconciliacao AND m.tipo = 'DEBITO'")
    BigDecimal sumDebitosByReconciliacao(@Param("reconciliacao") ReconciliacaoBancaria reconciliacao);

    /**
     * Soma o valor total dos movimentos de crédito.
     */
    @Query("SELECT SUM(m.valor) FROM MovimentoExtrato m WHERE m.reconciliacao = :reconciliacao AND m.tipo = 'CREDITO'")
    BigDecimal sumCreditosByReconciliacao(@Param("reconciliacao") ReconciliacaoBancaria reconciliacao);

    /**
     * Conta movimentos por reconciliação.
     */
    Long countByReconciliacao(ReconciliacaoBancaria reconciliacao);

    /**
     * Conta movimentos conciliados por reconciliação.
     */
    Long countByReconciliacaoAndConciliadoTrue(ReconciliacaoBancaria reconciliacao);

    /**
     * Busca movimento por referência.
     */
    Optional<MovimentoExtrato> findByReconciliacaoAndReferencia(ReconciliacaoBancaria reconciliacao, String referencia);

    /**
     * Busca movimentos por período e descrição similar (para matching automático).
     */
    @Query("SELECT m FROM MovimentoExtrato m WHERE m.reconciliacao = :reconciliacao " +
           "AND m.dataMovimento BETWEEN :dataInicio AND :dataFim " +
           "AND m.valor BETWEEN :valorMin AND :valorMax " +
           "AND m.conciliado = false")
    List<MovimentoExtrato> findPossiveisMatching(@Param("reconciliacao") ReconciliacaoBancaria reconciliacao,
                                                  @Param("dataInicio") LocalDate dataInicio,
                                                  @Param("dataFim") LocalDate dataFim,
                                                  @Param("valorMin") BigDecimal valorMin,
                                                  @Param("valorMax") BigDecimal valorMax);
}
