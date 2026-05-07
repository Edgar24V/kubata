package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "adm_moeda")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Moeda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_iso", nullable = false, unique = true, length = 3)
    private String codigoISO;  // AOA, USD, EUR, GBP

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "simbolo", length = 10)
    private String simbolo;  // Kz, $, €

    @Column(name = "taxa_cambio", precision = 18, scale = 6)
    private BigDecimal taxaCambio;

    @Column(name = "data_taxa_cambio")
    private LocalDate dataTaxaCambio;

    @Column(name = "moeda_base")
    @Builder.Default
    private Boolean moedaBase = false;

    @Column(name = "casas_decimais")
    @Builder.Default
    private Integer casasDecimais = 2;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    @Override
    public String toString() {
        return (codigoISO != null ? codigoISO : "") + " (" + (simbolo != null ? simbolo : "") + ") - " + (nome != null ? nome : "");
    }
}
