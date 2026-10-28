package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recibo_alocacoes")
public class ReciboAlocacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recibo_id", nullable = false)
    private Recibo recibo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    public ReciboAlocacao() {
    }

    public ReciboAlocacao(Recibo recibo, Fatura fatura, BigDecimal valor) {
        this.recibo = recibo;
        this.fatura = fatura;
        this.valor = valor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Recibo getRecibo() {
        return recibo;
    }

    public void setRecibo(Recibo recibo) {
        this.recibo = recibo;
    }

    public Fatura getFatura() {
        return fatura;
    }

    public void setFatura(Fatura fatura) {
        this.fatura = fatura;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
