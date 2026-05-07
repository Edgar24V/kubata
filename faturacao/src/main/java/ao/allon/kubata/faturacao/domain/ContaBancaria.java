package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.domain.enums.BancoAngola;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "contas_bancarias")
@SQLDelete(sql = "UPDATE contas_bancarias SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class ContaBancaria extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BancoAngola banco;

    @Column(nullable = false, unique = true)
    private String numeroConta;

    @Column(nullable = false, unique = true)
    private String iban;

    private String swift;

    @Column(nullable = false, length = 3)
    private String moeda = "AOA"; // Kwanza por padrão

    @Column(nullable = false)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false)
    private String descricao;

    public ContaBancaria() {}

    public ContaBancaria(BancoAngola banco, String numeroConta, String iban, String descricao) {
        this.banco = banco;
        this.numeroConta = numeroConta;
        this.iban = iban;
        this.descricao = descricao;
    }

    public BancoAngola getBanco() {
        return banco;
    }

    public void setBanco(BancoAngola banco) {
        this.banco = banco;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getSwift() {
        return swift;
    }

    public void setSwift(String swift) {
        this.swift = swift;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao + " - " + (banco != null ? banco.name() : "S/B") + " (" + moeda + ")";
    }
}
