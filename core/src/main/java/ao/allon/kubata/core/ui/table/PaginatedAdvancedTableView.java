package ao.allon.kubata.core.ui.table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Divide grandes conjuntos de dados em páginas,
 * mantendo performance mesmo com dezenas de milhares de linhas.
 */
public class PaginatedAdvancedTableView<S> extends AdvancedTableView<S> {

    private int pageSize = 50;
    private int currentPage = 0;
    private ObservableList<S> fullData = FXCollections.observableArrayList();

    // Barra de paginação
    private final Label pageLabel = new Label();
    private final Button prevBtn = new Button("‹");
    private final Button nextBtn = new Button("›");
    private final Button firstBtn = new Button("«");
    private final Button lastBtn  = new Button("»");
    private final ComboBox<Integer> pageSizeBox = new ComboBox<>(
        FXCollections.observableArrayList(25, 50, 100, 200));

    public PaginatedAdvancedTableView() {
        super();
        setupPaginationControls();
    }

    public PaginatedAdvancedTableView(ObservableList<S> items) {
        super();
        this.fullData = items;
        setupPaginationControls();
        goToPage(0);
    }

    @Override
    public void setData(ObservableList<S> items) {
        this.fullData = items;
        goToPage(0);
    }

    private void setupPaginationControls() {
        firstBtn.setOnAction(e -> goToPage(0));
        prevBtn.setOnAction(e -> goToPage(currentPage - 1));
        nextBtn.setOnAction(e -> goToPage(currentPage + 1));
        lastBtn.setOnAction(e -> goToPage(getPageCount() - 1));

        pageSizeBox.setValue(pageSize);
        pageSizeBox.setOnAction(e -> {
            pageSize = pageSizeBox.getValue();
            goToPage(0);
        });
    }

    public HBox getPaginationBar() {
        HBox bar = new HBox(6, firstBtn, prevBtn, pageLabel, nextBtn, lastBtn,
                            new Separator(Orientation.VERTICAL),
                            new Label("Linhas/pág:"), pageSizeBox);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("pagination-bar");
        return bar;
    }

    /**
     * Retorna um VBox com Barra de Pesquisa, Tabela e Paginação.
     */
    public VBox withFullControls() {
        VBox topAndTable = withSearchBar();
        topAndTable.getChildren().add(getPaginationBar());
        return topAndTable;
    }

    private void goToPage(int page) {
        if (fullData == null || fullData.isEmpty()) {
            super.setData(FXCollections.observableArrayList());
            pageLabel.setText("Pág. 0 de 0 (0 registos)");
            updateButtonStates(0, 0);
            return;
        }

        int pages = getPageCount();
        currentPage = Math.max(0, Math.min(page, pages - 1));
        
        int from = currentPage * pageSize;
        int to   = Math.min(from + pageSize, fullData.size());
        
        super.setData(FXCollections.observableArrayList(fullData.subList(from, to)));
        
        pageLabel.setText(String.format("Pág. %d de %d  (%d registos)",
            currentPage + 1, pages, fullData.size()));
        
        updateButtonStates(currentPage, pages);
    }

    private void updateButtonStates(int current, int total) {
        prevBtn.setDisable(current <= 0);
        firstBtn.setDisable(current <= 0);
        nextBtn.setDisable(current >= total - 1);
        lastBtn.setDisable(current >= total - 1);
    }

    private int getPageCount() {
        if (fullData == null || fullData.isEmpty()) return 0;
        return (int) Math.ceil((double) fullData.size() / pageSize);
    }
}
