package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "retencoes_fonte")
public class RetencaoFonte extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxa;

    @Column(name = "tipo_rendimento", nullable = false, length = 50)
    private String tipoRendimento;

    public RetencaoFonte() {
    }

    public RetencaoFonte(String codigo, String descricao, BigDecimal taxa, String tipoRendimento) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.taxa = taxa;
        this.tipoRendimento = tipoRendimento;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public void setTaxa(BigDecimal taxa) {
        this.taxa = taxa;
    }

    public String getTipoRendimento() {
        return tipoRendimento;
    }

    public void setTipoRendimento(String tipoRendimento) {
        this.tipoRendimento = tipoRendimento;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao + " (" + taxa + "%)";
    }
}
