package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_modulo_sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;  // BILLING, HR, STOCK, ADMIN, POS

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "versao", length = 20)
    private String versao;

    @Column(name = "versao_minima_core", length = 20)
    private String versaoMinimaCore;

    @Column(name = "icone_classe", length = 100)
    private String iconeClasse;

    @Column(name = "cor_hex", length = 7)
    private String corHex;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoModulo estado = EstadoModulo.DISPONIVEL;

    @Column(name = "obrigatorio")
    @Builder.Default
    private Boolean obrigatorio = false;

    @Column(name = "instalado_em")
    private LocalDateTime instaladoEm;

    @Column(name = "desactivado_em")
    private LocalDateTime desactivadoEm;

    @Column(name = "licenca_chave", length = 200)
    private String licencaChave;

    @Column(name = "licenca_validade")
    private LocalDateTime licencaValidade;

    @Column(name = "ordem_menu")
    @Builder.Default
    private Integer ordemMenu = 99;

    @Override
    public String toString() {
        return (nome != null ? nome : (codigo != null ? codigo : "Módulo sem nome"));
    }

    public enum EstadoModulo {
        DISPONIVEL("Disponível"),
        ACTIVO("Activo"),
        INACTIVO("Inactivo"),
        ERRO("Erro"),
        ACTUALIZACAO_PENDENTE("Actualização Pendente");

        private final String descricao;
        EstadoModulo(String d) { this.descricao = d; }
        @Override public String toString() { return descricao; }
    }
}
