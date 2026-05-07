package ao.allon.kubata.faturacao.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emails_enviados")
public class EmailEnviado extends BaseEntity {

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String assunto;

    @Column(name = "corpo", length = 4000)
    private String corpo;

    @Column(name = "anexo_nome")
    private String anexoNome;

    @Column(name = "anexo_caminho")
    private String anexoCaminho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEmail status = StatusEmail.PENDENTE;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "tentativas")
    private Integer tentativas = 0;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;

    @Column(name = "fatura_id")
    private Long faturaId;

    @Column(name = "fatura_numero")
    private String faturaNumero;

    @Column(name = "enviado_por")
    private String enviadoPor;

    @Column(name = "tipo_email")
    @Enumerated(EnumType.STRING)
    private TipoEmail tipo = TipoEmail.FATURA;

    public enum StatusEmail {
        PENDENTE,
        ENVIADO,
        ERRO,
        CANCELADO
    }

    public enum TipoEmail {
        FATURA,
        RECIBO,
        ORCAMENTO,
        RELATORIO,
        NOTIFICACAO,
        MARKETING
    }

    // Getters e Setters
    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public void setAssunto(String assunto) {
        this.assunto = assunto;
    }

    public String getCorpo() {
        return corpo;
    }

    public void setCorpo(String corpo) {
        this.corpo = corpo;
    }

    public String getAnexoNome() {
        return anexoNome;
    }

    public void setAnexoNome(String anexoNome) {
        this.anexoNome = anexoNome;
    }

    public String getAnexoCaminho() {
        return anexoCaminho;
    }

    public void setAnexoCaminho(String anexoCaminho) {
        this.anexoCaminho = anexoCaminho;
    }

    public StatusEmail getStatus() {
        return status;
    }

    public void setStatus(StatusEmail status) {
        this.status = status;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public Integer getTentativas() {
        return tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public Long getFaturaId() {
        return faturaId;
    }

    public void setFaturaId(Long faturaId) {
        this.faturaId = faturaId;
    }

    public String getFaturaNumero() {
        return faturaNumero;
    }

    public void setFaturaNumero(String faturaNumero) {
        this.faturaNumero = faturaNumero;
    }

    public String getEnviadoPor() {
        return enviadoPor;
    }

    public void setEnviadoPor(String enviadoPor) {
        this.enviadoPor = enviadoPor;
    }

    public TipoEmail getTipo() {
        return tipo;
    }

    public void setTipo(TipoEmail tipo) {
        this.tipo = tipo;
    }
}
