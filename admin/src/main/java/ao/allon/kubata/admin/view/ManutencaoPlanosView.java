package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/**
 * Orientação para planos de manutenção (backup, Flyway, revisão de logs).
 *
 * <p>A lógica e as recomendações são apresentadas numa interface
 * estruturada para consulta rápida pelo administrador.</p>
 */
@Component
public class ManutencaoPlanosView extends VBox {

    public ManutencaoPlanosView() {
        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
    }

    private void buildUi() {
        HBox header = buildHeader();

        VBox content = new VBox(16);
        content.setPadding(new Insets(18, 20, 24, 20));
        content.setFillWidth(true);

        content.getChildren().addAll(
                buildOverview(),
                buildMaintenancePlan(),
                buildClosingNote()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setMaxHeight(Double.MAX_VALUE);
        scroll.getStyleClass().add("application-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(header, scroll);
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-box");

        VBox titleBox = new VBox(2);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.CALENDAR, 18));
        Label title = new Label("Planos de Manutenção");
        title.getStyleClass().add("h3");

        titleLine.getChildren().addAll(icon, title);

        Label subtitle = new Label(
                "Rotinas recomendadas para backup, actualizações, auditoria e segurança do Kubata"
        );
        subtitle.getStyleClass().add("text-muted");
        subtitle.setWrapText(true);

        titleBox.getChildren().addAll(titleLine, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("OPERAÇÕES");
        badge.setStyle(
                "-fx-background-color: #E8F5E9;" +
                "-fx-text-fill: #217346;" +
                "-fx-background-radius: 20px;" +
                "-fx-padding: 6px 10px;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 800;"
        );

        header.getChildren().addAll(titleBox, spacer, badge);
        return header;
    }

    private HBox buildOverview() {
        HBox row = new HBox(12);
        row.setFillHeight(true);

        VBox daily = summaryCard(
                Feather.DATABASE,
                "BACKUP",
                "Diário",
                "Cópia completa da base de dados fora do horário de pico."
        );

        VBox restore = summaryCard(
                Feather.REFRESH_CW,
                "RESTAURO",
                "Mensal",
                "Teste de recuperação em ambiente de homologação."
        );

        VBox review = summaryCard(
                Feather.SHIELD,
                "AUDITORIA",
                "Semanal",
                "Revisão de auditoria no Kubata Administrator."
        );

        HBox.setHgrow(daily, Priority.ALWAYS);
        HBox.setHgrow(restore, Priority.ALWAYS);
        HBox.setHgrow(review, Priority.ALWAYS);

        row.getChildren().addAll(daily, restore, review);
        return row;
    }

    private VBox summaryCard(
            Feather iconType,
            String eyebrow,
            String value,
            String description) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");

        HBox top = new HBox(9);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(34, 34);
        iconBox.setPrefSize(34, 34);
        iconBox.setMaxSize(34, 34);
        iconBox.setStyle(
                "-fx-background-color: rgba(33,115,70,0.08);" +
                "-fx-background-radius: 9px;"
        );
        iconBox.getChildren().add(IconUtils.icon(iconType, 15));

        VBox labels = new VBox(1);

        Label small = new Label(eyebrow);
        small.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #6e7781;" +
                "-fx-letter-spacing: 0.6px;"
        );

        Label main = new Label(value);
        main.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: #24292f;"
        );

        labels.getChildren().addAll(small, main);
        top.getChildren().addAll(iconBox, labels);

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        card.getChildren().addAll(top, desc);
        return card;
    }

    private VBox buildMaintenancePlan() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(18));
        section.getStyleClass().add("card");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.LIST, 16));
        Label title = new Label("Plano recomendado");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");

        heading.getChildren().addAll(icon, title);

        Label description = new Label(
                "Use esta sequência como referência para manter o ambiente de produção estável e seguro."
        );
        description.setWrapText(true);
        description.getStyleClass().add("text-muted");

        VBox items = new VBox(8);

        items.getChildren().addAll(
                planItem(
                        "01",
                        "Backup completo da base de dados",
                        "DIÁRIO",
                        "Executar fora do horário de pico e repetir antes de cada actualização."
                ),
                planItem(
                        "02",
                        "Teste de restauro",
                        "MENSAL",
                        "Validar a recuperação da cópia em ambiente de homologação."
                ),
                planItem(
                        "03",
                        "Migrações Flyway",
                        "POR ACTUALIZAÇÃO",
                        "Aplicar apenas após backup verificado e rever scripts no ambiente de teste."
                ),
                planItem(
                        "04",
                        "Revisão de auditoria",
                        "SEMANAL",
                        "Consultar o separador «Auditoria» no Kubata Administrator."
                ),
                planItem(
                        "05",
                        "Rotação de chaves API / Webhooks",
                        "TRIMESTRAL",
                        "Executar trimestralmente ou após saída de pessoal com acesso."
                )
        );

        section.getChildren().addAll(heading, description, items);
        return section;
    }

    private HBox planItem(
            String number,
            String title,
            String cadence,
            String description) {

        HBox row = new HBox(11);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 11, 10, 11));
        row.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-background-radius: 9px;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 9px;"
        );

        Label index = new Label(number);
        index.setMinSize(28, 28);
        index.setPrefSize(28, 28);
        index.setAlignment(Pos.CENTER);
        index.setStyle(
                "-fx-background-color: #E8F5E9;" +
                "-fx-text-fill: #217346;" +
                "-fx-background-radius: 8px;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 800;"
        );

        VBox body = new VBox(3);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800;");

        Label cadenceLabel = new Label(cadence);
        cadenceLabel.setStyle(
                "-fx-background-color: #eef5f1;" +
                "-fx-text-fill: #217346;" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 4px 7px;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 800;"
        );

        titleLine.getChildren().addAll(titleLabel, cadenceLabel);

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        body.getChildren().addAll(titleLine, desc);
        HBox.setHgrow(body, Priority.ALWAYS);

        row.getChildren().addAll(index, body);
        return row;
    }

    private HBox buildClosingNote() {
        HBox note = new HBox(10);
        note.setPadding(new Insets(12, 14, 12, 14));
        note.setAlignment(Pos.CENTER_LEFT);
        note.getStyleClass().add("card");

        Label icon = new Label("", IconUtils.icon(Feather.INFO, 14));

        Label text = new Label(
                "Utilize o separador Backup para operações de cópia de segurança integradas na aplicação."
        );
        text.setWrapText(true);
        text.getStyleClass().add("text-muted");
        HBox.setHgrow(text, Priority.ALWAYS);

        note.getChildren().addAll(icon, text);
        return note;
    }
}
