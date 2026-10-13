package ao.allon.kubata.financeiro.enums;

/** Origem/destino de fundos usado ao liquidar um título. */
public enum MeioPagamentoFinanceiro {
    CAIXA("Caixa"),
    BANCO("Conta bancária");

    private final String descricao;

    MeioPagamentoFinanceiro(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
