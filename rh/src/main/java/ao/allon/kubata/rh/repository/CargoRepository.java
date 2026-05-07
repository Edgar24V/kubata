package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {

    List<Cargo> findByActiveTrueOrderByNome();

    List<Cargo> findByActiveFalseOrderByNome();

    List<Cargo> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    List<Cargo> findByNivelContainingIgnoreCaseOrderByNivel(String nivel);

    boolean existsByNomeIgnoreCase(String nome);

    @Query("SELECT COUNT(c) FROM Cargo c WHERE c.active = true")
    long countByActiveTrue();

    @Query("SELECT c FROM Cargo c WHERE " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:nivel IS NULL OR LOWER(c.nivel) LIKE LOWER(CONCAT('%', :nivel, '%'))) AND " +
           "(:ativo IS NULL OR c.active = :ativo) " +
           "ORDER BY c.nome")
    List<Cargo> findByFiltros(@Param("nome") String nome, 
                              @Param("nivel") String nivel, 
                              @Param("ativo") Boolean ativo);
}
