package ao.allon.kubata.faturacao.repository;

import ao.allon.kubata.faturacao.domain.EmailEnviado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailEnviadoRepository extends JpaRepository<EmailEnviado, Long> {

    List<EmailEnviado> findByStatusOrderByCreatedAtDesc(EmailEnviado.StatusEmail status);

    List<EmailEnviado> findByDestinatarioContainingIgnoreCaseOrderByCreatedAtDesc(String destinatario);

    List<EmailEnviado> findByFaturaNumeroContainingIgnoreCaseOrderByCreatedAtDesc(String faturaNumero);

    @Query("SELECT e FROM EmailEnviado e WHERE e.createdAt >= :startDate ORDER BY e.createdAt DESC")
    List<EmailEnviado> findRecentEmails(LocalDateTime startDate);

    List<EmailEnviado> findByStatusAndTentativasLessThan(EmailEnviado.StatusEmail status, Integer tentativas);

    List<EmailEnviado> findByTipoOrderByCreatedAtDesc(EmailEnviado.TipoEmail tipo);

    @Query("SELECT e FROM EmailEnviado e WHERE e.status = 'ERRO' AND e.tentativas < 3 ORDER BY e.createdAt ASC")
    List<EmailEnviado> findEmailsParaRetry();

    long countByStatus(EmailEnviado.StatusEmail status);

    @Query("SELECT COUNT(e) FROM EmailEnviado e WHERE e.dataEnvio >= :startDate AND e.status = 'ENVIADO'")
    long countEnviadosComSucessoDesde(LocalDateTime startDate);
}
