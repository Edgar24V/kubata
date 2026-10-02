package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.domain.Moeda;
import ao.allon.kubata.core.repository.MoedaRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    private final ModalManager modalManager;
    private final ObservableList<Moeda> moedas = FXCollections.observableArrayList();
    private final FilteredList<Moeda> filteredMoedas = new FilteredList<>(moedas, m -> true);
    private final TableView<Moeda> table = new TableView<>(filteredMoedas);

    private final Label totalValue = new Label("0");
    private final Label activeValue = new Label("0");
    private final Label baseValue = new Label("—");
    private final Label ratesValue = new Label("0");
    private final Label pendingValue = new Label("0");

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> baseFilter;

    private Label detailName;
    private Label detailCode;
    private Label detailSymbol;
    private Label detailRate;
    private Label detailRateDate;
    private Label detailDecimals;
    private Label detailBase;
    private Label detailActive;
    private Label detailRateStatus;

    public MoedasCambiosView(MoedaRepository repository, ModalManager modalManager) {
        this.repository = repository;
        this.modalManager = modalManager;
        getStyleClass().add("kubata-server-page");
        buildUi();
        load();
    }

    private void buildUi() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 22, 16, 22));
        header.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(13);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-server-title-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.DOLLAR_SIGN, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Moedas e Câmbios");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Administração central de moedas, moeda base, casas decimais e taxas de câmbio."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");
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
        context.getStyleClass().add("kubata-server-status-bar");

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
        tableTitle.getStyleClass().add("kubata-server-panel-title");
        Label tableSubtitle = new Label("Taxas e estado das moedas disponíveis para os módulos financeiros.");
        tableSubtitle.getStyleClass().add("kubata-server-note");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        tableHeader.getChildren().addAll(tableTitles, tableSpacer);

        VBox tableSection = new VBox(10, tableHeader, table);
        tableSection.getStyleClass().add("kubata-server-panel");
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

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("kubata-currency-filterbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar código, nome ou símbolo...");
        searchField.setPrefWidth(290);
        searchField.setGraphic(IconUtils.icon(Feather.SEARCH, 13));

        Label statusLabel = new Label("Estado");
        statusLabel.getStyleClass().add("kubata-currency-filter-label");

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "TODOS", "ACTIVA", "INACTIVA"
        ));
        statusFilter.setValue("TODOS");
        statusFilter.setPrefWidth(130);

        Label baseLabel = new Label("Base");
        baseLabel.getStyleClass().add("kubata-currency-filter-label");

        baseFilter = new ComboBox<>(FXCollections.observableArrayList(
                "TODAS", "BASE", "NÃO BASE"
        ));
        baseFilter.setValue("TODAS");
        baseFilter.setPrefWidth(130);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("TODOS");
            baseFilter.setValue("TODAS");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label note = new Label(
                "Taxa = referência configurada para a moeda",
                IconUtils.icon(Feather.INFO, 11)
        );
        note.getStyleClass().add("kubata-server-note");

        bar.getChildren().addAll(
                searchField, statusLabel, statusFilter,
                baseLabel, baseFilter, clear, spacer, note
        );
        return bar;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(14));
        pane.getStyleClass().add("kubata-currency-details");

        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-currency-details-icon");
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);
        icon.getChildren().add(IconUtils.icon(Feather.DOLLAR_SIGN, 17));

        VBox titleText = new VBox(2);
        detailName = new Label("Nenhuma moeda seleccionada");
        detailName.getStyleClass().add("kubata-currency-details-title");

        Label caption = new Label("Configuração e estado");
        caption.getStyleClass().add("kubata-currency-details-caption");
        titleText.getChildren().addAll(detailName, caption);

        titleRow.getChildren().addAll(icon, titleText);

        VBox facts = new VBox(4);
        facts.getStyleClass().add("kubata-currency-details-card");

        detailCode = detailRow(facts, "Código ISO");
        detailSymbol = detailRow(facts, "Símbolo");
        detailRateDate = detailRow(facts, "Data da taxa");
        detailDecimals = detailRow(facts, "Casas decimais");
        detailBase = detailRow(facts, "Moeda base");
        detailActive = detailRow(facts, "Estado");
        detailRateStatus = detailRow(facts, "Situação da taxa");

        VBox rateCard = new VBox(2);
        rateCard.getStyleClass().add("kubata-currency-rate-card");
        Label rateTitle = new Label("TAXA DE CÂMBIO CONFIGURADA");
        rateTitle.getStyleClass().add("kubata-currency-rate-title");

        detailRate = new Label("—");
        detailRate.getStyleClass().add("kubata-currency-rate-value");
        rateCard.getChildren().addAll(rateTitle, detailRate);

        Button edit = new Button("Editar moeda", IconUtils.icon(Feather.EDIT_2, 12));
        edit.getStyleClass().add("button-primary");
        edit.setOnAction(e -> openDialog(table.getSelectionModel().getSelectedItem()));

        Button delete = new Button("Remover", IconUtils.icon(Feather.TRASH_2, 12));
        delete.getStyleClass().addAll("button-outlined", "danger");
        delete.setOnAction(e -> delete(table.getSelectionModel().getSelectedItem()));

        HBox actions = new HBox(7, edit, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("kubata-currency-actionbar");

        Label info = new Label(
                "A moeda base é única. Alterá-la actualiza a configuração das restantes moedas.",
                IconUtils.icon(Feather.INFO, 11)
        );
        info.setWrapText(true);
        info.getStyleClass().add("kubata-currency-detail-note");

        pane.getChildren().addAll(titleRow, rateCard, facts, actions, info);
        return pane;
    }

    private Label detailRow(VBox parent, String title) {
        HBox row = new HBox(8);
        row.getStyleClass().add("kubata-currency-details-row");

        Label key = new Label(title.toUpperCase());
        key.getStyleClass().add("kubata-currency-details-key");
        key.setMinWidth(100);

        Label value = new Label("—");
        value.getStyleClass().add("kubata-currency-details-value");
        HBox.setHgrow(value, Priority.ALWAYS);

        row.getChildren().addAll(key, value);
        parent.getChildren().add(row);
        return value;
    }

    private void updateDetails(Moeda selected) {
        if (selected == null) {
            detailName.setText("Nenhuma moeda seleccionada");
            detailCode.setText("—");
            detailSymbol.setText("—");
            detailRate.setText("—");
            detailRateDate.setText("—");
            detailDecimals.setText("—");
            detailBase.setText("—");
            detailActive.setText("—");
            detailRateStatus.setText("—");
            return;
        }

        detailName.setText(safe(selected.getNome()));
        detailCode.setText(safe(selected.getCodigoISO()));
        detailSymbol.setText(safe(selected.getSimbolo()));
        detailRate.setText(selected.getTaxaCambio() == null
                ? "Não configurada"
                : selected.getTaxaCambio().toPlainString());
        detailRateDate.setText(selected.getDataTaxaCambio() == null
                ? "—"
                : selected.getDataTaxaCambio().toString());
        detailDecimals.setText(selected.getCasasDecimais() == null
                ? "2"
                : selected.getCasasDecimais().toString());
        detailBase.setText(Boolean.TRUE.equals(selected.getMoedaBase()) ? "SIM" : "NÃO");
        detailActive.setText(Boolean.TRUE.equals(selected.getActiva()) ? "ACTIVA" : "INACTIVA");

        if (selected.getTaxaCambio() == null) {
            detailRateStatus.setText("SEM TAXA");
        } else if (selected.getDataTaxaCambio() == null) {
            detailRateStatus.setText("SEM DATA");
        } else if (selected.getDataTaxaCambio().isBefore(LocalDate.now())) {
            detailRateStatus.setText("DESACTUALIZADA");
        } else {
            detailRateStatus.setText("ACTUALIZADA");
        }
    }

    private void applyFilters() {
        String query = searchField == null ? "" : searchField.getText();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        String status = statusFilter == null ? "TODOS" : statusFilter.getValue();
        String base = baseFilter == null ? "TODAS" : baseFilter.getValue();

        filteredMoedas.setPredicate(moeda -> {
            if (moeda == null) return false;

            boolean textMatch = normalized.isBlank()
                    || safe(moeda.getCodigoISO()).toLowerCase().contains(normalized)
                    || safe(moeda.getNome()).toLowerCase().contains(normalized)
                    || safe(moeda.getSimbolo()).toLowerCase().contains(normalized);

            boolean statusMatch = "TODOS".equals(status)
                    || ("ACTIVA".equals(status) && Boolean.TRUE.equals(moeda.getActiva()))
                    || ("INACTIVA".equals(status) && !Boolean.TRUE.equals(moeda.getActiva()));

            boolean baseMatch = "TODAS".equals(base)
                    || ("BASE".equals(base) && Boolean.TRUE.equals(moeda.getMoedaBase()))
                    || ("NÃO BASE".equals(base) && !Boolean.TRUE.equals(moeda.getMoedaBase()));

            return textMatch && statusMatch && baseMatch;
        });
    }

    private VBox metric(String title, Label value, Feather icon) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-server-metric-icon");
        iconBox.getChildren().add(IconUtils.icon(icon, 15));

        VBox text = new VBox(1);
        Label caption = new Label(title);
        caption.getStyleClass().add("kubata-server-metric-title");
        value.getStyleClass().add("kubata-server-metric-value");
        text.getChildren().addAll(caption, value);

        content.getChildren().addAll(iconBox, text);

        VBox card = new VBox(content);
        card.getStyleClass().add("kubata-server-metric");
        return card;
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("kubata-server-properties-table");
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
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(6));
        grid.add(new Label("Código ISO"), 0, 0);
        grid.add(codigo, 1, 0);
        grid.add(new Label("Nome"), 0, 1);
        grid.add(nome, 1, 1);
        grid.add(new Label("Símbolo"), 0, 2);
        grid.add(simbolo, 1, 2);
        grid.add(new Label("Taxa de câmbio"), 0, 3);
        grid.add(taxa, 1, 3);
        grid.add(new Label("Data da taxa"), 0, 4);
        grid.add(data, 1, 4);
        grid.add(new Label("Casas decimais"), 0, 5);
        grid.add(casas, 1, 5);
        grid.add(base, 1, 6);
        grid.add(activa, 1, 7);

        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(new ColumnConstraints(140), right);

        modalManager.showConfirmModal(
                grid,
                current == null ? "Nova moeda" : "Editar moeda",
                () -> {
                    String iso = codigo.getText().trim().toUpperCase();
                    String nomeValue = nome.getText().trim();

                    if (iso.length() != 3 || nomeValue.isBlank()) {
                        modalManager.warning(
                                "Moeda",
                                "Informe um código ISO com 3 caracteres e o nome da moeda."
                        );
                        return;
                    }

                    try {
                        BigDecimal cambio = taxa.getText().isBlank()
                                ? BigDecimal.ONE
                                : new BigDecimal(taxa.getText().trim());

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
                        modalManager.warning("Moeda", "A taxa de câmbio deve ser numérica.");
                    } catch (Exception ex) {
                        modalManager.showErrorModal(
                                "Moeda",
                                "Não foi possível guardar a moeda.",
                                ex
                        );
                    }
                },
                () -> {}
        );
    }

    private void delete(Moeda moeda) {
        if (moeda == null || moeda.getId() == null) return;

        modalManager.showConfirm(
                "Remover moeda",
                "Remover a moeda " + moeda.getCodigoISO() + "?",
                () -> {
                    try {
                        repository.delete(moeda);
                        load();
                    } catch (Exception ex) {
                        modalManager.showErrorModal(
                                "Moeda",
                                "Não foi possível remover a moeda.",
                                ex
                        );
                    }
                }
        );
    }

    private String safe(String value) {

        return value == null ? "" : value;
    }
}
