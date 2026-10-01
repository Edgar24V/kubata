package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = {"empresa", "filial", "perfis"})
    List<User> findAllByOrderByNomeAsc();
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.nome) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<User> pesquisar(String q);
    
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.perfis
        LEFT JOIN FETCH u.empresa
        LEFT JOIN FETCH u.filial
        WHERE u.id = :id
        """)
    Optional<User> findByIdWithPerfis(@Param("id") Long id);

}

