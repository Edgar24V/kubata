package ao.allon.kubata.contabilidade;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.ModuleView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/**
 * Módulo Contabilidade - SAFT e Relatórios
 */
@Component
public class ContabilidadeModule extends AbstractKubataModule {
    
    public static final String MODULE_ID = "contabilidade";
    
    @Override
    public String getModuleId() {
        return MODULE_ID;
    }
    
    @Override
    public String getModuleName() {
        return "Contabilidade";
    }
    
    @Override
    public String getModuleDescription() {
        return "Plano de contas, SAFT-AO, relatórios contabilísticos e auditoria";
    }
    
    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.PURPLE);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        addView(new ModuleView(
            "plano-contas",
            "Plano de Contas",
            "Gestão do plano de contas",
            null,
            "contabilidade.plano-contas",
            null,
            1
        ));
        
        addView(new ModuleView(
            "mapa-impostos",
            "Mapa de Impostos",
            "Mapa de impostos",
            null,
            "contabilidade.mapa-impostos",
            null,
            2
        ));
        
        addView(new ModuleView(
            "saft-export",
            "Exportar SAFT",
            "Exportação de SAFT-AO",
            null,
            "contabilidade.saft",
            null,
            3
        ));
        
        addView(new ModuleView(
            "auditoria",
            "Auditoria",
            "Trilha de auditoria",
            null,
            "contabilidade.auditoria",
            null,
            4
        ));
        
        addView(new ModuleView(
            "fecho-exercicio",
            "Fecho de Exercício",
            "Operações de fecho de exercício",
            null,
            "contabilidade.fecho",
            null,
            5
        ));
    }
}
