package ao.allon.kubata.core.ui.table;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

/**
 * Célula editável que utiliza um Spinner.
 */
public class SpinnerTableCell<S, T extends Number> extends EditableTableCell<S, T> {

    private final SpinnerValueFactory<T> valueFactory;
    private Spinner<T> spinner;

    public SpinnerTableCell(StringConverter<T> converter, SpinnerValueFactory<T> valueFactory) {
        super(converter);
        this.valueFactory = valueFactory;
    }

    @Override
    protected void createEditor() {
        spinner = new Spinner<>();
        spinner.setValueFactory(valueFactory);
        spinner.getStyleClass().add("cell-editor");
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.setEditable(true);
        
        if (getItem() != null) {
            valueFactory.setValue(getItem());
        }

        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && isEditing()) {
                commitValue(spinner.getValue());
            }
        });

        setGraphic(spinner);
        spinner.requestFocus();
    }
}
