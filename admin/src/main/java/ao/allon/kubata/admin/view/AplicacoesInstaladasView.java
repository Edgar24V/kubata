package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.ModuleInstallationService;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
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

import java.util.Comparator;

@Component
public class AplicacoesInstaladasView extends VBox {

    private final ModuloSistemaRepository repository;
    private final ModuleRegistry registry;
    private final ModuleInstallationService installationService;
    private final ObservableList<ModuloSistema> modules = FXCollections.observableArrayList();
    private final FilteredList<ModuloSistema> filteredModules = new FilteredList<>(modules, m -> true);
    private final TableView<ModuloSistema> table = new TableView<>(filteredModules);
    private final Label status = new Label("Pronto");
    private final Label totalValue = new Label("0");
    private final Label runtimeValue = new Label("0");
    private final Label activeValue = new Label("0");
    private final Label pendingValue = new Label("0");
    private final Label errorValue = new Label("0");
    private TextField searchField;
    private ComboBox<String> stateFilter;
    private Label detailName, detailCode, detailDescription, detailVersion,
            detailRuntime, detailState, detailViews, detailMandatory,
            detailInstalled, detailLicense;

    public AplicacoesInstaladasView(
            ModuloSistemaRepository repository,
            ModuleRegistry registry,
            ModuleInstallationService installationService) {
        this.repository = repository;
        this.registry = registry;
        this.installationService = installationService;

        setSpacing(0);
        getStyleClass().add("kubata-server-page");
        buildUi();
        refresh();
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
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.PACKAGE, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Aplicações Instaladas");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Catálogo administrativo dos módulos registados no runtime do Kubata."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sync = new Button("Sincronizar catálogo", IconUtils.icon(Feather.REFRESH_CW, 13));
        sync.getStyleClass().add("button-outlined");
        sync.setOnAction(e -> synchronize());

        line.getChildren().addAll(iconBox, titles, spacer, sync);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("kubata-server-status-bar");

        Button activate = new Button("Activar / instalar", IconUtils.icon(Feather.PLAY, 12));
        activate.getStyleClass().add("button-primary");
        activate.setOnAction(e -> activateSelected());

        Button init = new Button("Inicializar", IconUtils.icon(Feather.POWER, 12));
        init.getStyleClass().add("button-outlined");
        init.setOnAction(e -> initializeSelected());

        Label helper = new Label("Seleccione uma aplicação na tabela para executar uma operação.");
        helper.getStyleClass().add("kubata-server-note");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actions.getChildren().addAll(helper, actionSpacer, activate, init);

        header.getChildren().addAll(line, actions);

        buildTable();

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 20, 20));
        content.setFillWidth(true);

        content.getChildren().add(buildMetrics());

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox tableTitles = new VBox(2);
        Label tableTitle = new Label("Catálogo instalado");
        tableTitle.getStyleClass().add("kubata-server-panel-title");
        Label tableSubtitle = new Label("Estado do módulo, integração com runtime e funcionalidades disponíveis.");
        tableSubtitle.getStyleClass().add("kubata-server-note");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        status.getStyleClass().add("kubata-server-status-value");

        tableHeader.getChildren().addAll(tableTitles, tableSpacer, status);

        VBox tableSection = new VBox(10, tableHeader, table);
        tableSection.getStyleClass().add("kubata-server-panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        content.getChildren().add(tableSection);

        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    private HBox buildMetrics() {
        HBox row = new HBox(12);
        row.getChildren().addAll(
                metricCard("APLICAÇÕES", totalValue, Feather.PACKAGE),
                metricCard("NO RUNTIME", runtimeValue, Feather.CPU),
                metricCard("ACTIVAS", activeValue, Feather.CHECK_CIRCLE)
        );
        for (Node node : row.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        return row;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("kubata-applications-filterbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por aplicação ou código...");
        searchField.setPrefWidth(300);
        searchField.setGraphic(IconUtils.icon(Feather.SEARCH, 13));

        Label stateLabel = new Label("Estado");
        stateLabel.getStyleClass().add("kubata-app-filter-label");

        stateFilter = new ComboBox<>(FXCollections.observableArrayList(
                "TODOS", "ACTIVO", "DISPONIVEL", "INACTIVO", "ERRO", "ACTUALIZACAO_PENDENTE"
        ));
        stateFilter.setValue("TODOS");
        stateFilter.setPrefWidth(185);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            stateFilter.setValue("TODOS");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label info = new Label(
                "Fonte de runtime: ModuleRegistry",
                IconUtils.icon(Feather.CPU, 11)
        );
        info.getStyleClass().add("kubata-server-note");

        bar.getChildren().addAll(searchField, stateLabel, stateFilter, clear, spacer, info);
        return bar;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(10);
        pane.getStyleClass().add("kubata-applications-details");
        pane.setPadding(new Insets(14));

        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-app-details-icon");
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);
        icon.getChildren().add(IconUtils.icon(Feather.PACKAGE, 17));

        VBox titleText = new VBox(2);
        detailName = new Label("Nenhuma aplicação");
        detailName.getStyleClass().add("kubata-app-details-title");
        Label caption = new Label("Detalhes e operações");
        caption.getStyleClass().add("kubata-app-details-caption");
        titleText.getChildren().addAll(detailName, caption);
        titleRow.getChildren().addAll(icon, titleText);

        VBox facts = new VBox(4);
        facts.getStyleClass().add("kubata-app-details-card");
        detailCode = detailRow(facts, "Código");
        detailVersion = detailRow(facts, "Versão");
        detailState = detailRow(facts, "Estado");
        detailRuntime = detailRow(facts, "Runtime");
        detailViews = detailRow(facts, "Funcionalidades");
        detailMandatory = detailRow(facts, "Obrigatório");
        detailInstalled = detailRow(facts, "Instalado em");
        detailLicense = detailRow(facts, "Licença");

        Label descTitle = new Label("Descrição");
        descTitle.getStyleClass().add("kubata-app-details-section");
        detailDescription = new Label("Seleccione uma aplicação na tabela.");
        detailDescription.setWrapText(true);
        detailDescription.getStyleClass().add("kubata-app-details-description");

        Button activate = new Button("Activar / instalar", IconUtils.icon(Feather.PLAY, 12));
        activate.getStyleClass().add("button-primary");
        activate.setOnAction(e -> activateSelected());

        Button initialize = new Button("Inicializar", IconUtils.icon(Feather.POWER, 12));
        initialize.getStyleClass().add("button-outlined");
        initialize.setOnAction(e -> initializeSelected());

        Button sync = new Button("Sincronizar", IconUtils.icon(Feather.REFRESH_CW, 12));
        sync.getStyleClass().add("button-outlined");
        sync.setOnAction(e -> synchronize());

        HBox actions = new HBox(7, activate, initialize, sync);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label(
                "As operações utilizam apenas módulos realmente registados no runtime.",
                IconUtils.icon(Feather.SHIELD, 11)
        );
        info.getStyleClass().add("kubata-app-details-info");
        info.setWrapText(true);

        pane.getChildren().addAll(titleRow, facts, descTitle, detailDescription,
                new Separator(), actions, info);
        return pane;
    }

    private Label detailRow(VBox parent, String title) {
        HBox row = new HBox(8);
        row.getStyleClass().add("kubata-app-details-row");
        Label key = new Label(title.toUpperCase());
        key.getStyleClass().add("kubata-app-details-key");
        key.setMinWidth(92);
        Label value = new Label("—");
        value.getStyleClass().add("kubata-app-details-value");
        HBox.setHgrow(value, Priority.ALWAYS);
        row.getChildren().addAll(key, value);
        parent.getChildren().add(row);
        return value;
    }

    private void updateDetails(ModuloSistema selected) {
        if (selected == null) {
            detailName.setText("Nenhuma aplicação");
            detailCode.setText("—");
            detailVersion.setText("—");
            detailState.setText("—");
            detailRuntime.setText("—");
            detailViews.setText("—");
            detailMandatory.setText("—");
            detailInstalled.setText("—");
            detailLicense.setText("—");
            detailDescription.setText("Seleccione uma aplicação na tabela.");
            return;
        }

        detailName.setText(value(selected.getNome()));
        detailCode.setText(value(selected.getCodigo()));
        detailVersion.setText(value(selected.getVersao()));
        detailState.setText(selected.getEstado() == null ? "—" : selected.getEstado().toString());
        detailRuntime.setText(registry.isModuleRegistered(selected.getCodigo()) ? "REGISTADO" : "AUSENTE");
        detailViews.setText(Integer.toString(
                registry.getModule(selected.getCodigo())
                        .map(KubataModule::getModuleViews)
                        .map(java.util.List::size)
                        .orElse(0)
        ));
        detailMandatory.setText(Boolean.TRUE.equals(selected.getObrigatorio()) ? "SIM" : "NÃO");
        detailInstalled.setText(selected.getInstaladoEm() == null ? "—" : selected.getInstaladoEm().toString());
        detailLicense.setText(formatLicense(selected));
        detailDescription.setText(value(selected.getDescricao()));
    }

    private String formatLicense(ModuloSistema module) {
        if (module.getLicencaChave() == null || module.getLicencaChave().isBlank()) return "Não configurada";
        return module.getLicencaValidade() == null
                ? "Configurada · sem validade"
                : "Configurada · válida até " + module.getLicencaValidade();
    }

    private void applyFilters() {
        String query = searchField == null ? "" : searchField.getText();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        String selectedState = stateFilter == null ? "TODOS" : stateFilter.getValue();

        filteredModules.setPredicate(module -> {
            if (module == null) return false;
            boolean textMatch = normalized.isBlank()
                    || safeLower(module.getNome()).contains(normalized)
                    || safeLower(module.getCodigo()).contains(normalized);
            boolean stateMatch = "TODOS".equals(selectedState)
                    || (module.getEstado() != null && module.getEstado().name().equalsIgnoreCase(selectedState));
            return textMatch && stateMatch;
        });

        status.setText(filteredModules.size() + " aplicação(ões) visível(is)");
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private VBox metricCard(String title, Label value, Feather icon) {
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
        table.setPlaceholder(new Label("Nenhuma aplicação registada."));

        TableColumn<ModuloSistema, String> name = new TableColumn<>("Aplicação");
        name.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getNome())));

        TableColumn<ModuloSistema, String> code = new TableColumn<>("Código");
        code.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getCodigo())));

        TableColumn<ModuloSistema, String> version = new TableColumn<>("Versão");
        version.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getVersao())));

        TableColumn<ModuloSistema, String> views = new TableColumn<>("Funcionalidades");
        views.setCellValueFactory(c -> {
            int count = registry.getModule(c.getValue().getCodigo())
                    .map(KubataModule::getModuleViews)
                    .map(java.util.List::size)
                    .orElse(0);
            return new SimpleStringProperty(Integer.toString(count));
        });

        TableColumn<ModuloSistema, String> runtime = new TableColumn<>("Runtime");
        runtime.setCellValueFactory(c -> new SimpleStringProperty(
                registry.isModuleRegistered(c.getValue().getCodigo()) ? "Registado" : "Ausente"
        ));

        TableColumn<ModuloSistema, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "—" : c.getValue().getEstado().toString()
        ));

        table.getColumns().setAll(name, code, version, views, runtime, state);
    }

    private void refresh() {
        Platform.runLater(() -> {
            modules.setAll(repository.findAll().stream()
                    .sorted(Comparator.comparing(ModuloSistema::getNome,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList());

            totalValue.setText(Integer.toString(modules.size()));
            runtimeValue.setText(Long.toString(modules.stream()
                    .filter(m -> registry.isModuleRegistered(m.getCodigo()))
                    .count()));
            activeValue.setText(Long.toString(modules.stream()
                    .filter(m -> m.getEstado() == ModuloSistema.EstadoModulo.ACTIVO)
                    .count()));
            status.setText("Catálogo actualizado · " + modules.size() + " aplicação(ões)");
        });
    }

    private void synchronize() {
        run("A sincronizar catálogo...", installationService::synchronizeCatalog);
    }

    private void activateSelected() {
        ModuloSistema selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Seleccione uma aplicação.");
            return;
        }
        run("A activar " + selected.getCodigo() + "...",
                () -> installationService.installAndInitialize(selected.getCodigo()));
    }

    private void initializeSelected() {
        ModuloSistema selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Seleccione uma aplicação.");
            return;
        }
        run("A inicializar " + selected.getCodigo() + "...",
                () -> installationService.initializeInstalledModule(selected.getCodigo()));
    }

    private void run(String message, java.util.concurrent.Callable<?> action) {
        status.setText(message);
        Thread thread = new Thread(() -> {
            try {
                action.call();
                Platform.runLater(() -> {
                    status.setText("Operação concluída.");
                    refresh();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Falha: " + safe(ex.getMessage())));
            }
        }, "kubata-admin-module-op");
        thread.setDaemon(true);
        thread.start();
    }

    private String value(String value) {
        return value == null ? "—" : value;
    }

    private String safe(String message) {
        return message == null || message.isBlank() ? "erro desconhecido" : message;
    }
}
