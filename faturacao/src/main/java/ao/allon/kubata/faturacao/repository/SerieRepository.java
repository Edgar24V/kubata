package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SerieRepository extends JpaRepository<Serie, Long> {
    
    List<Serie> findByTipoDocumento(TipoDocumento tipoDocumento);
    
    List<Serie> findByAtivaTrue();
    
    Optional<Serie> findByTipoDocumentoAndPadraoTrue(TipoDocumento tipoDocumento);
    
    boolean existsByDesignacaoAndTipoDocumentoAndAno(String designacao, TipoDocumento tipoDocumento, Integer ano);
}
