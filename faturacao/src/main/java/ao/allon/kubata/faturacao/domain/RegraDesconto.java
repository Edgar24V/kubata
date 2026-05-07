package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "regras_desconto")
@SQLDelete(sql = "UPDATE regras_desconto SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class RegraDesconto extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EscopoRegra escopo;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "imposto_id")
    private Imposto imposto;

    @Column(name = "max_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercent;

    @Column(name = "inicio")
    private LocalDate inicio;

    @Column(name = "fim")
    private LocalDate fim;

    @Column(name = "requer_aprovacao", nullable = false)
    private Boolean requerAprovacao = false;

    @Column(name = "prioridade", nullable = false)
    private Integer prioridade = 0;

    @Column(length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RegraStatus status = RegraStatus.ATIVA;

    public EscopoRegra getEscopo() { return escopo; }
    public void setEscopo(EscopoRegra escopo) { this.escopo = escopo; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Imposto getImposto() { return imposto; }
    public void setImposto(Imposto imposto) { this.imposto = imposto; }
    public BigDecimal getMaxPercent() { return maxPercent; }
    public void setMaxPercent(BigDecimal maxPercent) { this.maxPercent = maxPercent; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate fim) { this.fim = fim; }
    public Boolean getRequerAprovacao() { return requerAprovacao; }
    public void setRequerAprovacao(Boolean requerAprovacao) { this.requerAprovacao = requerAprovacao; }
    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public RegraStatus getStatus() { return status; }
    public void setStatus(RegraStatus status) { this.status = status; }
}
