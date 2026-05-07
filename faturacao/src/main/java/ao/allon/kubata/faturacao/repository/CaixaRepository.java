package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Caixa;
import ao.allon.kubata.faturacao.domain.enums.StatusCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaixaRepository extends JpaRepository<Caixa, Long> {
    Optional<Caixa> findTopByStatusOrderByIdDesc(StatusCaixa status);
    Optional<Caixa> findTopByUsuarioAndStatusOrderByIdDesc(String usuario, StatusCaixa status);
}
