package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamentos_contabeis")
@SQLDelete(sql = "UPDATE lancamentos_contabeis SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class LancamentoContabil extends BaseEntity {

    @Column(nullable = false)
    private LocalDate dataMovimento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_debito_id", nullable = false)
    private PlanoConta contaDebito;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_credito_id", nullable = false)
    private PlanoConta contaCredito;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 500)
    private String historico;

    @Column(name = "documento_origem")
    private String documentoOrigem; // Ex: FAT 2024/001

    @Column(name = "tipo_documento")
    private String tipoDocumento; // Agora como String para maior flexibilidade entre módulos

    @Column(name = "usuario_id")
    private Long usuarioId; // Quem fez o lançamento

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    public LancamentoContabil() {}

    public LancamentoContabil(LocalDate dataMovimento, PlanoConta contaDebito, PlanoConta contaCredito, BigDecimal valor, String historico, String documentoOrigem, String tipoDocumento, Long usuarioId) {
        this.dataMovimento = dataMovimento;
        this.contaDebito = contaDebito;
        this.contaCredito = contaCredito;
        this.valor = valor;
        this.historico = historico;
        this.documentoOrigem = documentoOrigem;
        this.tipoDocumento = tipoDocumento;
        this.usuarioId = usuarioId;
    }

    public LocalDate getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDate dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public PlanoConta getContaDebito() {
        return contaDebito;
    }

    public void setContaDebito(PlanoConta contaDebito) {
        this.contaDebito = contaDebito;
    }

    public PlanoConta getContaCredito() {
        return contaCredito;
    }

    public void setContaCredito(PlanoConta contaCredito) {
        this.contaCredito = contaCredito;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public String getDocumentoOrigem() {
        return documentoOrigem;
    }

    public void setDocumentoOrigem(String documentoOrigem) {
        this.documentoOrigem = documentoOrigem;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "LancamentoContabil{" +
                "id=" + getId() +
                ", dataMovimento=" + dataMovimento +
                ", contaDebito=" + (contaDebito != null ? contaDebito.getCodigo() : "null") +
                ", contaCredito=" + (contaCredito != null ? contaCredito.getCodigo() : "null") +
                ", valor=" + valor +
                ", historico='" + historico + '\'' +
                ", documentoOrigem='" + documentoOrigem + '\'' +
                ", tipoDocumento='" + tipoDocumento + '\'' +
                '}';
    }
}
