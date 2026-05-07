package ao.allon.kubata.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String nif;

    @Column(name = "capital_social", precision = 19, scale = 4)
    private BigDecimal capitalSocial;

    private String morada;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private String locality;

    private String telefone;

    private String fax;

    private String email;

    private String website;

    @Column(name = "nif_fiscal")
    private String nifFiscal;

    @Column(name = "nif_seguranca_social")
    private String nifSegurancaSocial;

    @Column(name = "regime_fiscal")
    private String regimeFiscal;

    private String cae;

    @Column(name = "nome_comercial")
    private String nomeComercial;

    @Column(name = "tipo_contribuinte")
    private String tipoContribuinte;

    private String municipio;
    private String provincia;
    private String pais = "AO";

    @Column(name = "caixa_postal")
    private String caixaPostal;

    private String telemovel;
    private String iban;
    private String banco;

    @Column(name = "conta_bancaria")
    private String contaBancaria;

    @Column(name = "descricao_actividade")
    private String descricaoActividade;

    @Column(name = "data_constituicao")
    private java.time.LocalDate dataConstituicao;

    private String conservatoria;

    @Column(name = "matricula_comercial")
    private String matriculaComercial;

    @Column(name = "numero_certificado_agt")
    private String numeroCertificadoAGT;

    @Column(name = "versao_certificado_agt")
    private String versaoCertificadoAGT;

    @Column(name = "data_certificado_agt")
    private java.time.LocalDate dataCertificadoAGT;

    @Column(name = "hash_certificado_agt")
    private String hashCertificadoAGT;

    @Column(length = 3)
    private String identificador;

    @Column(name = "bairro_fiscal")
    private String bairroFiscal;

    @Column(name = "volume_negocios_previsto", precision = 19, scale = 4)
    private BigDecimal volumeNegociosPrevisto;

    @Column(name = "capital_nacional", precision = 19, scale = 4)
    private BigDecimal capitalNacional;

    @Override
    public String toString() {
        return nome != null ? nome : "Empresa sem nome";
    }

    @Column(name = "capital_estrangeiro", precision = 19, scale = 4)
    private BigDecimal capitalEstrangeiro;

    @Column(name = "capital_publico", precision = 19, scale = 4)
    private BigDecimal capitalPublico;

    @Column(name = "ano_inicio")
    private Integer anoInicio;

    @Column(name = "moeda_base")
    private String moedaBase = "AOA";

    @Column(name = "moeda_alternativa")
    private String moedaAlternativa;

    @Column(name = "casas_decimais_valor")
    private Integer casasDecimaisValor = 2;

    @Column(name = "casas_decimais_quantidade")
    private Integer casasDecimaisQuantidade = 3;

    @Column(name = "exercicio_actual")
    private Integer exercicioActual;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "logotipo")
    private byte[] logotipo;

    @Column(name = "logotipo_mime_type")
    private String logotipoMimeType;

    @Column(name = "rodape_documento")
    private String rodapeDocumento;

    @Column(name = "mensagem_fatura")
    private String mensagemFatura;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(columnDefinition = "TEXT")
    private String modulos;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNomeComercial() { return nomeComercial; }
    public void setNomeComercial(String nomeComercial) { this.nomeComercial = nomeComercial; }

    public String getTipoContribuinte() { return tipoContribuinte; }
    public void setTipoContribuinte(String tipoContribuinte) { this.tipoContribuinte = tipoContribuinte; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getCaixaPostal() { return caixaPostal; }
    public void setCaixaPostal(String caixaPostal) { this.caixaPostal = caixaPostal; }

    public String getTelemovel() { return telemovel; }
    public void setTelemovel(String telemovel) { this.telemovel = telemovel; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getContaBancaria() { return contaBancaria; }
    public void setContaBancaria(String contaBancaria) { this.contaBancaria = contaBancaria; }

    public String getDescricaoActividade() { return descricaoActividade; }
    public void setDescricaoActividade(String descricaoActividade) { this.descricaoActividade = descricaoActividade; }

    public java.time.LocalDate getDataConstituicao() { return dataConstituicao; }
    public void setDataConstituicao(java.time.LocalDate dataConstituicao) { this.dataConstituicao = dataConstituicao; }

    public String getConservatoria() { return conservatoria; }
    public void setConservatoria(String conservatoria) { this.conservatoria = conservatoria; }

    public String getMatriculaComercial() { return matriculaComercial; }
    public void setMatriculaComercial(String matriculaComercial) { this.matriculaComercial = matriculaComercial; }

    public String getNumeroCertificadoAGT() { return numeroCertificadoAGT; }
    public void setNumeroCertificadoAGT(String numeroCertificadoAGT) { this.numeroCertificadoAGT = numeroCertificadoAGT; }

    public String getVersaoCertificadoAGT() { return versaoCertificadoAGT; }
    public void setVersaoCertificadoAGT(String versaoCertificadoAGT) { this.versaoCertificadoAGT = versaoCertificadoAGT; }

    public java.time.LocalDate getDataCertificadoAGT() { return dataCertificadoAGT; }
    public void setDataCertificadoAGT(java.time.LocalDate dataCertificadoAGT) { this.dataCertificadoAGT = dataCertificadoAGT; }

    public String getHashCertificadoAGT() { return hashCertificadoAGT; }
    public void setHashCertificadoAGT(String hashCertificadoAGT) { this.hashCertificadoAGT = hashCertificadoAGT; }

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }

    public String getBairroFiscal() { return bairroFiscal; }
    public void setBairroFiscal(String bairroFiscal) { this.bairroFiscal = bairroFiscal; }

    public BigDecimal getVolumeNegociosPrevisto() { return volumeNegociosPrevisto; }
    public void setVolumeNegociosPrevisto(BigDecimal volumeNegociosPrevisto) { this.volumeNegociosPrevisto = volumeNegociosPrevisto; }

    public BigDecimal getCapitalNacional() { return capitalNacional; }
    public void setCapitalNacional(BigDecimal capitalNacional) { this.capitalNacional = capitalNacional; }

    public BigDecimal getCapitalEstrangeiro() { return capitalEstrangeiro; }
    public void setCapitalEstrangeiro(BigDecimal capitalEstrangeiro) { this.capitalEstrangeiro = capitalEstrangeiro; }

    public BigDecimal getCapitalPublico() { return capitalPublico; }
    public void setCapitalPublico(BigDecimal capitalPublico) { this.capitalPublico = capitalPublico; }

    public Integer getAnoInicio() { return anoInicio; }
    public void setAnoInicio(Integer anoInicio) { this.anoInicio = anoInicio; }

    public String getMoedaBase() { return moedaBase; }
    public void setMoedaBase(String moedaBase) { this.moedaBase = moedaBase; }

    public String getMoedaAlternativa() { return moedaAlternativa; }
    public void setMoedaAlternativa(String moedaAlternativa) { this.moedaAlternativa = moedaAlternativa; }

    public Integer getCasasDecimaisValor() { return casasDecimaisValor; }
    public void setCasasDecimaisValor(Integer casasDecimaisValor) { this.casasDecimaisValor = casasDecimaisValor; }

    public Integer getCasasDecimaisQuantidade() { return casasDecimaisQuantidade; }
    public void setCasasDecimaisQuantidade(Integer casasDecimaisQuantidade) { this.casasDecimaisQuantidade = casasDecimaisQuantidade; }

    public Integer getExercicioActual() { return exercicioActual; }
    public void setExercicioActual(Integer exercicioActual) { this.exercicioActual = exercicioActual; }

    public byte[] getLogotipo() { return logotipo; }
    public void setLogotipo(byte[] logotipo) { this.logotipo = logotipo; }

    public String getLogotipoMimeType() { return logotipoMimeType; }
    public void setLogotipoMimeType(String logotipoMimeType) { this.logotipoMimeType = logotipoMimeType; }

    public String getRodapeDocumento() { return rodapeDocumento; }
    public void setRodapeDocumento(String rodapeDocumento) { this.rodapeDocumento = rodapeDocumento; }

    public String getMensagemFatura() { return mensagemFatura; }
    public void setMensagemFatura(String mensagemFatura) { this.mensagemFatura = mensagemFatura; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public BigDecimal getCapitalSocial() { return capitalSocial; }
    public void setCapitalSocial(BigDecimal capitalSocial) { this.capitalSocial = capitalSocial; }

    public String getMorada() { return morada; }
    public void setMorada(String morada) { this.morada = morada; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getLocalidade() { return locality; }
    public void setLocalidade(String locality) { this.locality = locality; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getNifFiscal() { return nifFiscal; }
    public void setNifFiscal(String nifFiscal) { this.nifFiscal = nifFiscal; }

    public String getNifSegurancaSocial() { return nifSegurancaSocial; }
    public void setNifSegurancaSocial(String nifSegurancaSocial) { this.nifSegurancaSocial = nifSegurancaSocial; }

    public String getRegimeFiscal() { return regimeFiscal; }
    public void setRegimeFiscal(String regimeFiscal) { this.regimeFiscal = regimeFiscal; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public boolean getAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public String getModulos() { return modulos; }
    public void setModulos(String modulos) { this.modulos = modulos; }
}
