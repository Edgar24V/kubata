package ao.allon.kubata.core.domain.enums;

public enum NaturezaConta {
    DEVEDORA("Devedora"),
    CREDORA("Credora"),
    MISTA("Mista");

    private final String descricao;

    NaturezaConta(String descricao) {
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
