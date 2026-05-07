package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "estoques", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"produto_id", "armazem_id", "lote"})
})
@SQLDelete(sql = "UPDATE estoques SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Estoque extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "armazem_id", nullable = false)
    private Armazem armazem;

    @Column(name = "lote")
    private String lote;

    @Column(name = "validade")
    private LocalDate validade;

    @Column(nullable = false)
    private Integer quantidade = 0;

    @Column(name = "preco_compra", precision = 19, scale = 2)
    private BigDecimal precoCompra;

    @Column(name = "preco_venda", precision = 19, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada = LocalDate.now();

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public void setArmazem(Armazem armazem) {
        this.armazem = armazem;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public void setValidade(LocalDate validade) {
        this.validade = validade;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoCompra() {
        return precoCompra;
    }

    public void setPrecoCompra(BigDecimal precoCompra) {
        this.precoCompra = precoCompra;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDate dataEntrada) {
        this.dataEntrada = dataEntrada;
    }
}
