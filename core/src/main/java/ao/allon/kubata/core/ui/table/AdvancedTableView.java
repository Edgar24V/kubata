package ao.allon.kubata.core.ui.table;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * TableView avançada com suporte nativo a menus de contexto, 
 * filtragem e exportação de dados.
 */
public class AdvancedTableView<S> extends TableView<S> {

    private FilteredList<S> filteredData;
    private final ContextMenu contextMenu = new ContextMenu();
    private final Map<S, Boolean> selectedItems = new HashMap<>();
    private final Label sortIndicatorLabel = new Label();
    
    // Context menu callbacks
    private Consumer<S> onEditCallback;
    private Consumer<S> onDeleteCallback;
    private Consumer<S> onViewDetailsCallback;
    private Runnable onRefreshCallback;
    private String entityName = "Item";

    public AdvancedTableView() {
        super();
        setEditable(true);
        getStyleClass().addAll("advanced-table", "bordered", "striped");
        
        setupContextMenu();
        setupRowFactory();
        setupSortIndicator();
    }

    public AdvancedTableView(ObservableList<S> items) {
        this();
        setData(items);
    }

    /**
     * Builder fluente para AdvancedTableView.
     */
    public static <S> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S> {
        private ObservableList<S> data;
        private String placeholder;
        private SelectionMode selectionMode = SelectionMode.SINGLE;
        private Consumer<S> onEdit;
        private Consumer<S> onDelete;
        private Consumer<S> onViewDetails;
        private Runnable onRefresh;
        private String entityName = "Item";

        public Builder<S> data(ObservableList<S> data) {
            this.data = data;
            return this;
        }

        public Builder<S> placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder<S> selectionMode(SelectionMode mode) {
            this.selectionMode = mode;
            return this;
        }

        public Builder<S> onEdit(Consumer<S> callback) {
            this.onEdit = callback;
            return this;
        }

        public Builder<S> onDelete(Consumer<S> callback) {
            this.onDelete = callback;
            return this;
        }

        public Builder<S> onViewDetails(Consumer<S> callback) {
            this.onViewDetails = callback;
            return this;
        }

        public Builder<S> onRefresh(Runnable callback) {
            this.onRefresh = callback;
            return this;
        }

        public Builder<S> entityName(String name) {
            this.entityName = name;
            return this;
        }

        public AdvancedTableView<S> build() {
            AdvancedTableView<S> table = new AdvancedTableView<>();
            if (data != null) table.setData(data);
            if (placeholder != null) table.setPlaceholder(new Label(placeholder));
            table.getSelectionModel().setSelectionMode(selectionMode);
            
            // Configure context menu callbacks
            table.onEditCallback = this.onEdit;
            table.onDeleteCallback = this.onDelete;
            table.onViewDetailsCallback = this.onViewDetails;
            table.onRefreshCallback = this.onRefresh;
            table.entityName = this.entityName;
            
            // Re-setup context menu with new callbacks
            table.setupContextMenu();
            
            return table;
        }
    }

    /**
     * Define os dados da tabela com suporte a filtragem e ordenação automática.
     */
    public void setData(ObservableList<S> items) {
        filteredData = new FilteredList<>(items, p -> true);
        SortedList<S> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(this.comparatorProperty());
        setItems(sortedData);
        selectedItems.clear();
        
        // Listener para mudanças na fonte de dados original
        items.addListener((ListChangeListener<S>) c -> {
            while (c.next()) {
                if (c.wasUpdated() || c.wasAdded() || c.wasRemoved()) {
                    // Notifica listeners de mudança global se necessário
                    // Por agora apenas garante que a view está atualizada
                    refresh();
                }
            }
        });
    }

    /**
     * Aplica um filtro global à tabela.
     */
    public void setFilter(Predicate<S> predicate) {
        if (filteredData != null) {
            filteredData.setPredicate(predicate);
        }
    }

    /**
     * Mostra um spinner de carregamento enquanto os dados são buscados.
     */
    public void setLoading(boolean loading) {
        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(48, 48);
            Label msg = new Label("A carregar dados...");
            VBox loadingPane = new VBox(12, spinner, msg);
            loadingPane.setAlignment(Pos.CENTER);
            loadingPane.getStyleClass().add("table-loading");
            setPlaceholder(loadingPane);
            setDisable(true);
        } else {
            setPlaceholder(new Label("Nenhum registo encontrado."));
            setDisable(false);
        }
    }

    /**
     * Adiciona uma barra de pesquisa acima da tabela.
     */
    public VBox withSearchBar() {
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Pesquisar...");
        searchField.getStyleClass().add("table-search-field");
        searchField.setPrefWidth(280);

        searchField.textProperty().addListener((obs, oldVal, query) -> {
            String q = query.trim().toLowerCase();
            setFilter(item -> {
                if (q.isEmpty()) return true;
                return getColumns().stream().anyMatch(col -> {
                    Object data = col.getCellData(item);
                    return data != null && data.toString().toLowerCase().contains(q);
                });
            });
        });

        HBox toolbar = new HBox(8, new Label("Pesquisar:"), searchField, sortIndicatorLabel);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        toolbar.getStyleClass().add("table-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        return new VBox(toolbar, this);
    }

    /**
     * Cria uma coluna de seleção com checkbox no cabeçalho.
     */
    public TableColumn<S, Boolean> createSelectionColumn() {
        CheckBox headerCheck = new CheckBox();
        TableColumn<S, Boolean> col = new TableColumn<>();
        col.setGraphic(headerCheck);
        col.setSortable(false);
        col.setResizable(false);
        col.setPrefWidth(40);

        headerCheck.setOnAction(e -> {
            boolean selectAll = headerCheck.isSelected();
            getItems().forEach(item -> selectedItems.put(item, selectAll));
            refresh();
        });

        col.setCellFactory(tc -> new CheckBoxTableCell<S>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    S rowItem = getTableRow().getItem();
                    // Usar o mapa de seleção
                    CheckBox cb = (CheckBox) getGraphic();
                    if (cb != null) {
                        cb.setSelected(selectedItems.getOrDefault(rowItem, false));
                        cb.setOnAction(e -> selectedItems.put(rowItem, cb.isSelected()));
                    }
                }
            }
        });
        return col;
    }

    /**
     * Retorna a lista de itens selecionados manualmente via checkbox.
     */
    public List<S> getManuallySelectedItems() {
        List<S> selected = new ArrayList<>();
        selectedItems.forEach((item, isSelected) -> {
            if (isSelected) selected.add(item);
        });
        return selected;
    }

    private void setupContextMenu() {
        contextMenu.getItems().clear();

        // Ver Detalhes
        if (onViewDetailsCallback != null) {
            MenuItem viewItem = new MenuItem("Ver Detalhes");
            viewItem.setOnAction(e -> {
                S selected = getSelectionModel().getSelectedItem();
                if (selected != null) onViewDetailsCallback.accept(selected);
            });
            contextMenu.getItems().add(viewItem);
        }

        // Editar
        if (onEditCallback != null) {
            MenuItem editItem = new MenuItem("Editar " + entityName);
            editItem.setOnAction(e -> {
                S selected = getSelectionModel().getSelectedItem();
                if (selected != null) onEditCallback.accept(selected);
            });
            contextMenu.getItems().add(editItem);
        }

        // Copiar
        MenuItem copyItem = new MenuItem("Copiar");
        copyItem.setOnAction(e -> copySelectionToClipboard());
        contextMenu.getItems().add(copyItem);

        // Separador
        if ((onViewDetailsCallback != null || onEditCallback != null) && (onDeleteCallback != null)) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        // Excluir (com callback ou comportamento padrão)
        MenuItem deleteItem = new MenuItem("Excluir " + entityName);
        deleteItem.setOnAction(e -> {
            S selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (onDeleteCallback != null) {
                    confirmAndDelete(selected);
                } else if (filteredData != null) {
                    filteredData.getSource().remove(selected);
                }
            }
        });
        contextMenu.getItems().add(deleteItem);

        // Exportar
        contextMenu.getItems().add(new SeparatorMenuItem());
        Menu exportMenu = new Menu("Exportar");
        MenuItem exportCsvItem = new MenuItem("Exportar para CSV...");
        exportCsvItem.setOnAction(e -> exportToCSV());
        MenuItem exportExcelItem = new MenuItem("Exportar para Excel...");
        exportExcelItem.setOnAction(e -> exportToExcel());
        exportMenu.getItems().addAll(exportCsvItem, exportExcelItem);
        contextMenu.getItems().add(exportMenu);

        // Atualizar
        if (onRefreshCallback != null) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            MenuItem refreshItem = new MenuItem("Atualizar");
            refreshItem.setOnAction(e -> onRefreshCallback.run());
            contextMenu.getItems().add(refreshItem);
        }

        addBatchMenuItems();
        setContextMenu(contextMenu);
    }

    private void confirmAndDelete(S item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Exclusão");
        confirm.setHeaderText("Excluir " + entityName);
        confirm.setContentText("Tem certeza que deseja excluir este " + entityName.toLowerCase() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onDeleteCallback.accept(item);
        }
    }

    private void addBatchMenuItems() {
        MenuItem deleteSelected = new MenuItem("Eliminar selecionados");
        deleteSelected.setOnAction(e -> {
            List<S> toDelete = getManuallySelectedItems();
            if (toDelete.isEmpty()) return;
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Eliminar %d registos?", toDelete.size()),
                ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES && filteredData != null) {
                    filteredData.getSource().removeAll(toDelete);
                    selectedItems.clear();
                }
            });
        });

        MenuItem exportSelected = new MenuItem("Exportar selecionados para CSV");
        exportSelected.setOnAction(e -> {
            List<S> toExport = getManuallySelectedItems();
            if (!toExport.isEmpty()) exportToCSV(toExport);
        });

        contextMenu.getItems().addAll(deleteSelected, exportSelected);
    }

    private void setupRowFactory() {
        setRowFactory(tv -> {
            TableRow<S> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    // Seleciona o item clicado
                    getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void setupSortIndicator() {
        getSortOrder().addListener((ListChangeListener<TableColumn<S, ?>>) change -> {
            StringBuilder sortInfo = new StringBuilder();
            for (TableColumn<S, ?> col : getSortOrder()) {
                sortInfo.append(col.getText())
                        .append(col.getSortType() == TableColumn.SortType.ASCENDING ? " ↑ " : " ↓ ");
            }
            sortIndicatorLabel.setText(sortInfo.toString().trim());
            if (!sortIndicatorLabel.getText().isEmpty()) {
                sortIndicatorLabel.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold; -fx-padding: 0 0 0 10;");
            }
        });
    }

    private void copySelectionToClipboard() {
        S selected = getSelectionModel().getSelectedItem();
        if (selected == null) return;

        StringBuilder sb = new StringBuilder();
        for (TableColumn<S, ?> col : getColumns()) {
            Object cellData = col.getCellData(selected);
            sb.append(cellData != null ? cellData.toString() : "").append("\t");
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString().trim());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void exportToCSV() {
        exportToCSV(getItems());
    }

    private void exportToCSV(List<S> dataToExport) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Header
                StringBuilder header = new StringBuilder();
                List<TableColumn<S, ?>> columns = getColumns();
                for (int i = 0; i < columns.size(); i++) {
                    header.append(escapeCsvField(columns.get(i).getText()));
                    if (i < columns.size() - 1) header.append(",");
                }
                writer.println(header.toString());

                // Data
                for (S item : dataToExport) {
                    StringBuilder row = new StringBuilder();
                    for (int i = 0; i < columns.size(); i++) {
                        Object cellData = columns.get(i).getCellData(item);
                        row.append(escapeCsvField(cellData != null ? cellData.toString() : ""));
                        if (i < columns.size() - 1) row.append(",");
                    }
                    writer.println(row.toString());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void exportToExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar para Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Dados");

            // Estilo cabeçalho
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Cabeçalho
            Row headerRow = sheet.createRow(0);
            List<TableColumn<S, ?>> cols = getColumns();
            for (int i = 0; i < cols.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols.get(i).getText());
                cell.setCellStyle(headerStyle);
            }

            // Dados
            List<S> data = getItems();
            for (int r = 0; r < data.size(); r++) {
                Row row = sheet.createRow(r + 1);
                S item = data.get(r);
                for (int c = 0; c < cols.size(); c++) {
                    Object val = cols.get(c).getCellData(item);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    if (val instanceof Number n) cell.setCellValue(n.doubleValue());
                    else if (val instanceof Boolean b) cell.setCellValue(b);
                    else cell.setCellValue(val != null ? val.toString() : "");
                }
            }

            for (int i = 0; i < cols.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String escapeCsvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
