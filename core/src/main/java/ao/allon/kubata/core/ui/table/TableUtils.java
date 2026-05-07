package ao.allon.kubata.core.ui.table;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Classe utilitária para padronização e configuração rápida de TableViews.
 * Fornece métodos type-safe que evitam o uso de reflection (PropertyValueFactory).
 */
public final class TableUtils {

    private TableUtils() {}

    /**
     * Aplica as configurações base de estilo e comportamento a qualquer TableView.
     */
    public static void standardize(TableView<?> table) {
        table.getStyleClass().add("advanced-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setTableMenuButtonVisible(true);
        
        // Carrega o CSS global do Core se disponível
        /*String css = TableUtils.class.getResource("/css/kubata-table.css") != null 
            ? TableUtils.class.getResource("/css/kubata-table.css").toExternalForm() 
            : null;
        if (css != null && !table.getStylesheets().contains(css)) {
            table.getStylesheets().add(css);
        }*/
        
        // Ativa o ajuste inteligente para preencher espaço se houver sobra
        applySmartResize(table);
    }

    /**
     * Aplica uma lógica que preenche o espaço se sobrar, mas mantém scroll se faltar.
     */
    private static void applySmartResize(TableView<?> table) {
        table.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = table.getColumns().stream()
                    .mapToDouble(TableColumn::getPrefWidth)
                    .sum();
            
            // Margem para scrollbar vertical se necessário
            double availableWidth = newVal.doubleValue() - 20; 
            
            if (totalWidth < availableWidth) {
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            }
        });
        
        // Também verifica quando as colunas mudam
        table.getColumns().addListener((javafx.collections.ListChangeListener.Change<? extends TableColumn<?, ?>> c) -> {
            double totalWidth = table.getColumns().stream()
                    .mapToDouble(TableColumn::getPrefWidth)
                    .sum();
            double availableWidth = table.getWidth() - 20;
            if (totalWidth < availableWidth) {
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            }
        });
    }

    /**
     * Configurações padrão para diferentes contextos de uso no Kubata.
     */
    public static class TableProfiles {

        /** Tabela de leitura apenas — sem edição, sem seleção múltipla */
        public static void readOnly(TableView<?> table) {
            table.setEditable(false);
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            table.getStyleClass().addAll("advanced-table", "read-only-table");
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setTableMenuButtonVisible(true);
            applySmartResize(table);
        }

        /** Tabela de edição — editável, seleção múltipla, ações em lote */
        public static void editable(TableView<?> table) {
            table.setEditable(true);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            table.getStyleClass().addAll("advanced-table", "editable-table");
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setTableMenuButtonVisible(true);
            applySmartResize(table);
        }

        /** Tabela de relatório — sem edição, com linhas alternadas, exportação */
        public static void report(TableView<?> table) {
            table.setEditable(false);
            table.getStyleClass().addAll("advanced-table", "report-table", "striped");
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setTableMenuButtonVisible(true);
            applySmartResize(table);
        }
    }

    /**
     * Cria e configura uma coluna de texto editável de forma type-safe.
     */
    public static <S> TableColumn<S, String> createTextColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> valueFactory) {
        
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> TextTableCell.create());
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de texto com máscara (NIF, BI, etc).
     */
    public static <S> TableColumn<S, String> createMaskedColumn(
            String title, 
            String mask,
            Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> valueFactory) {
        
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new MaskedTextTableCell<>(mask));
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna com AutoComplete.
     */
    public static <S, T> TableColumn<S, T> createAutoCompleteColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> valueFactory,
            StringConverter<T> converter,
            Supplier<List<T>> suggestionsSupplier) {
        
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new AutoCompleteTableCell<>(converter, suggestionsSupplier));
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de ComboBox editável de forma type-safe.
     */
    public static <S, T> TableColumn<S, T> createComboColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> valueFactory,
            ObservableList<T> items, 
            StringConverter<T> converter) {
        
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new ComboBoxTableCell<>(converter, items));
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de CheckBox de forma type-safe.
     */
    public static <S> TableColumn<S, Boolean> createCheckColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, Boolean>, ObservableValue<Boolean>> valueFactory) {
        
        TableColumn<S, Boolean> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new CheckBoxTableCell<>());
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de DatePicker de forma type-safe.
     */
    public static <S> TableColumn<S, LocalDate> createDateColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, LocalDate>, ObservableValue<LocalDate>> valueFactory) {
        
        TableColumn<S, LocalDate> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new DatePickerTableCell<>("dd/MM/yyyy"));
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de Moeda (Kwanza) de forma type-safe.
     */
    public static <S> TableColumn<S, Double> createCurrencyColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, Double>, ObservableValue<Double>> valueFactory) {
        
        TableColumn<S, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new CurrencyTableCell<>());
        column.setEditable(true);
        return column;
    }

    /**
     * Cria e configura uma coluna de Avaliação (Estrelas) de forma type-safe.
     */
    public static <S> TableColumn<S, Integer> createRatingColumn(
            String title, 
            Callback<TableColumn.CellDataFeatures<S, Integer>, ObservableValue<Integer>> valueFactory) {
        
        TableColumn<S, Integer> column = new TableColumn<>(title);
        column.setCellValueFactory(valueFactory);
        column.setCellFactory(tc -> new RatingTableCell<>());
        column.setEditable(true);
        return column;
    }
}
