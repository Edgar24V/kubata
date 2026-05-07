package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.faturacao.domain.enums.StatusCaixa;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Caixa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario; // Nome do operador/usuário

    private LocalDateTime dataAbertura;
    private LocalDateTime dataFecho;

    @Column(name = "saldo_inicial")
    private BigDecimal saldoInicial;

    @Column(name = "saldo_final")
    private BigDecimal saldoFinal;
    
    // Totalizadores por forma de pagamento (calculados no fecho)
    @Column(name = "total_dinheiro")
    private BigDecimal totalDinheiro;

    @Column(name = "total_tpa")
    private BigDecimal totalTPA;

    @Column(name = "total_transferencia")
    private BigDecimal totalTransferencia;

    @Enumerated(EnumType.STRING)
    private StatusCaixa status;

    private String observacoes;

    @OneToMany(mappedBy = "caixa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MovimentoCaixa> movimentos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFecho() {
        return dataFecho;
    }

    public void setDataFecho(LocalDateTime dataFecho) {
        this.dataFecho = dataFecho;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public BigDecimal getTotalDinheiro() {
        return totalDinheiro;
    }

    public void setTotalDinheiro(BigDecimal totalDinheiro) {
        this.totalDinheiro = totalDinheiro;
    }

    public BigDecimal getTotalTPA() {
        return totalTPA;
    }

    public void setTotalTPA(BigDecimal totalTPA) {
        this.totalTPA = totalTPA;
    }

    public BigDecimal getTotalTransferencia() {
        return totalTransferencia;
    }

    public void setTotalTransferencia(BigDecimal totalTransferencia) {
        this.totalTransferencia = totalTransferencia;
    }

    public StatusCaixa getStatus() {
        return status;
    }

    public void setStatus(StatusCaixa status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<MovimentoCaixa> getMovimentos() {
        return movimentos;
    }

    public void setMovimentos(List<MovimentoCaixa> movimentos) {
        this.movimentos = movimentos;
    }

    @Override
    public String toString() {
        return "Caixa{" +
                "id=" + id +
                ", usuario='" + usuario + '\'' +
                ", status=" + status +
                ", dataAbertura=" + dataAbertura +
                '}';
    }
}
