package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "adm_perfil_acesso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PerfilAcesso {

    @EqualsAndHashCode.Include

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;  // Ex: ADMIN, FATURADOR, CONSULTOR

    @Column(name = "descricao", nullable = false, length = 100)
    private String descricao;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "sistema")
    @Builder.Default
    private Boolean sistema = false;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PermissaoPerfil> permissoes = new HashSet<>();

    @Override
    public String toString() {
        return codigo != null ? codigo : (descricao != null ? descricao : "Perfil #" + id);
    }
}
