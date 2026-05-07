package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.RegraDesconto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraDescontoRepository extends JpaRepository<RegraDesconto, Long> {
    List<RegraDesconto> findByActiveTrue();
}
