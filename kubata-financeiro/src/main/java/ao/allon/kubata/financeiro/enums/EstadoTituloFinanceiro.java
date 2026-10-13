package ao.allon.kubata.financeiro.enums;

/** Estado operacional de um título a pagar ou receber. */
public enum EstadoTituloFinanceiro {
    ABERTO("Em aberto"),
    PARCIAL("Parcialmente liquidado"),
    LIQUIDADO("Liquidado"),
    ANULADO("Anulado");

    private final String descricao;

    EstadoTituloFinanceiro(String descricao) {
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
