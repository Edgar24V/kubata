package ao.allon.kubata.vendas.enums;

public enum TipoDocumento {
    FATURA("FT"),
    FATURA_SIMPLIFICADA("FS"),
    FATURA_RECIBO("FR"),
    NOTA_CREDITO("NC"),
    NOTA_DEBITO("ND"),
    GUIA_REMessa("GR"),
    GUIA_TRANSPORTE("GT"),
    ORCAMENTO("OR"),
    RECIBO("RC"),
    DEVOLUCAO("DV");

    private final String codigo;

    TipoDocumento(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
