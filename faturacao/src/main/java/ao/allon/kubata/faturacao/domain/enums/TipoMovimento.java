package ao.allon.kubata.faturacao.domain.enums;

public enum TipoMovimento {
    ABERTURA("Abertura"),
    VENDA("Venda"),
    SANGRIA("Sangria"),
    SUPRIMENTO("Suprimento"),
    ESTORNO("Estorno"),
    FECHAMENTO("Fechamento");

    private final String descricao;

    TipoMovimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
