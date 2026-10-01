package ao.allon.kubata.core.domain;

/**
 * Classificação administrativa da conta. A função (Role) continua a definir
 * o papel funcional; o tipo de conta define a natureza da identidade.
 */
public enum TipoConta {
    PESSOAL,
    ADMINISTRATIVA,
    SERVICO,
    API,
    TECNICA,
    TEMPORARIA
}
