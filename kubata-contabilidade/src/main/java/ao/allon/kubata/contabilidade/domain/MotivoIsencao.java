package ao.allon.kubata.contabilidade.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "motivos_isencao")
@SQLDelete(sql = "UPDATE motivos_isencao SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class MotivoIsencao extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "is_ativo", nullable = false)
    private Boolean isAtivo = true;

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

    public Boolean getIsAtivo() {
        return isAtivo;
    }

    public void setIsAtivo(Boolean isAtivo) {
        this.isAtivo = isAtivo;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
