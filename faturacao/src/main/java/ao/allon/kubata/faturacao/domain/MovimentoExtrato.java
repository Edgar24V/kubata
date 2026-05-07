package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.core.domain.LancamentoContabil;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade para movimentos de extrato bancário.
 * Representa cada linha de um extrato importado para reconciliação.
 */
@Entity
@Table(name = "movimentos_extrato")
@SQLDelete(sql = "UPDATE movimentos_extrato SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class MovimentoExtrato extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "reconciliacao_id", nullable = false)
    private ReconciliacaoBancaria reconciliacao;

    @Column(nullable = false)
    private LocalDate dataMovimento;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(length = 50)
    private String referencia; // Nº de documento, cheque, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimento tipo; // DEBITO ou CREDITO

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(precision = 19, scale = 2)
    private BigDecimal saldoAposMovimento;

    @ManyToOne
    @JoinColumn(name = "lancamento_id")
    private LancamentoContabil lancamentoConciliado; // Ligação ao lançamento contábil

    @Column(name = "conciliado", nullable = false)
    private Boolean conciliado = false;

    @Column(name = "data_conciliacao")
    private java.time.LocalDateTime dataConciliacao;

    @Column(name = "usuario_conciliacao_id")
    private Long usuarioConciliacaoId;

    @Column(length = 255)
    private String observacoes;

    public enum TipoMovimento {
        DEBITO("Débito", "Saída"),
        CREDITO("Crédito", "Entrada");

        private final String descricao;
        private final String fluxo;

        TipoMovimento(String descricao, String fluxo) {
            this.descricao = descricao;
            this.fluxo = fluxo;
        }

        public String getDescricao() {
            return descricao;
        }

        public String getFluxo() {
            return fluxo;
        }
    }

    // Construtores
    public MovimentoExtrato() {}

    public MovimentoExtrato(ReconciliacaoBancaria reconciliacao, LocalDate dataMovimento, 
                             String descricao, TipoMovimento tipo, BigDecimal valor) {
        this.reconciliacao = reconciliacao;
        this.dataMovimento = dataMovimento;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valor = valor;
        this.conciliado = false;
    }

    // Getters e Setters
    public ReconciliacaoBancaria getReconciliacao() {
        return reconciliacao;
    }

    public void setReconciliacao(ReconciliacaoBancaria reconciliacao) {
        this.reconciliacao = reconciliacao;
    }

    public LocalDate getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDate dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getSaldoAposMovimento() {
        return saldoAposMovimento;
    }

    public void setSaldoAposMovimento(BigDecimal saldoAposMovimento) {
        this.saldoAposMovimento = saldoAposMovimento;
    }

    public LancamentoContabil getLancamentoConciliado() {
        return lancamentoConciliado;
    }

    public void setLancamentoConciliado(LancamentoContabil lancamentoConciliado) {
        this.lancamentoConciliado = lancamentoConciliado;
    }

    public Boolean getConciliado() {
        return conciliado;
    }

    public void setConciliado(Boolean conciliado) {
        this.conciliado = conciliado;
    }

    public java.time.LocalDateTime getDataConciliacao() {
        return dataConciliacao;
    }

    public void setDataConciliacao(java.time.LocalDateTime dataConciliacao) {
        this.dataConciliacao = dataConciliacao;
    }

    public Long getUsuarioConciliacaoId() {
        return usuarioConciliacaoId;
    }

    public void setUsuarioConciliacaoId(Long usuarioConciliacaoId) {
        this.usuarioConciliacaoId = usuarioConciliacaoId;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    /**
     * Marca o movimento como conciliado.
     */
    public void marcarConciliado(LancamentoContabil lancamento, Long usuarioId) {
        this.conciliado = true;
        this.lancamentoConciliado = lancamento;
        this.usuarioConciliacaoId = usuarioId;
        this.dataConciliacao = java.time.LocalDateTime.now();
    }
}
