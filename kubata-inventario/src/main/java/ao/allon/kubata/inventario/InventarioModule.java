package ao.allon.kubata.inventario;

import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.inventario.ui.views.ArmazensView;
import ao.allon.kubata.inventario.ui.views.CategoriasView;
import ao.allon.kubata.inventario.ui.views.EstoqueView;
import ao.allon.kubata.inventario.ui.views.ProdutosView;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventarioModule extends AbstractKubataModule implements KubataModule {

    public static final String MODULE_ID = "inventario";

    @Override public String getModuleId() { return MODULE_ID; }

    @Override public String getModuleName() { return "Inventário"; }

    @Override
    public String getModuleDescription() {
        return "Gestão de artigos, categorias, armazéns e controlo de stock";
    }

    @Override
    public List<String> getRequiredPermissions() {
        return List.of("INVENTARIO");
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

        addView(new ModuleView(
                "produtos", "Artigos", "Cadastro e consulta de artigos",
                null, "INVENTARIO", ProdutosView.class, 1));

        addView(new ModuleView(
                "categorias", "Categorias", "Classificação de artigos",
                null, "INVENTARIO", CategoriasView.class, 2));

        addView(new ModuleView(
                "armazens", "Armazéns", "Gestão de locais de armazenamento",
                null, "INVENTARIO", ArmazensView.class, 3));

        addView(new ModuleView(
                "estoque", "Gestão de Stock", "Existências, lotes e validades",
                null, "INVENTARIO", EstoqueView.class, 4));
    }
}
