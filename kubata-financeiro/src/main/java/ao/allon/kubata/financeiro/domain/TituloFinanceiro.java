package ao.allon.kubata.financeiro.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.financeiro.enums.EstadoTituloFinanceiro;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Título de fornecedor (PAGAR) ou cliente (RECEBER).
 * A liquidação é registada separadamente para preservar o histórico.
 */
@Entity
@Table(name = "titulos_financeiros")
@SQLDelete(sql = "UPDATE titulos_financeiros SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class TituloFinanceiro extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoTituloFinanceiro tipo;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String entidade;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao = LocalDate.now();

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "valor_original", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorOriginal = BigDecimal.ZERO;

    @Column(name = "valor_liquidado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorLiquidado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTituloFinanceiro estado = EstadoTituloFinanceiro.ABERTO;

    @Column(length = 500)
    private String observacoes;

    public TipoTituloFinanceiro getTipo() { return tipo; }
    public void setTipo(TipoTituloFinanceiro tipo) { this.tipo = tipo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public BigDecimal getValorOriginal() { return valorOriginal; }
    public void setValorOriginal(BigDecimal valorOriginal) { this.valorOriginal = valorOriginal; }
    public BigDecimal getValorLiquidado() { return valorLiquidado; }
    public void setValorLiquidado(BigDecimal valorLiquidado) { this.valorLiquidado = valorLiquidado; }
    public EstadoTituloFinanceiro getEstado() { return estado; }
    public void setEstado(EstadoTituloFinanceiro estado) { this.estado = estado; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Transient
    public BigDecimal getSaldoPendente() {
        BigDecimal original = valorOriginal == null ? BigDecimal.ZERO : valorOriginal;
        BigDecimal liquidado = valorLiquidado == null ? BigDecimal.ZERO : valorLiquidado;
        return original.subtract(liquidado).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isVencido() {
        return dataVencimento != null
                && dataVencimento.isBefore(LocalDate.now())
                && getSaldoPendente().signum() > 0
                && estado != EstadoTituloFinanceiro.ANULADO;
    }
}
