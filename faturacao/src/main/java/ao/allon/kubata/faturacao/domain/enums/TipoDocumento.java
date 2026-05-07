package ao.allon.kubata.faturacao.domain.enums;

public enum TipoDocumento {
    FATURA("FT", "Fatura"),
    FATURA_RECIBO("FR", "Fatura/Recibo"),
    VENDA_A_DINHEIRO("VD", "Venda a Dinheiro"),
    RECIBO("RC", "Recibo"),
    NOTA_CREDITO("NC", "Nota de Crédito"),
    NOTA_DEBITO("ND", "Nota de Débito"),
    GUIA_TRANSPORTE("GT", "Guia de Transporte"),
    GUIA_REMESSA("GR", "Guia de Remessa"),
    AUTO_FATURA("AF", "Autofactura"),
    ENCOMENDA("EC", "Encomenda"),
    ORCAMENTO("OR", "Orçamento"),
    PRO_FORMA("PP", "Fatura Pró-Forma"),
    DOC_CONFERENCIA("DC", "Documento de Conferência"),
    OUTROS("OT", "Outros");

    private final String codigo;
    private final String descricao;

    TipoDocumento(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
