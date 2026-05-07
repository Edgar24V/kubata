package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.MovimentoBancario;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimentoBancario;
import ao.allon.kubata.faturacao.service.ContaBancariaService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransferenciasView extends BorderPane {

    private final ContaBancariaService contaService;
    private final ModalService modalService;

    private TableView<MovimentoBancario> table;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TransferenciasView(ContaBancariaService contaService, ModalService modalService) {
        this.contaService = contaService;
        this.modalService = modalService;
        
        getStyleClass().add("transferencias-view");
        setPadding(new Insets(20));
        
        setTop(buildHeader());
        setCenter(buildTable());
        
        refreshData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(0, 0, 15, 0));

        HBox top = new HBox(15);
        top.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Transferências Bancárias");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Histórico de transferências entre contas");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNova = new Button("Nova Transferência", new FontIcon(Feather.REPEAT));
        btnNova.getStyleClass().add(Styles.ACCENT);
        btnNova.setOnAction(e -> showTransferenciaDialog());

        top.getChildren().addAll(titleBox, spacer, btnNova);
        header.getChildren().add(top);
        return header;
    }

    private TableView<MovimentoBancario> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MovimentoBancario, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataMovimento().format(DATE_FMT)));
        
        TableColumn<MovimentoBancario, String> colConta = new TableColumn<>("Conta");
        colConta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConta().getDescricao()));
        
        TableColumn<MovimentoBancario, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().getDescricao()));
        
        TableColumn<MovimentoBancario, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));

        TableColumn<MovimentoBancario, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(String.format("Kz %.2f", c.getValue().getValor())));
        colValor.setStyle("-fx-alignment: CENTER-RIGHT;");
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    MovimentoBancario m = getTableView().getItems().get(getIndex());
                    if (m.getTipo() == TipoMovimentoBancario.CREDITO) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: -color-danger-fg; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        table.getColumns().addAll(colData, colConta, colTipo, colDesc, colValor);
        return table;
    }

    private void refreshData() {
        // Obter todas as contas e seus movimentos categorizados como "Transferência"
        // Como não temos um método específico para buscar todos os movimentos de todas as contas filtrados,
        // vamos iterar (para MVP é ok, para prod idealmente teria filtro no repositório)
        List<MovimentoBancario> allMovs = contaService.findAll().stream()
                .flatMap(c -> contaService.getUltimosMovimentos(c.getId()).stream())
                .filter(m -> "Transferência".equals(m.getCategoria()))
                .sorted((m1, m2) -> m2.getDataMovimento().compareTo(m1.getDataMovimento()))
                .collect(Collectors.toList());
        
        table.setItems(FXCollections.observableArrayList(allMovs));
    }

    private void showTransferenciaDialog() {
        VBox form = new VBox(15);
        
        ComboBox<ContaBancaria> cbOrigem = new ComboBox<>();
        List<ContaBancaria> contas = contaService.findAll();
        cbOrigem.setItems(FXCollections.observableArrayList(contas));
        cbOrigem.setPromptText("Conta Origem");
        cbOrigem.setMaxWidth(Double.MAX_VALUE);
        
        ComboBox<ContaBancaria> cbDestino = new ComboBox<>();
        cbDestino.setPromptText("Conta Destino");
        cbDestino.setMaxWidth(Double.MAX_VALUE);
        
        // Atualizar destino quando origem muda para evitar seleção da mesma conta
        cbOrigem.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                List<ContaBancaria> destinos = contas.stream()
                        .filter(c -> !c.getId().equals(newV.getId()))
                        .collect(Collectors.toList());
                cbDestino.setItems(FXCollections.observableArrayList(destinos));
            }
        });
        
        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Motivo da transferência");

        form.getChildren().addAll(
            new Label("Conta Origem"), cbOrigem,
            new Label("Conta Destino"), cbDestino,
            new Label("Valor (Kz)"), txtValor,
            new Label("Descrição"), txtDesc
        );

        modalService.create()
            .title("Nova Transferência")
            .content(form)
            .width(400)
            .withConfirmButton("Transferir", () -> {
                try {
                    ContaBancaria origem = cbOrigem.getValue();
                    ContaBancaria destino = cbDestino.getValue();
                    
                    if (origem == null) throw new IllegalArgumentException("Selecione a conta de origem");
                    if (destino == null) throw new IllegalArgumentException("Selecione a conta de destino");
                    if (txtValor.getText().isEmpty()) throw new IllegalArgumentException("Informe o valor");
                    
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    if (valor.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("O valor deve ser positivo");
                    
                    String desc = txtDesc.getText();
                    
                    contaService.transferir(origem.getId(), destino.getId(), valor, desc);
                    
                    refreshData();
                    AlertUtils.showInfoAlert("Sucesso", "Transferência realizada com sucesso.");
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Falha na transferência", ex);
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
}
