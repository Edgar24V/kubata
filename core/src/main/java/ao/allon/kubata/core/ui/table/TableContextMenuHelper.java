package ao.allon.kubata.core.ui.table;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Helper para adicionar menus de contexto funcionais e completos a qualquer TableView.
 * Suporta operações: Copiar, Editar, Excluir, Exportar CSV/Excel, e ações customizadas.
 *
 * @param <S> Tipo do item da tabela
 */
public class TableContextMenuHelper<S> {

    private final TableView<S> tableView;
    private final ContextMenu contextMenu;
    private Consumer<S> onEdit;
    private Consumer<S> onDelete;
    private Consumer<S> onViewDetails;
    private Runnable onRefresh;
    private Function<S, String> exportFormatter;
    private boolean enableEdit = true;
    private boolean enableDelete = true;
    private boolean enableExport = true;
    private boolean enableCopy = true;
    private boolean enableRefresh = true;
    private boolean enableViewDetails = true;
    private String entityName = "Item";

    /**
     * Cria um helper para a TableView especificada.
     */
    public TableContextMenuHelper(TableView<S> tableView) {
        this.tableView = tableView;
        this.contextMenu = new ContextMenu();
        setupDefaultRowFactory();
    }

    /**
     * Define o nome da entidade para mensagens (ex: "Cliente", "Produto")
     */
    public TableContextMenuHelper<S> withEntityName(String name) {
        this.entityName = name;
        return this;
    }

    /**
     * Habilita/desabilita opção de edição
     */
    public TableContextMenuHelper<S> enableEdit(boolean enable) {
        this.enableEdit = enable;
        return this;
    }

    /**
     * Habilita/desabilita opção de exclusão
     */
    public TableContextMenuHelper<S> enableDelete(boolean enable) {
        this.enableDelete = enable;
        return this;
    }

    /**
     * Habilita/desabilita opções de exportação
     */
    public TableContextMenuHelper<S> enableExport(boolean enable) {
        this.enableExport = enable;
        return this;
    }

    /**
     * Habilita/desabilita opção de copiar
     */
    public TableContextMenuHelper<S> enableCopy(boolean enable) {
        this.enableCopy = enable;
        return this;
    }

    /**
     * Habilita/desabilita opção de atualizar
     */
    public TableContextMenuHelper<S> enableRefresh(boolean enable) {
        this.enableRefresh = enable;
        return this;
    }

    /**
     * Habilita/desabilita opção de ver detalhes
     */
    public TableContextMenuHelper<S> enableViewDetails(boolean enable) {
        this.enableViewDetails = enable;
        return this;
    }

    /**
     * Define callback para edição
     */
    public TableContextMenuHelper<S> onEdit(Consumer<S> callback) {
        this.onEdit = callback;
        return this;
    }

    /**
     * Define callback para exclusão
     */
    public TableContextMenuHelper<S> onDelete(Consumer<S> callback) {
        this.onDelete = callback;
        return this;
    }

    /**
     * Define callback para ver detalhes
     */
    public TableContextMenuHelper<S> onViewDetails(Consumer<S> callback) {
        this.onViewDetails = callback;
        return this;
    }

    /**
     * Define callback para atualizar
     */
    public TableContextMenuHelper<S> onRefresh(Runnable callback) {
        this.onRefresh = callback;
        return this;
    }

    /**
     * Define formatter para exportação
     */
    public TableContextMenuHelper<S> withExportFormatter(Function<S, String> formatter) {
        this.exportFormatter = formatter;
        return this;
    }

    /**
     * Aplica o menu de contexto configurado à tabela.
     * Deve ser chamado após configurar todas as opções.
     */
    public void apply() {
        buildMenuItems();
        tableView.setContextMenu(contextMenu);
    }

    /**
     * Adiciona um item de menu customizado ao context menu.
     */
    public TableContextMenuHelper<S> addCustomMenuItem(String text, Consumer<S> action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> {
            S selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                action.accept(selected);
            }
        });
        contextMenu.getItems().add(item);
        return this;
    }

    /**
     * Adiciona um separador ao menu de contexto.
     */
    public TableContextMenuHelper<S> addSeparator() {
        contextMenu.getItems().add(new SeparatorMenuItem());
        return this;
    }

    private void buildMenuItems() {
        contextMenu.getItems().clear();

        // Ver Detalhes
        if (enableViewDetails && onViewDetails != null) {
            MenuItem viewItem = new MenuItem("Ver Detalhes");
            viewItem.setOnAction(e -> {
                S selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) onViewDetails.accept(selected);
            });
            contextMenu.getItems().add(viewItem);
        }

        // Editar
        if (enableEdit && onEdit != null) {
            MenuItem editItem = new MenuItem("Editar " + entityName);
            editItem.setOnAction(e -> {
                S selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) onEdit.accept(selected);
            });
            contextMenu.getItems().add(editItem);
        }

        // Copiar
        if (enableCopy) {
            MenuItem copyItem = new MenuItem("Copiar");
            copyItem.setOnAction(e -> copySelectionToClipboard());
            contextMenu.getItems().add(copyItem);
        }

        // Separador
        if ((enableViewDetails || enableEdit || enableCopy) && (enableDelete || enableExport || enableRefresh)) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        // Excluir
        if (enableDelete && onDelete != null) {
            MenuItem deleteItem = new MenuItem("Excluir " + entityName);
            deleteItem.setOnAction(e -> {
                S selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) confirmAndDelete(selected);
            });
            contextMenu.getItems().add(deleteItem);
        }

        // Exportar
        if (enableExport) {
            Menu exportMenu = new Menu("Exportar");

            MenuItem exportCsvItem = new MenuItem("Exportar para CSV...");
            exportCsvItem.setOnAction(e -> exportToCSV());

            MenuItem exportExcelItem = new MenuItem("Exportar para Excel...");
            exportExcelItem.setOnAction(e -> exportToExcel());

            exportMenu.getItems().addAll(exportCsvItem, exportExcelItem);
            contextMenu.getItems().add(exportMenu);
        }

        // Atualizar
        if (enableRefresh && onRefresh != null) {
            if (!contextMenu.getItems().isEmpty()) {
                contextMenu.getItems().add(new SeparatorMenuItem());
            }
            MenuItem refreshItem = new MenuItem("Atualizar");
            refreshItem.setOnAction(e -> onRefresh.run());
            contextMenu.getItems().add(refreshItem);
        }
    }

    private void setupDefaultRowFactory() {
        tableView.setRowFactory(tv -> {
            TableRow<S> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void confirmAndDelete(S item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Exclusão");
        confirm.setHeaderText("Excluir " + entityName);
        confirm.setContentText("Tem certeza que deseja excluir este " + entityName.toLowerCase() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onDelete.accept(item);
        }
    }

    private void copySelectionToClipboard() {
        S selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        StringBuilder sb = new StringBuilder();
        for (TableColumn<S, ?> col : tableView.getColumns()) {
            Object cellData = col.getCellData(selected);
            sb.append(cellData != null ? cellData.toString() : "").append("\t");
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString().trim());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar para CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(entityName.toLowerCase() + "_export.csv");
        File file = fileChooser.showSaveDialog(tableView.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Header
                StringBuilder header = new StringBuilder();
                List<TableColumn<S, ?>> columns = tableView.getColumns();
                for (int i = 0; i < columns.size(); i++) {
                    header.append(escapeCsvField(columns.get(i).getText()));
                    if (i < columns.size() - 1) header.append(";");
                }
                writer.println(header.toString());

                // Data
                ObservableList<S> itemsToExport = tableView.getItems();
                for (S item : itemsToExport) {
                    StringBuilder row = new StringBuilder();
                    for (int i = 0; i < columns.size(); i++) {
                        Object cellData = columns.get(i).getCellData(item);
                        String value = exportFormatter != null && cellData != null
                                ? exportFormatter.apply(item)
                                : (cellData != null ? cellData.toString() : "");
                        row.append(escapeCsvField(value));
                        if (i < columns.size() - 1) row.append(";");
                    }
                    writer.println(row.toString());
                }

                showInfo("Exportação concluída", "Dados exportados para:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Erro na exportação", ex.getMessage());
            }
        }
    }

    private void exportToExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar para Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName(entityName.toLowerCase() + "_export.xlsx");
        File file = fc.showSaveDialog(tableView.getScene().getWindow());

        if (file == null) return;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(entityName);

            // Estilo cabeçalho
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Cabeçalho
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            List<TableColumn<S, ?>> cols = tableView.getColumns();
            for (int i = 0; i < cols.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols.get(i).getText());
                cell.setCellStyle(headerStyle);
            }

            // Dados
            ObservableList<S> data = tableView.getItems();
            for (int r = 0; r < data.size(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
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

            showInfo("Exportação concluída", "Dados exportados para:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Erro na exportação", ex.getMessage());
        }
    }

    private String escapeCsvField(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Cria um helper pré-configurado para tabelas de gestão CRUD padrão.
     */
    public static <S> TableContextMenuHelper<S> createCrudMenu(TableView<S> table,
                                                                String entityName,
                                                                Consumer<S> onEdit,
                                                                Consumer<S> onDelete,
                                                                Runnable onRefresh) {
        return new TableContextMenuHelper<S>(table)
                .withEntityName(entityName)
                .onEdit(onEdit)
                .onDelete(onDelete)
                .onRefresh(onRefresh)
                .enableEdit(true)
                .enableDelete(true)
                .enableExport(true)
                .enableCopy(true)
                .enableRefresh(true);
    }
}
