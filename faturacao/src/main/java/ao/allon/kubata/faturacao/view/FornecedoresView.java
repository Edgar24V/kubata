package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.service.FornecedorService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

public class FornecedoresView extends BorderPane {

    private final FornecedorService fornecedorService;
    private final SessionManager sessionManager;
    private final ModalService modalService;
    private final ObservableList<Fornecedor> data = FXCollections.observableArrayList();
    private AdvancedTableView<Fornecedor> table;
    private CustomTextField searchField;

    // KPI Labels
    private Label lblTotalFornecedores;
    private Label lblAtivos;
    private Label lblAvaliacaoMedia;
    private Label lblTotalDivida;

    public FornecedoresView(FornecedorService fornecedorService, SessionManager sessionManager, ModalService modalService) {
        this.fornecedorService = fornecedorService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;
        getStyleClass().add("fornecedores-view");
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setFillWidth(true);

        // 1. Header
        content.getChildren().add(createHeader());

        // 2. KPI Cards
        content.getChildren().add(createKPISection());

        // 3. Filtros e Toolbar
        content.getChildren().add(createFiltersAndToolbar());

        // 4. Tabela
        content.getChildren().add(createTable());

        setCenter(content);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Fornecedores");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Cadastro e avaliação de fornecedores");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> exportarCSV());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnAtualizar.setOnAction(e -> refreshData());

        header.getChildren().addAll(titleBox, spacer, btnExportar, btnAtualizar);
        return header;
    }

    private FlowPane createKPISection() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setAlignment(Pos.TOP_LEFT);

        lblTotalFornecedores = new Label("0");
        lblAtivos = new Label("0");
        lblAvaliacaoMedia = new Label("0.0");
        lblTotalDivida = new Label("Kz 0,00");

        pane.getChildren().addAll(
            createKPICard("Total Fornecedores", lblTotalFornecedores, Feather.USERS, Styles.ACCENT),
            createKPICard("Fornecedores Ativos", lblAtivos, Feather.USER_CHECK, Styles.SUCCESS),
            createKPICard("Avaliação Média", lblAvaliacaoMedia, Feather.STAR, Styles.WARNING),
            createKPICard("Total em Dívida", lblTotalDivida, Feather.DOLLAR_SIGN, Styles.DANGER)
        );

        return pane;
    }

    private Card createKPICard(String titulo, Label valorLabel, Feather icono, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(180);
        card.setMaxWidth(220);

        VBox content = new VBox(8);
        content.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(icono);
        icon.setIconSize(24);
        icon.getStyleClass().add(colorStyle);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(Styles.TEXT_MUTED);
        lblTitulo.setWrapText(true);

        header.getChildren().addAll(icon, lblTitulo);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        content.getChildren().addAll(header, valorLabel);
        card.setBody(content);

        return card;
    }

    private VBox createFiltersAndToolbar() {
        // Filtros
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(0, 0, 10, 0));

        searchField = new CustomTextField();
        searchField.setPromptText("Pesquisar por nome, NIF, email...");
        searchField.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, ov, nv) -> {
            String q = nv == null ? "" : nv.toLowerCase();
            table.setFilter(f ->
                q.isEmpty() ||
                (f.getNome() != null && f.getNome().toLowerCase().contains(q)) ||
                (f.getNif() != null && f.getNif().toLowerCase().contains(q)) ||
                (f.getEmail() != null && f.getEmail().toLowerCase().contains(q))
            );
        });

        filters.getChildren().addAll(searchField);

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnNew = new Button("Novo Fornecedor", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNew.getStyleClass().addAll(Styles.SUCCESS);
        btnNew.setOnAction(e -> openForm(null));

        Button btnEdit = new Button("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEdit.setOnAction(e -> {
            Fornecedor f = table.getSelectionModel().getSelectedItem();
            if (f != null) openForm(f);
            else AlertUtils.showWarningAlert("Seleção", "Selecione um fornecedor.");
        });

        Button btnDelete = new Button("Excluir", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
        btnDelete.getStyleClass().addAll(Styles.DANGER);
        btnDelete.setOnAction(e -> {
            Fornecedor f = table.getSelectionModel().getSelectedItem();
            if (f != null) deleteFornecedor(f);
            else AlertUtils.showWarningAlert("Seleção", "Selecione um fornecedor.");
        });

        Button btnDetalhes = new Button("Detalhes", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
        btnDetalhes.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnDetalhes.setOnAction(e -> {
            Fornecedor f = table.getSelectionModel().getSelectedItem();
            if (f != null) mostrarDetalhes(f);
            else AlertUtils.showWarningAlert("Seleção", "Selecione um fornecedor.");
        });

        // Permissões
        boolean canEdit = sessionManager.hasAccess("FORNECEDORES", "Editar");
        btnNew.setDisable(!canEdit);
        btnEdit.setDisable(!canEdit);
        btnDelete.setDisable(!canEdit);

        toolbar.getChildren().addAll(btnNew, btnEdit, btnDelete, new Separator(Orientation.VERTICAL), btnDetalhes);

        return new VBox(5, filters, toolbar);
    }

    private AdvancedTableView<Fornecedor> createTable() {
        table = new AdvancedTableView<>();
        table.setData(data);
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(400);

        TableColumn<Fornecedor, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setMinWidth(200);

        TableColumn<Fornecedor, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colNif.setMinWidth(100);

        TableColumn<Fornecedor, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setMinWidth(180);

        TableColumn<Fornecedor, String> colTel = new TableColumn<>("Telefone");
        colTel.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colTel.setMinWidth(120);

        TableColumn<Fornecedor, String> colTermos = new TableColumn<>("Termos Pgto");
        colTermos.setCellValueFactory(new PropertyValueFactory<>("termosPagamento"));
        colTermos.setMinWidth(100);

        TableColumn<Fornecedor, Integer> colAval = new TableColumn<>("Avaliação");
        colAval.setCellValueFactory(new PropertyValueFactory<>("avaliacao"));
        colAval.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox stars = new HBox(2);
                    for (int i = 0; i < 5; i++) {
                        FontIcon star = new FontIcon(Feather.STAR);
                        star.setIconSize(12);
                        if (i < item) star.getStyleClass().add(Styles.WARNING);
                        else star.getStyleClass().add(Styles.TEXT_MUTED);
                        stars.getChildren().add(star);
                    }
                    setGraphic(stars);
                }
            }
        });
        colAval.setMinWidth(100);

        table.getColumns().addAll(colNome, colNif, colEmail, colTel, colTermos, colAval);

        // Double click para editar
        table.setRowFactory(tv -> {
            TableRow<Fornecedor> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    private void refreshData() {
        data.setAll(fornecedorService.findAll());
        table.setData(data);

        // Atualizar KPIs
        int total = data.size();
        int ativos = (int) data.stream().filter(f -> f.getAvaliacao() != null && f.getAvaliacao() >= 3).count();
        double avaliacaoMedia = data.stream()
            .filter(f -> f.getAvaliacao() != null)
            .mapToInt(Fornecedor::getAvaliacao)
            .average()
            .orElse(0.0);

        lblTotalFornecedores.setText(String.valueOf(total));
        lblAtivos.setText(String.valueOf(ativos));
        lblAvaliacaoMedia.setText(String.format("%.1f/5", avaliacaoMedia));
    }

    private void mostrarDetalhes(Fornecedor f) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        content.getChildren().addAll(
            new Label("Nome: " + f.getNome()),
            new Label("NIF: " + f.getNif()),
            new Label("Email: " + f.getEmail()),
            new Label("Telefone: " + f.getTelefone()),
            new Label("Endereço: " + f.getEndereco()),
            new Label("Termos: " + f.getTermosPagamento()),
            new Label("Avaliação: " + f.getAvaliacao() + "/5")
        );

        modalService.create()
            .title("Detalhes do Fornecedor")
            .content(content)
            .autoSize()
            .withConfirmButton("Fechar", () -> true)
            .buildAndShow();
    }

    private void exportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Fornecedores");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("fornecedores_" + java.time.LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Nome;NIF;Email;Telefone;Endereco;Termos;Avaliacao\n");
                for (Fornecedor f : table.getItems()) {
                    writer.write(String.format("%s;%s;%s;%s;%s;%s;%d\n",
                        f.getNome(), f.getNif(), f.getEmail(),
                        f.getTelefone(), f.getEndereco(),
                        f.getTermosPagamento(), f.getAvaliacao() != null ? f.getAvaliacao() : 0));
                }
                AlertUtils.showInfoAlert("Sucesso", "Exportado com sucesso!");
            } catch (IOException e) {
                AlertUtils.showExceptionAlert("Erro", "Não foi possível exportar.", e);
            }
        }
    }

    private void openForm(Fornecedor fornecedor) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtNome = new TextField(fornecedor != null ? fornecedor.getNome() : "");
        TextField txtNif = new TextField(fornecedor != null ? fornecedor.getNif() : "");
        TextField txtEmail = new TextField(fornecedor != null ? fornecedor.getEmail() : "");
        CustomTextField txtTelefone = new CustomTextField();
        txtTelefone.setPromptText("(999) 999 999 999");
        txtTelefone.setText(fornecedor != null ? fornecedor.getTelefone() : "");
        txtTelefone.setLeft(IconUtils.icon(Feather.PHONE, IconUtils.SIZE_SMALL));

        TextField txtEndereco = new TextField(fornecedor != null ? fornecedor.getEndereco() : "");
        TextField txtTermos = new TextField(fornecedor != null ? fornecedor.getTermosPagamento() : "");
        Spinner<Integer> spAval = new Spinner<>(1, 5, fornecedor != null && fornecedor.getAvaliacao() != null ? fornecedor.getAvaliacao() : 3);

        grid.addRow(0, new Label("Nome:"), txtNome);
        grid.addRow(1, new Label("NIF:"), txtNif);
        grid.addRow(2, new Label("Email:"), txtEmail);
        grid.addRow(3, new Label("Telefone:"), txtTelefone);
        grid.addRow(4, new Label("Endereço:"), txtEndereco);
        grid.addRow(5, new Label("Termos de Pagamento:"), txtTermos);
        grid.addRow(6, new Label("Avaliação (1-5):"), spAval);

        modalService.create()
            .title(fornecedor == null ? "Novo Fornecedor" : "Editar Fornecedor")
            .content(grid)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    Fornecedor f = fornecedor != null ? fornecedor : new Fornecedor();
                    f.setNome(txtNome.getText());
                    f.setNif(ao.allon.kubata.faturacao.util.AngolaValidationUtils.normalizeNif(txtNif.getText()));
                    f.setEmail(txtEmail.getText());
                    f.setTelefone(txtTelefone.getText());
                    f.setEndereco(txtEndereco.getText());
                    f.setTermosPagamento(txtTermos.getText());
                    f.setAvaliacao(spAval.getValue());
                    fornecedorService.save(f);
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void deleteFornecedor(Fornecedor fornecedor) {
        Label msg = new Label("Confirma excluir este fornecedor?");
        modalService.create()
            .title("Excluir " + fornecedor.getNome())
            .content(msg)
            .autoSize()
            .withConfirmButton("Excluir", () -> {
                try {
                    fornecedorService.delete(fornecedor.getId());
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}
