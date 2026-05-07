package ao.allon.kubata.core.ui.table;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.util.StringConverter;

/**
 * Célula editável que utiliza um CheckBox.
 * <p>
 * Usa um StringConverter<Boolean> correto para evitar ClassCastException
 * quando EditableTableCell.updateItem invoca converter.toString(item).
 */
public class CheckBoxTableCell<S> extends EditableTableCell<S, Boolean> {

    private CheckBox checkBox;

    public CheckBoxTableCell() {
        super(new StringConverter<Boolean>() {
            @Override
            public String toString(Boolean value) {
                if (value == null) return "";
                return value ? "Sim" : "Não";
            }

            @Override
            public Boolean fromString(String string) {
                if (string == null) return Boolean.FALSE;
                return "true".equalsIgnoreCase(string) || "sim".equalsIgnoreCase(string);
            }
        });
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (checkBox == null) {
                createEditor();
            }
            checkBox.setSelected(item);
            setText(null);
            setGraphic(checkBox);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    @Override
    protected void createEditor() {
        checkBox = new CheckBox();
        checkBox.getStyleClass().add("cell-editor-checkbox");
        setAlignment(Pos.CENTER);

        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            commitValue(newVal);
        });
    }
}
