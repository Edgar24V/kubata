package ao.allon.kubata.inventario;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Módulo de Inventario - Gestão de Produtos, Categorias e Stock
 */
@Component
public class InventarioModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "inventario";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Inventário";
    }
    
    @Override
    public String getModuleDescription() {
        return "Gestão de produtos, categorias, armazéns e controlo de stock";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.DARKGREEN);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        // Registra as views do módulo
        addView(new ModuleView(
            "produtos",
            "Produtos",
            "Gestão de produtos e serviços",
            null,
            "inventario.produtos",
            null, // ProdutosView.class
            1
        ));
        
        addView(new ModuleView(
            "categorias",
            "Categorias",
            "Categorias de produtos",
            null,
            "inventario.categorias",
            null, // CategoriasView.class
            2
        ));
        
        addView(new ModuleView(
            "armazens",
            "Armazéns",
            "Gestão de armazéns",
            null,
            "inventario.armazens",
            null, // ArmazensView.class
            3
        ));
        
        addView(new ModuleView(
            "estoque",
            "Estoque",
            "Controlo de stock e movimentos",
            null,
            "inventario.estoque",
            null, // EstoqueView.class
            4
        ));
    }
}
