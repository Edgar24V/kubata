package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "impostos")
public class Imposto extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String codigo; // ex: ISE, NOR

    @Column(nullable = false, length = 50)
    private String descricao; // ex: Isento, Normal

    @Column(nullable = false, length = 10)
    private String tipo; // ex: IVA, IS

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual; // ex: 14.00, 0.00

    @ManyToOne
    @JoinColumn(name = "motivo_isencao_id")
    private MotivoIsencao motivoIsencao;

    public Imposto() {
    }

    public Imposto(String codigo, String descricao, String tipo, BigDecimal percentual, MotivoIsencao motivoIsencao) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.percentual = percentual;
        this.motivoIsencao = motivoIsencao;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public void setPercentual(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public MotivoIsencao getMotivoIsencao() {
        return motivoIsencao;
    }

    public void setMotivoIsencao(MotivoIsencao motivoIsencao) {
        this.motivoIsencao = motivoIsencao;
    }

    public String getCodigoIsencao() {
        return motivoIsencao != null ? motivoIsencao.getCodigo() : null;
    }

    public String getDescricaoIsencao() {
        return motivoIsencao != null ? motivoIsencao.getDescricao() : null;
    }

    @Override
    public String toString() {
        return codigo + " (" + percentual + "%)";
    }
}
