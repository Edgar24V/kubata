package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaturacaoEmpresaRepository extends JpaRepository<Empresa, Long> {
    Empresa findFirstByOrderByIdAsc();
}
