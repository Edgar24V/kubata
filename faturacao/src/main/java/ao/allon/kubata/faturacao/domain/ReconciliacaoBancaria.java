package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.core.domain.PlanoConta;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade para reconciliação bancária.
 * Permite conciliar lançamentos contábeis com extratos bancários.
 */
@Entity
@Table(name = "reconciliacao_bancaria")
@SQLDelete(sql = "UPDATE reconciliacao_bancaria SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class ReconciliacaoBancaria extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private PlanoConta contaBancaria; // Conta do plano de contas (12.x)

    @Column(nullable = false)
    private LocalDate dataExtrato; // Data do extrato

    @Column(precision = 19, scale = 2)
    private BigDecimal saldoInicial; // Saldo inicial do extrato

    @Column(precision = 19, scale = 2)
    private BigDecimal saldoFinal; // Saldo final do extrato

    @Column(precision = 19, scale = 2)
    private BigDecimal saldoContabilistico; // Saldo segundo contabilidade

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReconciliacao status; // PENDENTE, CONCILIADO, PARCIAL

    @Column(length = 500)
    private String observacoes;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "data_conciliacao")
    private LocalDateTime dataConciliacao;

    @Column(name = "total_movimentos_extrato")
    private Integer totalMovimentosExtrato;

    @Column(name = "total_movimentos_conciliados")
    private Integer totalMovimentosConciliados;

    @Column(name = "diferenca", precision = 19, scale = 2)
    private BigDecimal diferenca; // Diferença entre extrato e contabilidade

    @Column(name = "arquivo_extrato")
    private String arquivoExtrato; // Referência ao arquivo do extrato importado

    public enum StatusReconciliacao {
        PENDENTE("Pendente"),
        EM_PROCESSAMENTO("Em Processamento"),
        PARCIAL("Parcialmente Conciliado"),
        CONCILIADO("Totalmente Conciliado"),
        COM_DIVERGENCIAS("Com Divergências");

        private final String descricao;

        StatusReconciliacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtores
    public ReconciliacaoBancaria() {
        this.status = StatusReconciliacao.PENDENTE;
        this.totalMovimentosExtrato = 0;
        this.totalMovimentosConciliados = 0;
    }

    public ReconciliacaoBancaria(PlanoConta contaBancaria, LocalDate dataExtrato, Long usuarioId) {
        this();
        this.contaBancaria = contaBancaria;
        this.dataExtrato = dataExtrato;
        this.usuarioId = usuarioId;
    }

    // Getters e Setters
    public PlanoConta getContaBancaria() {
        return contaBancaria;
    }

    public void setContaBancaria(PlanoConta contaBancaria) {
        this.contaBancaria = contaBancaria;
    }

    public LocalDate getDataExtrato() {
        return dataExtrato;
    }

    public void setDataExtrato(LocalDate dataExtrato) {
        this.dataExtrato = dataExtrato;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public BigDecimal getSaldoContabilistico() {
        return saldoContabilistico;
    }

    public void setSaldoContabilistico(BigDecimal saldoContabilistico) {
        this.saldoContabilistico = saldoContabilistico;
    }

    public StatusReconciliacao getStatus() {
        return status;
    }

    public void setStatus(StatusReconciliacao status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getDataConciliacao() {
        return dataConciliacao;
    }

    public void setDataConciliacao(LocalDateTime dataConciliacao) {
        this.dataConciliacao = dataConciliacao;
    }

    public Integer getTotalMovimentosExtrato() {
        return totalMovimentosExtrato;
    }

    public void setTotalMovimentosExtrato(Integer totalMovimentosExtrato) {
        this.totalMovimentosExtrato = totalMovimentosExtrato;
    }

    public Integer getTotalMovimentosConciliados() {
        return totalMovimentosConciliados;
    }

    public void setTotalMovimentosConciliados(Integer totalMovimentosConciliados) {
        this.totalMovimentosConciliados = totalMovimentosConciliados;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }

    public void setDiferenca(BigDecimal diferenca) {
        this.diferenca = diferenca;
    }

    public String getArquivoExtrato() {
        return arquivoExtrato;
    }

    public void setArquivoExtrato(String arquivoExtrato) {
        this.arquivoExtrato = arquivoExtrato;
    }

    /**
     * Calcula a diferença entre saldo do extrato e saldo contabilístico.
     */
    public void calcularDiferenca() {
        if (saldoFinal != null && saldoContabilistico != null) {
            this.diferenca = saldoFinal.subtract(saldoContabilistico);
        }
    }

    /**
     * Atualiza o status baseado nos movimentos conciliados.
     */
    public void atualizarStatus() {
        if (totalMovimentosExtrato == null || totalMovimentosExtrato == 0) {
            this.status = StatusReconciliacao.PENDENTE;
        } else if (totalMovimentosConciliados == null) {
            this.status = StatusReconciliacao.PENDENTE;
        } else if (totalMovimentosConciliados.equals(totalMovimentosExtrato)) {
            this.status = (diferenca == null || diferenca.compareTo(BigDecimal.ZERO) == 0) 
                    ? StatusReconciliacao.CONCILIADO 
                    : StatusReconciliacao.COM_DIVERGENCIAS;
        } else {
            this.status = StatusReconciliacao.PARCIAL;
        }
    }
}
