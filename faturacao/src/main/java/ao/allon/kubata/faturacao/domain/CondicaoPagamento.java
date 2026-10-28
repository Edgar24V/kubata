package ao.allon.kubata.faturacao.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "condicoes_pagamento")
public class CondicaoPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(name = "prazo_dias", nullable = false)
    private Integer prazoDias = 0;

    @Column(nullable = false)
    private boolean ativa = true;

    public CondicaoPagamento() {
    }

    public CondicaoPagamento(String codigo, String descricao, Integer prazoDias) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.prazoDias = prazoDias != null ? prazoDias : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public void setPrazoDias(Integer prazoDias) {
        this.prazoDias = prazoDias;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
