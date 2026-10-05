package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.inventario.enums.EstadoReserva;
import ao.allon.kubata.inventario.enums.TipoReserva;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas_stock",
        indexes = {
                @Index(name = "idx_reserva_produto_armazem_estado", columnList = "produto_id,armazem_id,estado"),
                @Index(name = "idx_reserva_documento", columnList = "documento_tipo,documento_id")
        })
public class ReservaStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false)
    private Armazem armazem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoReserva tipo = TipoReserva.OUTRA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoReserva estado = EstadoReserva.ATIVA;

    @Column(nullable = false, precision = 19, scale = 3)
    private java.math.BigDecimal quantidade = java.math.BigDecimal.ZERO;

    @Column(name = "documento_tipo", length = 50)
    private String documentoTipo;

    @Column(name = "documento_id")
    private Long documentoId;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "data_reserva", nullable = false)
    private LocalDateTime dataReserva = LocalDateTime.now();

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }
    public TipoReserva getTipo() { return tipo; }
    public void setTipo(TipoReserva tipo) { this.tipo = tipo; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public java.math.BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(java.math.BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getDocumentoTipo() { return documentoTipo; }
    public void setDocumentoTipo(String documentoTipo) { this.documentoTipo = documentoTipo; }
    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long documentoId) { this.documentoId = documentoId; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public LocalDateTime getDataReserva() { return dataReserva; }
    public void setDataReserva(LocalDateTime dataReserva) { this.dataReserva = dataReserva; }
}
