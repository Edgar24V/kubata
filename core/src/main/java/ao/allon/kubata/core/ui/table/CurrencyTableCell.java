package ao.allon.kubata.core.ui.table;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Célula editável para valores monetários no formato angolano.
 * Exibe: 1.250.000,00 Kz
 */
public class CurrencyTableCell<S> extends EditableTableCell<S, Double> {

    private static final Locale LOCALE_AO = new Locale("pt", "AO");
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(LOCALE_AO);
    private TextField textField;

    static {
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    public CurrencyTableCell() {
        super(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : FORMAT.format(value) + " Kz";
            }
            @Override
            public Double fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try {
                    String clean = s.replace(" Kz", "")
                                    .replace(".", "")
                                    .replace(",", ".");
                    return Double.parseDouble(clean);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });
        getStyleClass().add("currency-cell");
        setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    @Override
    protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(converter.toString(item));
                }
                setGraphic(textField);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(converter.toString(item));
                setContentDisplay(javafx.scene.control.ContentDisplay.TEXT_ONLY);
            }
        }
    }

    @Override
    protected void createEditor() {
        textField = new TextField(converter.toString(getItem()));
        textField.getStyleClass().addAll("cell-editor", "currency-editor");
        textField.setAlignment(Pos.CENTER_RIGHT);

        textField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                commitValue(converter.fromString(textField.getText()));
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        textField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && isEditing()) {
                commitValue(converter.fromString(textField.getText()));
            }
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (textField == null) {
            createEditor();
        }
        textField.setText(converter.toString(getItem()));
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }
}
