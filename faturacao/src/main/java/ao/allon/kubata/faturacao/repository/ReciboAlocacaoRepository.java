package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.ReciboAlocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReciboAlocacaoRepository extends JpaRepository<ReciboAlocacao, Long> {

    List<ReciboAlocacao> findByReciboId(Long reciboId);

    List<ReciboAlocacao> findByFaturaId(Long faturaId);

    @Query("SELECT COALESCE(SUM(a.valor), 0) FROM ReciboAlocacao a WHERE a.fatura.id = :faturaId")
    BigDecimal sumValorByFaturaId(Long faturaId);

    @Query("SELECT COALESCE(SUM(a.valor), 0) FROM ReciboAlocacao a WHERE a.recibo.id = :reciboId")
    BigDecimal sumValorByReciboId(Long reciboId);

    @Query("SELECT a FROM ReciboAlocacao a WHERE a.fatura.id IN :faturaIds")
    List<ReciboAlocacao> findByFaturaIds(List<Long> faturaIds);
}
