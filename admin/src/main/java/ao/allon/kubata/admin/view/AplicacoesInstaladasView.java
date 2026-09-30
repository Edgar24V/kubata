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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final TableView<ModuloSistema> table = new TableView<>(modules);
    private final Label status = new Label("Pronto");
    private final Label totalValue = new Label("0");
    private final Label runtimeValue = new Label("0");
    private final Label activeValue = new Label("0");

    public AplicacoesInstaladasView(
            ModuloSistemaRepository repository,
            ModuleRegistry registry,
            ModuleInstallationService installationService) {
        this.repository = repository;
        this.registry = registry;
        this.installationService = installationService;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        refresh();
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
        iconBox.getChildren().add(IconUtils.icon(Feather.PACKAGE, 21));

        VBox titles = new VBox(2);
        Label title = new Label("Aplicações Instaladas");
        title.getStyleClass().add("kubata-infra-title");

        Label subtitle = new Label(
                "Catálogo administrativo dos módulos registados no runtime do Kubata."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-infra-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sync = new Button("Sincronizar catálogo", IconUtils.icon(Feather.REFRESH_CW, 13));
        sync.getStyleClass().add("button-outlined");
        sync.setOnAction(e -> synchronize());

        line.getChildren().addAll(iconBox, titles, spacer, sync);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("kubata-infra-toolbar");

        Button activate = new Button("Activar / instalar", IconUtils.icon(Feather.PLAY, 12));
        activate.getStyleClass().add("button-primary");
        activate.setOnAction(e -> activateSelected());

        Button init = new Button("Inicializar", IconUtils.icon(Feather.POWER, 12));
        init.getStyleClass().add("button-outlined");
        init.setOnAction(e -> initializeSelected());

        Label helper = new Label("Seleccione uma aplicação na tabela para executar uma operação.");
        helper.getStyleClass().add("kubata-infra-muted");

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
        tableTitle.getStyleClass().add("kubata-infra-section-title");
        Label tableSubtitle = new Label("Estado do módulo, integração com runtime e funcionalidades disponíveis.");
        tableSubtitle.getStyleClass().add("kubata-infra-section-subtitle");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        status.getStyleClass().add("kubata-infra-status");

        tableHeader.getChildren().addAll(tableTitles, tableSpacer, status);

        VBox tableSection = new VBox(10, tableHeader, table);
        tableSection.getStyleClass().add("kubata-infra-section");
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

    private VBox metricCard(String title, Label value, Feather icon) {
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
