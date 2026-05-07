package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.StatusDespesa;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByFornecedor(Fornecedor fornecedor);
    List<Despesa> findByStatus(StatusDespesa status);
    List<Despesa> findByDataEmissaoBetween(LocalDate inicio, LocalDate fim);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.status = :status")
    BigDecimal sumValorByStatus(@Param("status") StatusDespesa status);

    @Query("SELECT SUM(d.valorPago) FROM Despesa d WHERE d.dataEmissao BETWEEN :inicio AND :fim")
    BigDecimal sumPagoPorPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}

