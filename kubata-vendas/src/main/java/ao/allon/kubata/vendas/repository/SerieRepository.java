package ao.allon.kubata.vendas.repository;

import ao.allon.kubata.vendas.domain.Serie;
import ao.allon.kubata.vendas.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SerieRepository extends JpaRepository<Serie, Long> {

    List<Serie> findByTipoDocumento(TipoDocumento tipoDocumento);

    Optional<Serie> findByTipoDocumentoAndPadraoTrue(TipoDocumento tipoDocumento);

    List<Serie> findByAno(Integer ano);

    boolean existsByDesignacaoAndTipoDocumento(String designacao, TipoDocumento tipoDocumento);
}
