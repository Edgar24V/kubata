package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LancamentoContabilRepository extends JpaRepository<LancamentoContabil, Long> {

    List<LancamentoContabil> findByDataMovimentoBetween(LocalDate inicio, LocalDate fim);

    List<LancamentoContabil> findByContaDebitoOrContaCredito(PlanoConta contaDebito, PlanoConta contaCredito);

    @Query("SELECT SUM(l.valor) FROM LancamentoContabil l WHERE l.contaDebito = :conta AND l.dataMovimento <= :data")
    BigDecimal sumDebitosAteData(@Param("conta") PlanoConta conta, @Param("data") LocalDate data);

    @Query("SELECT SUM(l.valor) FROM LancamentoContabil l WHERE l.contaCredito = :conta AND l.dataMovimento <= :data")
    BigDecimal sumCreditosAteData(@Param("conta") PlanoConta conta, @Param("data") LocalDate data);

    @Query("SELECT l.contaDebito.id, SUM(l.valor) FROM LancamentoContabil l WHERE l.dataMovimento <= :data GROUP BY l.contaDebito.id")
    List<Object[]> sumDebitosPorConta(@Param("data") LocalDate data);

    @Query("SELECT l.contaCredito.id, SUM(l.valor) FROM LancamentoContabil l WHERE l.dataMovimento <= :data GROUP BY l.contaCredito.id")
    List<Object[]> sumCreditosPorConta(@Param("data") LocalDate data);

    @Query("SELECT l FROM LancamentoContabil l WHERE (l.contaDebito = :conta OR l.contaCredito = :conta) AND l.dataMovimento BETWEEN :inicio AND :fim ORDER BY l.dataMovimento")
    List<LancamentoContabil> findExtratoConta(@Param("conta") PlanoConta conta, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
