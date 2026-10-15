package ao.allon.kubata.faturacao.domain.enums;

/**
 * Finalidade fiscal da nota de crédito conforme o regime jurídico angolano.
 * A nota continua a identificar a factura original e o motivo concreto.
 */
public enum FinalidadeNotaCredito {
    ANULACAO("Anulação"),
    RECTIFICACAO("Rectificação");

    private final String descricao;

    FinalidadeNotaCredito(String descricao) {
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
