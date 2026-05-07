package ao.allon.kubata.admin.extensibilidade;

/**
 * Classe que disponibiliza os serviços utilitários da aplicação.
 */
public interface ClsServicos {
    /**
     * Inicializa os serviços core da aplicação externa.
     */
    void inicializarServicos();
    
    /**
     * Termina os serviços utilitários da aplicação externa.
     */
    void encerrarServicos();
    
    /**
     * Executa um serviço utilitário específico da aplicação.
     * @param nomeServico Nome do serviço a ser executado
     * @param parametros Parâmetros para a execução
     * @return Resultado da execução
     */
    Object executarServico(String nomeServico, Object... parametros);
}
