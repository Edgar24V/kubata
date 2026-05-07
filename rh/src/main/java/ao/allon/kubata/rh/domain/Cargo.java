package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "rh_cargos")
public class Cargo extends BaseEntity {

    @NotBlank(message = "Nome do cargo é obrigatório")
    @Size(max = 100, message = "Nome não pode exceder 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Size(max = 50, message = "Nível não pode exceder 50 caracteres")
    @Column(name = "nivel", length = 50)
    private String nivel;

    @Column(name = "descricao", length = 500)
    private String descricao;

    public Cargo() {}

    public Cargo(String nome) {
        this.nome = nome;
        this.setActive(true);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return nome;
    }
}
