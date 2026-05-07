package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "adm_taxa_iva")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxaIVA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;  // IVA14, ISE, RED7

    @Column(name = "descricao", nullable = false, length = 100)
    private String descricao;

    @Column(name = "percentagem", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentagem;

    @Column(name = "codigo_saft", length = 10)
    private String codigoSAFT;

    @Column(name = "vigente_desde")
    private LocalDate vigentDesde;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "padrao")
    @Builder.Default
    private Boolean padrao = false;

    @Override
    public String toString() {
        return (descricao != null ? descricao : "") + " (" + (percentagem != null ? percentagem.stripTrailingZeros().toPlainString() : "0") + "%)";
    }
}
