package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transferencias_stock_linhas")
public class TransferenciaStockLinha extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private TransferenciaStock transferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(precision = 19, scale = 3, nullable = false)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(length = 80)
    private String lote;

    @Column(name = "localizacao_origem", length = 80)
    private String localizacaoOrigem;

    @Column(name = "localizacao_destino", length = 80)
    private String localizacaoDestino;

    public TransferenciaStock getTransferencia() { return transferencia; }
    public void setTransferencia(TransferenciaStock transferencia) { this.transferencia = transferencia; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public String getLocalizacaoOrigem() { return localizacaoOrigem; }
    public void setLocalizacaoOrigem(String localizacaoOrigem) { this.localizacaoOrigem = localizacaoOrigem; }
    public String getLocalizacaoDestino() { return localizacaoDestino; }
    public void setLocalizacaoDestino(String localizacaoDestino) { this.localizacaoDestino = localizacaoDestino; }
}
