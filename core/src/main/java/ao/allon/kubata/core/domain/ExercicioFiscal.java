package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "adm_exercicio_fiscal",
    uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "ano"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExercicioFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "ano", nullable = false)
    private Integer ano;  // 2024, 2025

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoExercicio estado = EstadoExercicio.ABERTO;

    @Column(name = "encerrado_em")
    private java.time.LocalDateTime encerradoEm;

    @Column(name = "encerrado_por", length = 50)
    private String encerradoPor;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Override
    public String toString() {
        return ano != null ? "Exercício " + ano : "Novo Exercício";
    }

    public enum EstadoExercicio {
        FUTURO("Futuro"),
        ABERTO("Aberto"),
        ENCERRAMENTO("Em Encerramento"),
        FECHADO("Fechado");

        private final String descricao;
        EstadoExercicio(String d) { this.descricao = d; }
        @Override public String toString() { return descricao; }
    }
}
