package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Armazem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArmazemRepository extends JpaRepository<Armazem, Long> {
    Optional<Armazem> findByNome(String nome);
    Optional<Armazem> findByIsPrincipalTrue();
}
