package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.Caixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaixaRepository extends JpaRepository<Caixa, Long> {

    Optional<Caixa> findByIsPrincipalTrue();

    List<Caixa> findByIsAtivoTrue();

    boolean existsByNome(String nome);
}
