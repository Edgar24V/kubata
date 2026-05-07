package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "rh_documento_colaborador")
public class DocumentoColaborador extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoDocumento tipo;

    @NotBlank(message = "Nome do documento é obrigatório")
    @Size(max = 255, message = "Nome não pode exceder 255 caracteres")
    @Column(name = "nome_documento", nullable = false, length = 255)
    private String nomeDocumento;

    @NotBlank(message = "Caminho do arquivo é obrigatório")
    @Size(max = 500, message = "Caminho não pode exceder 500 caracteres")
    @Column(name = "caminho_arquivo", nullable = false, length = 500)
    private String caminhoArquivo;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "emissor", length = 100)
    private String emissor; // Entidade emissora do documento

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento; // Número do documento se aplicável

    @Size(max = 1000, message = "Descrição não pode exceder 1000 caracteres")
    @Column(name = "descricao", length = 1000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_por")
    private ao.allon.kubata.core.domain.User uploadPor;

    @Column(name = "data_upload", nullable = false)
    private LocalDate dataUpload = LocalDate.now();

    public enum TipoDocumento {
        BI,                    // Bilhete de Identidade
        PASSAPORTE,           // Passaporte
        NIF,                  // Número de Identificação Fiscal
        CARTEIRA_CONDUCAO,    // Carteira de Condução
        CERTIFICADO_HABILITACOES, // Certificado de Habilitações
        CURRICULUM,           // Currículo Vitae
        CONTRATO_TRABALHO,    // Contrato de Trabalho
        ATESTADO_MEDICO,      // Atestado Médico
        EXAME_MEDICO,         // Exame Médico Admissional/Periódico
        CERTIFICADO_RESIDENCIA, // Certificado de Residência
        DECLARACAO_IRS,       // Declaração IRS
        OUTRO                 // Outro tipo de documento
    }

    public DocumentoColaborador() {
        this.dataUpload = LocalDate.now();
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public void setTipo(TipoDocumento tipo) {
        this.tipo = tipo;
    }

    public String getNomeDocumento() {
        return nomeDocumento;
    }

    public void setNomeDocumento(String nomeDocumento) {
        this.nomeDocumento = nomeDocumento;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getEmissor() {
        return emissor;
    }

    public void setEmissor(String emissor) {
        this.emissor = emissor;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public ao.allon.kubata.core.domain.User getUploadPor() {
        return uploadPor;
    }

    public void setUploadPor(ao.allon.kubata.core.domain.User uploadPor) {
        this.uploadPor = uploadPor;
    }

    public LocalDate getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(LocalDate dataUpload) {
        this.dataUpload = dataUpload;
    }

    /**
     * Verifica se o documento está expirado
     */
    public boolean isExpirado() {
        if (dataValidade == null) {
            return false;
        }
        return LocalDate.now().isAfter(dataValidade);
    }

    /**
     * Verifica se o documento está próximo de expirar (dentro de X dias)
     */
    public boolean isExpirandoEm(int dias) {
        if (dataValidade == null) {
            return false;
        }
        return LocalDate.now().plusDays(dias).isAfter(dataValidade) || LocalDate.now().plusDays(dias).equals(dataValidade);
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + tipo + " - " + nomeDocumento : "DocumentoColaborador";
    }
}
