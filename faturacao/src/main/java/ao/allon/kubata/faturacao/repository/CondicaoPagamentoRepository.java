package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.CondicaoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CondicaoPagamentoRepository extends JpaRepository<CondicaoPagamento, Long> {

    List<CondicaoPagamento> findByAtivaTrueOrderByPrazoDiasAsc();

    Optional<CondicaoPagamento> findByCodigo(String codigo);
}
