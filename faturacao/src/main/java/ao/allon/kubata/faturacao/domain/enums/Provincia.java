package ao.allon.kubata.faturacao.domain.enums;

public enum Provincia {
    BENGO("Bengo"),
    BENGUELA("Benguela"),
    BIE("Bié"),
    CABINDA("Cabinda"),
    CUANDO_CUBANGO("Cuando Cubango"),
    CUANZA_NORTE("Cuanza Norte"),
    CUANZA_SUL("Cuanza Sul"),
    CUNENE("Cunene"),
    HUAMBO("Huambo"),
    HUILA("Huíla"),
    LUANDA("Luanda"),
    LUNDA_NORTE("Lunda Norte"),
    LUNDA_SUL("Lunda Sul"),
    MALANJE("Malanje"),
    MOXICO("Moxico"),
    NAMIBE("Namibe"),
    UIGE("Uíge"),
    ZAIRE("Zaire");

    private final String nome;

    Provincia(String nome) {
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
