package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SerieRepository extends JpaRepository<Serie, Long> {
    
    List<Serie> findByTipoDocumento(TipoDocumento tipoDocumento);
    
    List<Serie> findByAtivaTrue();
    
    Optional<Serie> findByTipoDocumentoAndPadraoTrue(TipoDocumento tipoDocumento);
    
    boolean existsByDesignacaoAndTipoDocumentoAndAno(String designacao, TipoDocumento tipoDocumento, Integer ano);

    /**
     * Incremento atómico do contador da série na base de dados.
     * Evita colisões entre processos/instâncias; o chamador deve estar numa transacção.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Serie s SET s.ultimoNumero = COALESCE(s.ultimoNumero, 0) + 1 "
            + "WHERE s.id = :id AND s.ativa = true AND s.ano = :ano")
    int incrementarNumeroAtomico(@Param("id") Long id, @Param("ano") Integer ano);
}
