package ao.allon.kubata.faturacao.domain.enums;

/**
 * Estado do documento fiscal conforme normas SAF-T-AO (AGT Angola).
 * 
 * Segundo a legislação angolana, todo documento fiscal deve ter um estado que indica
 * se é original, duplicado, segunda via ou emitido por terceiros.
 * 
 * Valores possíveis conforme SAF-T-AO:
 * - O: Original - Documento emitido pela primeira vez
 * - D: Duplicado - Cópia do documento original para o emitente
 * - S: Segunda Via - Cópia do documento original para o cliente/adquirente
 * - E: Emissão em terceiros - Documento emitido por terceiros em nome do emitente
 * 
 * @see <a href="https://github.com/assoft-portugal/SAF-T-AO">SAF-T-AO Specification</a>
 */
public enum EstadoDocumento {
    
    /** 
     * O - Original 
     * Documento emitido pela primeira vez e válido fiscalmente.
     * Este é o estado padrão para todos os documentos novos.
     */
    ORIGINAL("O", "Original", "Documento emitido pela primeira vez"),
    
    /**
     * D - Duplicado
     * Cópia do documento original destinada ao emitente para arquivo.
     * Deve conter a indicação "DUPLICADO" visível no documento.
     */
    DUPLICADO("D", "Duplicado", "Cópia do documento original para o emitente"),
    
    /**
     * S - Segunda Via
     * Cópia do documento original destinada ao cliente/adquirente.
     * Usado quando o cliente solicita uma nova cópia do documento.
     * Deve conter a indicação "SEGUNDA VIA" visível no documento.
     */
    SEGUNDA_VIA("S", "Segunda Via", "Cópia do documento original para o cliente"),
    
    /**
     * E - Emissão em terceiros
     * Documento emitido por terceiros em nome e por conta do emitente.
     * Requer autorização prévia da AGT.
     * Deve conter identificação do terceiro emitente.
     */
    EMISSAO_TERCEIROS("E", "Emissão em Terceiros", "Documento emitido por terceiros em nome do emitente");
    
    private final String codigo;
    private final String descricao;
    private final String detalhes;
    
    EstadoDocumento(String codigo, String descricao, String detalhes) {
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
     * Retorna o EstadoDocumento pelo código SAF-T-AO.
     * 
     * @param codigo Código do estado (O, D, S, E)
     * @return EstadoDocumento correspondente ou null se não encontrado
     */
    public static EstadoDocumento fromCodigo(String codigo) {
        if (codigo == null) return null;
        
        for (EstadoDocumento estado : values()) {
            if (estado.codigo.equalsIgnoreCase(codigo)) {
                return estado;
            }
        }
        return null;
    }
    
    /**
     * Verifica se o estado permite alterações no documento.
     * Apenas documentos ORIGINAL podem ser alterados antes de emitidos.
     */
    public boolean isEditavel() {
        return this == ORIGINAL;
    }
    
    /**
     * Verifica se é um documento que requer indicação visual especial no PDF/impressão.
     */
    public boolean requerIndicacaoVisual() {
        return this == DUPLICADO || this == SEGUNDA_VIA || this == EMISSAO_TERCEIROS;
    }
    
    /**
     * Retorna o texto a ser exibido no documento impresso/PDF.
     */
    public String getTextoDocumento() {
        switch (this) {
            case DUPLICADO:
                return "DUPLICADO";
            case SEGUNDA_VIA:
                return "SEGUNDA VIA";
            case EMISSAO_TERCEIROS:
                return "EMISSÃO EM TERCEIROS";
            default:
                return "";
        }
    }
}
