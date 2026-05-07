package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import ao.allon.kubata.faturacao.domain.Imposto;

@Entity
@Table(name = "produtos")
@SQLDelete(sql = "UPDATE produtos SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Produto extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, unique = true)
    private String codigoBarra;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "preco_compra", precision = 19, scale = 2)
    private BigDecimal precoCompra = BigDecimal.ZERO;

    @Column(name = "percentual_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualIva = new BigDecimal("14.00"); // Default IVA 14%

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false)
    private UnidadeMedida unidadeMedida = UnidadeMedida.UNIDADE;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_compra")
    private UnidadeMedida unidadeCompra = UnidadeMedida.UNIDADE;

    @Column(name = "fator_conversao", precision = 19, scale = 4)
    private BigDecimal fatorConversao = BigDecimal.ONE;

    @Column(name = "step_venda", precision = 19, scale = 3)
    private BigDecimal stepVenda;

    @Column(name = "casas_decimais_quantidade")
    private Integer casasDecimaisQuantidade;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @Column(name = "stock_maximo")
    private Integer stockMaximo;

    @Column(name = "marca")
    private String marca;

    @Column(name = "localizacao")
    private String localizacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "imposto_id")
    private Imposto imposto;

    @OneToOne(mappedBy = "produto", cascade = CascadeType.ALL)
    private FichaTecnica fichaTecnica;

    @Column(name = "sujeito_retencao", nullable = false)
    private Boolean sujeitoRetencao = false;

    public FichaTecnica getFichaTecnica() {
        return fichaTecnica;
    }

    public void setFichaTecnica(FichaTecnica fichaTecnica) {
        this.fichaTecnica = fichaTecnica;
    }

    public boolean hasFichaTecnica() {
        return fichaTecnica != null && fichaTecnica.getItens() != null && !fichaTecnica.getItens().isEmpty();
    }

    public String getCodigo() {
        return codigoBarra;
    }

    public boolean isServico() {
        return unidadeMedida == UnidadeMedida.SERVICO || unidadeMedida == UnidadeMedida.HORA;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getPrecoCompra() {
        return precoCompra;
    }

    public void setPrecoCompra(BigDecimal precoCompra) {
        this.precoCompra = precoCompra;
    }

    public BigDecimal getPercentualIva() {
        return percentualIva;
    }

    public void setPercentualIva(BigDecimal percentualIva) {
        this.percentualIva = percentualIva;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public UnidadeMedida getUnidadeCompra() {
        return unidadeCompra;
    }

    public void setUnidadeCompra(UnidadeMedida unidadeCompra) {
        this.unidadeCompra = unidadeCompra;
    }

    public BigDecimal getFatorConversao() {
        return fatorConversao != null ? fatorConversao : BigDecimal.ONE;
    }

    public void setFatorConversao(BigDecimal fatorConversao) {
        this.fatorConversao = fatorConversao;
    }

    public BigDecimal getStepVenda() {
        return stepVenda;
    }

    public void setStepVenda(BigDecimal stepVenda) {
        this.stepVenda = stepVenda;
    }

    public Integer getCasasDecimaisQuantidade() {
        return casasDecimaisQuantidade;
    }

    public void setCasasDecimaisQuantidade(Integer casasDecimaisQuantidade) {
        this.casasDecimaisQuantidade = casasDecimaisQuantidade;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Integer getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(Integer stockMaximo) {
        this.stockMaximo = stockMaximo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Imposto getImposto() {
        return imposto;
    }

    public void setImposto(Imposto imposto) {
        this.imposto = imposto;
    }

    public Boolean getSujeitoRetencao() {
        return sujeitoRetencao;
    }

    public void setSujeitoRetencao(Boolean sujeitoRetencao) {
        this.sujeitoRetencao = sujeitoRetencao;
    }

    @Override
    public String toString() {
        return nome + " (" + codigoBarra + ")";
    }
}
