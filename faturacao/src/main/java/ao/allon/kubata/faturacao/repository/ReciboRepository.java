package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Recibo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReciboRepository extends JpaRepository<Recibo, Long> {

    List<Recibo> findByFaturaId(Long faturaId);

    @Query("SELECT SUM(r.valor) FROM Recibo r WHERE r.fatura.id = :faturaId")
    BigDecimal sumValorByFaturaId(Long faturaId);

    @Query("SELECT r FROM Recibo r WHERE r.dataRecebimento BETWEEN :inicio AND :fim")
    List<Recibo> findByDataRecebimentoBetween(LocalDate inicio, LocalDate fim);
}
