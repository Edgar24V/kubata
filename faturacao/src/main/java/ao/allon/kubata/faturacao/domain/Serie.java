package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "series")
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String designacao; // Ex: 2024, A, B

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private Integer ano; // Ex: 2024

    @Column(name = "ultimo_numero")
    private Long ultimoNumero = 0L;
    
    @Column(nullable = false)
    private boolean ativa = true;
    
    @Column(nullable = false)
    private boolean padrao = false;
    
    @Column(name = "data_inicio")
    private LocalDate dataInicio;
    
    @Column(name = "data_fim")
    private LocalDate dataFim;
    
    @Column(name = "hash_anterior", length = 512)
    private String hashAnterior = "";

    @Column(name = "agt_series_code", length = 60)
    private String agtSeriesCode;

    @Column(name = "establishment_number", length = 200)
    private String establishmentNumber;

    @Column(name = "series_contingency_indicator", length = 1)
    private String seriesContingencyIndicator;

    @Column(name = "agt_authorized_quantity")
    private Long agtAuthorizedQuantity;

    @Column(name = "agt_first_document_number")
    private Long agtFirstDocumentNumber;

    @Column(name = "agt_last_document_number")
    private Long agtLastDocumentNumber;

    public Serie() {
    }

    public Serie(String designacao, TipoDocumento tipoDocumento, Integer ano) {
        this.designacao = designacao;
        this.tipoDocumento = tipoDocumento;
        this.ano = ano;
        this.dataInicio = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDesignacao() {
        return designacao;
    }

    public void setDesignacao(String designacao) {
        this.designacao = designacao;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Long getUltimoNumero() {
        return ultimoNumero;
    }

    public void setUltimoNumero(Long ultimoNumero) {
        this.ultimoNumero = ultimoNumero;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public boolean isPadrao() {
        return padrao;
    }

    public void setPadrao(boolean padrao) {
        this.padrao = padrao;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }
    
    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }
    
    public String getCodigoSerie() {
        return codigoDocumentoFiscal() + " " + designacao;
    }

    /** Código de documento usado na numeração fiscal electrónica AGT. */
    public String codigoDocumentoFiscal() {
        if (tipoDocumento == TipoDocumento.VENDA_A_DINHEIRO) return "TV";
        return tipoDocumento.getCodigo();
    }

    public boolean isAutorizadaAgt() {
        return agtSeriesCode != null && !agtSeriesCode.isBlank()
            && agtFirstDocumentNumber != null && agtFirstDocumentNumber > 0
            && agtLastDocumentNumber != null && agtLastDocumentNumber >= agtFirstDocumentNumber
            && agtAuthorizedQuantity != null && agtAuthorizedQuantity > 0
            && establishmentNumber != null && !establishmentNumber.isBlank()
            && ("N".equals(seriesContingencyIndicator) || "C".equals(seriesContingencyIndicator));
    }

    public String getAgtSeriesCode() { return agtSeriesCode; }
    public void setAgtSeriesCode(String value) { this.agtSeriesCode = value; }
    public String getEstablishmentNumber() { return establishmentNumber; }
    public void setEstablishmentNumber(String value) { this.establishmentNumber = value; }
    public String getSeriesContingencyIndicator() { return seriesContingencyIndicator; }
    public void setSeriesContingencyIndicator(String value) { this.seriesContingencyIndicator = value; }
    public Long getAgtAuthorizedQuantity() { return agtAuthorizedQuantity; }
    public void setAgtAuthorizedQuantity(Long value) { this.agtAuthorizedQuantity = value; }
    public Long getAgtFirstDocumentNumber() { return agtFirstDocumentNumber; }
    public void setAgtFirstDocumentNumber(Long value) { this.agtFirstDocumentNumber = value; }
    public Long getAgtLastDocumentNumber() { return agtLastDocumentNumber; }
    public void setAgtLastDocumentNumber(Long value) { this.agtLastDocumentNumber = value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Serie serie = (Serie) o;
        return Objects.equals(id, serie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return designacao + " (" + ano + ")";
    }
}
