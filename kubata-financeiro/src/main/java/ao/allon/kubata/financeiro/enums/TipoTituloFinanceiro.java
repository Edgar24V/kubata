package ao.allon.kubata.financeiro.enums;

/** Natureza do compromisso financeiro. */
public enum TipoTituloFinanceiro {
    PAGAR("Contas a Pagar"),
    RECEBER("Contas a Receber");

    private final String descricao;

    TipoTituloFinanceiro(String descricao) {
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
