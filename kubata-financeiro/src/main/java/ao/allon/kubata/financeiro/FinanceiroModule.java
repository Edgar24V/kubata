package ao.allon.kubata.financeiro;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo Financeiro - Gestão de Caixa e Bancos
 */
@Component
public class FinanceiroModule extends AbstractKubataModule implements KubataModule {
    
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
            "visao-geral",
            "Visão Geral Financeira",
            "Indicadores de liquidez, saldos e movimentos recentes",
            null,
            "financeiro.dashboard",
            null,
            1
        ));

        addView(new ModuleView(
            "caixas",
            "Caixas",
            "Gestão de caixas e movimentos",
            null,
            "financeiro.caixas",
            null,
            3
        ));
        
        addView(new ModuleView(
            "contas-bancarias",
            "Contas Bancárias",
            "Gestão de contas bancárias",
            null,
            "financeiro.contas-bancarias",
            null,
            3
        ));
        
        addView(new ModuleView(
            "contas-pagar",
            "Contas a Pagar",
            "Títulos de fornecedor e liquidações ligadas à tesouraria",
            null,
            "financeiro.titulos",
            null,
            4
        ));
        
        addView(new ModuleView(
            "contas-receber",
            "Contas a Receber",
            "Títulos de cliente e liquidações ligadas à tesouraria",
            null,
            "financeiro.titulos",
            null,
            5
        ));
        
        addView(new ModuleView(
            "retencao-fonte",
            "Retenção na Fonte",
            "Gestão de retenções",
            null,
            "financeiro.retencao",
            null,
            6
        ));
    }
}
