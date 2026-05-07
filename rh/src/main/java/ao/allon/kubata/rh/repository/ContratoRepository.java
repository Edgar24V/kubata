package ao.allon.kubata.rh.repository;

import ao.allon.kubata.rh.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    List<Contrato> findByColaboradorIdOrderByDataInicioDesc(Long colaboradorId);

    List<Contrato> findBySituacaoOrderByDataInicioDesc(Contrato.SituacaoContrato situacao);

    List<Contrato> findByTipoContratoOrderByDataInicioDesc(Contrato.TipoContrato tipoContrato);

    Optional<Contrato> findFirstByColaboradorIdOrderByDataInicioDesc(Long colaboradorId);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.situacao = 'ATIVO'")
    long countBySituacaoAtivo();

    @Query("SELECT c FROM Contrato c WHERE " +
           "(:colaboradorId IS NULL OR c.colaborador.id = :colaboradorId) AND " +
           "(:tipoContrato IS NULL OR c.tipoContrato = :tipoContrato) AND " +
           "(:situacao IS NULL OR c.situacao = :situacao) AND " +
           "(:dataInicio IS NULL OR c.dataInicio >= :dataInicio) AND " +
           "(:dataFim IS NULL OR c.dataFim <= :dataFim) " +
           "ORDER BY c.dataInicio DESC")
    List<Contrato> findByFiltros(@Param("colaboradorId") Long colaboradorId,
                                 @Param("tipoContrato") Contrato.TipoContrato tipoContrato,
                                 @Param("situacao") Contrato.SituacaoContrato situacao,
                                 @Param("dataInicio") LocalDate dataInicio,
                                 @Param("dataFim") LocalDate dataFim);

    @Query("SELECT c FROM Contrato c WHERE c.situacao = 'ATIVO' AND " +
           "c.dataFim IS NOT NULL AND c.dataFim BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY c.dataFim ASC")
    List<Contrato> findContratosAExpirar(@Param("dataInicio") LocalDate dataInicio,
                                         @Param("dataFim") LocalDate dataFim);

    @Query("SELECT c FROM Contrato c WHERE c.situacao = 'ATIVO' AND " +
           "c.dataFim IS NOT NULL AND c.dataFim < :dataAtual " +
           "ORDER BY c.dataFim ASC")
    List<Contrato> findContratosExpirados(@Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT c FROM Contrato c WHERE c.colaborador.id = :colaboradorId AND " +
           "c.situacao = 'ATIVO' AND " +
           "c.dataInicio <= :data AND (c.dataFim IS NULL OR c.dataFim >= :data)")
    Optional<Contrato> findContratoVigente(@Param("colaboradorId") Long colaboradorId,
                                           @Param("data") LocalDate data);

    @Query("SELECT COUNT(c) FROM Contrato c WHERE c.tipoContrato = :tipoContrato AND c.situacao = 'ATIVO'")
    long countByTipoContratoAndSituacaoAtivo(@Param("tipoContrato") Contrato.TipoContrato tipoContrato);
}
