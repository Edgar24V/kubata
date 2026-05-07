package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rh_contratos")
public class Contrato extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 30)
    private TipoContrato tipoContrato;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim; // Nulo para contratos por tempo indeterminado

    @Size(max = 100, message = "Horário não pode exceder 100 caracteres")
    @Column(name = "horario", length = 100)
    private String horario; // Ex: "08:00-17:00"

    @Enumerated(EnumType.STRING)
    @Column(name = "regime", length = 30)
    private RegimeContrato regime = RegimeContrato.TEMPO_INTEGRAL;

    @Column(name = "salario_base")
    private BigDecimal salarioBase; // Guardado mas não processado no MVP

    @Column(name = "subsidio_alimentacao")
    private BigDecimal subsidioAlimentacao; // Guardado mas não processado no MVP

    @Column(name = "subsidio_transporte")
    private BigDecimal subsidioTransporte; // Guardado mas não processado no MVP

    @Column(name = "outras_remuneracoes")
    private BigDecimal outrasRemuneracoes; // Guardado mas não processado no MVP

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoContrato situacao = SituacaoContrato.ATIVO;

    @Column(name = "data_rescisao")
    private LocalDate dataRescisao;

    @Size(max = 500, message = "Motivo da rescisão não pode exceder 500 caracteres")
    @Column(name = "motivo_rescisao", length = 500)
    private String motivoRescisao;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public enum TipoContrato {
        EFETIVO,
        TERMO_CERTO,
        TERMO_INDETERMINADO,
        ESTAGIO,
        PRESTACAO_SERVICOS,
        TRABALHO_TEMPORARIO
    }

    public enum RegimeContrato {
        TEMPO_INTEGRAL,
        TEMPO_PARCIAL,
        MEIO_PERIODO
    }

    public enum SituacaoContrato {
        ATIVO,
        RESCINDIDO,
        SUSPENSO,
        VENCIDO
    }

    public Contrato() {}

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(TipoContrato tipoContrato) {
        this.tipoContrato = tipoContrato;
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

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public RegimeContrato getRegime() {
        return regime;
    }

    public void setRegime(RegimeContrato regime) {
        this.regime = regime;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public BigDecimal getSubsidioAlimentacao() {
        return subsidioAlimentacao;
    }

    public void setSubsidioAlimentacao(BigDecimal subsidioAlimentacao) {
        this.subsidioAlimentacao = subsidioAlimentacao;
    }

    public BigDecimal getSubsidioTransporte() {
        return subsidioTransporte;
    }

    public void setSubsidioTransporte(BigDecimal subsidioTransporte) {
        this.subsidioTransporte = subsidioTransporte;
    }

    public BigDecimal getOutrasRemuneracoes() {
        return outrasRemuneracoes;
    }

    public void setOutrasRemuneracoes(BigDecimal outrasRemuneracoes) {
        this.outrasRemuneracoes = outrasRemuneracoes;
    }

    public SituacaoContrato getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoContrato situacao) {
        this.situacao = situacao;
    }

    public LocalDate getDataRescisao() {
        return dataRescisao;
    }

    public void setDataRescisao(LocalDate dataRescisao) {
        this.dataRescisao = dataRescisao;
    }

    public String getMotivoRescisao() {
        return motivoRescisao;
    }

    public void setMotivoRescisao(String motivoRescisao) {
        this.motivoRescisao = motivoRescisao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + tipoContrato : "Contrato";
    }
}
