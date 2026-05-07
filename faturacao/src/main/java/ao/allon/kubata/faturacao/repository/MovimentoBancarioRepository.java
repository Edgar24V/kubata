package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.MovimentoBancario;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimentoBancario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentoBancarioRepository extends JpaRepository<MovimentoBancario, Long> {
    List<MovimentoBancario> findByContaOrderByDataMovimentoDesc(ContaBancaria conta);
    List<MovimentoBancario> findByContaAndDataMovimentoBetween(ContaBancaria conta, LocalDateTime inicio, LocalDateTime fim);
    
    @Query("SELECT SUM(m.valor) FROM MovimentoBancario m WHERE m.conta = :conta AND m.tipo = :tipo AND m.dataMovimento BETWEEN :inicio AND :fim")
    BigDecimal sumValorByContaAndTipoAndPeriodo(@Param("conta") ContaBancaria conta, @Param("tipo") TipoMovimentoBancario tipo, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
