package ao.allon.kubata.core.domain;

import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.domain.enums.NaturezaConta;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plano_contas")
@SQLDelete(sql = "UPDATE plano_contas SET active = false WHERE id = ?")
@SQLRestriction("active = true")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoConta extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String codigo; // Ex: 1, 11, 11.1

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClasseConta classe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NaturezaConta natureza;

    @Column(nullable = false)
    private Integer nivel; // 1 (Classe), 2 (Conta), 3 (Subconta)

    @Column(nullable = false)
    private Boolean movimento; // true se aceita lançamentos

    @ManyToOne
    @JoinColumn(name = "conta_pai_id")
    private PlanoConta contaPai;

    @OneToMany(mappedBy = "contaPai", fetch = FetchType.LAZY)
    private List<PlanoConta> subContas = new ArrayList<>();

    @Override
    public String toString() {
        return (codigo != null ? codigo : "") + " - " + (descricao != null ? descricao : "");
    }

    public PlanoConta(String codigo, String descricao, ClasseConta classe, NaturezaConta natureza, Integer nivel, Boolean movimento, PlanoConta contaPai) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.classe = classe;
        this.natureza = natureza;
        this.nivel = nivel;
        this.movimento = movimento;
        this.contaPai = contaPai;
    }
}
