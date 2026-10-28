package ao.allon.kubata.core.repository;

import ao.allon.kubata.core.domain.SessionTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionTicketRepository extends JpaRepository<SessionTicket, Long> {

    Optional<SessionTicket> findByToken(String token);

    /**
     * Consumo atómico: só o primeiro chamador obtém 1 linha alterada. A validade também é
     * verificada na escrita, para que uma corrida não converta um bilhete expirado em sessão.
     */
    @Modifying
    @Query("update SessionTicket t set t.consumedAt = :now, t.updatedAt = :now "
            + "where t.token = :token and t.consumedAt is null and t.expiresAt > :now")
    int consumir(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from SessionTicket t where t.consumedAt is null and t.expiresAt < :corte")
    int eliminarExpirados(@Param("corte") LocalDateTime corte);
}
