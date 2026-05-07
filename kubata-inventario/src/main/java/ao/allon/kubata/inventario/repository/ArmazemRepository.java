package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Armazem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArmazemRepository extends JpaRepository<Armazem, Long> {

    Optional<Armazem> findByIsPrincipalTrue();

    List<Armazem> findByIsPrincipalFalse();

    boolean existsByNome(String nome);
}
