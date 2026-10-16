package ao.allon.kubata.financeiro.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.financeiro.enums.MeioPagamentoFinanceiro;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uma liquidação parcial ou total, associada ao movimento de caixa ou banco. */
@Entity
@Table(name = "liquidacoes_titulos")
@SQLDelete(sql = "UPDATE liquidacoes_titulos SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class LiquidacaoTitulo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titulo_id", nullable = false)
    private TituloFinanceiro titulo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_liquidacao", nullable = false)
    private LocalDateTime dataLiquidacao = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", nullable = false, length = 15)
    private MeioPagamentoFinanceiro meioPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caixa_id")
    private Caixa caixa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    @Column(length = 120)
    private String referencia;

    public TituloFinanceiro getTitulo() { return titulo; }
    public void setTitulo(TituloFinanceiro titulo) { this.titulo = titulo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDateTime getDataLiquidacao() { return dataLiquidacao; }
    public void setDataLiquidacao(LocalDateTime dataLiquidacao) { this.dataLiquidacao = dataLiquidacao; }
    public MeioPagamentoFinanceiro getMeioPagamento() { return meioPagamento; }
    public void setMeioPagamento(MeioPagamentoFinanceiro meioPagamento) { this.meioPagamento = meioPagamento; }
    public Caixa getCaixa() { return caixa; }
    public void setCaixa(Caixa caixa) { this.caixa = caixa; }
    public ContaBancaria getContaBancaria() { return contaBancaria; }
    public void setContaBancaria(ContaBancaria contaBancaria) { this.contaBancaria = contaBancaria; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
