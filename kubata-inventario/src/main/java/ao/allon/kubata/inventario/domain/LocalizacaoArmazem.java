package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "localizacoes_armazem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"armazem_id", "codigo"}))
public class LocalizacaoArmazem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false)
    private Armazem armazem;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String descricao;

    @Column(name = "localizacao_pai_id")
    private Long localizacaoPaiId;

    @Column(name = "permite_stock", nullable = false)
    private Boolean permiteStock = true;

    public Armazem getArmazem() { return armazem; }
    public void setArmazem(Armazem armazem) { this.armazem = armazem; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Long getLocalizacaoPaiId() { return localizacaoPaiId; }
    public void setLocalizacaoPaiId(Long localizacaoPaiId) { this.localizacaoPaiId = localizacaoPaiId; }

    public Boolean getPermiteStock() { return permiteStock; }
    public void setPermiteStock(Boolean permiteStock) { this.permiteStock = permiteStock; }

    @Override
    public String toString() { return codigo + " — " + descricao; }
}
