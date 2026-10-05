package ao.allon.kubata.inventario;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.inventario.ui.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

@Component
public class InventarioModule extends AbstractKubataModule implements KubataModule {

    public static final String MODULE_ID = "inventario";

    @Override
    public String getModuleId() { return MODULE_ID; }

    @Override
    public String getModuleName() { return "Inventário"; }

    @Override
    public String getModuleDescription() {
        return "Gestão profissional de artigos, armazéns, stocks, reservas, rastreabilidade e inventários físicos";
    }

    @Override
    public Node getModuleIcon() {
        Rectangle rect = new Rectangle(24, 24);
        rect.setFill(Color.web("#217346"));
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        return rect;
    }

    @Override
    public void initialize() {
        super.initialize();

        addView(new ModuleView("inventario_dashboard", "Dashboard", "Visão operacional do Inventário", null,
                "inventario.dashboard", InventarioDashboardView.class, 1));
        addView(new ModuleView("produtos", "Artigos / Produtos", "Cadastro e gestão do catálogo", null,
                "inventario.produtos", ProdutosView.class, 2));
        addView(new ModuleView("categorias", "Categorias", "Classificação dos artigos", null,
                "inventario.categorias", CategoriasView.class, 3));
        addView(new ModuleView("armazens", "Armazéns", "Estrutura e regras de armazenamento", null,
                "inventario.armazens", ArmazensView.class, 4));
        addView(new ModuleView("stock", "Consulta de Stock", "Existências por artigo, armazém e lote", null,
                "inventario.stock", StockConsultaView.class, 5));
        addView(new ModuleView("movimentos_stock", "Movimentos", "Livro de movimentos de stock", null,
                "inventario.movimentos", MovimentosStockView.class, 6));
        addView(new ModuleView("reservas_stock", "Reservas", "Stock comprometido e disponível", null,
                "inventario.reservas", ReservasStockView.class, 7));
        addView(new ModuleView("transferencias_stock", "Transferências", "Movimentação entre armazéns", null,
                "inventario.transferencias", TransferenciasStockView.class, 8));
        addView(new ModuleView("lotes_series", "Lotes & Nº de Série", "Rastreabilidade", null,
                "inventario.rastreabilidade", LotesSeriesView.class, 9));
        addView(new ModuleView("inventarios_fisicos", "Inventários Físicos", "Contagem e regularização", null,
                "inventario.inventarios", InventariosFisicosView.class, 10));
        addView(new ModuleView("valorizacao_stock", "Valorização", "Valor económico do stock", null,
                "inventario.valorizacao", ValorizacaoStockView.class, 11));
    }
}
