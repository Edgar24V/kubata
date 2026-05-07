package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_parametro_sistema",
    uniqueConstraints = @UniqueConstraint(columnNames = {"chave", "empresa_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;  // null = parâmetro global

    @Column(name = "chave", nullable = false, length = 100)
    private String chave;  // Ex: IVA_PADRAO, MOEDA_BASE, DIAS_VENCIMENTO

    @Column(name = "valor", length = 1000)
    private String valor;

    @Column(name = "tipo_valor", length = 20)
    @Builder.Default
    private String tipoValor = "STRING";  // STRING, INTEGER, DECIMAL, BOOLEAN, DATE

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "editavel")
    @Builder.Default
    private Boolean editavel = true;

    @Column(name = "grupo", length = 50)
    private String grupo;  // FISCAL, FINANCEIRO, SISTEMA, INTERFACE

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "atualizado_por", length = 50)
    private String atualizadoPor;
}
