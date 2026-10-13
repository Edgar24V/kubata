package ao.allon.kubata.financeiro;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.financeiro.view.CaixasView;
import ao.allon.kubata.financeiro.view.ContasBancariasView;
import ao.allon.kubata.financeiro.view.FinanceiroDashboardView;
import ao.allon.kubata.financeiro.view.RelatoriosFinanceirosView;
import ao.allon.kubata.financeiro.view.TitulosFinanceirosView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

/** Catálogo funcional do módulo Financeiro e das suas páginas operacionais. */
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
        return "Tesouraria, caixas, contas bancárias, pagar/receber e relatórios financeiros";
    }

    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.web("#107C41"));
        rect.setArcWidth(6);
        rect.setArcHeight(6);
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
                FinanceiroDashboardView.class,
                1
        ));

        addView(new ModuleView(
                "caixas",
                "Caixas",
                "Gestão de caixas, abertura, entradas, saídas e livro-caixa",
                null,
                "financeiro.caixas",
                CaixasView.class,
                2
        ));

        addView(new ModuleView(
                "contas-bancarias",
                "Contas Bancárias",
                "Cadastro bancário, créditos, débitos, transferências e extractos",
                null,
                "financeiro.contas-bancarias",
                ContasBancariasView.class,
                3
        ));

        addView(new ModuleView(
                "contas-pagar",
                "Contas a Pagar",
                "Títulos de fornecedor, vencimentos e liquidações",
                null,
                "financeiro.titulos.pagar",
                TitulosFinanceirosView.class,
                4
        ));

        addView(new ModuleView(
                "contas-receber",
                "Contas a Receber",
                "Títulos de cliente, cobranças e liquidações",
                null,
                "financeiro.titulos.receber",
                TitulosFinanceirosView.class,
                5
        ));

        addView(new ModuleView(
                "relatorios",
                "Relatórios Financeiros",
                "Consulta por período e exportação consolidada de movimentos",
                null,
                "financeiro.relatorios",
                RelatoriosFinanceirosView.class,
                6
        ));
    }
}
