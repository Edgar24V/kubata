# AdvancedTableView — Guia de Melhorias e Novos Recursos

> **Projeto:** Kubata Core · `ao.allon.kubata.core.ui.table`  
> **Stack:** JavaFX 21 · AtlantaFX · Spring Boot 3.2 · Java 21  
> **Versão do documento:** 1.0.0 · Março 2026

---

## Índice

1. [Diagnóstico do Estado Atual](#1-diagnóstico-do-estado-atual)
2. [Bugs Críticos e Correções Imediatas](#2-bugs-críticos-e-correções-imediatas)
3. [Melhorias de Arquitetura](#3-melhorias-de-arquitetura)
4. [Novos Recursos — Células Editáveis](#4-novos-recursos--células-editáveis)
5. [Novos Recursos — AdvancedTableView](#5-novos-recursos--advancedtableview)
6. [Novos Recursos — TableUtils](#6-novos-recursos--tableutils)
7. [Sistema de Estilos e CSS](#7-sistema-de-estilos-e-css)
8. [Performance e Virtualização](#8-performance-e-virtualização)
9. [Acessibilidade e UX](#9-acessibilidade-e-ux)
10. [Roadmap de Implementação](#10-roadmap-de-implementação)

---

## 1. Diagnóstico do Estado Atual

### 1.1 Visão Geral da Hierarquia Existente

```
EditableTableCell<S, T>          ← Classe base abstrata
├── TextTableCell<S, T>          ← TextField
├── CheckBoxTableCell<S>         ← CheckBox  ⚠️ Bug corrigido
├── ComboBoxTableCell<S, T>      ← ComboBox
├── DatePickerTableCell<S>       ← DatePicker
└── SpinnerTableCell<S, T>       ← Spinner

AdvancedTableView<S>             ← TableView estendida
TableUtils                       ← Factory estática
```

### 1.2 Pontos Fortes

- Estrutura de herança sólida com `EditableTableCell` como base genérica
- Sistema de validação integrado via `Predicate<T>` com feedback visual (`cell-error`)
- Filtragem reativa com `FilteredList` + `SortedList` já encadeados
- Exportação CSV e cópia para clipboard nativos
- Menu de contexto extensível por default

### 1.3 Lacunas Identificadas

| Categoria | Problema | Impacto |
|-----------|----------|---------|
| **Bug** | `CheckBoxTableCell` usa `DefaultStringConverter` para `Boolean` | `ClassCastException` em runtime |
| **Bug** | `deleteItem` em `ContextMenu` chama `getItems().remove()` em `SortedList` | `UnsupportedOperationException` |
| **UX** | Sem suporte a seleção múltipla no menu de contexto | Limitação severa em tabelas de dados |
| **UX** | Sem paginação — listas longas penalizam performance | Degrada com >5000 linhas |
| **Segurança** | Exportação CSV não escapa campos com vírgulas ou quebras de linha | CSV inválido / injeção de fórmula |
| **Arquitetura** | `TableUtils` usa apenas `PropertyValueFactory` (reflection) | Sem type safety; não funciona com módulos Java |
| **Arquitetura** | Nenhuma API de callbacks/eventos para edições | Dificulta integração com Spring |
| **Acessibilidade** | Sem suporte a `tooltip`, ARIA ou navegação por teclado avançada | Não cumpre padrões de acessibilidade |

---

## 2. Bugs Críticos e Correções Imediatas

### 2.1 `CheckBoxTableCell` — `ClassCastException` ✅ Corrigido

**Causa:** Cast ilegal de `DefaultStringConverter` (que opera em `String`) para `StringConverter<Boolean>`.

```java
// ❌ ANTES — compila, mas explode em runtime
super((javafx.util.StringConverter<Boolean>) (Object) new DefaultStringConverter());

// ✅ DEPOIS — converter tipado corretamente
super(new StringConverter<Boolean>() {
    @Override public String toString(Boolean v) {
        return v == null ? "" : v ? "Sim" : "Não";
    }
    @Override public Boolean fromString(String s) {
        return "true".equalsIgnoreCase(s) || "sim".equalsIgnoreCase(s);
    }
});
```

### 2.2 `ContextMenu` — Exclusão em `SortedList`

O item "Eliminar" tenta remover da lista ordenada, que é imutável.

```java
// ❌ ANTES — lança UnsupportedOperationException
deleteItem.setOnAction(e -> {
    S selected = getSelectionModel().getSelectedItem();
    if (selected != null) getItems().remove(selected); // SortedList é read-only!
});

// ✅ DEPOIS — remover da fonte original
deleteItem.setOnAction(e -> {
    S selected = getSelectionModel().getSelectedItem();
    if (selected != null && filteredData != null) {
        filteredData.getSource().remove(selected);
    }
});
```

### 2.3 Exportação CSV — Escaping Correto

```java
// ❌ ANTES — campos com vírgula ou aspas quebram o arquivo
row.append(cellData.toString()).append(",");

// ✅ DEPOIS — RFC 4180 compliant
private String escapeCsvField(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

---

## 3. Melhorias de Arquitetura

### 3.1 `EditableTableCell` — Refatoração da Base

#### 3.1.1 Adicionar suporte a tooltip de erro

```java
public abstract class EditableTableCell<S, T> extends TableCell<S, T> {

    protected final StringConverter<T> converter;
    protected Predicate<T> validator;
    protected String errorMessage = "Valor inválido";
    private Tooltip errorTooltip;           // NOVO

    protected void commitValue(T newValue) {
        if (validator == null || validator.test(newValue)) {
            commitEdit(newValue);
            getStyleClass().remove("cell-error");
            Tooltip.uninstall(this, errorTooltip); // NOVO
        } else {
            getStyleClass().add("cell-error");
            // NOVO — mostrar tooltip com mensagem descritiva
            errorTooltip = new Tooltip(errorMessage);
            errorTooltip.getStyleClass().add("error-tooltip");
            Tooltip.install(this, errorTooltip);
        }
    }
}
```

#### 3.1.2 Callback de edição confirmada

```java
// NOVO — permite que controllers Spring reajam a edições
private Consumer<T> onCommitCallback;

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
```

### 3.2 Substituir `PropertyValueFactory` por Lambda

`PropertyValueFactory` usa reflection e é incompatível com **Java Modules** (JPMS). A alternativa moderna usa lambdas type-safe:

```java
// ❌ ANTES — reflection, sem type safety, falha em módulos
column.setCellValueFactory(new PropertyValueFactory<>("nome"));

// ✅ DEPOIS — lambda type-safe, compatível com JPMS
column.setCellValueFactory(data -> data.getValue().nomeProperty());

// Em TableUtils — API fluente type-safe
public static <S> TableColumn<S, String> createTextColumn(
        String title,
        Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> valueFactory) {

    TableColumn<S, String> column = new TableColumn<>(title);
    column.setCellValueFactory(valueFactory);
    column.setCellFactory(tc -> TextTableCell.create());
    column.setEditable(true);
    return column;
}

// Uso — totalmente type-safe
TableColumn<Aluno, String> col = TableUtils.createTextColumn(
    "Nome",
    data -> data.getValue().nomeProperty()
);
```

### 3.3 Padrão Builder para `AdvancedTableView`

```java
// API fluente para configuração declarativa
AdvancedTableView<Aluno> tabela = AdvancedTableView.<Aluno>builder()
    .data(alunoService.findAll())
    .placeholder("Nenhum aluno encontrado")
    .selectionMode(SelectionMode.MULTIPLE)
    .onDelete(this::eliminarAlunos)
    .onExport(ExportFormat.CSV, ExportFormat.EXCEL)
    .searchable(true)
    .paginated(50)
    .build();
```

---

## 4. Novos Recursos — Células Editáveis

### 4.1 `CurrencyTableCell` — Kwanza Angolano (AOA)

Célula especializada para valores monetários no formato angolano com máscara de entrada.

```java
package ao.allon.kubata.core.ui.table;

import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Célula editável para valores monetários no formato angolano.
 * Exibe: 1.250.000,00 Kz
 */
public class CurrencyTableCell<S> extends EditableTableCell<S, Double> {

    private static final Locale LOCALE_AO = new Locale("pt", "AO");
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(LOCALE_AO);
    private TextField textField;

    static {
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    public CurrencyTableCell() {
        super(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : FORMAT.format(value) + " Kz";
            }
            @Override
            public Double fromString(String s) {
                try {
                    String clean = s.replace(" Kz", "").replace(".", "").replace(",", ".");
                    return Double.parseDouble(clean);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });
        getStyleClass().add("currency-cell");
        setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    @Override
    protected void createEditor() {
        textField = new TextField(converter.toString(getItem()));
        textField.getStyleClass().addAll("cell-editor", "currency-editor");
        textField.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        textField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                commitValue(converter.fromString(textField.getText()));
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        textField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && isEditing()) commitValue(converter.fromString(textField.getText()));
        });

        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }
}
```

### 4.2 `MaskedTextTableCell` — Campo com Máscara

Para NIF, BI, telefone, etc.:

```java
/**
 * Célula com máscara de entrada configurável.
 * Exemplos de máscara:
 *   "###.######.#"  → NIF angolano
 *   "### ### ###"   → Telefone AO
 *   "##########LA"  → Bilhete de Identidade
 */
public class MaskedTextTableCell<S> extends EditableTableCell<S, String> {

    private final String mask;          // Ex: "###.######.#"
    private MaskField maskField;        // Componente com ControlsFX ou implementação própria

    public MaskedTextTableCell(String mask) {
        super(new DefaultStringConverter());
        this.mask = mask;
    }

    @Override
    protected void createEditor() {
        maskField = new MaskField(mask);
        maskField.setText(getItem());
        maskField.getStyleClass().add("cell-editor");

        maskField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && isEditing()) commitValue(maskField.getPlainText());
        });

        setGraphic(maskField);
        maskField.requestFocus();
    }
}
```

### 4.3 `RatingTableCell` — Avaliação por Estrelas

```java
/**
 * Célula de avaliação visual com estrelas (1–5).
 * Ideal para: avaliações de desempenho, notas, etc.
 */
public class RatingTableCell<S> extends EditableTableCell<S, Integer> {

    private HBox starsBox;
    private static final int MAX_STARS = 5;

    public RatingTableCell() {
        super(new StringConverter<Integer>() {
            @Override public String toString(Integer v) { return v == null ? "" : "★".repeat(v); }
            @Override public Integer fromString(String s) { return (int) s.chars().filter(c -> c == '★').count(); }
        });
        getStyleClass().add("rating-cell");
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) { setGraphic(null); return; }

        starsBox = new HBox(2);
        starsBox.setAlignment(Pos.CENTER);
        for (int i = 1; i <= MAX_STARS; i++) {
            Label star = new Label(i <= item ? "★" : "☆");
            star.getStyleClass().add(i <= item ? "star-filled" : "star-empty");
            final int rating = i;
            star.setOnMouseClicked(e -> commitValue(rating));
            star.setOnMouseEntered(e -> highlightStars(rating));
            starsBox.getChildren().add(star);
        }
        setGraphic(starsBox);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void highlightStars(int upTo) {
        for (int i = 0; i < MAX_STARS; i++) {
            Label star = (Label) starsBox.getChildren().get(i);
            star.getStyleClass().setAll(i < upTo ? "star-filled" : "star-empty");
        }
    }

    @Override protected void createEditor() { /* edição inline via clique nas estrelas */ }
}
```

### 4.4 `AutoCompleteTableCell` — Com Sugestões

```java
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
            List<T> all = suggestionsSupplier.get();
            List<T> filtered = all.stream()
                .filter(i -> itemConverter.toString(i).toLowerCase().contains(text.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());
            suggestionList.setItems(FXCollections.observableArrayList(filtered));
            if (!filtered.isEmpty()) showPopup();
            else popup.hide();
        });

        suggestionList.setOnMouseClicked(e -> {
            T selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                commitValue(selected);
                popup.hide();
            }
        });

        setGraphic(searchField);
        searchField.requestFocus();
    }

    private void showPopup() {
        // Posicionar popup abaixo do campo de texto
        Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
        popup.show(searchField, bounds.getMinX(), bounds.getMaxY());
    }
}
```

---

## 5. Novos Recursos — `AdvancedTableView`

### 5.1 Barra de Pesquisa Integrada

```java
/**
 * Adiciona uma barra de pesquisa acima da tabela.
 * Requer que as colunas implementem um extrator de texto.
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

    HBox toolbar = new HBox(8, new Label("Pesquisar:"), searchField);
    toolbar.setPadding(new Insets(6, 8, 6, 8));
    toolbar.getStyleClass().add("table-toolbar");

    return new VBox(toolbar, this);
}
```

### 5.2 Paginação Nativa

```java
/**
 * Divide grandes conjuntos de dados em páginas,
 * mantendo performance mesmo com dezenas de milhares de linhas.
 */
public class PaginatedAdvancedTableView<S> extends AdvancedTableView<S> {

    private int pageSize = 50;
    private int currentPage = 0;
    private ObservableList<S> fullData;

    // Barra de paginação
    private final Label pageLabel = new Label();
    private final Button prevBtn = new Button("‹");
    private final Button nextBtn = new Button("›");

    public HBox getPaginationBar() {
        prevBtn.setOnAction(e -> goToPage(currentPage - 1));
        nextBtn.setOnAction(e -> goToPage(currentPage + 1));

        Button firstBtn = new Button("«");
        Button lastBtn  = new Button("»");
        firstBtn.setOnAction(e -> goToPage(0));
        lastBtn.setOnAction(e -> goToPage(getPageCount() - 1));

        ComboBox<Integer> pageSizeBox = new ComboBox<>(
            FXCollections.observableArrayList(25, 50, 100, 200));
        pageSizeBox.setValue(pageSize);
        pageSizeBox.setOnAction(e -> {
            pageSize = pageSizeBox.getValue();
            goToPage(0);
        });

        HBox bar = new HBox(6, firstBtn, prevBtn, pageLabel, nextBtn, lastBtn,
                            new Separator(Orientation.VERTICAL),
                            new Label("Linhas/pág:"), pageSizeBox);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("pagination-bar");
        return bar;
    }

    private void goToPage(int page) {
        int pages = getPageCount();
        currentPage = Math.max(0, Math.min(page, pages - 1));
        int from = currentPage * pageSize;
        int to   = Math.min(from + pageSize, fullData.size());
        setData(FXCollections.observableArrayList(fullData.subList(from, to)));
        pageLabel.setText(String.format("Pág. %d de %d  (%d registos)",
            currentPage + 1, pages, fullData.size()));
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= pages - 1);
    }

    private int getPageCount() {
        return (int) Math.ceil((double) fullData.size() / pageSize);
    }
}
```

### 5.3 Seleção Múltipla e Operações em Lote

```java
// Coluna de seleção com checkbox no cabeçalho
private TableColumn<S, Boolean> createSelectionColumn() {
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

    col.setCellFactory(tc -> new CheckBoxTableCell<>() {
        @Override protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                checkBox.setSelected(selectedItems.getOrDefault(getTableRow().getItem(), false));
            }
        }
    });
    return col;
}

// Operações em lote no menu de contexto
private void addBatchMenuItems() {
    MenuItem deleteSelected = new MenuItem("Eliminar selecionados");
    deleteSelected.setOnAction(e -> {
        List<S> toDelete = getSelectedItems();
        if (toDelete.isEmpty()) return;
        // Confirmação
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            String.format("Eliminar %d registos?", toDelete.size()),
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) filteredData.getSource().removeAll(toDelete);
        });
    });

    MenuItem exportSelected = new MenuItem("Exportar selecionados para CSV");
    exportSelected.setOnAction(e -> exportToCSV(getSelectedItems()));

    contextMenu.getItems().addAll(new SeparatorMenuItem(), deleteSelected, exportSelected);
}
```

### 5.4 Exportação para Excel (XLSX)

```java
/**
 * Exporta dados da tabela para XLSX com formatação profissional.
 * Requer Apache POI no classpath.
 */
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
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)33, (byte)82, (byte)137}));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont headerFont = wb.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        List<TableColumn<S, ?>> cols = getColumns();
        for (int i = 0; i < cols.size(); i++) {
            Cell cell = headerRow.createCell(i);
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
                Cell cell = row.createCell(c);
                if (val instanceof Number n) cell.setCellValue(n.doubleValue());
                else if (val instanceof Boolean b) cell.setCellValue(b);
                else cell.setCellValue(val != null ? val.toString() : "");
            }
        }

        cols.forEach(col -> sheet.autoSizeColumn(cols.indexOf(col)));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
```

### 5.5 Ordenação Multi-Coluna com Indicador Visual

```java
// Ativar ordenação multi-coluna (Shift+Click no cabeçalho)
getSortOrder().addListener((ListChangeListener<TableColumn<S, ?>>) change -> {
    // Atualizar indicadores visuais: "Nome ↑  Turma ↓"
    StringBuilder sortInfo = new StringBuilder();
    for (TableColumn<S, ?> col : getSortOrder()) {
        sortInfo.append(col.getText())
                .append(col.getSortType() == TableColumn.SortType.ASCENDING ? " ↑ " : " ↓ ");
    }
    sortIndicatorLabel.setText(sortInfo.toString().trim());
});
```

### 5.6 Estado de Carregamento

```java
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

// Uso com Task do JavaFX
Task<List<Aluno>> task = new Task<>() {
    @Override protected List<Aluno> call() { return alunoRepository.findAll(); }
};

task.setOnRunning(e -> table.setLoading(true));
task.setOnSucceeded(e -> {
    table.setLoading(false);
    table.setData(FXCollections.observableList(task.getValue()));
});
new Thread(task).start();
```

---

## 6. Novos Recursos — `TableUtils`

### 6.1 API Fluente com Builder de Colunas

```java
// Antes — verboso e sem type safety
TableColumn<Aluno, String> col = TableUtils.createTextColumn("Nome", "nome");

// Depois — builder fluente e type-safe
TableColumn<Aluno, String> nomeCol = TableUtils.column("Nome", Aluno::nomeProperty)
    .asText()
    .width(200)
    .editable(true)
    .validator(s -> s != null && !s.isBlank(), "Nome é obrigatório")
    .onCommit(aluno -> alunoService.save(aluno))
    .build();

TableColumn<Aluno, Double> notaCol = TableUtils.column("Nota Final", Aluno::notaFinalProperty)
    .asCurrency()
    .width(120)
    .editable(false)
    .styleClass("nota-column")
    .build();

TableColumn<Aluno, LocalDate> nascCol = TableUtils.column("Nascimento", Aluno::dataNascimentoProperty)
    .asDate("dd/MM/yyyy")
    .width(130)
    .build();
```

### 6.2 Configurações Predefinidas por Contexto

```java
/**
 * Configurações padrão para diferentes contextos de uso no Kubata.
 */
public static class TableProfiles {

    /** Tabela de leitura apenas — sem edição, sem seleção múltipla */
    public static void readOnly(TableView<?> table) {
        table.setEditable(false);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().addAll("advanced-table", "read-only-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /** Tabela de edição — editável, seleção múltipla, ações em lote */
    public static void editable(TableView<?> table) {
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getStyleClass().addAll("advanced-table", "editable-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    /** Tabela de relatório — sem edição, com linhas alternadas, exportação */
    public static void report(TableView<?> table) {
        table.setEditable(false);
        table.getStyleClass().addAll("advanced-table", "report-table", "striped");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
}
```

---

## 7. Sistema de Estilos e CSS

### 7.1 Variáveis CSS Alinhadas com AtlantaFX

```css
/* kubata-table.css */

/* ============================================================
   VARIÁVEIS GLOBAIS — compatíveis com AtlantaFX themes
   ============================================================ */
.root {
    -kub-table-header-bg:       -color-accent-subtle;
    -kub-table-header-fg:       -color-accent-fg;
    -kub-table-row-hover:       -color-cell-hover;
    -kub-table-row-selected:    -color-accent-muted;
    -kub-table-row-alt:         -color-bg-subtle;
    -kub-table-border:          -color-border-default;
    -kub-table-error:           -color-danger-fg;
    -kub-table-error-bg:        -color-danger-subtle;
    -kub-cell-padding-v:        6px;
    -kub-cell-padding-h:        10px;
    -kub-table-radius:          6px;
}

/* ============================================================
   TABELA BASE
   ============================================================ */
.advanced-table {
    -fx-border-color: -kub-table-border;
    -fx-border-width: 1px;
    -fx-border-radius: -kub-table-radius;
    -fx-background-radius: -kub-table-radius;
}

.advanced-table .column-header {
    -fx-background-color: -kub-table-header-bg;
    -fx-text-fill: -kub-table-header-fg;
    -fx-font-weight: 600;
    -fx-font-size: 12px;
    -fx-padding: -kub-cell-padding-v -kub-cell-padding-h;
    -fx-border-color: transparent -kub-table-border transparent transparent;
}

.advanced-table .table-row-cell {
    -fx-cell-size: 36px;
    -fx-border-color: transparent transparent -kub-table-border transparent;
}

.advanced-table .table-row-cell:hover {
    -fx-background-color: -kub-table-row-hover;
}

.advanced-table .table-row-cell:selected {
    -fx-background-color: -kub-table-row-selected;
}

/* Linhas alternadas */
.report-table .table-row-cell:even {
    -fx-background-color: -kub-table-row-alt;
}

/* ============================================================
   CÉLULAS EDITÁVEIS
   ============================================================ */
.editable-table-cell {
    -fx-padding: -kub-cell-padding-v -kub-cell-padding-h;
    -fx-cursor: text;
}

.cell-editor {
    -fx-background-color: -color-bg-default;
    -fx-border-color: -color-accent-fg;
    -fx-border-width: 0 0 2px 0;
    -fx-background-radius: 3px 3px 0 0;
    -fx-padding: 3px 6px;
    -fx-font-size: 12px;
}

.cell-error {
    -fx-background-color: -kub-table-error-bg;
    -fx-border-color: -kub-table-error;
    -fx-border-width: 0 0 2px 0;
}

.error-tooltip {
    -fx-background-color: -color-danger-fg;
    -fx-text-fill: white;
    -fx-font-size: 11px;
    -fx-padding: 4px 8px;
    -fx-background-radius: 4px;
}

/* ============================================================
   CÉLULA MONETÁRIA (Kwanza)
   ============================================================ */
.currency-cell {
    -fx-alignment: CENTER-RIGHT;
    -fx-font-family: "JetBrains Mono", monospace;
    -fx-font-size: 12px;
}

.currency-editor {
    -fx-alignment: CENTER-RIGHT;
    -fx-font-family: "JetBrains Mono", monospace;
}

/* ============================================================
   CÉLULA DE AVALIAÇÃO (Estrelas)
   ============================================================ */
.star-filled {
    -fx-text-fill: #F5A623;
    -fx-font-size: 16px;
    -fx-cursor: hand;
}

.star-empty {
    -fx-text-fill: -color-border-default;
    -fx-font-size: 16px;
    -fx-cursor: hand;
}

/* ============================================================
   BARRA DE PESQUISA
   ============================================================ */
.table-toolbar {
    -fx-background-color: -color-bg-subtle;
    -fx-border-color: transparent transparent -kub-table-border transparent;
    -fx-padding: 6px 10px;
}

.table-search-field {
    -fx-background-radius: 20px;
    -fx-border-radius: 20px;
    -fx-padding: 4px 12px;
    -fx-prompt-text-fill: -color-fg-muted;
}

/* ============================================================
   PAGINAÇÃO
   ============================================================ */
.pagination-bar {
    -fx-background-color: -color-bg-subtle;
    -fx-border-color: -kub-table-border transparent transparent transparent;
    -fx-padding: 6px 10px;
}

.pagination-bar .button {
    -fx-background-radius: 4px;
    -fx-padding: 3px 10px;
    -fx-cursor: hand;
}

/* ============================================================
   ESTADO DE CARREGAMENTO
   ============================================================ */
.table-loading {
    -fx-background-color: -color-bg-overlay;
    -fx-opacity: 0.85;
}
```

---

## 8. Performance e Virtualização

### 8.1 Boas Práticas para Listas Grandes

| Cenário | Recomendação |
|---------|-------------|
| **< 500 linhas** | `FilteredList` + `SortedList` — configuração atual, sem mudanças |
| **500 – 5.000 linhas** | Adicionar paginação (página de 50–100 itens) |
| **5.000 – 50.000 linhas** | Paginação server-side via Spring Data `Pageable` |
| **> 50.000 linhas** | Carregar apenas o que está visível (virtual scrolling customizado ou `VirtualFlow`) |

### 8.2 Paginação Server-Side com Spring Data

```java
// Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Page<Aluno> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}

// Controller JavaFX
private int currentPage = 0;
private static final int PAGE_SIZE = 50;

private void loadPage(int page, String filter) {
    Task<Page<Aluno>> task = new Task<>() {
        @Override protected Page<Aluno> call() {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("nome"));
            return alunoRepository.findByNomeContainingIgnoreCase(filter, pageable);
        }
    };
    task.setOnRunning(e  -> table.setLoading(true));
    task.setOnSucceeded(e -> {
        Page<Aluno> p = task.getValue();
        table.setLoading(false);
        table.setData(FXCollections.observableList(p.getContent()));
        totalPages = p.getTotalPages();
        updatePaginationBar(page, p.getTotalElements());
    });
    new Thread(task).start();
}
```

### 8.3 Evitar Re-criação de Células

```java
// ❌ ERRADO — cria novo editor a cada updateItem
@Override
protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    createEditor(); // chamado desnecessariamente
}

// ✅ CORRETO — reutilizar editor existente
@Override
protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
        setText(null); setGraphic(null); return;
    }
    if (isEditing()) {
        // Apenas atualizar valor no editor existente
        if (editor != null) updateEditorValue(item);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    } else {
        setText(converter.toString(item));
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }
}
```

---

## 9. Acessibilidade e UX

### 9.1 Navegação por Teclado Completa

```java
// Teclas padrão a suportar em todas as células editáveis
textField.setOnKeyPressed(e -> {
    switch (e.getCode()) {
        case ENTER  -> commitValue(converter.fromString(textField.getText()));
        case ESCAPE -> cancelEdit();
        case TAB    -> {
            commitValue(converter.fromString(textField.getText()));
            // Mover para próxima coluna
            e.consume();
            moveToNextCell();
        }
        case F2     -> startEdit(); // Activar edição sem duplo clique
    }
});

private void moveToNextCell() {
    TableView<S> tv = getTableView();
    int col = tv.getColumns().indexOf(getTableColumn());
    int row = getIndex();
    if (col < tv.getColumns().size() - 1) {
        tv.edit(row, tv.getColumns().get(col + 1));
    } else if (row < tv.getItems().size() - 1) {
        tv.edit(row + 1, tv.getColumns().get(0));
    }
}
```

### 9.2 Propriedades de Acessibilidade (ARIA equivalente JavaFX)

```java
// Adicionar em EditableTableCell e subclasses
getAccessibleRole();  // já definido como CELL por padrão

// Personalizar para leitores de tela
setAccessibleText(converter.toString(getItem()));
setAccessibleRoleDescription("Célula editável de " + getTableColumn().getText());

// Após edição
setAccessibleText("Valor alterado para: " + converter.toString(getItem()));
```

### 9.3 Placeholder Rico para Tabelas Vazias

```java
public static Node createEmptyPlaceholder(String title, String subtitle, String iconCode) {
    Label icon  = new Label(iconCode);
    icon.getStyleClass().addAll("placeholder-icon", "ikonli-font-icon");

    Label lbl   = new Label(title);
    lbl.getStyleClass().add("placeholder-title");

    Label sub   = new Label(subtitle);
    sub.getStyleClass().add("placeholder-subtitle");

    VBox box    = new VBox(12, icon, lbl, sub);
    box.setAlignment(Pos.CENTER);
    box.getStyleClass().add("table-placeholder");
    return box;
}

// Uso
table.setPlaceholder(TableUtils.createEmptyPlaceholder(
    "Nenhum aluno encontrado",
    "Adicione alunos clicando no botão '+ Novo'",
    "\uf007"  // fa-user (Ikonli)
));
```

---

## 10. Roadmap de Implementação

### Fase 1 — Estabilização (Sprint Atual)

| Tarefa | Prioridade | Esforço |
|--------|-----------|---------|
| ✅ Corrigir `ClassCastException` no `CheckBoxTableCell` | **Crítico** | 30 min |
| 🔲 Corrigir remoção em `SortedList` no menu de contexto | **Alto** | 30 min |
| 🔲 Corrigir escaping CSV | **Alto** | 1h |
| 🔲 Migrar `PropertyValueFactory` para lambdas nos módulos de uso crítico | **Alto** | 2h |

### Fase 2 — Melhorias de UX (Próxima Sprint)

| Tarefa | Prioridade | Esforço |
|--------|-----------|---------|
| 🔲 Barra de pesquisa integrada | **Alto** | 3h |
| 🔲 `CurrencyTableCell` (AOA / Kz) | **Alto** | 2h |
| 🔲 Estado de carregamento (`setLoading`) | **Médio** | 2h |
| 🔲 Tooltip de erro em `EditableTableCell` | **Médio** | 1h |
| 🔲 Placeholder rico (ícone + mensagem) | **Médio** | 1h |
| 🔲 CSS consolidado `kubata-table.css` | **Alto** | 3h |

### Fase 3 — Novos Recursos (Backlog)

| Tarefa | Prioridade | Esforço |
|--------|-----------|---------|
| 🔲 Paginação client-side | **Alto** | 4h |
| 🔲 Exportação XLSX com Apache POI | **Médio** | 3h |
| 🔲 `MaskedTextTableCell` (NIF, BI, telefone) | **Médio** | 3h |
| 🔲 `AutoCompleteTableCell` | **Médio** | 4h |
| 🔲 `RatingTableCell` | **Baixo** | 2h |
| 🔲 Seleção múltipla com checkbox no cabeçalho | **Alto** | 4h |
| 🔲 Paginação server-side com Spring Data | **Alto** | 5h |
| 🔲 Builder de colunas fluente em `TableUtils` | **Médio** | 4h |
| 🔲 Suporte a navegação Tab entre células | **Médio** | 2h |
| 🔲 Perfis de tabela (`TableProfiles`) | **Baixo** | 2h |

---

## Dependências Adicionais (pom.xml)

```xml
<!-- Exportação Excel — Apache POI -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Já existente — garantir versão compatível -->
<dependency>
    <groupId>org.controlsfx</groupId>
    <artifactId>controlsfx</artifactId>
    <version>11.2.1</version>
</dependency>

<!-- AtlantaFX — já existente -->
<dependency>
    <groupId>io.github.mkpaz</groupId>
    <artifactId>atlantafx-base</artifactId>
    <version>2.0.1</version>
</dependency>
```

---

*Documento gerado em Março 2026 · Kubata Core · ao.allon.kubata*
