package ao.allon.kubata.faturacao.domain.enums;

/**
 * Estado do documento fiscal conforme Decreto Executivo n.º 683/25 (Anexo I).
 * 
 * Representa o status do documento para fins de comunicação à AGT:
 * - N: Normal - Documento fiscal regular
 * - S: Autofacturação - Documento emitido pelo adquirente (autofacturação)
 * - A: Anulado - Documento cancelado/anulado
 * - C: Correção de rejeitado - Documento que corrige um documento rejeitado pela AGT
 * 
 * @see <a href="https://portalcontribuinte.minfin.gov.ao">Portal Contribuinte AGT</a>
 */
public enum DocumentStatus {
    
    /**
     * N - Normal
     * Documento fiscal emitido em condições normais.
     * Este é o estado padrão para documentos novos.
     */
    NORMAL("N", "Normal", "Documento fiscal regular"),
    
    /**
     * S - Autofacturação
     * Documento emitido pelo adquirente em nome do emitente.
     * Requer autorização prévia da AGT.
     */
    AUTOFATURACAO("S", "Autofacturação", "Documento emitido pelo adquirente"),
    
    /**
     * A - Anulado
     * Documento cancelado/anulado.
     * Obrigatório informar documentCancelReason quando este status é usado.
     */
    ANULADO("A", "Anulado", "Documento cancelado"),
    
    /**
     * C - Correção de rejeitado
     * Documento que corrige um documento previamente rejeitado pela AGT.
     * Obrigatório informar rejectedDocumentNo quando este status é usado.
     */
    CORRECAO_REJEITADO("C", "Correção de Rejeitado", "Documento que corrige um rejeitado");
    
    private final String codigo;
    private final String descricao;
    private final String detalhes;
    
    DocumentStatus(String codigo, String descricao, String detalhes) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.detalhes = detalhes;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public String getDetalhes() {
        return detalhes;
    }
    
    /**
     * Retorna o DocumentStatus pelo código (N, S, A, C).
     * 
     * @param codigo Código do status
     * @return DocumentStatus correspondente ou null se não encontrado
     */
    public static DocumentStatus fromCodigo(String codigo) {
        if (codigo == null) return null;
        
        for (DocumentStatus status : values()) {
            if (status.codigo.equalsIgnoreCase(codigo)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Verifica se o status permite edição do documento.
     * Apenas documentos NORMAL podem ser editados antes de emitidos.
     */
    public boolean isEditavel() {
        return this == NORMAL;
    }
    
    /**
     * Verifica se o status requer motivo de cancelamento.
     */
    public boolean requerMotivoCancelamento() {
        return this == ANULADO;
    }
    
    /**
     * Verifica se o status requer número do documento rejeitado.
     */
    public boolean requerDocumentoRejeitado() {
        return this == CORRECAO_REJEITADO;
    }
    
    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
