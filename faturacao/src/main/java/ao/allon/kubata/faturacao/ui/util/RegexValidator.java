package ao.allon.kubata.faturacao.ui.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.util.function.Predicate;

public class RegexValidator {

    /**
     * Apply validation to a TextField using a regex.
     *
     * @param textField    the TextField to validate.
     * @param regex        the regex pattern for validation.
     * @param errorMessage the message to display in the tooltip when validation fails.
     */
    public static void applyToTextField(TextField textField, String regex, String errorMessage) {
        Tooltip tooltip = new Tooltip(errorMessage);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(regex)) {
                setErrorStyle(textField, tooltip);
            } else {
                resetStyle(textField);
            }
        });
    }

    /**
     * Apply validation to a ComboBox using a predicate.
     *
     * @param comboBox     the ComboBox to validate.
     * @param condition    a Predicate for validation.
     * @param errorMessage the message to display in the tooltip when validation fails.
     * @param <T>          the type of items in the ComboBox.
     */
    public static <T> void applyToComboBox(ComboBox<T> comboBox, Predicate<T> condition, String errorMessage) {
        Tooltip tooltip = new Tooltip(errorMessage);

        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!condition.test(newValue)) {
                setErrorStyle(comboBox, tooltip);
            } else {
                resetStyle(comboBox);
            }
        });
    }

    /**
     * Apply validation to a DatePicker using a predicate.
     *
     * @param datePicker   the DatePicker to validate.
     * @param condition    a Predicate for validation.
     * @param errorMessage the message to display in the tooltip when validation fails.
     */
    public static void applyToDatePicker(DatePicker datePicker, Predicate<LocalDate> condition, String errorMessage) {
        Tooltip tooltip = new Tooltip(errorMessage);

        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!condition.test(newValue)) {
                setErrorStyle(datePicker, tooltip);
            } else {
                resetStyle(datePicker);
            }
        });
    }

    // Set error style and attach a Tooltip
    private static void setErrorStyle(javafx.scene.control.Control control, Tooltip tooltip) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        control.setTooltip(tooltip);
    }

    // Reset the style of the control
    private static void resetStyle(javafx.scene.control.Control control) {
        control.setStyle(null);
        control.setTooltip(null);
    }
}
