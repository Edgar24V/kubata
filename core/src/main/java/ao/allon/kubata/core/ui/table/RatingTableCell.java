package ao.allon.kubata.core.ui.table;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/**
 * Célula de avaliação visual com estrelas (1–5).
 * Ideal para: avaliações de desempenho, notas, etc.
 */
public class RatingTableCell<S> extends EditableTableCell<S, Integer> {

    private HBox starsBox;
    private static final int MAX_STARS = 5;

    public RatingTableCell() {
        super(new StringConverter<Integer>() {
            @Override 
            public String toString(Integer v) { 
                if (v == null) return "";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < v; i++) sb.append("★");
                return sb.toString();
            }
            
            @Override 
            public Integer fromString(String s) { 
                if (s == null) return 0;
                return (int) s.chars().filter(c -> c == '★').count(); 
            }
        });
        getStyleClass().add("rating-cell");
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) { 
            setGraphic(null); 
            return; 
        }

        if (starsBox == null) {
            starsBox = new HBox(2);
            starsBox.setAlignment(Pos.CENTER);
        }
        
        starsBox.getChildren().clear();
        for (int i = 1; i <= MAX_STARS; i++) {
            Label star = new Label(i <= item ? "★" : "☆");
            star.getStyleClass().add(i <= item ? "star-filled" : "star-empty");
            final int rating = i;
            star.setOnMouseClicked(e -> commitValue(rating));
            star.setOnMouseEntered(e -> highlightStars(rating));
            star.setOnMouseExited(e -> highlightStars(getItem() != null ? getItem() : 0));
            starsBox.getChildren().add(star);
        }
        setGraphic(starsBox);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void highlightStars(int upTo) {
        if (starsBox == null) return;
        for (int i = 0; i < MAX_STARS; i++) {
            Label star = (Label) starsBox.getChildren().get(i);
            star.getStyleClass().setAll(i < upTo ? "star-filled" : "star-empty");
        }
    }

    @Override 
    protected void createEditor() { 
        // A edição é feita diretamente via clique nas estrelas no updateItem
    }
}
