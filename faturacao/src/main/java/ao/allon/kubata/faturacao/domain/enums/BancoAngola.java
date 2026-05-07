package ao.allon.kubata.faturacao.domain.enums;

public enum BancoAngola {
    BFA("Banco de Fomento Angola"),
    BAI("Banco Angolano de Investimentos"),
    BIC("Banco BIC"),
    BMA("Banco Millennium Atlântico"),
    SOL("Banco Sol"),
    BPC("Banco de Poupança e Crédito"),
    KEVE("Banco Keve"),
    BCA("Banco Comercial Angolano"),
    BCH("Banco Caixa Geral Angola"),
    SBA("Standard Bank Angola"),
    OUTRO("Outro Banco");

    private final String nome;

    BancoAngola(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
