package ao.allon.kubata.compras.repository;

import ao.allon.kubata.compras.domain.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByNif(String nif);

    List<Fornecedor> findByNomeContainingIgnoreCase(String nome);

    boolean existsByNif(String nif);
}
