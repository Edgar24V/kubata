package ao.allon.kubata.core.module;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Classe base abstrata para módulos Kubata.
 * Fornece implementações padrão para métodos comuns.
 */
public abstract class AbstractKubataModule implements KubataModule {
    
    protected final List<ModuleView> views = new ArrayList<>();
    protected boolean active = true;
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public Node getModuleIcon() {
        // Ícone padrão - retângulo colorido
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.CORNFLOWERBLUE);
        return rect;
    }
    
    @Override
    public List<String> getRequiredPermissions() {
        return Collections.singletonList("module." + getModuleId());
    }
    
    @Override
    public void initialize() {
        // Implementação padrão vazia
        // Módulos devem sobrescrever para inicialização específica
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public List<ModuleView> getModuleViews() {
        return Collections.unmodifiableList(views);
    }
    
    /**
     * Adiciona uma view ao módulo
     */
    protected void addView(ModuleView view) {
        views.add(view);
        // Reordena por displayOrder
        views.sort(Comparator.comparingInt(ModuleView::displayOrder));
    }
    
    /**
     * Remove uma view do módulo
     */
    protected void removeView(String viewId) {
        views.removeIf(v -> v.viewId().equals(viewId));
    }
}
