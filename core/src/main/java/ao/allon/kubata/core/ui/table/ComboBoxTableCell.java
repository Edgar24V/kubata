package ao.allon.kubata.core.ui.table;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Célula editável que utiliza um ComboBox.
 */
public class ComboBoxTableCell<S, T> extends EditableTableCell<S, T> {

    private final ObservableList<T> items;
    private ComboBox<T> comboBox;

    public ComboBoxTableCell(StringConverter<T> converter, ObservableList<T> items) {
        super(converter);
        this.items = items;
    }

    @Override
    protected void createEditor() {
        comboBox = new ComboBox<>(items);
        comboBox.setConverter(converter);
        comboBox.getStyleClass().add("cell-editor");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        
        comboBox.setValue(getItem());

        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isEditing()) {
                commitValue(newVal);
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && isEditing()) {
                commitValue(comboBox.getValue());
            }
        });

        setGraphic(comboBox);
        comboBox.requestFocus();
    }
}
