
package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.theme.Styles;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

public class IndexedTableView<S> extends TableView<S> {

    public IndexedTableView() {
        super();
        initializeIndexColumn();
        setTableEditable();
        getStyleClass().addAll(Styles.BORDERED, Styles.STRIPED);
        setPlaceholder(new Label("Nenhum registro encontrado"));
    }

    private void initializeIndexColumn() {
        TableColumn<S, Number> indexColumn = new TableColumn<>("#");
        indexColumn.setCellValueFactory(cellData -> 
            new ReadOnlyObjectWrapper<>(getItems().indexOf(cellData.getValue()) + 1)
        );
        indexColumn.setSortable(false); // Evita a ordenação da coluna de índice

        // Adiciona a coluna de índice no início da tabela
        getColumns().add(0, indexColumn);
    }
    
     public void applyTo(TableView targetTableView) {
       TableColumn<S, Number> indexColumn = new TableColumn<>("#");
        indexColumn.setCellValueFactory(cellData -> 
            new ReadOnlyObjectWrapper<>(targetTableView.getItems().indexOf(cellData.getValue()) + 1)
        );
        indexColumn.setSortable(false); // Evita a ordenação da coluna de índice
        indexColumn.setMinWidth(50);
        indexColumn.setMaxWidth(100);
        // Adiciona a coluna de índice no início da tabela
        targetTableView.getColumns().add(0, indexColumn);
        targetTableView.getStyleClass().addAll(Styles.BORDERED, Styles.STRIPED);
        targetTableView.setPlaceholder(new Label("Nenhum registro encontrado"));
        setTableEditable();
    }
     
    private void setTableEditable() {
        setEditable(true);
        // allows the individual cells to be selected
        getSelectionModel().cellSelectionEnabledProperty().set(true);
        // when character or numbers pressed it will start edit in editable
        // fields
        setOnKeyPressed(event -> {
                if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                        editFocusedCell();
                } else if (event.getCode() == KeyCode.RIGHT
                                || event.getCode() == KeyCode.TAB) {
                        getSelectionModel().selectNext();
                        event.consume();
                } else if (event.getCode() == KeyCode.LEFT) {
                        // work around due to
                        // TableView.getSelectionModel().selectPrevious() due to a bug
                        // stopping it from working on
                        // the first column in the last row of the table
                        selectPrevious();
                        event.consume();
                }
        });
    }
    
    @SuppressWarnings("unchecked")
    private void editFocusedCell() {
            final TablePosition<S, ?> focusedCell = this
                            .focusModelProperty().get().focusedCellProperty().get();
           this.edit(focusedCell.getRow(), focusedCell.getTableColumn());
    }
    
    @SuppressWarnings("unchecked")
    private void selectPrevious() {
            if (getSelectionModel().isCellSelectionEnabled()) {
                    // in cell selection mode, we have to wrap around, going from
                    // right-to-left, and then wrapping to the end of the previous line
                    TablePosition<S, ?> pos = getFocusModel()
                                    .getFocusedCell();
                    if (pos.getColumn() - 1 >= 0) {
                            // go to previous row
                            getSelectionModel().select(pos.getRow(),
                                            getTableColumn(pos.getTableColumn(), -1));
                    } else if (pos.getRow() < getItems().size()) {
                            // wrap to end of previous row
                            getSelectionModel().select(pos.getRow() - 1,
                                            getVisibleLeafColumn(
                                                            getVisibleLeafColumns().size() - 1));
                    }
            } else {
                    int focusIndex = getFocusModel().getFocusedIndex();
                    if (focusIndex == -1) {
                            getSelectionModel().select(getItems().size() - 1);
                    } else if (focusIndex > 0) {
                            getSelectionModel().select(focusIndex - 1);
                    }
            }
    }
    
    private TableColumn<S, ?> getTableColumn(
                    final TableColumn<S, ?> column, int offset) {
            int columnIndex = getVisibleLeafIndex(column);
            int newColumnIndex = columnIndex + offset;
            return getVisibleLeafColumn(newColumnIndex);
    }
    
    
     
}
