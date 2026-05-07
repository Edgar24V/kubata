package ao.allon.kubata.core.ui.table;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Célula com autocompletar baseado em lista de sugestões.
 * Integra com dados do servidor via Supplier<List<T>>.
 */
public class AutoCompleteTableCell<S, T> extends EditableTableCell<S, T> {

    private final Supplier<List<T>> suggestionsSupplier;
    private final StringConverter<T> itemConverter;
    private TextField searchField;
    private ListView<T> suggestionList;
    private PopupControl popup;

    public AutoCompleteTableCell(StringConverter<T> converter, Supplier<List<T>> supplier) {
        super(converter);
        this.itemConverter = converter;
        this.suggestionsSupplier = supplier;
    }

    @Override
    protected void createEditor() {
        searchField = new TextField(itemConverter.toString(getItem()));
        searchField.getStyleClass().add("cell-editor");

        popup = new PopupControl();
        suggestionList = new ListView<>();
        suggestionList.getStyleClass().add("autocomplete-popup");
        suggestionList.setPrefHeight(150);

        searchField.textProperty().addListener((obs, old, text) -> {
            if (text == null || text.isEmpty()) {
                popup.hide();
                return;
            }
            
            List<T> all = suggestionsSupplier.get();
            if (all == null) return;

            List<T> filtered = all.stream()
                .filter(i -> itemConverter.toString(i).toLowerCase().contains(text.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());
            
            suggestionList.setItems(FXCollections.observableArrayList(filtered));
            if (!filtered.isEmpty()) {
                showPopup();
            } else {
                popup.hide();
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            T selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                commitValue(selected);
                popup.hide();
            }
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                if (popup.isShowing()) {
                    suggestionList.requestFocus();
                    suggestionList.getSelectionModel().selectFirst();
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
                popup.hide();
            }
        });

        popup.getScene().setRoot(suggestionList);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (searchField == null) {
            createEditor();
        }
        searchField.setText(itemConverter.toString(getItem()));
        setGraphic(searchField);
        searchField.requestFocus();
        searchField.selectAll();
    }

    private void showPopup() {
        if (!isEditing()) return;
        Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
        if (bounds != null) {
            popup.show(searchField, bounds.getMinX(), bounds.getMaxY());
        }
    }
}
