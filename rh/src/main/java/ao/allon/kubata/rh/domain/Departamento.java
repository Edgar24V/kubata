package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "rh_departamentos")
public class Departamento extends BaseEntity {

    @NotBlank(message = "Nome do departamento é obrigatório")
    @Size(max = 100, message = "Nome não pode exceder 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Size(max = 20, message = "Sigla não pode exceder 20 caracteres")
    @Column(name = "sigla", length = 20)
    private String sigla;

    @Column(name = "descricao", length = 500)
    private String descricao;

    public Departamento() {}

    public Departamento(String nome) {
        this.nome = nome;
        this.setActive(true);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
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
