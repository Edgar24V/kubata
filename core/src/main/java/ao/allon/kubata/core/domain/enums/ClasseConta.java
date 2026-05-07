package ao.allon.kubata.core.domain.enums;

public enum ClasseConta {
    CLASSE_1("Meios Fixos e Investimentos"),
    CLASSE_2("Existências"),
    CLASSE_3("Terceiros"),
    CLASSE_4("Meios Monetários"),
    CLASSE_5("Capital e Reservas"),
    CLASSE_6("Proveitos e Ganhos por Natureza"),
    CLASSE_7("Custos e Perdas por Natureza"),
    CLASSE_8("Resultados");

    private final String descricao;

    ClasseConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return name().replace("CLASSE_", "Classe ") + " - " + descricao;
    }
}
