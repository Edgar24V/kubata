package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.core.domain.LancamentoContabil;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Entidade para trilha de auditoria dos lançamentos contábeis.
 * Conforme requisitos do SAF-T-AO e normas contabilísticas angolanas.
 */
@Entity
@Table(name = "audit_trail_contabilidade")
@SQLDelete(sql = "UPDATE audit_trail_contabilidade SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class AuditTrailContabilidade extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "lancamento_id", nullable = false)
    private LancamentoContabil lancamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOperacao tipoOperacao; // CREATE, UPDATE, DELETE

    @Column(nullable = false, length = 500)
    private String descricao; // Descrição da alteração

    @Column(name = "valor_anterior", precision = 19, scale = 2)
    private java.math.BigDecimal valorAnterior;

    @Column(name = "valor_novo", precision = 19, scale = 2)
    private java.math.BigDecimal valorNovo;

    @Column(name = "conta_debito_anterior")
    private String contaDebitoAnterior;

    @Column(name = "conta_debito_nova")
    private String contaDebitoNova;

    @Column(name = "conta_credito_anterior")
    private String contaCreditoAnterior;

    @Column(name = "conta_credito_nova")
    private String contaCreditoNova;

    @Column(name = "historico_anterior", length = 500)
    private String historicoAnterior;

    @Column(name = "historico_novo", length = 500)
    private String historicoNovo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId; // Quem fez a alteração

    @Column(name = "usuario_nome", length = 100)
    private String usuarioNome; // Nome do utilizador

    @Column(name = "data_operacao", nullable = false)
    private LocalDateTime dataOperacao;

    @Column(name = "ip_address", length = 50)
    private String ipAddress; // Endereço IP da operação

    @Column(name = "motivo_alteracao", length = 255)
    private String motivoAlteracao; // Justificativa para a alteração

    @Column(name = "anulado", nullable = false)
    private Boolean anulado = false; // Se o lançamento foi anulado

    @Column(name = "data_anulacao")
    private LocalDateTime dataAnulacao;

    @Column(name = "usuario_anulacao_id")
    private Long usuarioAnulacaoId;

    @Column(name = "motivo_anulacao", length = 255)
    private String motivoAnulacao;

    public enum TipoOperacao {
        CREATE("Criação"),
        UPDATE("Alteração"),
        DELETE("Eliminação"),
        ANULAR("Anulação"),
        REATIVAR("Reativação");

        private final String descricao;

        TipoOperacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtores
    public AuditTrailContabilidade() {
        this.dataOperacao = LocalDateTime.now();
    }

    public AuditTrailContabilidade(LancamentoContabil lancamento, TipoOperacao tipoOperacao, 
                                    Long usuarioId, String usuarioNome) {
        this();
        this.lancamento = lancamento;
        this.tipoOperacao = tipoOperacao;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
    }

    // Getters e Setters
    public LancamentoContabil getLancamento() {
        return lancamento;
    }

    public void setLancamento(LancamentoContabil lancamento) {
        this.lancamento = lancamento;
    }

    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacao tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public java.math.BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(java.math.BigDecimal valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public java.math.BigDecimal getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(java.math.BigDecimal valorNovo) {
        this.valorNovo = valorNovo;
    }

    public String getContaDebitoAnterior() {
        return contaDebitoAnterior;
    }

    public void setContaDebitoAnterior(String contaDebitoAnterior) {
        this.contaDebitoAnterior = contaDebitoAnterior;
    }

    public String getContaDebitoNova() {
        return contaDebitoNova;
    }

    public void setContaDebitoNova(String contaDebitoNova) {
        this.contaDebitoNova = contaDebitoNova;
    }

    public String getContaCreditoAnterior() {
        return contaCreditoAnterior;
    }

    public void setContaCreditoAnterior(String contaCreditoAnterior) {
        this.contaCreditoAnterior = contaCreditoAnterior;
    }

    public String getContaCreditoNova() {
        return contaCreditoNova;
    }

    public void setContaCreditoNova(String contaCreditoNova) {
        this.contaCreditoNova = contaCreditoNova;
    }

    public String getHistoricoAnterior() {
        return historicoAnterior;
    }

    public void setHistoricoAnterior(String historicoAnterior) {
        this.historicoAnterior = historicoAnterior;
    }

    public String getHistoricoNovo() {
        return historicoNovo;
    }

    public void setHistoricoNovo(String historicoNovo) {
        this.historicoNovo = historicoNovo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public LocalDateTime getDataOperacao() {
        return dataOperacao;
    }

    public void setDataOperacao(LocalDateTime dataOperacao) {
        this.dataOperacao = dataOperacao;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMotivoAlteracao() {
        return motivoAlteracao;
    }

    public void setMotivoAlteracao(String motivoAlteracao) {
        this.motivoAlteracao = motivoAlteracao;
    }

    public Boolean getAnulado() {
        return anulado;
    }

    public void setAnulado(Boolean anulado) {
        this.anulado = anulado;
    }

    public LocalDateTime getDataAnulacao() {
        return dataAnulacao;
    }

    public void setDataAnulacao(LocalDateTime dataAnulacao) {
        this.dataAnulacao = dataAnulacao;
    }

    public Long getUsuarioAnulacaoId() {
        return usuarioAnulacaoId;
    }

    public void setUsuarioAnulacaoId(Long usuarioAnulacaoId) {
        this.usuarioAnulacaoId = usuarioAnulacaoId;
    }

    public String getMotivoAnulacao() {
        return motivoAnulacao;
    }

    public void setMotivoAnulacao(String motivoAnulacao) {
        this.motivoAnulacao = motivoAnulacao;
    }
}
