package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

    @Query("SELECT c FROM Colaborador c LEFT JOIN FETCH c.departamento LEFT JOIN FETCH c.cargo ORDER BY c.nomeCompleto")
    List<Colaborador> findAllWithDetails();

    List<Colaborador> findByEstadoOrderByNomeCompleto(Colaborador.EstadoColaborador estado);

    List<Colaborador> findByDepartamentoIdOrderByNomeCompleto(Long departamentoId);

    List<Colaborador> findByCargoIdOrderByNomeCompleto(Long cargoId);

    List<Colaborador> findByNomeCompletoContainingIgnoreCaseOrderByNomeCompleto(String nome);

    Optional<Colaborador> findByBiIgnoreCase(String bi);

    Optional<Colaborador> findByNifIgnoreCase(String nif);

    Optional<Colaborador> findByNumeroMecanograficoIgnoreCase(String numeroMecanografico);

    boolean existsByBiIgnoreCase(String bi);

    boolean existsByNifIgnoreCase(String nif);

    boolean existsByNumeroMecanograficoIgnoreCase(String numeroMecanografico);

    @Query("SELECT COUNT(c) FROM Colaborador c WHERE c.estado = 'ATIVO'")
    long countByEstadoAtivo();

    @Query("SELECT c FROM Colaborador c LEFT JOIN FETCH c.departamento LEFT JOIN FETCH c.cargo WHERE " +
           "(:nome IS NULL OR LOWER(c.nomeCompleto) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:bi IS NULL OR LOWER(c.bi) LIKE LOWER(CONCAT('%', :bi, '%'))) AND " +
           "(:nif IS NULL OR LOWER(c.nif) LIKE LOWER(CONCAT('%', :nif, '%'))) AND " +
           "(:departamentoId IS NULL OR c.departamento.id = :departamentoId) AND " +
           "(:cargoId IS NULL OR c.cargo.id = :cargoId) AND " +
           "(:estado IS NULL OR c.estado = :estado) " +
           "ORDER BY c.nomeCompleto")
    List<Colaborador> findByFiltros(@Param("nome") String nome,
                                    @Param("bi") String bi,
                                    @Param("nif") String nif,
                                    @Param("departamentoId") Long departamentoId,
                                    @Param("cargoId") Long cargoId,
                                    @Param("estado") Colaborador.EstadoColaborador estado);

    @Query("SELECT c FROM Colaborador c WHERE c.estado = 'ATIVO' AND " +
           "EXISTS (SELECT 1 FROM Contrato co WHERE co.colaborador.id = c.id AND co.situacao = 'ATIVO' AND " +
           "co.dataFim IS NOT NULL AND co.dataFim BETWEEN :dataInicio AND :dataFim)")
    List<Colaborador> findContratosAExpirar(@Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(c) FROM Colaborador c WHERE c.departamento.id = :departamentoId AND c.estado = 'ATIVO'")
    long countByDepartamentoAndEstadoAtivo(@Param("departamentoId") Long departamentoId);

    @Query("SELECT COUNT(c) FROM Colaborador c WHERE c.cargo.id = :cargoId AND c.estado = 'ATIVO'")
    long countByCargoAndEstadoAtivo(@Param("cargoId") Long cargoId);
}
