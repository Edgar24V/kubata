package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ExercicioFiscal.EstadoExercicio;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.ExercicioFiscalService;
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
public class ExerciciosFiscaisView extends VBox {

    private final ExercicioFiscalService exercicioService;
    private final ExercicioFiscalRepository exercicioRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;

    private AdvancedTableView<ExercicioFiscal> table;
    private ObservableList<ExercicioFiscal> exercicios;

    public ExerciciosFiscaisView(ExercicioFiscalService exercicioService, 
                                ExercicioFiscalRepository exercicioRepository,
                                EmpresaRepository empresaRepository,
                                AcessoService acessoService, 
                                SessionManager sessionManager, 
                                ModalManager modalManager,
                                PersistenceService persistenceService) {
        this.exercicioService = exercicioService;
        this.exercicioRepository = exercicioRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;

        exercicios = FXCollections.observableArrayList();
        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadExercicios();
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
        box.getStyleClass().add("toolbar");
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestão de Exercícios");
        title.getStyleClass().add("h3");

        Button btnNovo = new Button("Novo Exercício", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showNovoExercicioDialog());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> {
            loadExercicios();
            modalManager.alert("Atualização", "Lista de exercícios atualizada com sucesso.", "info", null);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, btnNovo, btnRefresh);
        return box;
    }

    private AdvancedTableView<ExercicioFiscal> buildTable() {
        AdvancedTableView<ExercicioFiscal> tv = new AdvancedTableView<>();
        tv.setData(exercicios);
        tv.setEditable(true);
        TableUtils.standardize(tv);

        TableColumn<ExercicioFiscal, Integer> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("ano"));
        colAno.setPrefWidth(100);

        TableColumn<ExercicioFiscal, LocalDate> colInicio = new TableColumn<>("Data Início");
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colInicio.setPrefWidth(150);

        TableColumn<ExercicioFiscal, LocalDate> colFim = new TableColumn<>("Data Fim");
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colFim.setPrefWidth(150);

        TableColumn<ExercicioFiscal, EstadoExercicio> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoExercicio item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item == EstadoExercicio.ABERTO) setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    else if (item == EstadoExercicio.FECHADO) setStyle("-fx-text-fill: red;");
                    else setStyle("-fx-text-fill: orange;");
                }
            }
        });
        colEstado.setPrefWidth(150);

        TableColumn<ExercicioFiscal, String> colObs = new TableColumn<>("Observações");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacoes"));
        colObs.setCellFactory(tc -> TextTableCell.create());
        colObs.setOnEditCommit(event -> {
            ExercicioFiscal ef = event.getRowValue();
            ef.setObservacoes(event.getNewValue());
            saveExercicioInline(ef);
        });
        colObs.setPrefWidth(300);

        tv.getColumns().addAll(colAno, colInicio, colFim, colEstado, colObs);
        return tv;
    }

    private void saveExercicioInline(ExercicioFiscal ef) {
        persistenceService.saveAsync(exercicioRepository, ef, "EXERCICIO", 
                "Atualização inline do exercício: " + ef.getAno(), null);
    }

    private void loadExercicios() {
        table.setLoading(true);
        persistenceService.executeAsync(() -> {
            try {
                java.util.List<ExercicioFiscal> result = new java.util.ArrayList<>();
                empresaRepository.findFirstByAtivaTrue().ifPresent(empresa ->
                    result.addAll(exercicioService.listarPorEmpresa(empresa.getId()))
                );
                Platform.runLater(() -> {
                    exercicios.setAll(result);
                    table.setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    table.setLoading(false);
                    modalManager.alert("Erro", "Erro ao carregar exercícios: " + e.getMessage(), "error", e);
                });
            }
        }, "READ", "EXERCICIO", "Carregamento de exercícios", null);
    }

    public void showNovoExercicioDialog() {
        if (empresaRepository.findFirstByAtivaTrue().isEmpty()) {
            modalManager.alert("Empresa Inativa", "Nenhuma empresa ativa selecionada. Por favor, ative uma empresa no menu 'Empresas' antes de gerir exercícios.", "warning", null);
            return;
        }

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(400);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtAno = new TextField(String.valueOf(LocalDate.now().getYear() + 1));
        
        ComboBox<ExercicioFiscal> cmbBase = new ComboBox<>(exercicios);
        cmbBase.setPromptText("Basear no exercício (opcional)");
        cmbBase.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Ano do Exercício:"), 0, 0);
        grid.add(txtAno, 1, 0);
        grid.add(new Label("Exercício Base:"), 0, 1);
        grid.add(cmbBase, 1, 1);

        modalManager.showConfirmModal(grid, "Criar Novo Exercício", () -> {
            try {
                int ano = Integer.parseInt(txtAno.getText().trim());
                Empresa empresa = empresaRepository.findFirstByAtivaTrue()
                        .orElseThrow(() -> new IllegalStateException("Nenhuma empresa ativa selecionada."));

                persistenceService.executeAsync(() -> {
                    try {
                        exercicioService.criarExercicio(empresa, ano);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, "CREATE", "EXERCICIO", "Criado novo exercício fiscal: " + ano, this::loadExercicios);
            } catch (Exception ex) {
                modalManager.alert("Erro", "Erro ao preparar criação de exercício: " + ex.getMessage(), "error", ex);
            }
        }, null);
    }
}
