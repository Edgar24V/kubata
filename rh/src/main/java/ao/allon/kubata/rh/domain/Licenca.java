package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "rh_licencas")
public class Licenca extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoLicenca tipo;

    @NotNull(message = "Data de início é obrigatória")
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim; // Nulo para licenças sem data fim definida

    @Column(name = "quantidade_dias")
    private Integer quantidadeDias;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLicenca estado = EstadoLicenca.PENDENTE;

    @Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
    @Column(name = "descricao", length = 500)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private ao.allon.kubata.core.domain.User aprovadoPor;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    @Size(max = 500, message = "Motivo da rejeição não pode exceder 500 caracteres")
    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "documento_anexo", length = 255)
    private String documentoAnexo; // Caminho para documento justificativo

    public enum TipoLicenca {
        MATERNIDADE,
        PATERNIDADE,
        DOENCA,
        ACIDENTE_TRABALHO,
        LUTO,
        CASAMENTO,
        ACOMPANHAMENTO_FAMILIAR,
        FORMACAO,
        SERVICO_MILITAR,
        OUTRA
    }

    public enum EstadoLicenca {
        PENDENTE,
        APROVADA,
        REJEITADA,
        CONCLUIDA,
        CANCELADA
    }

    public Licenca() {}

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public TipoLicenca getTipo() {
        return tipo;
    }

    public void setTipo(TipoLicenca tipo) {
        this.tipo = tipo;
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

    public EstadoLicenca getEstado() {
        return estado;
    }

    public void setEstado(EstadoLicenca estado) {
        this.estado = estado;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public String getDocumentoAnexo() {
        return documentoAnexo;
    }

    public void setDocumentoAnexo(String documentoAnexo) {
        this.documentoAnexo = documentoAnexo;
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + tipo : "Licenca";
    }
}
