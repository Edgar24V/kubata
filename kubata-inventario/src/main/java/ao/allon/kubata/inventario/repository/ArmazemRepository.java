package ao.allon.kubata.inventario.repository;

import ao.allon.kubata.inventario.domain.Armazem;
import ao.allon.kubata.inventario.view.ArmazemLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArmazemRepository extends JpaRepository<Armazem, Long> {

    Optional<Armazem> findByIsPrincipalTrue();

    List<Armazem> findByIsPrincipalFalse();

    boolean existsByNome(String nome);

    @Query("""
            SELECT new ao.allon.kubata.inventario.view.ArmazemLinha(
                a.id, a.nome, a.provincia, a.municipio, a.responsavel, a.telefone, a.isPrincipal)
            FROM Armazem a
            WHERE LOWER(a.nome) LIKE :termo OR LOWER(a.municipio) LIKE :termo OR LOWER(a.responsavel) LIKE :termo
            """)
    Page<ArmazemLinha> pesquisarLinhas(@Param("termo") String termo, Pageable pageable);
}
