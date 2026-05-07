package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rh_horas_extra")
public class HoraExtra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @NotNull(message = "Data é obrigatória")
    @Column(name = "data", nullable = false)
    private LocalDate data;

    @NotNull(message = "Quantidade de horas é obrigatória")
    @DecimalMin(value = "0.25", message = "Mínimo de 15 minutos")
    @DecimalMax(value = "12.0", message = "Máximo de 12 horas por dia")
    @Column(name = "quantidade_horas", nullable = false, precision = 4, scale = 2)
    private BigDecimal quantidadeHoras;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 50)
    private MotivoHoraExtra motivo;

    @Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
    @Column(name = "descricao", length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoHoraExtra estado = EstadoHoraExtra.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private ao.allon.kubata.core.domain.User aprovadoPor;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public enum MotivoHoraExtra {
        PROJETO_ESPECIAL,
        ENTREGA_PRAZO,
        SUPORTE_CLIENTE,
        MANUTENCAO_EMERGENCIA,
        EVENTO_EMPRESA,
        TREINAMENTO_EXTRA,
        OUTROS
    }

    public enum EstadoHoraExtra {
        PENDENTE,
        APROVADO,
        REJEITADO
    }

    public HoraExtra() {}

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getQuantidadeHoras() {
        return quantidadeHoras;
    }

    public void setQuantidadeHoras(BigDecimal quantidadeHoras) {
        this.quantidadeHoras = quantidadeHoras;
    }

    public MotivoHoraExtra getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoHoraExtra motivo) {
        this.motivo = motivo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public EstadoHoraExtra getEstado() {
        return estado;
    }

    public void setEstado(EstadoHoraExtra estado) {
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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + quantidadeHoras + "h" : "HoraExtra";
    }
}
