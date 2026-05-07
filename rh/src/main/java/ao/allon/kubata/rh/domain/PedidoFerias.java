package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "rh_pedidos_ferias")
public class PedidoFerias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @NotNull(message = "Quantidade de dias é obrigatória")
    @Min(value = 1, message = "Mínimo de 1 dia")
    @Column(name = "quantidade_dias", nullable = false)
    private Integer quantidadeDias;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private ao.allon.kubata.core.domain.User aprovadoPor;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    @Size(max = 500, message = "Justificativa não pode exceder 500 caracteres")
    @Column(name = "justificativa", length = 500)
    private String justificativa;

    @Size(max = 500, message = "Motivo da rejeição não pode exceder 500 caracteres")
    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_ferias_id")
    private PeriodoFerias periodoFerias;

    public enum EstadoPedido {
        PENDENTE,
        APROVADO,
        REJEITADO,
        CANCELADO
    }

    public PedidoFerias() {}

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getQuantidadeDias() {
        return quantidadeDias;
    }

    public void setQuantidadeDias(Integer quantidadeDias) {
        this.quantidadeDias = quantidadeDias;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public ao.allon.kubata.core.domain.User getAprovadoPor() {
        return aprovadoPor;
    }

    public void setAprovadoPor(ao.allon.kubata.core.domain.User aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }

    public LocalDate getDataAprovacao() {
        return dataAprovacao;
    }

    public void setDataAprovacao(LocalDate dataAprovacao) {
        this.dataAprovacao = dataAprovacao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public PeriodoFerias getPeriodoFerias() {
        return periodoFerias;
    }

    public void setPeriodoFerias(PeriodoFerias periodoFerias) {
        this.periodoFerias = periodoFerias;
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + dataInicio + " a " + dataFim : "PedidoFerias";
    }
}
