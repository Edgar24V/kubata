package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    Optional<Fornecedor> findByNif(String nif);
    boolean existsByNif(String nif);
    boolean existsByNome(String nome);
}
