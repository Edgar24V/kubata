package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "motivos_isencao")
public class MotivoIsencao extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "legislacao", length = 255)
    private String legislacao;

    public MotivoIsencao() {
    }

    public MotivoIsencao(String codigo, String descricao, String legislacao) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.legislacao = legislacao;
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

    public String getLegislacao() {
        return legislacao;
    }

    public void setLegislacao(String legislacao) {
        this.legislacao = legislacao;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
