package ao.allon.kubata.core.ui.table;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Célula editável que utiliza um DatePicker.
 */
public class DatePickerTableCell<S> extends EditableTableCell<S, LocalDate> {

    private DatePicker datePicker;
    private final DateTimeFormatter formatter;

    public DatePickerTableCell(String pattern) {
        super(new StringConverter<LocalDate>() {
            private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
            @Override
            public String toString(LocalDate date) {
                return date != null ? dtf.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                return string != null && !string.isEmpty() ? LocalDate.parse(string, dtf) : null;
            }
        });
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    protected void createEditor() {
        datePicker = new DatePicker(getItem());
        datePicker.getStyleClass().add("cell-editor");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        datePicker.setOnAction(e -> {
            commitValue(datePicker.getValue());
        });

        datePicker.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && isEditing()) {
                commitValue(datePicker.getValue());
            }
        });

        setGraphic(datePicker);
        datePicker.requestFocus();
    }
}
