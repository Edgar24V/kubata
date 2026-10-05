package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.inventario.enums.EstadoTransferencia;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transferencias_stock")
public class TransferenciaStock extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_origem_id", nullable = false)
    private Armazem armazemOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_destino_id", nullable = false)
    private Armazem armazemDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransferencia estado = EstadoTransferencia.RASCUNHO;

    @Column(name = "data_expedicao")
    private LocalDateTime dataExpedicao;

    @Column(name = "data_rececao")
    private LocalDateTime dataRececao;

    @Column(length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferenciaStockLinha> linhas = new ArrayList<>();

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Armazem getArmazemOrigem() { return armazemOrigem; }
    public void setArmazemOrigem(Armazem armazemOrigem) { this.armazemOrigem = armazemOrigem; }
    public Armazem getArmazemDestino() { return armazemDestino; }
    public void setArmazemDestino(Armazem armazemDestino) { this.armazemDestino = armazemDestino; }
    public EstadoTransferencia getEstado() { return estado; }
    public void setEstado(EstadoTransferencia estado) { this.estado = estado; }
    public LocalDateTime getDataExpedicao() { return dataExpedicao; }
    public void setDataExpedicao(LocalDateTime dataExpedicao) { this.dataExpedicao = dataExpedicao; }
    public LocalDateTime getDataRececao() { return dataRececao; }
    public void setDataRececao(LocalDateTime dataRececao) { this.dataRececao = dataRececao; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<TransferenciaStockLinha> getLinhas() { return linhas; }
    public void setLinhas(List<TransferenciaStockLinha> linhas) { this.linhas = linhas; }
    public void addLinha(TransferenciaStockLinha linha) {
        linhas.add(linha);
        linha.setTransferencia(this);
    }
}
