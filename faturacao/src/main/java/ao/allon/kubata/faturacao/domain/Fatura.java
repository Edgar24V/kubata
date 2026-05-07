package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faturas")
@SQLDelete(sql = "UPDATE faturas SET active = false WHERE id = ?")
@SQLRestriction("active = true")
public class Fatura extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento = TipoDocumento.FATURA;

    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie;

    @Column(name = "numero_sequencial")
    private Long numeroSequencial;

    @Column(length = 200)
    private String hash;

    @Column(name = "hash_control", length = 4)
    private String hashControl;

    @Column(name = "system_entry_date", nullable = false)
    private LocalDateTime systemEntryDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "hora_emissao", nullable = false)
    private LocalTime horaEmissao;

    @ManyToOne(optional = true)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemFatura> itens = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "total_retencao", nullable = false)
    private BigDecimal totalRetencao = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFatura status = StatusFatura.RASCUNHO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private MetodoPagamento metodoPagamento;

    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pagamento> pagamentos = new ArrayList<>();

    private String observacoes;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    @Column(name = "modo_formacao")
    private Boolean modoFormacao = false;

    @Convert(converter = ao.allon.kubata.faturacao.domain.enums.DocumentStatusConverter.class)
    @Column(name = "document_status", nullable = false, length = 1)
    private DocumentStatus documentStatus = DocumentStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_documento", nullable = false, length = 1)
    private ao.allon.kubata.faturacao.domain.enums.EstadoDocumento estadoDocumento = ao.allon.kubata.faturacao.domain.enums.EstadoDocumento.ORIGINAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_cancel_reason", length = 1)
    private DocumentCancelReason documentCancelReason;

    @Column(name = "rejected_document_no", length = 60)
    private String rejectedDocumentNo;

    @Column(name = "jws_document_signature", length = 256)
    private String jwsDocumentSignature;

    @Column(name = "customer_country", nullable = false, length = 2)
    private String customerCountry = "AO";

    @Column(name = "eac_code", length = 5)
    private String eacCode;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "data_geracao_copia")
    private LocalDateTime dataGeracaoCopia;

    @Column(name = "usuario_geracao_copia_id")
    private Long usuarioGeracaoCopiaId;

    @Column(name = "motivo_geracao_copia", length = 255)
    private String motivoGeracaoCopia;

    @ManyToOne
    @JoinColumn(name = "fatura_original_id")
    private Fatura faturaOriginal;

    @Column(name = "hash_anterior")
    private String hashAnterior;

    @Column(name = "agt_validation_code")
    private String agtValidationCode;

    @Column(name = "agt_submission_status")
    private String agtSubmissionStatus; // PENDING, SUCCESS, FAILED

    @Column(name = "agt_submission_date")
    private LocalDateTime agtSubmissionDate;

    public BigDecimal getTotalImposto() {
        return iva;
    }

    public LocalTime getHoraEmissao() {
        return horaEmissao;
    }

    public void setHoraEmissao(LocalTime horaEmissao) {
        this.horaEmissao = horaEmissao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.systemEntryDate == null) {
            this.systemEntryDate = LocalDateTime.now();
        }
        if (this.dataEmissao == null) {
            this.dataEmissao = LocalDate.now();
        }
        if (this.horaEmissao == null) {
            this.horaEmissao = LocalTime.now();
        }
    }
    @ManyToOne
    @JoinColumn(name = "fatura_referencia_id")
    private Fatura faturaReferencia;

    // Campos para Guias de Transporte/Remessa
    @Column(name = "local_carga")
    private String localCarga;

    @Column(name = "local_descarga")
    private String localDescarga;

    @Column(name = "data_carga")
    private LocalDateTime dataCarga;

    @Column(name = "data_descarga")
    private LocalDateTime dataDescarga;

    @Column(name = "matricula_viatura")
    private String matriculaViatura;

    @Column(name = "tipo_transporte")
    private String tipoTransporte;

    @Column(name = "motorista")
    private String motorista;

    @Column(name = "created_by")
    private String createdBy;

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Serie getSerie() {
        return serie;
    }

    public void setSerie(Serie serie) {
        this.serie = serie;
    }

    public Long getNumeroSequencial() {
        return numeroSequencial;
    }

    public void setNumeroSequencial(Long numeroSequencial) {
        this.numeroSequencial = numeroSequencial;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHashControl() {
        return hashControl;
    }

    public void setHashControl(String hashControl) {
        this.hashControl = hashControl;
    }

    public LocalDateTime getSystemEntryDate() {
        return systemEntryDate;
    }

    public void setSystemEntryDate(LocalDateTime systemEntryDate) {
        this.systemEntryDate = systemEntryDate;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public List<ItemFatura> getItens() {
        return itens;
    }

    public void setItens(List<ItemFatura> itens) {
        this.itens = itens;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotalRetencao() {
        return totalRetencao;
    }

    public void setTotalRetencao(BigDecimal totalRetencao) {
        this.totalRetencao = totalRetencao;
    }

    public StatusFatura getStatus() {
        return status;
    }

    public void setStatus(StatusFatura status) {
        this.status = status;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public List<Pagamento> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<Pagamento> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public LocalDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(LocalDateTime dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public Boolean getModoFormacao() {
        return modoFormacao;
    }

    public void setModoFormacao(Boolean modoFormacao) {
        this.modoFormacao = modoFormacao;
    }

    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
    }

    public ao.allon.kubata.faturacao.domain.enums.EstadoDocumento getEstadoDocumento() {
        return estadoDocumento;
    }

    public void setEstadoDocumento(ao.allon.kubata.faturacao.domain.enums.EstadoDocumento estadoDocumento) {
        this.estadoDocumento = estadoDocumento;
    }

    public DocumentCancelReason getDocumentCancelReason() {
        return documentCancelReason;
    }

    public void setDocumentCancelReason(DocumentCancelReason documentCancelReason) {
        this.documentCancelReason = documentCancelReason;
    }

    public String getRejectedDocumentNo() {
        return rejectedDocumentNo;
    }

    public void setRejectedDocumentNo(String rejectedDocumentNo) {
        this.rejectedDocumentNo = rejectedDocumentNo;
    }

    public String getJwsDocumentSignature() {
        return jwsDocumentSignature;
    }

    public void setJwsDocumentSignature(String jwsDocumentSignature) {
        this.jwsDocumentSignature = jwsDocumentSignature;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }

    public String getEacCode() {
        return eacCode;
    }

    public void setEacCode(String eacCode) {
        this.eacCode = eacCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDateTime getDataGeracaoCopia() {
        return dataGeracaoCopia;
    }

    public void setDataGeracaoCopia(LocalDateTime dataGeracaoCopia) {
        this.dataGeracaoCopia = dataGeracaoCopia;
    }

    public Long getUsuarioGeracaoCopiaId() {
        return usuarioGeracaoCopiaId;
    }

    public void setUsuarioGeracaoCopiaId(Long usuarioGeracaoCopiaId) {
        this.usuarioGeracaoCopiaId = usuarioGeracaoCopiaId;
    }

    public String getMotivoGeracaoCopia() {
        return motivoGeracaoCopia;
    }

    public void setMotivoGeracaoCopia(String motivoGeracaoCopia) {
        this.motivoGeracaoCopia = motivoGeracaoCopia;
    }

    public Fatura getFaturaOriginal() {
        return faturaOriginal;
    }

    public void setFaturaOriginal(Fatura faturaOriginal) {
        this.faturaOriginal = faturaOriginal;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }

    public String getAgtValidationCode() {
        return agtValidationCode;
    }

    public void setAgtValidationCode(String agtValidationCode) {
        this.agtValidationCode = agtValidationCode;
    }

    public String getAgtSubmissionStatus() {
        return agtSubmissionStatus;
    }

    public void setAgtSubmissionStatus(String agtSubmissionStatus) {
        this.agtSubmissionStatus = agtSubmissionStatus;
    }

    public LocalDateTime getAgtSubmissionDate() {
        return agtSubmissionDate;
    }

    public void setAgtSubmissionDate(LocalDateTime agtSubmissionDate) {
        this.agtSubmissionDate = agtSubmissionDate;
    }

    public void addPagamento(Pagamento pagamento) {
        pagamentos.add(pagamento);
        pagamento.setFatura(this);
    }

    public void addItem(ItemFatura item) {
        itens.add(item);
        item.setFatura(this);
        recalculateTotals();
    }

    public void removeItem(ItemFatura item) {
        itens.remove(item);
        item.setFatura(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        this.iva = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.totalRetencao = BigDecimal.ZERO;

        for (ItemFatura item : itens) {
            item.calculateTotals();
            this.subtotal = this.subtotal.add(item.getSubtotal());
            this.iva = this.iva.add(item.getValorIva());
            this.total = this.total.add(item.getTotal());
            
            if (item.getProduto() != null && Boolean.TRUE.equals(item.getProduto().getSujeitoRetencao())) {
                 BigDecimal retencao = item.getSubtotal().multiply(new BigDecimal("0.065"));
                 this.totalRetencao = this.totalRetencao.add(retencao);
            }
        }
    }

    public Fatura getFaturaReferencia() {
        return faturaReferencia;
    }

    public void setFaturaReferencia(Fatura faturaReferencia) {
        this.faturaReferencia = faturaReferencia;
    }

    public String getLocalCarga() {
        return localCarga;
    }

    public void setLocalCarga(String localCarga) {
        this.localCarga = localCarga;
    }

    public String getLocalDescarga() {
        return localDescarga;
    }

    public void setLocalDescarga(String localDescarga) {
        this.localDescarga = localDescarga;
    }

    public LocalDateTime getDataCarga() {
        return dataCarga;
    }

    public void setDataCarga(LocalDateTime dataCarga) {
        this.dataCarga = dataCarga;
    }

    public LocalDateTime getDataDescarga() {
        return dataDescarga;
    }

    public void setDataDescarga(LocalDateTime dataDescarga) {
        this.dataDescarga = dataDescarga;
    }

    public String getMatriculaViatura() {
        return matriculaViatura;
    }

    public void setMatriculaViatura(String matriculaViatura) {
        this.matriculaViatura = matriculaViatura;
    }

    public String getTipoTransporte() {
        return tipoTransporte;
    }

    public void setTipoTransporte(String tipoTransporte) {
        this.tipoTransporte = tipoTransporte;
    }

    public String getMotorista() {
        return motorista;
    }

    public void setMotorista(String motorista) {
        this.motorista = motorista;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    @Override
    public String toString() {
        return "Fatura{id=" + getId() + ", numero='" + numero + "', status=" + status + ", total=" + total + "}";
    }
}
