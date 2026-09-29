package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Moeda;
import ao.allon.kubata.core.repository.MoedaRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

@Component
public class MoedasCambiosView extends BorderPane {

    private final MoedaRepository repository;
    private final ObservableList<Moeda> moedas = FXCollections.observableArrayList();
    private final TableView<Moeda> table = new TableView<>(moedas);
    private final Label totalValue = new Label("0");
    private final Label activeValue = new Label("0");
    private final Label baseValue = new Label("—");

    public MoedasCambiosView(MoedaRepository repository) {
        this.repository = repository;
        getStyleClass().add("application-view");
        buildUi();
        load();
    }

    private void buildUi() {
        VBox header = new VBox(5);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.getStyleClass().add("header-box");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.DOLLAR_SIGN, 20));
        Label title = new Label("Moedas e Câmbios");
        title.getStyleClass().add("h3");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button novo = new Button("Nova moeda", IconUtils.icon(Feather.PLUS, 13));
        novo.getStyleClass().add("button-primary");
        novo.setOnAction(e -> openDialog(null));

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> load());

        line.getChildren().addAll(icon, title, spacer, refresh, novo);

        Label subtitle = new Label(
                "Administre moedas, moeda base, taxa de câmbio e casas decimais do ambiente."
        );
        subtitle.getStyleClass().add("text-muted");

        header.getChildren().addAll(line, subtitle);

        buildTable();

        VBox center = new VBox(10);
        center.setPadding(new Insets(16, 18, 18, 18));
        center.getChildren().addAll(buildSummary(), table);
        VBox.setVgrow(table, Priority.ALWAYS);

        setTop(header);
        setCenter(center);
    }

    private HBox buildSummary() {
        HBox row = new HBox(12);

        Label total = metric("MOEDAS", totalValue);
        Label active = metric("ACTIVAS", activeValue);
        Label base = metric("MOEDA BASE", baseValue);

        HBox.setHgrow(total, Priority.ALWAYS);
        HBox.setHgrow(active, Priority.ALWAYS);
        HBox.setHgrow(base, Priority.ALWAYS);

        row.getChildren().addAll(total, active, base);
        return row;
    }

    private Label metric(String title, Label value) {
        Label node = new Label();
        node.setMinHeight(58);
        node.setMaxWidth(Double.MAX_VALUE);
        node.setPadding(new Insets(12));
        VBox box = new VBox(2);
        Label caption = new Label(title);
        caption.setStyle("-fx-font-size:9px;-fx-font-weight:800;-fx-text-fill:#6e7781;");
        value.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:#24292f;");
        box.getChildren().addAll(caption, value);
        node.setGraphic(box);
        node.setStyle(
                "-fx-background-color:#ffffff;" +
                "-fx-border-color:#d0d7de;" +
                "-fx-border-radius:9px;" +
                "-fx-background-radius:9px;"
        );
        return node;
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhuma moeda configurada."));

        TableColumn<Moeda, String> codigo = new TableColumn<>("Código");
        codigo.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getCodigoISO())));

        TableColumn<Moeda, String> nome = new TableColumn<>("Moeda");
        nome.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNome())));

        TableColumn<Moeda, String> simbolo = new TableColumn<>("Símbolo");
        simbolo.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getSimbolo())));

        TableColumn<Moeda, String> taxa = new TableColumn<>("Taxa de câmbio");
        taxa.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTaxaCambio() == null ? "—" : c.getValue().getTaxaCambio().toPlainString()
        ));

        TableColumn<Moeda, String> data = new TableColumn<>("Data da taxa");
        data.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataTaxaCambio() == null ? "—" : c.getValue().getDataTaxaCambio().toString()
        ));

        TableColumn<Moeda, String> base = new TableColumn<>("Base");
        base.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getMoedaBase()) ? "Sim" : "—"
        ));

        TableColumn<Moeda, String> estado = new TableColumn<>("Estado");
        estado.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActiva()) ? "Activa" : "Inactiva"
        ));

        TableColumn<Moeda, Void> acao = new TableColumn<>("Acções");
        acao.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("", IconUtils.icon(Feather.EDIT_2, 12));
            private final Button del = new Button("", IconUtils.icon(Feather.TRASH_2, 12));
            private final HBox box = new HBox(5, edit, del);
            {
                edit.getStyleClass().add("button-outlined");
                del.getStyleClass().addAll("button-outlined", "danger");
                edit.setOnAction(e -> openDialog(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().setAll(codigo, nome, simbolo, taxa, data, base, estado, acao);
    }

    private void load() {
        Platform.runLater(() -> {
            moedas.setAll(repository.findAll().stream()
                    .sorted(Comparator.comparing(Moeda::getCodigoISO, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList());
            updateSummary();
        });
    }

    private void updateSummary() {
        totalValue.setText(Integer.toString(moedas.size()));
        activeValue.setText(Long.toString(moedas.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActiva()))
                .count()));
        baseValue.setText(moedas.stream()
                .filter(m -> Boolean.TRUE.equals(m.getMoedaBase()))
                .map(m -> safe(m.getCodigoISO()))
                .findFirst()
                .orElse("—"));
    }

    private void openDialog(Moeda current) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(current == null ? "Nova moeda" : "Editar moeda");
        dialog.setHeaderText(current == null
                ? "Adicionar moeda ao catálogo"
                : "Actualizar dados da moeda");

        TextField codigo = new TextField(current == null ? "" : safe(current.getCodigoISO()));
        codigo.setPromptText("AOA");
        codigo.setDisable(current != null);

        TextField nome = new TextField(current == null ? "" : safe(current.getNome()));
        TextField simbolo = new TextField(current == null ? "" : safe(current.getSimbolo()));
        TextField taxa = new TextField(current == null || current.getTaxaCambio() == null ? "" : current.getTaxaCambio().toPlainString());
        DatePicker data = new DatePicker(current == null ? LocalDate.now() : current.getDataTaxaCambio());
        Spinner<Integer> casas = new Spinner<>(0, 6, current == null || current.getCasasDecimais() == null ? 2 : current.getCasasDecimais());
        CheckBox base = new CheckBox("Moeda base");
        base.setSelected(current != null && Boolean.TRUE.equals(current.getMoedaBase()));
        CheckBox activa = new CheckBox("Activa");
        activa.setSelected(current == null || Boolean.TRUE.equals(current.getActiva()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(9);
        grid.add(new Label("Código ISO"), 0, 0); grid.add(codigo, 1, 0);
        grid.add(new Label("Nome"), 0, 1); grid.add(nome, 1, 1);
        grid.add(new Label("Símbolo"), 0, 2); grid.add(simbolo, 1, 2);
        grid.add(new Label("Taxa de câmbio"), 0, 3); grid.add(taxa, 1, 3);
        grid.add(new Label("Data da taxa"), 0, 4); grid.add(data, 1, 4);
        grid.add(new Label("Casas decimais"), 0, 5); grid.add(casas, 1, 5);
        grid.add(base, 1, 6);
        grid.add(activa, 1, 7);

        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(new ColumnConstraints(120), right);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;

            String iso = codigo.getText().trim().toUpperCase();
            String nomeValue = nome.getText().trim();
            if (iso.length() != 3 || nomeValue.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Informe um código ISO com 3 caracteres e o nome da moeda.").showAndWait();
                return null;
            }

            try {
                BigDecimal cambio = taxa.getText().isBlank() ? BigDecimal.ONE : new BigDecimal(taxa.getText().trim());
                Moeda moeda = current == null
                        ? Moeda.builder().codigoISO(iso).build()
                        : current;

                moeda.setNome(nomeValue);
                moeda.setSimbolo(simbolo.getText().trim());
                moeda.setTaxaCambio(cambio);
                moeda.setDataTaxaCambio(data.getValue());
                moeda.setCasasDecimais(casas.getValue());
                moeda.setMoedaBase(base.isSelected());
                moeda.setActiva(activa.isSelected());

                if (base.isSelected()) {
                    repository.findAll().stream()
                            .filter(m -> current == null || !m.getId().equals(current.getId()))
                            .forEach(m -> {
                                m.setMoedaBase(false);
                                repository.save(m);
                            });
                }

                repository.save(moeda);
                load();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "A taxa de câmbio deve ser numérica.").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Não foi possível guardar a moeda: " + ex.getMessage()).showAndWait();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void delete(Moeda moeda) {
        if (moeda == null || moeda.getId() == null) return;
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Remover a moeda " + moeda.getCodigoISO() + "?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirm.setHeaderText("Remover moeda");
        confirm.showAndWait().filter(ButtonType.OK::equals).ifPresent(ok -> {
            try {
                repository.delete(moeda);
                load();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Não foi possível remover a moeda: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
