package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "rh_faltas")
public class Falta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @NotNull(message = "Data da falta é obrigatória")
    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoFalta tipo = TipoFalta.NAO_JUSTIFICADA;

    @Size(max = 200, message = "Motivo não pode exceder 200 caracteres")
    @Column(name = "motivo", length = 200)
    private String motivo;

    @Column(name = "justificada", nullable = false)
    private Boolean justificada = false;

    @Column(name = "data_justificacao")
    private LocalDate dataJustificacao;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private ao.allon.kubata.core.domain.User aprovadoPor;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    public enum TipoFalta {
        NAO_JUSTIFICADA,
        DOENCA,
        FAMILIA,
        ACOMPANHAMENTO,
        MOTIVO_PESSOAL,
        ATRASO,
        ABANDONO
    }

    public Falta() {}

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

    public TipoFalta getTipo() {
        return tipo;
    }

    public void setTipo(TipoFalta tipo) {
        this.tipo = tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Boolean getJustificada() {
        return justificada;
    }

    public void setJustificada(Boolean justificada) {
        this.justificada = justificada;
    }

    public LocalDate getDataJustificacao() {
        return dataJustificacao;
    }

    public void setDataJustificacao(LocalDate dataJustificacao) {
        this.dataJustificacao = dataJustificacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + data : "Falta";
    }
}
