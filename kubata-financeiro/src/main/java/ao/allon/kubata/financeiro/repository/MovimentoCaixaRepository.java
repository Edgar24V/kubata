package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentoCaixaRepository extends JpaRepository<MovimentoCaixa, Long> {

    List<MovimentoCaixa> findByCaixa_IdOrderByDataMovimentoDesc(Long caixaId);

    List<MovimentoCaixa> findByCaixa_IdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
            Long caixaId, LocalDateTime inicio, LocalDateTime fim);

    boolean existsByCaixa_Id(Long caixaId);
}
