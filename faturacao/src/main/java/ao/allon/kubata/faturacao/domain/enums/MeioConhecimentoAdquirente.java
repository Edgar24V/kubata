package ao.allon.kubata.faturacao.domain.enums;

/**
 * Meio pelo qual o adquirente tomou conhecimento da anulação/rectificação.
 * O valor escolhido não substitui o registo da prova concreta no documento.
 */
public enum MeioConhecimentoAdquirente {
    CARTA("Carta escrita"),
    CORREIO_ELECTRONICO("Correio electrónico"),
    ASSINATURA_NA_NOTA("Assinatura na nota (nome e NIF)"),
    CARIMBO("Carimbo do adquirente"),
    REGULARIZACAO_IMPOSTO("Regularização do imposto a favor do Estado"),
    OUTRO("Outro meio documentado");

    private final String descricao;

    MeioConhecimentoAdquirente(String descricao) {
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
