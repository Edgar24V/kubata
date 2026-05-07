package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.domain.SerieDocumento.EstadoSerie;
import ao.allon.kubata.core.repository.SerieDocumentoRepository;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class SeriesView extends VBox {

    private final SerieDocumentoRepository serieRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final SerieDocumentoWizardView wizardView;
    private final PersistenceService persistenceService;

    private AdvancedTableView<SerieDocumento> table;
    private ObservableList<SerieDocumento> series;

    public SeriesView(SerieDocumentoRepository serieRepository, EmpresaRepository empresaRepository,
                      AcessoService acessoService, SessionManager sessionManager, ModalManager modalManager,
                      SerieDocumentoWizardView wizardView, PersistenceService persistenceService) {
        this.serieRepository = serieRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.wizardView = wizardView;
        this.persistenceService = persistenceService;

        series = FXCollections.observableArrayList();
        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // Lazy load data when view is first shown
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadSeries();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        HBox toolbar = buildToolbar();
        table = buildTable();

        getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(10);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 20, 10, 20));

        Label title = new Label("Séries Documentais");
        title.getStyleClass().add("h3");

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar série...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, old, val) -> filterSeries(val));

        Button btnNovo = new Button("Nova Série", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showSerieDialog(null));

        Button btnBatch = new Button("Criar por Lote", IconUtils.icon(Feather.LAYERS, IconUtils.SIZE_SMALL));
        btnBatch.getStyleClass().add("button-outlined");
        btnBatch.setOnAction(e -> wizardView.start());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> {
            loadSeries();
            modalManager.alert("Atualização", "Lista de séries atualizada com sucesso.", "info", null);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, searchField, btnBatch, btnNovo, btnRefresh);
        return box;
    }

    private AdvancedTableView<SerieDocumento> buildTable() {
        AdvancedTableView<SerieDocumento> tv = new AdvancedTableView<>();
        tv.setData(series);
        tv.setEditable(true);
        TableUtils.standardize(tv);

        TableColumn<SerieDocumento, String> colSerie = new TableColumn<>("Série");
        colSerie.setCellValueFactory(new PropertyValueFactory<>("serie"));
        colSerie.setCellFactory(tc -> TextTableCell.create());
        colSerie.setOnEditCommit(event -> {
            SerieDocumento s = event.getRowValue();
            s.setSerie(event.getNewValue().toUpperCase());
            saveSerieInline(s);
        });
        colSerie.setPrefWidth(120);

        TableColumn<SerieDocumento, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDesc.setCellFactory(tc -> TextTableCell.create());
        colDesc.setOnEditCommit(event -> {
            SerieDocumento s = event.getRowValue();
            s.setDescricao(event.getNewValue());
            saveSerieInline(s);
        });
        colDesc.setPrefWidth(250);

        TableColumn<SerieDocumento, String> colTipo = TableUtils.createTextColumn("Tipo", col -> new SimpleStringProperty(col.getValue().getTipoDocumento().getDescricao()));
        colTipo.setPrefWidth(180);

        TableColumn<SerieDocumento, Integer> colExercicio = new TableColumn<>("Exercício");
        colExercicio.setCellValueFactory(new PropertyValueFactory<>("exercicio"));
        colExercicio.setCellFactory(tc -> TextTableCell.createInteger());
        colExercicio.setOnEditCommit(event -> {
            SerieDocumento s = event.getRowValue();
            s.setExercicio(event.getNewValue());
            saveSerieInline(s);
        });
        colExercicio.setPrefWidth(100);

        TableColumn<SerieDocumento, Long> colUltimo = new TableColumn<>("Último Nº");
        colUltimo.setCellValueFactory(new PropertyValueFactory<>("ultimoNumero"));
        colUltimo.setCellFactory(tc -> TextTableCell.createLong());
        colUltimo.setOnEditCommit(event -> {
            SerieDocumento s = event.getRowValue();
            s.setUltimoNumero(event.getNewValue());
            saveSerieInline(s);
        });
        colUltimo.setPrefWidth(120);

        TableColumn<SerieDocumento, EstadoSerie> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(120);

        tv.getColumns().addAll(colSerie, colDesc, colTipo, colExercicio, colUltimo, colEstado);
        
        return tv;
    }

    private void saveSerieInline(SerieDocumento s) {
        persistenceService.saveAsync(serieRepository, s, "SERIE", 
                "Atualização inline da série: " + s.getSerie(), null);
    }

    private void filterSeries(String val) {
        if (val == null || val.isBlank()) {
            loadSeries();
            return;
        }
        String filter = val.toLowerCase().trim();
        List<SerieDocumento> filtered = series.stream()
                .filter(s -> s.getSerie().toLowerCase().contains(filter) || 
                           s.getDescricao().toLowerCase().contains(filter) ||
                           s.getTipoDocumento().getDescricao().toLowerCase().contains(filter))
                .toList();
        series.setAll(filtered);
    }

    public void loadSeries() {
        table.setLoading(true);
        persistenceService.executeAsync(() -> {
            try {
                List<SerieDocumento> all = serieRepository.findAll();
                Platform.runLater(() -> {
                    series.setAll(all);
                    table.setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    table.setLoading(false);
                    modalManager.alert("Erro", "Erro ao carregar séries: " + e.getMessage(), "error", e);
                });
            }
        }, "READ", "SERIE", "Carregamento de séries", null);
    }

    public void showSerieDialog(SerieDocumento s) {
        // Validação inicial de empresa ativa
        if (empresaRepository.findFirstByAtivaTrue().isEmpty()) {
            modalManager.alert("Empresa Inativa", "Nenhuma empresa ativa selecionada. Por favor, ative uma empresa no menu 'Empresas' antes de gerir séries.", "warning", null);
            return;
        }

        boolean isNew = (s == null);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField txtSerie = new TextField(s != null ? s.getSerie() : "");
        txtSerie.setPromptText("Ex: 2024");

        ComboBox<TipoDocumentoSAFT> cmbTipo = new ComboBox<>(FXCollections.observableArrayList(TipoDocumentoSAFT.values()));
        cmbTipo.setValue(s != null ? s.getTipoDocumento() : TipoDocumentoSAFT.FT);

        TextField txtExercicio = new TextField(s != null ? String.valueOf(s.getExercicio()) : String.valueOf(LocalDate.now().getYear()));

        CheckBox chkPredefinida = new CheckBox("Série Predefinida");
        chkPredefinida.setSelected(s != null && s.getPredefinida());

        grid.add(new Label("Série:*"), 0, 0);
        grid.add(txtSerie, 1, 0);
        grid.add(new Label("Tipo:*"), 0, 1);
        grid.add(cmbTipo, 1, 1);
        grid.add(new Label("Exercício:*"), 0, 2);
        grid.add(txtExercicio, 1, 2);
        grid.add(chkPredefinida, 1, 3);

        modalManager.showConfirmModal(grid, isNew ? "Nova Série" : "Editar Série", () -> {
            try {
                if (txtSerie.getText().isBlank()) {
                    modalManager.alert("Aviso", "A série é obrigatória.", "warning", null);
                    return;
                }

                Empresa empresa = empresaRepository.findFirstByAtivaTrue()
                        .orElseThrow(() -> new IllegalStateException("Nenhuma empresa ativa selecionada. Por favor, ative uma empresa antes de criar séries."));

                SerieDocumento serie = isNew ? new SerieDocumento() : s;
                serie.setSerie(txtSerie.getText().trim().toUpperCase());
                serie.setTipoDocumento(cmbTipo.getValue());
                serie.setExercicio(Integer.parseInt(txtExercicio.getText().trim()));
                serie.setPredefinida(chkPredefinida.isSelected());
                serie.setEmpresa(empresa);
                
                if (isNew) {
                    serie.setEstado(SerieDocumento.EstadoSerie.ACTIVA);
                    serie.setNumeroInicial(1L);
                    serie.setUltimoNumero(0L);
                    serie.setFormatoNumero("{PREFIXO} {SERIE}/{NUMERO}");
                    serie.setPrefixo(serie.getTipoDocumento().getPrefixo());
                    serie.setDescricao("Série " + serie.getSerie() + " - " + serie.getExercicio());
                    serie.setCriadoEm(java.time.LocalDateTime.now());
                }

                persistenceService.saveAsync(serieRepository, serie, "SERIE", 
                        (isNew ? "Criada" : "Editada") + " série: " + serie.getSerie(),
                        saved -> {
                            Platform.runLater(this::loadSeries);
                        });
            } catch (Exception ex) {
                ex.printStackTrace();
                modalManager.alert("Erro", "Erro ao preparar salvamento: " + ex.getMessage(), "error", ex);
            }
        }, null);
    }

    private void removeSerie() {
        SerieDocumento selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione uma série para remover.", "warning", null);
            return;
        }

        modalManager.showConfirmModal(new Label("Tem certeza que deseja remover a série: " + selected.getSerie() + "?"),
                "Remover Série", () -> {
            persistenceService.deleteAsync(serieRepository, selected, null, "SERIE", 
                    "Removida série: " + selected.getSerie(), this::loadSeries);
        }, null);
    }

    private void cloneSerie() {
        SerieDocumento selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione uma série para clonar.", "warning", null);
            return;
        }

        SerieDocumento clone = new SerieDocumento();
        clone.setSerie(selected.getSerie() + " (Cópia)");
        clone.setTipoDocumento(selected.getTipoDocumento());
        clone.setExercicio(selected.getExercicio());
        clone.setEmpresa(selected.getEmpresa());
        clone.setEstado(SerieDocumento.EstadoSerie.ACTIVA);
        clone.setNumeroInicial(selected.getNumeroInicial());
        clone.setUltimoNumero(0L);
        clone.setFormatoNumero(selected.getFormatoNumero());
        clone.setPrefixo(selected.getPrefixo());

        persistenceService.saveAsync(serieRepository, clone, "SERIE", 
                "Clonada série: " + selected.getSerie() + " para " + clone.getSerie(),
                saved -> loadSeries());
    }
}
