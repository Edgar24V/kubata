package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.faturacao.domain.ReconciliacaoBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para reconciliações bancárias.
 */
@Repository
public interface ReconciliacaoBancariaRepository extends JpaRepository<ReconciliacaoBancaria, Long> {

    /**
     * Busca reconciliação por conta bancária e data.
     */
    Optional<ReconciliacaoBancaria> findByContaBancariaAndDataExtrato(PlanoConta contaBancaria, LocalDate dataExtrato);

    /**
     * Busca todas as reconciliações de uma conta bancária.
     */
    List<ReconciliacaoBancaria> findByContaBancariaOrderByDataExtratoDesc(PlanoConta contaBancaria);

    /**
     * Busca reconciliações por status.
     */
    List<ReconciliacaoBancaria> findByStatusOrderByDataExtratoDesc(ReconciliacaoBancaria.StatusReconciliacao status);

    /**
     * Busca reconciliações por período.
     */
    @Query("SELECT r FROM ReconciliacaoBancaria r WHERE r.contaBancaria = :conta " +
           "AND r.dataExtrato BETWEEN :dataInicio AND :dataFim ORDER BY r.dataExtrato DESC")
    List<ReconciliacaoBancaria> findByContaAndPeriodo(@Param("conta") PlanoConta conta,
                                                       @Param("dataInicio") LocalDate dataInicio,
                                                       @Param("dataFim") LocalDate dataFim);

    /**
     * Verifica se existe reconciliação para a conta e data.
     */
    boolean existsByContaBancariaAndDataExtrato(PlanoConta contaBancaria, LocalDate dataExtrato);

    /**
     * Busca última reconciliação conciliada de uma conta.
     */
    @Query("SELECT r FROM ReconciliacaoBancaria r WHERE r.contaBancaria = :conta " +
           "AND r.status = 'CONCILIADO' ORDER BY r.dataExtrato DESC")
    List<ReconciliacaoBancaria> findUltimaConciliacao(@Param("conta") PlanoConta conta);

    /**
     * Conta reconciliações pendentes.
     */
    Long countByStatus(ReconciliacaoBancaria.StatusReconciliacao status);

    /**
     * Busca reconciliações com divergências.
     */
    @Query("SELECT r FROM ReconciliacaoBancaria r WHERE r.diferenca IS NOT NULL AND r.diferenca != 0 ORDER BY r.dataExtrato DESC")
    List<ReconciliacaoBancaria> findComDivergencias();
}
