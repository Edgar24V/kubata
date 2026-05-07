package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentoCaixaRepository extends JpaRepository<MovimentoCaixa, Long> {
    List<MovimentoCaixa> findByCaixaId(Long caixaId);
}
