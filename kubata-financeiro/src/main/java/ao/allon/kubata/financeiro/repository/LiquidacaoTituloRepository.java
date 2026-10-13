package ao.allon.kubata.financeiro.repository;

import ao.allon.kubata.financeiro.domain.LiquidacaoTitulo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiquidacaoTituloRepository extends JpaRepository<LiquidacaoTitulo, Long> {
    @EntityGraph(attributePaths = {"caixa", "contaBancaria"})
    List<LiquidacaoTitulo> findByTitulo_IdOrderByDataLiquidacaoDesc(Long tituloId);
    boolean existsByTitulo_Id(Long tituloId);
}
