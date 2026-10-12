package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentoBancarioRepository extends JpaRepository<MovimentoBancario, Long> {

    List<MovimentoBancario> findByConta_IdOrderByDataMovimentoDesc(Long contaId);

    List<MovimentoBancario> findByConta_IdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
            Long contaId, LocalDateTime inicio, LocalDateTime fim);
}
