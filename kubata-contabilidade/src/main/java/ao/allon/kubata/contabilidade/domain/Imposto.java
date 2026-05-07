package ao.allon.kubata.contabilidade.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "impostos")
@SQLDelete(sql = "UPDATE impostos SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Imposto extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(name = "is_ativo", nullable = false)
    private Boolean isAtivo = true;

    @Column(name = "is_retencao_fonte")
    private Boolean isRetencaoFonte = false;

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

    public BigDecimal getPercentual() {
        return percentual;
    }

    public void setPercentual(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public Boolean getIsAtivo() {
        return isAtivo;
    }

    public void setIsAtivo(Boolean isAtivo) {
        this.isAtivo = isAtivo;
    }

    public Boolean getIsRetencaoFonte() {
        return isRetencaoFonte;
    }

    public void setIsRetencaoFonte(Boolean isRetencaoFonte) {
        this.isRetencaoFonte = isRetencaoFonte;
    }

    @Override
    public String toString() {
        return descricao + " (" + percentual + "%)";
    }
}
