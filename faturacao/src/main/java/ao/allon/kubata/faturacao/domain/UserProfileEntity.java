package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.profile.model.UserType;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity extends BaseEntity {
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, unique = true)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType tipo;
    private String telefone;
    private String departamento;
    @Column(length = 2000)
    private String permissoesCsv;
    @Column(length = 2000)
    private String atividadesCsv;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserType getTipo() {
        return tipo;
    }

    public void setTipo(UserType tipo) {
        this.tipo = tipo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getPermissoesCsv() {
        return permissoesCsv;
    }

    public void setPermissoesCsv(String permissoesCsv) {
        this.permissoesCsv = permissoesCsv;
    }

    public String getAtividadesCsv() {
        return atividadesCsv;
    }

    public void setAtividadesCsv(String atividadesCsv) {
        this.atividadesCsv = atividadesCsv;
    }
}
