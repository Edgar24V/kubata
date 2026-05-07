package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {
    Optional<ParametroSistema> findByEmpresa_IdAndChave(Long empresaId, String chave);
    Optional<ParametroSistema> findByChaveAndEmpresaIdIsNull(String chave);

    List<ParametroSistema> findAllByEmpresaIsNullOrderByGrupoAscChaveAsc();

    List<ParametroSistema> findAllByEmpresa_IdOrderByGrupoAscChaveAsc(Long empresaId);
}
