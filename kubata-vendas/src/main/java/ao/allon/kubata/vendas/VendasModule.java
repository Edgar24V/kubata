package ao.allon.kubata.vendas;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo de Vendas - Gestão de Faturação e Clientes
 */
@Component
public class VendasModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "vendas";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Vendas";
    }
    
    @Override
    public String getModuleDescription() {
        return "Gestão de faturação, clientes, orçamentos e documentos de venda";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.DODGERBLUE);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        addView(new ModuleView(
            "faturas",
            "Faturas",
            "Emissão e gestão de faturas",
            null,
            "vendas.faturas",
            null,
            1
        ));
        
        addView(new ModuleView(
            "clientes",
            "Clientes",
            "Gestão de clientes",
            null,
            "vendas.clientes",
            null,
            2
        ));
        
        addView(new ModuleView(
            "orcamentos",
            "Orçamentos",
            "Orçamentos e proformas",
            null,
            "vendas.orcamentos",
            null,
            3
        ));
        
        addView(new ModuleView(
            "notas-credito",
            "Notas de Crédito",
            "Gestão de notas de crédito",
            null,
            "vendas.notas-credito",
            null,
            4
        ));
        
        addView(new ModuleView(
            "notas-debito",
            "Notas de Débito",
            "Gestão de notas de débito",
            null,
            "vendas.notas-debito",
            null,
            5
        ));
        
        addView(new ModuleView(
            "guias-remessa",
            "Guias de Remessa",
            "Emissão de guias de remessa",
            null,
            "vendas.guias-remessa",
            null,
            6
        ));
        
        addView(new ModuleView(
            "guias-transporte",
            "Guias de Transporte",
            "Emissão de guias de transporte",
            null,
            "vendas.guias-transporte",
            null,
            7
        ));
        
        addView(new ModuleView(
            "series",
            "Séries",
            "Gestão de séries de documentos",
            null,
            "vendas.series",
            null,
            8
        ));
    }
}
