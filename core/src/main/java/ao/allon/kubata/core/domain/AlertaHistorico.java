package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_alerta_historico", indexes = {
        @Index(name = "idx_adm_alerta_hist_alerta", columnList = "alerta_id"),
        @Index(name = "idx_adm_alerta_hist_evento", columnList = "event_at")
})
public class AlertaHistorico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 30)
    private Acao acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private Alerta.Estado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", length = 20)
    private Alerta.Estado estadoNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id")
    private User utilizador;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @PrePersist
    protected void onHistoryCreate() {
        if (eventAt == null) {
            eventAt = LocalDateTime.now();
        }
    }

    public enum Acao {
        CREATED,
        UPDATED,
        ACKNOWLEDGED,
        RESOLVED,
        IGNORED,
        REOPENED,
        ASSIGNED,
        MIGRATED
    }

    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }

    public Acao getAcao() { return acao; }
    public void setAcao(Acao acao) { this.acao = acao; }

    public Alerta.Estado getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(Alerta.Estado estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public Alerta.Estado getEstadoNovo() { return estadoNovo; }
    public void setEstadoNovo(Alerta.Estado estadoNovo) { this.estadoNovo = estadoNovo; }

    public User getUtilizador() { return utilizador; }
    public void setUtilizador(User utilizador) { this.utilizador = utilizador; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
}
