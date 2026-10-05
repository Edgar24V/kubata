package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.service.InventarioDashboardService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class InventarioDashboardView extends BorderPane {

    private final InventarioDashboardService service;
    private final GridPane metrics = new GridPane();
    private final VBox alerts = new VBox(8);

    public InventarioDashboardView(InventarioDashboardService service) {
        this.service = service;
        build();
    }

    private void build() {
        InventarioUI.installCss(this);
        setPadding(new Insets(20));

        VBox header = new VBox(4);
        Label title = new Label("Inventário & Gestão de Stocks");
        title.getStyleClass().add("inventario-page-title");
        Label subtitle = new Label("Centro operacional de artigos, armazéns, movimentos, reservas e inventários físicos.");
        subtitle.getStyleClass().add("inventario-page-subtitle");
        header.getChildren().addAll(title, subtitle);

        Button refresh = InventarioUI.primaryButton("Atualizar");
        refresh.setOnAction(e -> refresh());
        HBox top = new HBox(12, header, new Region(), refresh);
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        metrics.setHgap(12);
        metrics.setVgap(12);
        metrics.setPadding(new Insets(18, 0, 0, 0));
        setCenter(new VBox(12, metrics, alerts));
        refresh();
    }

    public void refresh() {
        var r = service.resumo();
        metrics.getChildren().clear();
        metrics.add(InventarioUI.metric("ARTIGOS", InventarioUI.integer(r.produtos()), "Artigos ativos no catálogo"), 0, 0);
        metrics.add(InventarioUI.metric("ARMAZÉNS", InventarioUI.integer(r.armazens()), "Locais operacionais ativos"), 1, 0);
        metrics.add(InventarioUI.metric("UNIDADES EM STOCK", InventarioUI.integer(r.unidades()), "Existência física consolidada"), 2, 0);
        metrics.add(InventarioUI.metric("UNIDADES RESERVADAS", r.reservado().stripTrailingZeros().toPlainString(), "Stock comprometido por reservas"), 3, 0);
        metrics.add(InventarioUI.metric("VALOR DO STOCK", InventarioUI.money(r.valorStock()), "Valorização ao custo registado"), 0, 1);
        metrics.add(InventarioUI.metric("STOCK BAIXO", InventarioUI.integer(r.stockBaixo()), "Artigos no mínimo ou abaixo"), 1, 1);

        alerts.getChildren().clear();
        Label section = new Label("Alertas operacionais");
        section.getStyleClass().add("inventario-card-title");
        alerts.getChildren().add(section);
        ProdutoAlert.alerts(r.stockBaixo()).forEach(a -> {
            Label l = new Label(a);
            l.setWrapText(true);
            l.getStyleClass().add("inventario-card");
            alerts.getChildren().add(l);
        });
        if (r.stockBaixo() == 0) {
            Label ok = new Label("✓ Não existem artigos abaixo do stock mínimo.");
            ok.getStyleClass().add("inventario-metric-detail");
            alerts.getChildren().add(ok);
        }
    }

    private record ProdutoAlert() {
        static java.util.List<String> alerts(long stockBaixo) {
            return stockBaixo > 0
                    ? java.util.List.of(stockBaixo + " artigo(s) requer(em) reposição.")
                    : java.util.List.of();
        }
    }
}
