package ao.allon.kubata.core.ui.table;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import javafx.scene.control.Tooltip;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Classe base abstrata para células de tabela editáveis com validação.
 *
 * @param <S> O tipo do objeto da linha da tabela.
 * @param <T> O tipo do valor da célula.
 */
public abstract class EditableTableCell<S, T> extends TableCell<S, T> {

    protected final StringConverter<T> converter;
    protected Predicate<T> validator;
    protected String errorMessage = "Valor inválido";
    private Tooltip errorTooltip;
    private Consumer<T> onCommitCallback;

    public EditableTableCell(StringConverter<T> converter) {
        this.converter = converter;
        getStyleClass().add("editable-table-cell");
    }

    /**
     * Define um validador para o valor inserido.
     */
    public void setValidator(Predicate<T> validator, String errorMessage) {
        this.validator = validator;
        this.errorMessage = errorMessage;
    }

    /**
     * Define um callback para quando a edição for confirmada.
     */
    public void setOnCommit(Consumer<T> callback) {
        this.onCommitCallback = callback;
    }

    @Override
    public void commitEdit(T newValue) {
        super.commitEdit(newValue);
        if (onCommitCallback != null) {
            onCommitCallback.accept(newValue);
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();
        createEditor();
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(converter.toString(getItem()));
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        getStyleClass().remove("cell-error");
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(converter.toString(item));
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }

    /**
     * Cria o componente editor (TextField, ComboBox, etc.)
     */
    protected abstract void createEditor();

    /**
     * Valida e submete o novo valor.
     */
    protected void commitValue(T newValue) {
        if (validator == null || validator.test(newValue)) {
            commitEdit(newValue);
            getStyleClass().remove("cell-error");
            if (errorTooltip != null) {
                Tooltip.uninstall(this, errorTooltip);
                errorTooltip = null;
            }
        } else {
            getStyleClass().add("cell-error");
            if (errorTooltip == null) {
                errorTooltip = new Tooltip(errorMessage);
                errorTooltip.getStyleClass().add("error-tooltip");
            } else {
                errorTooltip.setText(errorMessage);
            }
            Tooltip.install(this, errorTooltip);
        }
    }
}
