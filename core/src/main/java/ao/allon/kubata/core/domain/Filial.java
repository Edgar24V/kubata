package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "filiais", uniqueConstraints = {
        @UniqueConstraint(name = "uk_filial_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 255)
    private String morada;

    @Column(length = 40)
    private String telefone;

    @Column(length = 150)
    private String email;

    @Override
    public String toString() {
        return codigo + " — " + nome;
    }
}
