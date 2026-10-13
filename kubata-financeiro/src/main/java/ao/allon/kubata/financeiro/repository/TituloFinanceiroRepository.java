package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.TituloFinanceiro;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TituloFinanceiroRepository extends JpaRepository<TituloFinanceiro, Long> {
    List<TituloFinanceiro> findByTipoOrderByDataVencimentoAsc(TipoTituloFinanceiro tipo);
    Optional<TituloFinanceiro> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}
