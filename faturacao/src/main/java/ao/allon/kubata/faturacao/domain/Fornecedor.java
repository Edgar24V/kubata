package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "fornecedores")
@SQLDelete(sql = "UPDATE fornecedores SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Fornecedor extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String nif;

    private String telefone;

    @Email
    private String email;

    private String endereco;

    @Column(name = "termos_pagamento")
    private String termosPagamento;

    @Column(name = "avaliacao")
    private Integer avaliacao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getTermosPagamento() {
        return termosPagamento;
    }

    public void setTermosPagamento(String termosPagamento) {
        this.termosPagamento = termosPagamento;
    }

    public Integer getAvaliacao() {
        return avaliacao;
    }

    public void setAvaliacao(Integer avaliacao) {
        this.avaliacao = avaliacao;
    }

    @Override
    public String toString() {
        return nome + (nif != null ? " (" + nif + ")" : "");
    }
}
