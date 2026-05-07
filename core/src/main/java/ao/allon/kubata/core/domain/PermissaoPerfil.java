package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adm_permissao_perfil",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"perfil_id", "modulo", "recurso", "operacao"}
    ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissaoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilAcesso perfil;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;       // Ex: FATURACAO, CLIENTES, STOCK

    @Column(name = "recurso", nullable = false, length = 50)
    private String recurso;      // Ex: FATURAS, CLIENTES, PRODUTOS

    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false)
    private Operacao operacao;   // VER, CRIAR, EDITAR, APAGAR, IMPRIMIR, EXPORTAR

    @Column(name = "permitido", nullable = false)
    @Builder.Default
    private Boolean permitido = false;

    @Column(name = "valor_restricao", length = 255)
    private String valorRestricao; // Ex: Serie=2026, Armazem=A1

    public enum Operacao {
        VER, CRIAR, EDITAR, APAGAR, IMPRIMIR, EXPORTAR, APROVAR, ANULAR,
        ALTERAR_PRECO_MARGEM, FORCAR_CREDITO, APROVACAO_INTERNA
    }
}
