package ao.allon.kubata.core.ui.table;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.BiConsumer;

/**
 * Manager para facilitar a configuração de tabelas editáveis inline.
 * Fornece métodos type-safe para criar colunas editáveis de diferentes tipos.
 *
 * @param <S> Tipo do item da tabela
 */
public class EditableTableManager<S> {

    private final TableView<S> tableView;
    private boolean autoCommit = true;

    /**
     * Cria um manager para a TableView especificada.
     */
    public EditableTableManager(TableView<S> tableView) {
        this.tableView = tableView;
        tableView.setEditable(true);
    }

    /**
     * Define se as edições devem ser automaticamente commitadas.
     */
    public EditableTableManager<S> withAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }

    /**
     * Cria uma coluna de texto editável.
     */
    public TableColumn<S, String> createTextColumn(String title,
                                                      Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> valueFactory,
                                                      BiConsumer<S, String> onEditCommit) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            String newValue = event.getNewValue();
            onEditCommit.accept(item, newValue);
            if (autoCommit) {
                refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de texto editável usando PropertyValueFactory.
     */
    public TableColumn<S, String> createTextColumn(String title,
                                                      String propertyName,
                                                      BiConsumer<S, String> onEditCommit) {
        return createTextColumn(title, new PropertyValueFactory<>(propertyName), onEditCommit);
    }

    /**
     * Cria uma coluna de número inteiro editável.
     */
    public TableColumn<S, Integer> createIntegerColumn(String title,
                                                          Callback<TableColumn.CellDataFeatures<S, Integer>, ObservableValue<Integer>> valueFactory,
                                                          BiConsumer<S, Integer> onEditCommit) {
        TableColumn<S, Integer> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            Integer newValue = event.getNewValue();
            if (newValue != null) {
                onEditCommit.accept(item, newValue);
                if (autoCommit) refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de número longo editável.
     */
    public TableColumn<S, Long> createLongColumn(String title,
                                                    Callback<TableColumn.CellDataFeatures<S, Long>, ObservableValue<Long>> valueFactory,
                                                    BiConsumer<S, Long> onEditCommit) {
        TableColumn<S, Long> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(TextFieldTableCell.forTableColumn(new LongStringConverter()));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            Long newValue = event.getNewValue();
            if (newValue != null) {
                onEditCommit.accept(item, newValue);
                if (autoCommit) refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de número decimal (Double) editável.
     */
    public TableColumn<S, Double> createDoubleColumn(String title,
                                                        Callback<TableColumn.CellDataFeatures<S, Double>, ObservableValue<Double>> valueFactory,
                                                        BiConsumer<S, Double> onEditCommit) {
        TableColumn<S, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            Double newValue = event.getNewValue();
            if (newValue != null) {
                onEditCommit.accept(item, newValue);
                if (autoCommit) refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de BigDecimal (moeda) editável.
     */
    public TableColumn<S, BigDecimal> createBigDecimalColumn(String title,
                                                               Callback<TableColumn.CellDataFeatures<S, BigDecimal>, ObservableValue<BigDecimal>> valueFactory,
                                                               BiConsumer<S, BigDecimal> onEditCommit) {
        TableColumn<S, BigDecimal> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            BigDecimal newValue = event.getNewValue();
            if (newValue != null) {
                onEditCommit.accept(item, newValue);
                if (autoCommit) refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de data editável usando DatePickerTableCell.
     */
    public TableColumn<S, LocalDate> createDateColumn(String title,
                                                        Callback<TableColumn.CellDataFeatures<S, LocalDate>, ObservableValue<LocalDate>> valueFactory,
                                                        BiConsumer<S, LocalDate> onEditCommit) {
        TableColumn<S, LocalDate> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new DatePickerTableCell<>("dd/MM/yyyy"));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            LocalDate newValue = event.getNewValue();
            onEditCommit.accept(item, newValue);
            if (autoCommit) refreshTable();
        });

        return column;
    }

    /**
     * Cria uma coluna de checkbox editável.
     */
    public <T> TableColumn<S, T> createComboColumn(String title,
                                                    Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> valueFactory,
                                                    ObservableList<T> items,
                                                    StringConverter<T> converter,
                                                    BiConsumer<S, T> onEditCommit) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new ComboBoxTableCell<>(converter, items));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            T newValue = event.getNewValue();
            onEditCommit.accept(item, newValue);
            if (autoCommit) refreshTable();
        });

        return column;
    }

    /**
     * Cria uma coluna de checkbox (booleano) editável.
     */
    public TableColumn<S, Boolean> createCheckBoxColumn(String title,
                                                         Callback<TableColumn.CellDataFeatures<S, Boolean>, ObservableValue<Boolean>> valueFactory,
                                                         BiConsumer<S, Boolean> onEditCommit) {
        TableColumn<S, Boolean> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new ao.allon.kubata.core.ui.table.CheckBoxTableCell<>());
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            Boolean newValue = event.getNewValue();
            if (newValue != null) {
                onEditCommit.accept(item, newValue);
                if (autoCommit) refreshTable();
            }
        });

        return column;
    }

    /**
     * Cria uma coluna de texto editável com máscara (ex: NIF, telefone).
     */
    public TableColumn<S, String> createMaskedColumn(String title,
                                                       String mask,
                                                       Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> valueFactory,
                                                       BiConsumer<S, String> onEditCommit) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new MaskedTextTableCell<>(mask));
        column.setEditable(true);

        column.setOnEditCommit(event -> {
            S item = event.getRowValue();
            String newValue = event.getNewValue();
            onEditCommit.accept(item, newValue);
            if (autoCommit) refreshTable();
        });

        return column;
    }

    /**
     * Atualiza a tabela para refletir mudanças.
     */
    public void refreshTable() {
        tableView.refresh();
    }

    /**
     * Aplica configurações padrão para tornar a tabela completamente editável.
     * Detecta automaticamente os tipos das colunas baseado nos dados.
     */
    public EditableTableManager<S> applyDefaultEditBehavior() {
        tableView.setEditable(true);

        // Configurar duplo clique para iniciar edição
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TableColumn<S, ?> column = tableView.getFocusModel().getFocusedCell().getTableColumn();
                if (column != null && column.isEditable()) {
                    tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
                }
            }
        });

        return this;
    }

    /**
     * Helper para criar SimpleStringProperty a partir de getter.
     */
    public static <S> Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>>
    stringProperty(java.util.function.Function<S, String> getter) {
        return cdf -> new SimpleStringProperty(getter.apply(cdf.getValue()));
    }

    /**
     * Helper para criar SimpleObjectProperty a partir de getter.
     */
    public static <S, T> Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>>
    objectProperty(java.util.function.Function<S, T> getter) {
        return cdf -> new SimpleObjectProperty<>(getter.apply(cdf.getValue()));
    }
}
