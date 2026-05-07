package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilAcessoRepository extends JpaRepository<PerfilAcesso, Long> {
    Optional<PerfilAcesso> findByCodigo(String codigo);
    List<PerfilAcesso> findByEmpresa(Empresa empresa);
    List<PerfilAcesso> findByEmpresaIsNull(); // Perfis globais
}
