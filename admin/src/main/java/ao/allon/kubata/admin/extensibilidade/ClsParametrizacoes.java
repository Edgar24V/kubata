package ao.allon.kubata.admin.extensibilidade;

import javafx.scene.Node;

/**
 * Classe que disponibiliza os formulários para as parametrizações base da aplicação.
 */
public interface ClsParametrizacoes {
    /**
     * Retorna o nó visual (formulário/view) com as parametrizações gerais da aplicação.
     */
    Node getFormularioParametrizacao();
    
    /**
     * Guarda as parametrizações definidas no formulário.
     */
    void guardarParametrizacoes();
    
    /**
     * Carrega as parametrizações atualmente definidas.
     */
    void carregarParametrizacoes();
}
