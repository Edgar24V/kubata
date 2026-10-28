package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.view.CategoriaLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNome(String nome);

    boolean existsByNome(String nome);

    @Query("""
            SELECT new ao.allon.kubata.inventario.view.CategoriaLinha(c.id, c.nome, c.descricao)
            FROM Categoria c
            WHERE LOWER(c.nome) LIKE :termo OR LOWER(c.descricao) LIKE :termo
            """)
    Page<CategoriaLinha> pesquisarLinhas(@Param("termo") String termo, Pageable pageable);
}
