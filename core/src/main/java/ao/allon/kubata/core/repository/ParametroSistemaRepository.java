package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.projection.ParametroSistemaLinha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {

    Optional<ParametroSistema> findByEmpresa_IdAndChave(Long empresaId, String chave);

    Optional<ParametroSistema> findByChaveAndEmpresaIdIsNull(String chave);

    List<ParametroSistema> findAllByEmpresaIsNullOrderByGrupoAscChaveAsc();

    List<ParametroSistema> findAllByEmpresa_IdOrderByGrupoAscChaveAsc(Long empresaId);

    @Query("""
            SELECT new ao.allon.kubata.core.projection.ParametroSistemaLinha(
                p.id, p.chave, p.valor, p.tipoValor, p.descricao, p.grupo,
                p.editavel, p.atualizadoEm, p.atualizadoPor)
            FROM ParametroSistema p
            WHERE p.empresa IS NULL AND LOWER(p.grupo) = LOWER(:grupo)
              AND (LOWER(p.chave) LIKE :termo OR LOWER(p.valor) LIKE :termo OR LOWER(p.descricao) LIKE :termo)
            """)
    Page<ParametroSistemaLinha> pesquisarLinhasGlobais(@Param("grupo") String grupo,
                                                       @Param("termo") String termo,
                                                       Pageable pageable);
}
