package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdmPlataformaItemRepository extends JpaRepository<AdmPlataformaItem, Long> {
    List<AdmPlataformaItem> findByTipoOrderByUpdatedAtDesc(String tipo);
    Optional<AdmPlataformaItem> findByTipoAndCodigo(String tipo, String codigo);
    long countByTipo(String tipo);
    long countByTipoAndEstado(String tipo, String estado);
}