package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "adm_conexao_auxiliar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConexaoAuxiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(name = "jdbc_url", nullable = false, length = 500)
    private String jdbcUrl;

    @Column(length = 100)
    private String username;

    @Column(name = "password_enc", length = 1000)
    private String passwordEnc;

    @Column(name = "driver_class", length = 200)
    private String driverClass;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "actualizado_em")
    private LocalDateTime actualizadoEm;
}