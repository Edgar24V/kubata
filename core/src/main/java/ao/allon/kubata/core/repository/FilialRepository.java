package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilialRepository extends JpaRepository<Filial, Long> {
    List<Filial> findByEmpresaAndActiveTrueOrderByNomeAsc(Empresa empresa);
    List<Filial> findByEmpresaOrderByNomeAsc(Empresa empresa);
    Optional<Filial> findByEmpresaAndCodigoIgnoreCase(Empresa empresa, String codigo);
}
