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
        getStyleClass().addAll("application-view", "kubata-infra-page");
        buildUi();
        load();
    }

    private void buildUi() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.getStyleClass().add("kubata-infra-header");

        HBox line = new HBox(13);
        line.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-infra-title-icon");
        iconBox.getChildren().add(IconUtils.icon(Feather.DOLLAR_SIGN, 21));

        VBox titles = new VBox(2);
        Label title = new Label("Moedas e Câmbios");
        title.getStyleClass().add("kubata-infra-title");

        Label subtitle = new Label(
                "Administração central de moedas, moeda base, casas decimais e taxas de câmbio."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-infra-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> load());

        Button novo = new Button("Nova moeda", IconUtils.icon(Feather.PLUS, 13));
        novo.getStyleClass().add("button-primary");
        novo.setOnAction(e -> openDialog(null));

        line.getChildren().addAll(iconBox, titles, spacer, refresh, novo);

        Label context = new Label("CATÁLOGO FINANCEIRO · uma moeda pode ser definida como base");
        context.getStyleClass().add("kubata-infra-status");

        header.getChildren().addAll(line, context);

        buildTable();

        VBox center = new VBox(14);
        center.setPadding(new Insets(16, 20, 20, 20));
        center.setFillWidth(true);

        center.getChildren().add(buildSummary());

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox tableTitles = new VBox(2);
        Label tableTitle = new Label("Catálogo de moedas");
        tableTitle.getStyleClass().add("kubata-infra-section-title");
        Label tableSubtitle = new Label("Taxas e estado das moedas disponíveis para os módulos financeiros.");
        tableSubtitle.getStyleClass().add("kubata-infra-section-subtitle");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        tableHeader.getChildren().addAll(tableTitles, tableSpacer);

        VBox tableSection = new VBox(10, tableHeader, table);
        tableSection.getStyleClass().add("kubata-infra-section");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        center.getChildren().add(tableSection);

        setTop(header);
        setCenter(center);
    }

    private HBox buildSummary() {
        HBox row = new HBox(12);

        row.getChildren().addAll(
                metric("MOEDAS", totalValue, Feather.LAYERS),
                metric("ACTIVAS", activeValue, Feather.CHECK_CIRCLE),
                metric("MOEDA BASE", baseValue, Feather.FLAG)
        );

        for (Node node : row.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        return row;
    }

    private VBox metric(String title, Label value, Feather icon) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-infra-kpi-icon");
        iconBox.getChildren().add(IconUtils.icon(icon, 15));

        VBox text = new VBox(1);
        Label caption = new Label(title);
        caption.getStyleClass().add("kubata-infra-kpi-title");
        value.getStyleClass().add("kubata-infra-kpi-value");
        text.getChildren().addAll(caption, value);

        content.getChildren().addAll(iconBox, text);

        VBox card = new VBox(content);
        card.getStyleClass().add("kubata-infra-kpi");
        return card;
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("kubata-infra-table");
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
