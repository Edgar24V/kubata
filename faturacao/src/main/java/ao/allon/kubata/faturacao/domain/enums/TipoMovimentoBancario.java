package ao.allon.kubata.faturacao.domain.enums;

public enum TipoMovimentoBancario {
    CREDITO("Crédito"),
    DEBITO("Débito");

    private final String descricao;

    TipoMovimentoBancario(String descricao) {
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
