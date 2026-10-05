package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.inventario.service.InventarioDashboardService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
        Label subtitle = new Label(
                "Centro operacional de artigos, armazéns, movimentos, reservas e inventários físicos.");
        subtitle.getStyleClass().add("inventario-page-subtitle");

        header.getChildren().addAll(title, subtitle);

        Button refresh = InventarioUI.primaryButton("Atualizar");
        refresh.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, header, spacer, refresh);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        metrics.setHgap(12);
        metrics.setVgap(12);
        metrics.setPadding(new Insets(18, 0, 0, 0));

        VBox center = new VBox(12, metrics, alerts);
        setCenter(center);

        refresh();
    }

    public void refresh() {
        var resumo = service.resumo();

        metrics.getChildren().clear();
        metrics.add(
                InventarioUI.metric(
                        "ARTIGOS",
                        InventarioUI.integer(resumo.produtos()),
                        "Artigos ativos no catálogo"),
                0, 0);
        metrics.add(
                InventarioUI.metric(
                        "ARMAZÉNS",
                        InventarioUI.integer(resumo.armazens()),
                        "Locais operacionais ativos"),
                1, 0);
        metrics.add(
                InventarioUI.metric(
                        "UNIDADES EM STOCK",
                        InventarioUI.integer(resumo.unidades()),
                        "Existência física consolidada"),
                2, 0);
        metrics.add(
                InventarioUI.metric(
                        "UNIDADES RESERVADAS",
                        resumo.reservado().stripTrailingZeros().toPlainString(),
                        "Stock comprometido por reservas"),
                3, 0);
        metrics.add(
                InventarioUI.metric(
                        "VALOR DO STOCK",
                        InventarioUI.money(resumo.valorStock()),
                        "Valorização ao custo registado"),
                0, 1);
        metrics.add(
                InventarioUI.metric(
                        "STOCK BAIXO",
                        InventarioUI.integer(resumo.stockBaixo()),
                        "Artigos no mínimo ou abaixo"),
                1, 1);

        alerts.getChildren().clear();

        Label section = new Label("Alertas operacionais");
        section.getStyleClass().add("inventario-card-title");
        alerts.getChildren().add(section);

        if (resumo.stockBaixo() > 0) {
            Label warning = new Label(
                    resumo.stockBaixo() + " artigo(s) requer(em) reposição.");
            warning.setWrapText(true);
            warning.getStyleClass().add("inventario-card");
            alerts.getChildren().add(warning);
        } else {
            Label ok = new Label("✓ Não existem artigos abaixo do stock mínimo.");
            ok.getStyleClass().add("inventario-metric-detail");
            alerts.getChildren().add(ok);
        }
    }
}
