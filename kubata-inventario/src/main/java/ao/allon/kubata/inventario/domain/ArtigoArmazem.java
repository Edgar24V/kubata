package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.inventario.enums.MetodoValorizacao;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "artigos_armazem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "armazem_id"}))
public class ArtigoArmazem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false)
    private Armazem armazem;

    @Column(name = "stock_minimo", precision = 19, scale = 3, nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "stock_maximo", precision = 19, scale = 3)
    private BigDecimal stockMaximo;

    @Column(name = "ponto_reposicao", precision = 19, scale = 3)
    private BigDecimal pontoReposicao;

    @Column(name = "quantidade_reposicao", precision = 19, scale = 3)
    private BigDecimal quantidadeReposicao;

    @Column(name = "permite_stock_negativo", nullable = false)
    private Boolean permiteStockNegativo = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_valorizacao", nullable = false, length = 30)
    private MetodoValorizacao metodoValorizacao = MetodoValorizacao.PMP;

    @Column(name = "stock_seguro", precision = 19, scale = 3)
    private BigDecimal stockSeguro;

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public BigDecimal getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(BigDecimal stockMaximo) { this.stockMaximo = stockMaximo; }

    public BigDecimal getPontoReposicao() { return pontoReposicao; }
    public void setPontoReposicao(BigDecimal pontoReposicao) { this.pontoReposicao = pontoReposicao; }

    public BigDecimal getQuantidadeReposicao() { return quantidadeReposicao; }
    public void setQuantidadeReposicao(BigDecimal quantidadeReposicao) { this.quantidadeReposicao = quantidadeReposicao; }

    public Boolean getPermiteStockNegativo() { return permiteStockNegativo; }
    public void setPermiteStockNegativo(Boolean permiteStockNegativo) { this.permiteStockNegativo = permiteStockNegativo; }

    public MetodoValorizacao getMetodoValorizacao() { return metodoValorizacao; }
    public void setMetodoValorizacao(MetodoValorizacao metodoValorizacao) { this.metodoValorizacao = metodoValorizacao; }

    public BigDecimal getStockSeguro() { return stockSeguro; }
    public void setStockSeguro(BigDecimal stockSeguro) { this.stockSeguro = stockSeguro; }
}
