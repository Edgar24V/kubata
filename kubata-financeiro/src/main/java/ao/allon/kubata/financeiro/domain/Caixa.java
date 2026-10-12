package ao.allon.kubata.financeiro.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "caixas")
@SQLDelete(sql = "UPDATE caixas SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Caixa extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private String responsavel;

    @Column(nullable = false)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "is_principal", nullable = false)
    private Boolean isPrincipal = false;

    @Column(name = "is_ativo", nullable = false)
    private Boolean isAtivo = true;

    private String observacoes;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public Boolean getIsPrincipal() {
        return isPrincipal;
    }

    public void setIsPrincipal(Boolean isPrincipal) {
        this.isPrincipal = isPrincipal;
    }

    public Boolean getIsAtivo() {
        return isAtivo;
    }

    public void setIsAtivo(Boolean isAtivo) {
        this.isAtivo = isAtivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return nome + (Boolean.TRUE.equals(isPrincipal) ? " (Principal)" : "");
    }
}
