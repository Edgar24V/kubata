package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.Fornecedor;
import ao.allon.kubata.core.projection.FornecedorLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByNif(String nif);

    List<Fornecedor> findByNomeContainingIgnoreCase(String nome);

    boolean existsByNif(String nif);

    boolean existsByNome(String nome);

    @Query("""
            SELECT new ao.allon.kubata.core.projection.FornecedorLinha(
                f.id, f.nome, f.nif, f.telefone, f.email, f.cidade, f.pessoaContacto,
                f.termosPagamento, f.avaliacao)
            FROM Fornecedor f
            WHERE LOWER(f.nome) LIKE :termo OR LOWER(f.nif) LIKE :termo OR LOWER(f.telefone) LIKE :termo
            """)
    Page<FornecedorLinha> pesquisarLinhas(@Param("termo") String termo, Pageable pageable);
}
