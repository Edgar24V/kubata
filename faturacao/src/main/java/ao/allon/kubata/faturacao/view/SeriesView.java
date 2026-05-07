package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.SerieService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.Comparator;

public class SeriesView extends VBox {

    private final SerieService serieService;
    private final ModalService modalService;
    private TableView<Serie> table;

    public SeriesView(SerieService serieService, ModalService modalService) {
        this.serieService = serieService;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("series-view");

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Séries de Faturação");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Gerencie as séries de numeração para documentos fiscais.");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Button btnNew = new Button("Nova Série");
        btnNew.setGraphic(new FontIcon(Feather.PLUS));
        btnNew.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnNew.setOnAction(e -> showSerieDialog(null));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer, btnNew);

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Serie, String> colDesignacao = new TableColumn<>("Designação");
        colDesignacao.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDesignacao()));

        TableColumn<Serie, String> colTipo = new TableColumn<>("Tipo Documento");
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipoDocumento().getDescricao()));

        TableColumn<Serie, Integer> colAno = new TableColumn<>("Ano Fiscal");
        colAno.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAno()));

        TableColumn<Serie, Long> colUltimo = new TableColumn<>("Último Nº");
        colUltimo.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getUltimoNumero()));

        TableColumn<Serie, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isAtiva() ? "Ativa" : "Inativa"));
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    if ("Ativa".equals(item)) {
                        getStyleClass().add(Styles.SUCCESS);
                    } else {
                        getStyleClass().add(Styles.DANGER);
                    }
                }
            }
        });

        TableColumn<Serie, String> colPadrao = new TableColumn<>("Padrão");
        colPadrao.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isPadrao() ? "Sim" : "Não"));

        // Actions Column
        TableColumn<Serie, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("", new FontIcon(Feather.EDIT));
            private final Button btnToggle = new Button("", new FontIcon(Feather.POWER));
            
            {
                btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnEdit.setTooltip(new Tooltip("Editar Série"));
                btnEdit.setOnAction(e -> showSerieDialog(getTableView().getItems().get(getIndex())));
                
                btnToggle.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnToggle.setTooltip(new Tooltip("Ativar/Desativar"));
                btnToggle.setOnAction(e -> toggleSerie(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Serie s = getTableView().getItems().get(getIndex());
                    // Visual feedback for active state
                    if (s.isAtiva()) {
                        btnToggle.getStyleClass().remove(Styles.DANGER);
                        btnToggle.getStyleClass().add(Styles.SUCCESS);
                    } else {
                        btnToggle.getStyleClass().remove(Styles.SUCCESS);
                        btnToggle.getStyleClass().add(Styles.DANGER);
                    }
                    
                    HBox box = new HBox(5, btnEdit, btnToggle);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(colDesignacao, colTipo, colAno, colUltimo, colEstado, colPadrao, colActions);

        getChildren().addAll(header, table);

        loadData();
    }

    private void loadData() {
        var items = serieService.findAll();
        items.sort(Comparator.comparing(Serie::getAno).reversed()
                .thenComparing(Serie::getDesignacao));
        table.getItems().setAll(items);
    }
    
    private void toggleSerie(Serie serie) {
        // Confirmação para desativar se estiver ativa
        if (serie.isAtiva()) {
            modalService.create()
                .title("Desativar Série")
                .content(new Label("Deseja realmente desativar a série " + serie.getDesignacao() + "?\nNão será possível emitir documentos com ela."))
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    executeToggle(serie);
                    return true;
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
        } else {
            executeToggle(serie);
        }
    }
    
    private void executeToggle(Serie serie) {
        serie.setAtiva(!serie.isAtiva());
        try {
            serieService.save(serie);
            loadData();
            AlertUtils.showInfoAlert("Sucesso", "Estado da série alterado com sucesso.");
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erro", "Falha ao alterar estado: " + e.getMessage());
        }
    }

    private void showSerieDialog(Serie serie) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        TextField txtDesignacao = new TextField();
        txtDesignacao.setPromptText("Ex: 2024, A, B");
        
        ComboBox<TipoDocumento> cbTipo = new ComboBox<>();
        cbTipo.getItems().setAll(TipoDocumento.values());
        cbTipo.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtAno = new TextField();
        txtAno.setText(String.valueOf(LocalDate.now().getYear()));
        
        CheckBox chkPadrao = new CheckBox("Definir como série padrão");
        
        if (serie != null) {
            txtDesignacao.setText(serie.getDesignacao());
            cbTipo.setValue(serie.getTipoDocumento());
            txtAno.setText(String.valueOf(serie.getAno()));
            chkPadrao.setSelected(serie.isPadrao());
            
            // Não permitir alterar tipo e ano se já tiver uso
            if (serie.getUltimoNumero() > 0) {
                cbTipo.setDisable(true);
                txtAno.setDisable(true);
                Label warning = new Label("Esta série já possui documentos emitidos. Tipo e Ano não podem ser alterados.");
                warning.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.WARNING);
                warning.setWrapText(true);
                content.getChildren().add(0, warning);
            }
        } else {
            // Default values for new serie
            cbTipo.getSelectionModel().selectFirst();
        }

        content.getChildren().addAll(
            TileFactory.createFormField("Designação", "Nome da série (ex: 2024)", txtDesignacao),
            TileFactory.createFormField("Tipo de Documento", "Qual documento esta série numera", cbTipo),
            TileFactory.createFormField("Ano Fiscal", "Ano de referência", txtAno),
            chkPadrao
        );

        modalService.create()
            .title(serie == null ? "Nova Série" : "Editar Série")
            .content(content)
            .autoSize()
            .withConfirmButton("Salvar", () -> {
                  try {
                      if (txtDesignacao.getText().isEmpty()) throw new IllegalArgumentException("Designação é obrigatória");
                      if (cbTipo.getValue() == null) throw new IllegalArgumentException("Tipo de documento é obrigatório");
                      if (txtAno.getText().isEmpty()) throw new IllegalArgumentException("Ano é obrigatório");

                      int ano;
                      try {
                          ano = Integer.parseInt(txtAno.getText());
                      } catch (NumberFormatException e) {
                          throw new IllegalArgumentException("Ano deve ser um número válido");
                      }

                      Serie s = serie != null ? serie : new Serie();
                      s.setDesignacao(txtDesignacao.getText());
                      s.setTipoDocumento(cbTipo.getValue());
                      s.setAno(ano);
                      s.setPadrao(chkPadrao.isSelected());
                      s.setAtiva(true);

                      serieService.save(s);
                      loadData();
                      return true;
                  } catch (Exception e) {
                      AlertUtils.showErrorAlert("Erro", e.getMessage());
                      return false;
                  }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}
