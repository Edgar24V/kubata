package ao.allon.kubata.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bilhete de sessão usado pelo Kubata Administrator para abrir outro módulo sem segundo login.
 * É prova de um login já validado, nunca uma credencial: o módulo que o recebe revalida a
 * autorização do utilizador antes de iniciar a sessão local.
 */
@Entity
@Table(name = "adm_session_ticket")
public class SessionTicket extends BaseEntity {

    public static final int VALIDADE_SEGUNDOS = 60;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "application_code", nullable = false)
    private String applicationCode;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "exercicio_ano")
    private Integer exercicioAno;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "issued_by_user_id")
    private Long issuedByUserId;

    private String workstation;

    public SessionTicket() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Integer getExercicioAno() {
        return exercicioAno;
    }

    public void setExercicioAno(Integer exercicioAno) {
        this.exercicioAno = exercicioAno;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Long getIssuedByUserId() {
        return issuedByUserId;
    }

    public void setIssuedByUserId(Long issuedByUserId) {
        this.issuedByUserId = issuedByUserId;
    }

    public String getWorkstation() {
        return workstation;
    }

    public void setWorkstation(String workstation) {
        this.workstation = workstation;
    }
}
