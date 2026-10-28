package ao.allon.kubata.core.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Fornecedor partilhado por Faturação, Compras e Administrator.
 * Única entidade mapeada para a tabela {@code fornecedores} da BD Kubata.
 */
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

    private String cidade;

    @Column(name = "pessoa_contacto")
    private String pessoaContacto;

    private String observacoes;

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

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getPessoaContacto() {
        return pessoaContacto;
    }

    public void setPessoaContacto(String pessoaContacto) {
        this.pessoaContacto = pessoaContacto;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
