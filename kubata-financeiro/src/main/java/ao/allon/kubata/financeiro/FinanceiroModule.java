package ao.allon.kubata.financeiro;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo Financeiro - Gestão de Caixa e Bancos
 */
@Component
public class FinanceiroModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "financeiro";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Financeiro";
    }
    
    @Override
    public String getModuleDescription() {
        return "Gestão de caixa, contas bancárias, tesouraria e pagamentos";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.GOLD);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        addView(new ModuleView(
            "caixas",
            "Caixas",
            "Gestão de caixas e movimentos",
            null,
            "financeiro.caixas",
            null,
            1
        ));
        
        addView(new ModuleView(
            "contas-bancarias",
            "Contas Bancárias",
            "Gestão de contas bancárias",
            null,
            "financeiro.contas-bancarias",
            null,
            2
        ));
        
        addView(new ModuleView(
            "contas-pagar",
            "Contas a Pagar",
            "Gestão de contas a pagar",
            null,
            "financeiro.contas-pagar",
            null,
            3
        ));
        
        addView(new ModuleView(
            "contas-receber",
            "Contas a Receber",
            "Gestão de contas a receber",
            null,
            "financeiro.contas-receber",
            null,
            4
        ));
        
        addView(new ModuleView(
            "retencao-fonte",
            "Retenção na Fonte",
            "Gestão de retenções",
            null,
            "financeiro.retencao",
            null,
            5
        ));
    }
}
