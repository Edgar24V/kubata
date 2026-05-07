package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimentoBancario;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_bancarios")
@SQLDelete(sql = "UPDATE movimentos_bancarios SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class MovimentoBancario extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria conta;

    @Column(nullable = false)
    private LocalDateTime dataMovimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoBancario tipo;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private BigDecimal saldoAnterior;

    @Column(nullable = false)
    private BigDecimal saldoAtual;

    @Column(nullable = false)
    private String descricao;

    private String categoria; // "Fatura", "Despesa", "Transferência", "Taxas", etc.

    private String referenciaDocumento; // ID ou Número do documento relacionado

    public MovimentoBancario() {}

    public MovimentoBancario(ContaBancaria conta, LocalDateTime dataMovimento, TipoMovimentoBancario tipo, BigDecimal valor, BigDecimal saldoAnterior, BigDecimal saldoAtual, String descricao) {
        this.conta = conta;
        this.dataMovimento = dataMovimento;
        this.tipo = tipo;
        this.valor = valor;
        this.saldoAnterior = saldoAnterior;
        this.saldoAtual = saldoAtual;
        this.descricao = descricao;
    }

    public ContaBancaria getConta() {
        return conta;
    }

    public void setConta(ContaBancaria conta) {
        this.conta = conta;
    }

    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public TipoMovimentoBancario getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentoBancario tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(BigDecimal saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(BigDecimal saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getReferenciaDocumento() {
        return referenciaDocumento;
    }

    public void setReferenciaDocumento(String referenciaDocumento) {
        this.referenciaDocumento = referenciaDocumento;
    }
}
