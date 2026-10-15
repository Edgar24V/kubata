package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "itens_devolucao")
public class ItemDevolucao extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "devolucao_id", nullable = false)
    private Devolucao devolucao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * Linha exacta da factura que originou a devolução. Nullable para preservar
     * registos legados; novas solicitações comerciais devem preencher a referência.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_fatura_origem_id",
            foreignKey = @ForeignKey(name = "fk_item_devolucao_fatura_origem"))
    private ItemFatura itemFaturaOrigem;

    @NotNull
    @Min(value = 1)
    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(columnDefinition = "TEXT")
    private String motivoItem;

    public Devolucao getDevolucao() {
        return devolucao;
    }

    public void setDevolucao(Devolucao devolucao) {
        this.devolucao = devolucao;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public ItemFatura getItemFaturaOrigem() {
        return itemFaturaOrigem;
    }

    public void setItemFaturaOrigem(ItemFatura itemFaturaOrigem) {
        this.itemFaturaOrigem = itemFaturaOrigem;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calculateTotal();
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
        calculateTotal();
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getMotivoItem() {
        return motivoItem;
    }

    public void setMotivoItem(String motivoItem) {
        this.motivoItem = motivoItem;
    }

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (quantidade != null && valorUnitario != null) {
            this.valorTotal = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }
}
