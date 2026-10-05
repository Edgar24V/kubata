package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "inventarios_fisicos_linhas")
public class InventarioFisicoLinha extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false)
    private InventarioFisico inventario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @Column(name = "stock_sistema", precision = 19, scale = 3, nullable = false)
    private BigDecimal stockSistema = BigDecimal.ZERO;

    @Column(name = "quantidade_contada", precision = 19, scale = 3)
    private BigDecimal quantidadeContada;

    @Column(name = "diferenca", precision = 19, scale = 3)
    private BigDecimal diferenca = BigDecimal.ZERO;

    @Column(name = "custo_unitario", precision = 19, scale = 2)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(length = 80)
    private String lote;

    public InventarioFisico getInventario() { return inventario; }
    public void setInventario(InventarioFisico inventario) { this.inventario = inventario; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }
    public BigDecimal getStockSistema() { return stockSistema; }
    public void setStockSistema(BigDecimal stockSistema) { this.stockSistema = stockSistema; }
    public BigDecimal getQuantidadeContada() { return quantidadeContada; }
    public void setQuantidadeContada(BigDecimal quantidadeContada) { this.quantidadeContada = quantidadeContada; }
    public BigDecimal getDiferenca() { return diferenca; }
    public void setDiferenca(BigDecimal diferenca) { this.diferenca = diferenca; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
}
