package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_alerta", indexes = {
        @Index(name = "idx_adm_alerta_estado", columnList = "estado"),
        @Index(name = "idx_adm_alerta_severidade", columnList = "severidade"),
        @Index(name = "idx_adm_alerta_origem", columnList = "origem"),
        @Index(name = "idx_adm_alerta_aberto", columnList = "opened_at")
})
public class Alerta extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 120)
    private String codigo;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private Severidade severidade = Severidade.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.OPEN;

    @Column(name = "origem", nullable = false, length = 120)
    private String origem;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private User responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconhecido_por_id")
    private User reconhecidoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvido_por_id")
    private User resolvidoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ignorado_por_id")
    private User ignoradoPor;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "ignored_at")
    private LocalDateTime ignoredAt;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "ultima_observacao", length = 2000)
    private String ultimaObservacao;

    @PrePersist
    protected void onAlertCreate() {
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
        if (estado == null) {
            estado = Estado.OPEN;
        }
        if (severidade == null) {
            severidade = Severidade.MEDIUM;
        }
    }

    public enum Estado {
        OPEN,
        ACKNOWLEDGED,
        RESOLVED,
        IGNORED
    }

    public enum Severidade {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Severidade getSeveridade() { return severidade; }
    public void setSeveridade(Severidade severidade) { this.severidade = severidade; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public User getResponsavel() { return responsavel; }
    public void setResponsavel(User responsavel) { this.responsavel = responsavel; }

    public User getReconhecidoPor() { return reconhecidoPor; }
    public void setReconhecidoPor(User reconhecidoPor) { this.reconhecidoPor = reconhecidoPor; }

    public User getResolvidoPor() { return resolvidoPor; }
    public void setResolvidoPor(User resolvidoPor) { this.resolvidoPor = resolvidoPor; }

    public User getIgnoradoPor() { return ignoradoPor; }
    public void setIgnoradoPor(User ignoradoPor) { this.ignoradoPor = ignoradoPor; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getIgnoredAt() { return ignoredAt; }
    public void setIgnoredAt(LocalDateTime ignoredAt) { this.ignoredAt = ignoredAt; }

    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(LocalDateTime reopenedAt) { this.reopenedAt = reopenedAt; }

    public String getUltimaObservacao() { return ultimaObservacao; }
    public void setUltimaObservacao(String ultimaObservacao) { this.ultimaObservacao = ultimaObservacao; }
}
