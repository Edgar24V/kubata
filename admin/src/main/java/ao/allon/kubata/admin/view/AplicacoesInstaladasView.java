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
        VBox header = new VBox(9);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.getStyleClass().add("header-box");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.PACKAGE, 20));
        VBox titles = new VBox(2);

        Label title = new Label("Aplicações Instaladas");
        title.getStyleClass().add("h3");

        Label subtitle = new Label(
                "Catálogo administrativo dos módulos realmente registados no runtime do Kubata."
        );
        subtitle.getStyleClass().add("text-muted");

        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sync = new Button("Sincronizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        sync.getStyleClass().add("button-outlined");
        sync.setOnAction(e -> synchronize());

        line.getChildren().addAll(icon, titles, spacer, sync);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button activate = new Button("Activar / instalar", IconUtils.icon(Feather.PLAY, 12));
        activate.getStyleClass().add("button-primary");
        activate.setOnAction(e -> activateSelected());

        Button init = new Button("Inicializar", IconUtils.icon(Feather.POWER, 12));
        init.getStyleClass().add("button-outlined");
        init.setOnAction(e -> initializeSelected());

        actions.getChildren().addAll(activate, init);
        header.getChildren().addAll(line, actions);

        buildTable();

        VBox content = new VBox(10, table, status);
        content.setPadding(new Insets(15, 18, 18, 18));
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(header, content);
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        Platform.runLater(() -> modules.setAll(
                repository.findAll().stream()
                        .sorted(Comparator.comparing(ModuloSistema::getNome,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList()
        ));
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
