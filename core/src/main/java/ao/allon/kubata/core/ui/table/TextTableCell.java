package ao.allon.kubata.core.ui.table;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * Célula editável que utiliza um TextField.
 */
public class TextTableCell<S, T> extends EditableTableCell<S, T> {

    private TextField textField;

    public TextTableCell(StringConverter<T> converter) {
        super(converter);
    }

    public static <S> TextTableCell<S, String> create() {
        return new TextTableCell<>(new DefaultStringConverter());
    }

    public static <S> TextTableCell<S, Integer> createInteger() {
        return new TextTableCell<>(new javafx.util.converter.IntegerStringConverter());
    }

    public static <S> TextTableCell<S, Long> createLong() {
        return new TextTableCell<>(new javafx.util.converter.LongStringConverter());
    }

    @Override
    protected void createEditor() {
        textField = new TextField(converter.toString(getItem()));
        textField.getStyleClass().add("cell-editor");
        
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                commitValue(converter.fromString(textField.getText()));
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && isEditing()) {
                commitValue(converter.fromString(textField.getText()));
            }
        });

        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }
}
