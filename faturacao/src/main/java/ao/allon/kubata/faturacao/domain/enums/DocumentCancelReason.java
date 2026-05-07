package ao.allon.kubata.faturacao.domain.enums;

/**
 * Motivo de anulação do documento fiscal conforme Decreto Executivo n.º 683/25 (Anexo I).
 * 
 * Valores permitidos quando documentStatus = "A" (Anulado):
 * - I: Incorreta identificação - Erro na identificação do adquirente
 * - N: Não enviado ao adquirente - Documento não foi entregue ao cliente
 * 
 * @see <a href="https://portalcontribuinte.minfin.gov.ao">Portal Contribuinte AGT</a>
 */
public enum DocumentCancelReason {
    
    /**
     * I - Incorreta identificação
     * Erro na identificação do adquirente (NIF, nome, morada, etc.)
     */
    INCORRETA_IDENTIFICACAO("I", "Incorreta Identificação", "Erro na identificação do adquirente"),
    
    /**
     * N - Não enviado ao adquirente
     * Documento não foi entregue/senviado ao cliente
     */
    NAO_ENVIADO("N", "Não Enviado ao Adquirente", "Documento não foi entregue ao cliente");
    
    private final String codigo;
    private final String descricao;
    private final String detalhes;
    
    DocumentCancelReason(String codigo, String descricao, String detalhes) {
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
     * Retorna o DocumentCancelReason pelo código (I, N).
     * 
     * @param codigo Código do motivo
     * @return DocumentCancelReason correspondente ou null se não encontrado
     */
    public static DocumentCancelReason fromCodigo(String codigo) {
        if (codigo == null) return null;
        
        for (DocumentCancelReason reason : values()) {
            if (reason.codigo.equalsIgnoreCase(codigo)) {
                return reason;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return codigo + " - " + descricao;
    }
}
