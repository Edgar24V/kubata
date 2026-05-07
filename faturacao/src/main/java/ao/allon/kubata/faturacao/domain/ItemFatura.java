package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import ao.allon.kubata.faturacao.util.Money;
import ao.allon.kubata.faturacao.util.Qty;

@Entity
@Table(name = "itens_fatura")
@SQLDelete(sql = "UPDATE itens_fatura SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class ItemFatura extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;
    
    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @NotBlank(message = "Descrição é obrigatória")
    @Column(nullable = false)
    private String descricao;

    @NotNull
    @Min(value = 1, message = "Quantidade deve ser maior que 0")
    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "quantidade_decimal", precision = 19, scale = 3)
    private BigDecimal quantidadeDecimal;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "percentual_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualIva;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "valor_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorIva;

    @Column(name = "codigo_isencao", length = 10)
    private String codigoIsencao;

    @Column(name = "motivo_isencao", length = 255)
    private String motivoIsencao;

    @Column(name = "lote", length = 50)
    private String lote;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    
    @Column(name = "peso_unitario", precision = 19, scale = 2)
    private BigDecimal pesoUnitario = BigDecimal.ZERO;
    
    @Column(name = "peso_total", precision = 19, scale = 2)
    private BigDecimal pesoTotal = BigDecimal.ZERO;
    
    @Transient
    private BigDecimal descontoPercentual = BigDecimal.ZERO;

    @Transient
    private boolean isNew = true; // Default to true for new items

    public Fatura getFatura() {
        return fatura;
    }

    public void setFatura(Fatura fatura) {
        this.fatura = fatura;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Long getProdutoId() {
        return produto != null ? produto.getId() : null;
    }

    public void setProdutoId(Long produtoId) {
        // Método de conveniência/compatibilidade
        // Não faz nada diretamente, pois precisamos da entidade Produto
        // Quem chamar deve usar setProduto()
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getQuantidadeDecimal() {
        return quantidadeDecimal;
    }

    public void setQuantidadeDecimal(BigDecimal quantidadeDecimal) {
        this.quantidadeDecimal = quantidadeDecimal;
        if (quantidadeDecimal != null) {
            // Mantém compatibilidade com regras antigas que assumem inteiro
            int asInt = quantidadeDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
            this.quantidade = Math.max(asInt, 1);
        }
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getPercentualIva() {
        return percentualIva;
    }

    public void setPercentualIva(BigDecimal percentualIva) {
        this.percentualIva = percentualIva;
    }

    public BigDecimal getTaxaIva() {
        return percentualIva;
    }

    public void setTaxaIva(BigDecimal taxaIva) {
        this.percentualIva = taxaIva;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getValorIva() {
        return valorIva;
    }

    public void setValorIva(BigDecimal valorIva) {
        this.valorIva = valorIva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCodigoIsencao() {
        return codigoIsencao;
    }

    public void setCodigoIsencao(String codigoIsencao) {
        this.codigoIsencao = codigoIsencao;
    }

    public String getMotivoIsencao() {
        return motivoIsencao;
    }

    public void setMotivoIsencao(String motivoIsencao) {
        this.motivoIsencao = motivoIsencao;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public BigDecimal getDescontoPercentual() {
        return descontoPercentual;
    }

    public void setDescontoPercentual(BigDecimal descontoPercentual) {
        this.descontoPercentual = descontoPercentual;
    }

    public BigDecimal getDesconto() {
        return descontoPercentual;
    }

    public void setDesconto(BigDecimal desconto) {
        if (desconto != null && (desconto.compareTo(BigDecimal.ZERO) < 0 || desconto.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("O desconto deve estar entre 0 e 100% (Norma AGT).");
        }
        this.descontoPercentual = desconto;
    }

    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        if (quantidade != null && precoUnitario != null && percentualIva != null) {
            BigDecimal q = Qty.qtyOrInt(quantidadeDecimal, quantidade);
            BigDecimal bruto = Money.scale(precoUnitario.multiply(q));
            BigDecimal percDesc = descontoPercentual != null ? descontoPercentual : BigDecimal.ZERO;
            BigDecimal fator = BigDecimal.ONE.subtract(percDesc.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.subtotal = Money.scale(bruto.multiply(fator));
            this.valorIva = Money.scale(this.subtotal.multiply(percentualIva).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            this.total = Money.scale(this.subtotal.add(this.valorIva));
        }
        if (quantidade != null && pesoUnitario != null) {
            BigDecimal q = Qty.qtyOrInt(quantidadeDecimal, quantidade);
            this.pesoTotal = Money.scale(pesoUnitario.multiply(q));
        }
    }

    public BigDecimal getPesoUnitario() {
        return pesoUnitario;
    }

    public void setPesoUnitario(BigDecimal pesoUnitario) {
        this.pesoUnitario = pesoUnitario;
    }

    public BigDecimal getPesoTotal() {
        return pesoTotal;
    }

    public void setPesoTotal(BigDecimal pesoTotal) {
        this.pesoTotal = pesoTotal;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    @Override
    public String toString() {
        return "ItemFatura{id=" + getId() + ", descricao='" + descricao + "', total=" + total + "}";
    }
}
