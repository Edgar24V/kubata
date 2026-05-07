package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    List<Departamento> findByActiveTrueOrderByNome();

    List<Departamento> findByActiveFalseOrderByNome();

    List<Departamento> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    Optional<Departamento> findBySiglaIgnoreCase(String sigla);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsBySiglaIgnoreCase(String sigla);

    @Query("SELECT COUNT(d) FROM Departamento d WHERE d.active = true")
    long countByActiveTrue();

    @Query("SELECT d FROM Departamento d WHERE " +
           "(:nome IS NULL OR LOWER(d.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:sigla IS NULL OR LOWER(d.sigla) LIKE LOWER(CONCAT('%', :sigla, '%'))) AND " +
           "(:ativo IS NULL OR d.active = :ativo) " +
           "ORDER BY d.nome")
    List<Departamento> findByFiltros(@Param("nome") String nome, 
                                     @Param("sigla") String sigla, 
                                     @Param("ativo") Boolean ativo);
}
