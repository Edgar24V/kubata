package ao.allon.kubata.faturacao.domain.enums;

public enum StatusDevolucao {
    PENDENTE("Pendente de Análise"),
    ANALISE("Em Análise"),
    APROVADA("Aprovada - Aguardando Processamento"),
    REJEITADA("Rejeitada"),
    CONCLUIDA("Concluída (NC Gerada)");

    private final String descricao;

    StatusDevolucao(String descricao) {
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
