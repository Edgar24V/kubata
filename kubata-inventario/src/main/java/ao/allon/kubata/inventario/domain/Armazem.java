package ao.allon.kubata.inventario.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.inventario.enums.Provincia;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "armazens")
@SQLDelete(sql = "UPDATE armazens SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Armazem extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;

    @Enumerated(EnumType.STRING)
    private Provincia provincia;

    private String municipio;

    @Column(columnDefinition = "TEXT")
    private String endereco;
    
    @Column(columnDefinition = "TEXT")
    private String descricao;
    
    private String responsavel;

    private String telefone;

    @Column(name = "is_principal", nullable = false)
    private Boolean isPrincipal = false;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Boolean getIsPrincipal() {
        return isPrincipal;
    }

    public void setIsPrincipal(Boolean isPrincipal) {
        this.isPrincipal = isPrincipal;
    }

    @Override
    public String toString() {
        return nome + (Boolean.TRUE.equals(isPrincipal) ? " (Principal)" : "");
    }
}
