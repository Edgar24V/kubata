package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamentos")
public class Pagamento extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodo;

    @Column(nullable = false)
    private BigDecimal valor;

    public Pagamento() {}

    public Pagamento(MetodoPagamento metodo, BigDecimal valor) {
        this.metodo = metodo;
        this.valor = valor;
    }

    public Fatura getFatura() {
        return fatura;
    }

    public void setFatura(Fatura fatura) {
        this.fatura = fatura;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPagamento metodo) {
        this.metodo = metodo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
