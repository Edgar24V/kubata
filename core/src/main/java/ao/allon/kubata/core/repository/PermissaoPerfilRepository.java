package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PermissaoPerfilRepository extends JpaRepository<PermissaoPerfil, Long> {
    List<PermissaoPerfil> findByPerfil(PerfilAcesso perfil);
    void deleteByPerfil(PerfilAcesso perfil);
}
