package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "numeros_serie_stock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "numero"}))
public class NumeroSerieStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @Column(nullable = false, length = 120)
    private String numero;

    @Column(name = "estado_serie", nullable = false, length = 30)
    private String estadoSerie = "EM_STOCK";

    @Column(name = "lote_id")
    private Long loteId;

    @Column(length = 255)
    private String observacoes;

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getEstadoSerie() { return estadoSerie; }
    public void setEstadoSerie(String estadoSerie) { this.estadoSerie = estadoSerie; }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
