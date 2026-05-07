package ao.allon.kubata.compras;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo de Compras - Gestão de Fornecedores
 */
@Component
public class ComprasModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "compras";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Compras";
    }
    
    @Override
    public String getModuleDescription() {
        return "Gestão de fornecedores, encomendas e compras";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.ORANGE);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        addView(new ModuleView(
            "fornecedores",
            "Fornecedores",
            "Gestão de fornecedores",
            null,
            "compras.fornecedores",
            null,
            1
        ));
        
        addView(new ModuleView(
            "encomendas",
            "Encomendas",
            "Gestão de encomendas",
            null,
            "compras.encomendas",
            null,
            2
        ));
    }
}
