package ao.allon.kubata.fiscal;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo Fiscal - Validações AGT e Conformidade
 */
@Component
public class FiscalModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "fiscal";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Fiscal / AGT";
    }
    
    @Override
    public String getModuleDescription() {
        return "Validações fiscais AGT, correções e conformidade";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.CRIMSON);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        addView(new ModuleView(
            "agt-correcoes",
            "Correções AGT",
            "Correções e validações AGT",
            null,
            "fiscal.agt-correcoes",
            null,
            1
        ));
        
        addView(new ModuleView(
            "relatorio-fiscal",
            "Relatório Fiscal",
            "Relatórios fiscais",
            null,
            "fiscal.relatorio",
            null,
            2
        ));
        
        addView(new ModuleView(
            "impostos",
            "Impostos",
            "Configuração de impostos e motivos de isenção",
            null,
            "fiscal.impostos",
            null,
            3
        ));
    }
}
