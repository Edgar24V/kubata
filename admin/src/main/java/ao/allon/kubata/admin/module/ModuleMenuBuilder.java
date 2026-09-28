package ao.allon.kubata.admin.module;

import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Construtor de menus dinâmicos baseado nos módulos carregados.
 *
 * O componente não decide qual janela concreta deve abrir para "Gerenciar";
 * disponibiliza um handler configurável para que o shell do Administrator
 * mantenha o controlo da navegação.
 */
@Component
public class ModuleMenuBuilder {

    private final Map<String, Menu> moduleMenus = new HashMap<>();
    private final Map<String, MenuItemClickHandler> clickHandlers = new HashMap<>();
    private ModuleManageHandler moduleManageHandler;

    public Menu buildModuleMenu(KubataModule module) {
        Objects.requireNonNull(module, "module");

        Menu menu = new Menu(module.getModuleName());
        menu.setGraphic(module.getModuleIcon());

        for (ModuleView view : module.getModuleViews()) {
            MenuItem item = new MenuItem(view.viewName());
            item.setOnAction(e ->
                    handleViewClick(module.getModuleId(), view.viewId()));
            menu.getItems().add(item);
        }

        if (!module.getModuleViews().isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem manageItem = new MenuItem("Gerenciar Módulo");
        manageItem.setOnAction(e -> handleManageModule(module));
        menu.getItems().add(manageItem);

        moduleMenus.put(module.getModuleId(), menu);
        return menu;
    }

    public void removeModuleMenu(String moduleId) {
        if (moduleId != null) {
            moduleMenus.remove(moduleId);
        }
    }

    public void updateModuleMenu(KubataModule module) {
        Objects.requireNonNull(module, "module");
        removeModuleMenu(module.getModuleId());
        buildModuleMenu(module);
    }

    public void registerViewClickHandler(
            String viewId,
            MenuItemClickHandler handler) {

        if (viewId == null || viewId.isBlank()) {
            throw new IllegalArgumentException("viewId é obrigatório.");
        }

        if (handler == null) {
            clickHandlers.remove(viewId);
        } else {
            clickHandlers.put(viewId, handler);
        }
    }

    public void setModuleManageHandler(ModuleManageHandler handler) {
        this.moduleManageHandler = handler;
    }

    private void handleViewClick(String moduleId, String viewId) {
        MenuItemClickHandler handler = clickHandlers.get(viewId);
        if (handler != null) {
            handler.onClick(moduleId, viewId);
        }
    }

    private void handleManageModule(KubataModule module) {
        if (moduleManageHandler != null) {
            moduleManageHandler.onManage(module);
        }
    }

    @FunctionalInterface
    public interface MenuItemClickHandler {
        void onClick(String moduleId, String viewId);
    }

    @FunctionalInterface
    public interface ModuleManageHandler {
        void onManage(KubataModule module);
    }
}
