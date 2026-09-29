package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SerieDocumentoRepository extends JpaRepository<SerieDocumento, Long> {
    List<SerieDocumento> findByTipoDocumento(TipoDocumentoSAFT tipo);
    List<SerieDocumento> findByTipoDocumentoAndEstado(
        TipoDocumentoSAFT tipo, SerieDocumento.EstadoSerie estado);
    Optional<SerieDocumento> findByTipoDocumentoAndPredefinidaTrue(TipoDocumentoSAFT tipo);
    List<SerieDocumento> findByEmpresaId(Long empresaId);
    Optional<SerieDocumento> findByEmpresaIdAndTipoDocumentoAndSerieAndExercicio(
        Long empresaId, TipoDocumentoSAFT tipo, String serie, Integer exercicio);
    @org.springframework.data.jpa.repository.Query("""
        select s
        from SerieDocumento s
        join fetch s.empresa
        order by s.exercicio desc, s.tipoDocumento asc, s.serie asc
        """)
    List<SerieDocumento> findAllWithEmpresa();
}
