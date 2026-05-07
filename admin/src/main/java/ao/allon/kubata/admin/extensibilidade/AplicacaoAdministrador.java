package ao.allon.kubata.admin.extensibilidade;

/**
 * Interface principal que define uma aplicação externa a integrar no Administrator.
 * Transforma uma biblioteca de software externa numa parte integrante e gerível do universo Kubata.
 */
public interface AplicacaoAdministrador {
    
    /**
     * Retorna o nome da aplicação.
     */
    String getNome();
    
    /**
     * Abreviatura única de 3 caracteres alfanuméricos (ex: ABC, D01).
     * Funciona como identificador chave para a aplicação.
     */
    String getAbreviatura();
    
    /**
     * Retorna o componente Audit (Segurança e Permissões).
     */
    Audit getAudit();
    
    /**
     * Retorna o componente de Operações de Aplicação.
     */
    ClsOperacoesAplicacao getOperacoesAplicacao();
    
    /**
     * Retorna o componente de Operações de Log.
     */
    ClsOperacoesLog getOperacoesLog();
    
    /**
     * Retorna o componente de Parametrizações (Formulários).
     */
    ClsParametrizacoes getParametrizacoes();
    
    /**
     * Retorna o componente de Serviços.
     */
    ClsServicos getServicos();
    
    /**
     * Retorna o componente de Logins Associados (Opcional, pode retornar null).
     */
    default ClsLoginsAssociados getLoginsAssociados() {
        return null;
    }
}
