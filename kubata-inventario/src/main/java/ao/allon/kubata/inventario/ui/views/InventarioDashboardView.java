package ao.allon.kubata.inventario.ui.views;

import ao.allon.kubata.inventario.service.ArmazemService;
import ao.allon.kubata.inventario.service.CategoriaService;
import ao.allon.kubata.inventario.service.EstoqueService;
import ao.allon.kubata.inventario.service.ProdutoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class InventarioDashboardView extends VBox {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final ArmazemService armazemService;
    private final EstoqueService estoqueService;

    public InventarioDashboardView(ProdutoService produtoService,
                                    CategoriaService categoriaService,
                                    ArmazemService armazemService,
                                    EstoqueService estoqueService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.armazemService = armazemService;
        this.estoqueService = estoqueService;
        build();
    }

    private void build() {
        setSpacing(18);
        setPadding(new Insets(24));
        getStyleClass().add("inventario-page");

        Label title = new Label("Gestão de Stock");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Centro de operações de Inventário • Artigos, armazéns e existências");
        subtitle.getStyleClass().add("page-subtitle");

        GridPane cards = new GridPane();
        cards.setHgap(14);
        cards.setVgap(14);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cards.getColumnConstraints().add(cc);
        }

        cards.add(card("Artigos", String.valueOf(produtoService.findAll().size()), "Catálogo ativo"), 0, 0);
        cards.add(card("Categorias", String.valueOf(categoriaService.findAll().size()), "Classificação"), 1, 0);
        cards.add(card("Armazéns", String.valueOf(armazemService.findAll().size()), "Locais de stock"), 2, 0);
        cards.add(card("Existências", String.valueOf(estoqueService.findAll().size()), "Registos de stock"), 3, 0);

        VBox panel = new VBox(8,
                sectionTitle("Operações correntes"),
                new Label("Utilize o separador superior para consultar e gerir Artigos, Categorias, Armazéns e Stock.")
        );
        panel.getStyleClass().add("info-panel");

        getChildren().addAll(title, subtitle, cards, panel);
    }

    private VBox card(String title, String value, String hint) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("kpi-card");

        Label t = new Label(title);
        t.getStyleClass().add("kpi-title");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        Label h = new Label(hint);
        h.getStyleClass().add("kpi-hint");
        box.getChildren().addAll(t, v, h);
        return box;
    }

    private Label sectionTitle(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("section-title");
        return label;
    }
}
