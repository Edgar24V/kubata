package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "adm_serie_documento",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"tipo_documento", "serie", "empresa_id"}
    ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // ── Identificação da Série ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumentoSAFT tipoDocumento;

    @Column(name = "serie", nullable = false, length = 10)
    @NotBlank
    private String serie;  // Ex: A, B, 2024, SEDE

    @Column(name = "descricao", length = 200)
    private String descricao;  // Ex: "Série Principal 2024"

    // ── Numeração ─────────────────────────────────────────────────
    @Column(name = "ultimo_numero", nullable = false)
    @Builder.Default
    private Long ultimoNumero = 0L;

    @Column(name = "numero_inicial", nullable = false)
    @Builder.Default
    private Long numeroInicial = 1L;

    @Column(name = "prefixo", length = 10)
    private String prefixo;  // Ex: FT, FR, NC

    @Column(name = "formato_numero", length = 30)
    @Builder.Default
    private String formatoNumero = "{PREFIXO} {SERIE}/{NUMERO}";

    // ── Validade ──────────────────────────────────────────────────
    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "exercicio")
    private Integer exercicio;  // Ano fiscal: 2024, 2025

    // ── Estado ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoSerie estado = EstadoSerie.ACTIVA;

    @Column(name = "predefinida")
    @Builder.Default
    private Boolean predefinida = false;

    // ── Comunicação AGT ───────────────────────────────────────────
    @Column(name = "registada_agt")
    @Builder.Default
    private Boolean registadaAGT = false;

    @Column(name = "data_registo_agt")
    private LocalDateTime dataRegistoAGT;

    @Column(name = "codigo_validacao_agt", length = 50)
    private String codigoValidacaoAGT;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Override
    public String toString() {
        return (tipoDocumento != null ? tipoDocumento.getPrefixo() : "") + " " + (serie != null ? serie : "");
    }

    public enum TipoDocumentoSAFT {
        FT("FT", "Factura", "Vendas"),
        FR("FR", "Factura Recibo", "Vendas"),
        NC("NC", "Nota de Crédito", "Vendas"),
        ND("ND", "Nota de Débito", "Vendas"),
        GD("GD", "Guia de Devolução", "Stock"),
        GT("GT", "Guia de Transporte", "Stock"),
        GR("GR", "Guia de Remessa", "Stock"),
        CC("CC", "Consulta de Cotação", "Vendas"),
        ORC("ORC", "Orçamento", "Vendas"),
        ECF("ECF", "Extracto de Conta Factura", "Financeiro"),
        RG("RG", "Recibo", "Financeiro"),
        PG("PG", "Pagamento", "Financeiro"),
        DC("DC", "Documento de Compra", "Compras"),
        VD("VD", "Venda a Dinheiro", "Vendas");

        private final String prefixo;
        private final String nome;
        private final String area;

        TipoDocumentoSAFT(String prefixo, String nome, String area) {
            this.prefixo = prefixo;
            this.nome = nome;
            this.area = area;
        }

        public String getPrefixo() { return prefixo; }
        public String getNome() { return nome; }
        public String getArea() { return area; }
        public String getDescricao() { return prefixo + " - " + nome; }
        @Override public String toString() { return getDescricao(); }
    }

    public enum EstadoSerie {
        ACTIVA("Activa"),
        INACTIVA("Inactiva"),
        ENCERRADA("Encerrada"),
        PENDENTE_AGT("Pendente Registo AGT");

        private final String descricao;
        EstadoSerie(String d) { this.descricao = d; }
        public String getDescricao() { return descricao; }
        @Override public String toString() { return descricao; }
    }
}
