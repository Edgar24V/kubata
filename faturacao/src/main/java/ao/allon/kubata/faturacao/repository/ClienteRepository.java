package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNif(String nif);
    boolean existsByNif(String nif);
}
