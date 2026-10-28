package ao.allon.kubata.vendas.repository;

import ao.allon.kubata.vendas.domain.Cliente;
import ao.allon.kubata.vendas.view.ClienteLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNif(String nif);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    boolean existsByNif(String nif);

    @Query("""
            SELECT new ao.allon.kubata.vendas.view.ClienteLinha(
                c.id, c.nome, c.nif, c.tipo, c.telefone, c.email, c.cidade, c.limiteCredito)
            FROM Cliente c
            WHERE LOWER(c.nome) LIKE :termo OR LOWER(c.nif) LIKE :termo OR LOWER(c.telefone) LIKE :termo
            """)
    Page<ClienteLinha> pesquisarLinhas(@Param("termo") String termo, Pageable pageable);
}
