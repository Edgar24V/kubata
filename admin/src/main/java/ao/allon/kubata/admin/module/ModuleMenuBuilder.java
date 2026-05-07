package ao.allon.kubata.admin.module;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Construtor de menus dinâmicos baseado nos módulos carregados.
 * Cria menus JavaFX para cada módulo registrado.
 */
@Component
public class ModuleMenuBuilder {
    
    private final Map<String, Menu> moduleMenus = new HashMap<>();
    private final Map<String, MenuItemClickHandler> clickHandlers = new HashMap<>();
    
    /**
     * Constrói o menu para um módulo.
     */
    public Menu buildModuleMenu(KubataModule module) {
        Menu menu = new Menu(module.getModuleName());
        menu.setGraphic(module.getModuleIcon());
        
        // Adiciona views como itens de menu
        for (ModuleView view : module.getModuleViews()) {
            MenuItem item = new MenuItem(view.viewName());
            item.setOnAction(e -> handleViewClick(module.getModuleId(), view.viewId()));
            menu.getItems().add(item);
        }
        
        // Adiciona separador se houver views
        if (!module.getModuleViews().isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
        }
        
        // Adiciona opção de gerenciar módulo
        MenuItem manageItem = new MenuItem("Gerenciar Módulo");
        manageItem.setOnAction(e -> handleManageModule(module.getModuleId()));
        menu.getItems().add(manageItem);
        
        moduleMenus.put(module.getModuleId(), menu);
        return menu;
    }
    
    /**
     * Remove o menu de um módulo.
     */
    public void removeModuleMenu(String moduleId) {
        moduleMenus.remove(moduleId);
    }
    
    /**
     * Atualiza o menu de um módulo.
     */
    public void updateModuleMenu(KubataModule module) {
        removeModuleMenu(module.getModuleId());
        buildModuleMenu(module);
    }
    
    /**
     * Registra um handler para cliques em views.
     */
    public void registerViewClickHandler(String viewId, MenuItemClickHandler handler) {
        clickHandlers.put(viewId, handler);
    }
    
    private void handleViewClick(String moduleId, String viewId) {
        MenuItemClickHandler handler = clickHandlers.get(viewId);
        if (handler != null) {
            handler.onClick(moduleId, viewId);
        }
    }
    
    private void handleManageModule(String moduleId) {
        System.out.println("Manage module: " + moduleId);
        // TODO: Abrir tela de gestão do módulo
    }
    
    /**
     * Interface para handlers de clique em menus.
     */
    @FunctionalInterface
    public interface MenuItemClickHandler {
        void onClick(String moduleId, String viewId);
    }
}
