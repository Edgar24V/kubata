package ao.allon.kubata.financeiro.enums;

/** Tipos de movimento permitidos no livro bancário. */
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
