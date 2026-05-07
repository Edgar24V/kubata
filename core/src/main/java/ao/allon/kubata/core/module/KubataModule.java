package ao.allon.kubata.core.module;

import javafx.scene.Node;

import java.util.List;

/**
 * Interface base para todos os módulos do Kubata.
 * Cada módulo deve implementar esta interface para ser registrado no sistema.
 */
public interface KubataModule {
    
    /**
     * ID único do módulo (ex: "inventario", "vendas", "financeiro")
     */
    String getModuleId();
    
    /**
     * Nome de exibição do módulo
     */
    String getModuleName();
    
    /**
     * Descrição do módulo
     */
    String getModuleDescription();
    
    /**
     * Versão do módulo
     */
    String getVersion();
    
    /**
     * Ícone do módulo (JavaFX Node)
     */
    Node getModuleIcon();
    
    /**
     * Lista de permissões necessárias para acessar o módulo
     */
    List<String> getRequiredPermissions();
    
    /**
     * Inicializa o módulo
     */
    void initialize();
    
    /**
     * Verifica se o módulo está ativo/disponível
     */
    boolean isActive();
    
    /**
     * Retorna as views/views principais do módulo
     */
    List<ModuleView> getModuleViews();
}
