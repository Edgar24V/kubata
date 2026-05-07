package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class StatsCard {
    private StatsCard() {}

    public static Card create(String title, String value) {
        Card card = new Card();
        VBox body = new VBox(6);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(15));
        Label lblValue = new Label(value);
        lblValue.getStyleClass().addAll(Styles.TITLE_1, Styles.TEXT_BOLD);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add(Styles.TEXT_MUTED);
        body.getChildren().addAll(lblValue, lblTitle);
        card.setBody(body);
        card.setPrefWidth(200);
        return card;
    }
}
