package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.domain.enums.StatusDevolucao;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucoes")
public class Devolucao extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "fatura_origem_id")
    private Fatura faturaOrigem;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "data_analise")
    private LocalDateTime dataAnalise;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDevolucao status = StatusDevolucao.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String motivoSolicitacao;

    @Column(columnDefinition = "TEXT")
    private String parecerAnalise;

    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemDevolucao> itens = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "nota_credito_id")
    private Fatura notaCredito;
    
    @Column(name = "usuario_solicitante")
    private String usuarioSolicitante;
    
    @Column(name = "usuario_analista")
    private String usuarioAnalista;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (dataSolicitacao == null) {
            dataSolicitacao = LocalDateTime.now();
        }
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Fatura getFaturaOrigem() {
        return faturaOrigem;
    }

    public void setFaturaOrigem(Fatura faturaOrigem) {
        this.faturaOrigem = faturaOrigem;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public LocalDateTime getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(LocalDateTime dataAnalise) {
        this.dataAnalise = dataAnalise;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public StatusDevolucao getStatus() {
        return status;
    }

    public void setStatus(StatusDevolucao status) {
        this.status = status;
    }

    public String getMotivoSolicitacao() {
        return motivoSolicitacao;
    }

    public void setMotivoSolicitacao(String motivoSolicitacao) {
        this.motivoSolicitacao = motivoSolicitacao;
    }

    public String getParecerAnalise() {
        return parecerAnalise;
    }

    public void setParecerAnalise(String parecerAnalise) {
        this.parecerAnalise = parecerAnalise;
    }

    public List<ItemDevolucao> getItens() {
        return itens;
    }

    public void setItens(List<ItemDevolucao> itens) {
        this.itens = itens;
    }

    public Fatura getNotaCredito() {
        return notaCredito;
    }

    public void setNotaCredito(Fatura notaCredito) {
        this.notaCredito = notaCredito;
    }
    
    public String getUsuarioSolicitante() {
        return usuarioSolicitante;
    }

    public void setUsuarioSolicitante(String usuarioSolicitante) {
        this.usuarioSolicitante = usuarioSolicitante;
    }

    public String getUsuarioAnalista() {
        return usuarioAnalista;
    }

    public void setUsuarioAnalista(String usuarioAnalista) {
        this.usuarioAnalista = usuarioAnalista;
    }

    public void addItem(ItemDevolucao item) {
        itens.add(item);
        item.setDevolucao(this);
    }

    public void removeItem(ItemDevolucao item) {
        itens.remove(item);
        item.setDevolucao(null);
    }
}
