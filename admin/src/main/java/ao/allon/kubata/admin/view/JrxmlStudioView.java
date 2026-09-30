package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Estúdio de modelos JRXML do Kubata.
 *
 * <p>Focado em gestão/diagnóstico dos templates JasperReports disponíveis no
 * classpath. A publicação do modelo continua a ser uma operação externa e
 * controlada, evitando alteração silenciosa dos recursos empacotados.</p>
 */
@Component
@Lazy
public class JrxmlStudioView extends VBox {

    private final ModalManager modalManager;
    private final ObservableList<ReportTemplate> allTemplates = FXCollections.observableArrayList();
    private final ObservableList<ReportTemplate> filteredTemplates = FXCollections.observableArrayList();

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> moduleFilter;
    private TableView<ReportTemplate> table;
    private TextArea previewArea;
    private Label totalValue;
    private Label validValue;
    private Label invalidValue;
    private Label selectedValue;
    private Label statusValue;

    public JrxmlStudioView(ModalManager modalManager) {
        this.modalManager = modalManager;
        setSpacing(0);
        getStyleClass().add("kubata-jrxml-studio");
        buildUi();
        Platform.runLater(this::refreshCatalog);
    }

    private void buildUi() {
        getChildren().addAll(
                buildHeader(),
                buildKpis(),
                buildFilterBar(),
                buildWorkspace(),
                buildFooter()
        );
        VBox.setVgrow(getChildren().get(3), Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-jrxml-header");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-jrxml-title-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.FILE_TEXT, 22)));

        VBox titleBox = new VBox(2);
        Label title = new Label("Estúdio JRXML");
        title.getStyleClass().add("kubata-jrxml-title");

        Label subtitle = new Label(
                "Catálogo central de templates JasperReports, validação técnica, consulta do conteúdo e preparação para publicação."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-jrxml-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Recarregar catálogo", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> refreshCatalog());

        Button importButton = new Button("Importar / analisar", IconUtils.icon(Feather.UPLOAD, 13));
        importButton.getStyleClass().add("button-primary");
        importButton.setOnAction(e -> importAndAnalyze());

        line.getChildren().addAll(icon, titleBox, spacer, refresh, importButton);
        header.getChildren().add(line);
        return header;
    }

    private HBox buildKpis() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(0, 22, 14, 22));

        totalValue = new Label("0");
        validValue = new Label("0");
        invalidValue = new Label("0");
        selectedValue = new Label("—");

        box.getChildren().addAll(
                kpi("TEMPLATES", totalValue, Feather.FILE_TEXT),
                kpi("VÁLIDOS", validValue, Feather.CHECK_CIRCLE),
                kpi("COM ERROS", invalidValue, Feather.ALERT_CIRCLE),
                kpi("SELECCIONADO", selectedValue, Feather.FILE)
        );
        return box;
    }

    private VBox kpi(String title, Label value, Feather icon) {
        VBox card = new VBox(3);
        card.getStyleClass().add("kubata-jrxml-kpi");
        card.setPadding(new Insets(9, 13, 9, 13));
        card.setMinWidth(160);

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 13));
        iconLabel.getStyleClass().add("kubata-jrxml-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-jrxml-kpi-label");

        row.getChildren().addAll(iconLabel, titleLabel);
        value.getStyleClass().add("kubata-jrxml-kpi-value");
        card.getChildren().addAll(row, value);
        return card;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 22, 10, 22));
        bar.getStyleClass().add("kubata-jrxml-filterbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome, ficheiro, módulo...");
        searchField.setPrefWidth(300);

        moduleFilter = new ComboBox<>();
        moduleFilter.setPromptText("Módulo");
        moduleFilter.setPrefWidth(150);

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Válidos", "Com erros", "Não validados"
        ));
        statusFilter.setValue("Todos");
        statusFilter.setPrefWidth(145);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            moduleFilter.setValue(null);
            statusFilter.setValue("Todos");
            applyFilter();
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        moduleFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusValue = new Label("Aguardando validação");
        statusValue.getStyleClass().add("kubata-jrxml-filter-status");

        bar.getChildren().addAll(
                new Label("", IconUtils.icon(Feather.SEARCH, 13)),
                searchField,
                moduleFilter,
                statusFilter,
                clear,
                spacer,
                statusValue
        );
        return bar;
    }

    private SplitPane buildWorkspace() {
        table = new TableView<>(filteredTemplates);
        table.setPlaceholder(new Label("Nenhum template JRXML encontrado."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("kubata-jrxml-table");

        TableColumn<ReportTemplate, String> name = new TableColumn<>("Template");
        name.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().displayName()));

        TableColumn<ReportTemplate, String> file = new TableColumn<>("Ficheiro");
        file.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().fileName()));

        TableColumn<ReportTemplate, String> module = new TableColumn<>("Módulo");
        module.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().module()));

        TableColumn<ReportTemplate, String> size = new TableColumn<>("Tamanho");
        size.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().sizeLabel()));

        TableColumn<ReportTemplate, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().validationLabel()));
        state.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("kubata-jrxml-badge");
                badge.getStyleClass().add(
                        switch (item) {
                            case "Válido" -> "kubata-jrxml-badge-ok";
                            case "Erro" -> "kubata-jrxml-badge-error";
                            default -> "kubata-jrxml-badge-neutral";
                        }
                );
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<ReportTemplate, Void> actions = new TableColumn<>("Acções");
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button validate = new Button("Validar", IconUtils.icon(Feather.CHECK, 11));

            {
                validate.getStyleClass().add("button-outlined");
                validate.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        validateTemplate(getTableView().getItems().get(index));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : validate);
            }
        });

        table.getColumns().addAll(name, file, module, size, state, actions);
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showSelected(selected)
        );

        VBox left = new VBox(table);
        left.getStyleClass().add("kubata-jrxml-table-pane");
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox right = buildDetailsPane();

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.66);
        split.getStyleClass().add("kubata-jrxml-split");
        return split;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(14));
        pane.getStyleClass().add("kubata-jrxml-details");

        Label heading = new Label("Detalhes do template");
        heading.getStyleClass().add("kubata-jrxml-details-title");

        previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setPromptText("Seleccione um template para consultar o XML.");
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        previewArea.getStyleClass().add("kubata-jrxml-editor");

        HBox actions = new HBox(7);
        Button analyze = new Button("Analisar estrutura", IconUtils.icon(Feather.SEARCH, 12));
        analyze.getStyleClass().add("button-outlined");
        analyze.setOnAction(e -> selected().ifPresent(this::analyzeTemplate));

        Button export = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, 12));
        export.getStyleClass().add("button-outlined");
        export.setOnAction(e -> selected().ifPresent(this::exportTemplate));

        Button preview = new Button("Visualizar XML", IconUtils.icon(Feather.MAXIMIZE_2, 12));
        preview.getStyleClass().add("button-outlined");
        preview.setOnAction(e -> selected().ifPresent(this::openXmlPreview));

        actions.getChildren().addAll(analyze, export, preview);

        pane.getChildren().addAll(heading, previewArea, actions);
        return pane;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(7, 14, 7, 14));
        footer.getStyleClass().add("kubata-jrxml-footer");

        Label left = new Label("JRXML é a fonte declarativa; a compilação é validada com o JasperReports instalado no Kubata.");
        left.getStyleClass().add("kubata-jrxml-footer-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label("Publicação não altera automaticamente o classpath empacotado.");
        right.getStyleClass().add("kubata-jrxml-footer-text");

        footer.getChildren().addAll(left, spacer, right);
        return footer;
    }

    private void refreshCatalog() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/reports/*.jrxml");

            List<ReportTemplate> found = java.util.Arrays.stream(resources)
                    .filter(Objects::nonNull)
                    .filter(Resource::exists)
                    .map(this::toTemplate)
                    .sorted(Comparator.comparing(ReportTemplate::fileName))
                    .collect(Collectors.toList());

            allTemplates.setAll(found);
            rebuildModuleFilter();
            applyFilter();

            int valid = (int) found.stream().filter(t -> "Válido".equals(t.validationLabel())).count();
            int invalid = (int) found.stream().filter(t -> "Erro".equals(t.validationLabel())).count();

            totalValue.setText(String.valueOf(found.size()));
            validValue.setText(String.valueOf(valid));
            invalidValue.setText(String.valueOf(invalid));
            selectedValue.setText("—");
            statusValue.setText(found.size() + " template(s) no classpath");
        } catch (Exception ex) {
            allTemplates.clear();
            filteredTemplates.clear();
            totalValue.setText("0");
            validValue.setText("0");
            invalidValue.setText("—");
            statusValue.setText("Falha ao ler catálogo");
            modalManager.alert(
                    "Catálogo JRXML",
                    "Não foi possível carregar os templates: " + ex.getMessage(),
                    "error",
                    ex
            );
        }
    }

    private ReportTemplate toTemplate(Resource resource) {
        String fileName = resource.getFilename() == null ? "desconhecido.jrxml" : resource.getFilename();
        String path = resource.getDescription() == null ? "" : resource.getDescription();
        String module = inferModule(path);
        long size = -1L;
        try {
            size = resource.contentLength();
        } catch (Exception ignored) {
        }

        ValidationResult validation = validateResource(resource);
        return new ReportTemplate(
                fileName,
                fileName.replaceAll("(?i)\\.jrxml$", ""),
                module,
                size,
                validation.status(),
                validation.message(),
                resource
        );
    }

    private ValidationResult validateResource(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            JasperDesign design = JRXmlLoader.load(in);
            JasperCompileManager.compileReport(design);
            return new ValidationResult("Válido", "JRXML carregado e compilado pelo JasperReports.");
        } catch (Exception ex) {
            return new ValidationResult("Erro", compact(ex.getMessage()));
        }
    }

    private void rebuildModuleFilter() {
        List<String> modules = allTemplates.stream()
                .map(ReportTemplate::module)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        moduleFilter.getItems().setAll(modules);
    }

    private void applyFilter() {
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String module = moduleFilter == null ? null : moduleFilter.getValue();
        String status = statusFilter == null ? "Todos" : statusFilter.getValue();

        filteredTemplates.setAll(allTemplates.stream()
                .filter(t -> q.isBlank()
                        || t.fileName().toLowerCase(Locale.ROOT).contains(q)
                        || t.displayName().toLowerCase(Locale.ROOT).contains(q)
                        || t.module().toLowerCase(Locale.ROOT).contains(q))
                .filter(t -> module == null || module.equals(t.module()))
                .filter(t -> switch (status) {
                    case "Válidos" -> "Válido".equals(t.validationLabel());
                    case "Com erros" -> "Erro".equals(t.validationLabel());
                    case "Não validados" -> "Não validado".equals(t.validationLabel());
                    default -> true;
                })
                .collect(Collectors.toList()));
    }

    private void showSelected(ReportTemplate template) {
        if (template == null) {
            selectedValue.setText("—");
            previewArea.clear();
            return;
        }

        selectedValue.setText(template.fileName());
        statusValue.setText(template.validationLabel() + " · " + template.message());

        try (InputStream in = template.resource().getInputStream()) {
            previewArea.setText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            previewArea.positionCaret(0);
        } catch (Exception ex) {
            previewArea.setText("Não foi possível ler o conteúdo: " + ex.getMessage());
        }
    }

    private Optional<ReportTemplate> selected() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private void validateTemplate(ReportTemplate template) {
        ValidationResult result = validateResource(template.resource());
        ReportTemplate updated = template.withValidation(result.status(), result.message());
        int index = allTemplates.indexOf(template);
        if (index >= 0) {
            allTemplates.set(index, updated);
        }
        applyFilter();
        table.getSelectionModel().select(updated);

        if ("Válido".equals(result.status())) {
            modalManager.alert(
                    "JRXML válido",
                    template.fileName() + "\n\n" + result.message(),
                    "info",
                    null
            );
        } else {
            modalManager.alert(
                    "JRXML com erro",
                    template.fileName() + "\n\n" + result.message(),
                    "error",
                    null
            );
        }
    }

    private long countBands(JasperDesign design) {
        long count = 0;

        if (design.getTitle() != null) count++;
        if (design.getPageHeader() != null) count++;
        if (design.getColumnHeader() != null) count++;
        if (design.getColumnFooter() != null) count++;
        if (design.getPageFooter() != null) count++;
        if (design.getLastPageFooter() != null) count++;
        if (design.getSummary() != null) count++;
        if (design.getBackground() != null) count++;
        if (design.getNoData() != null) count++;

        if (design.getDetailSection() != null && design.getDetailSection().getBands() != null) {
            count += design.getDetailSection().getBands().length;
        }

        if (design.getGroups() != null) {
            for (net.sf.jasperreports.engine.JRGroup group : design.getGroups()) {
                if (group == null) continue;
                if (group.getGroupHeaderSection() != null
                        && group.getGroupHeaderSection().getBands() != null) {
                    count += group.getGroupHeaderSection().getBands().length;
                }
                if (group.getGroupFooterSection() != null
                        && group.getGroupFooterSection().getBands() != null) {
                    count += group.getGroupFooterSection().getBands().length;
                }
            }
        }

        return count;
    }

    private void analyzeTemplate(ReportTemplate template) {
        try (InputStream in = template.resource().getInputStream()) {
            JasperDesign d = JRXmlLoader.load(in);

            long parameters = d.getParametersList() == null ? 0 : d.getParametersList().size();
            long fields = d.getFieldsList() == null ? 0 : d.getFieldsList().size();
            long variables = d.getVariablesList() == null ? 0 : d.getVariablesList().size();
            long bands = countBands(d);

            String text =
                    "Template: " + d.getName() + "\n"
                    + "Dimensão: " + d.getPageWidth() + " × " + d.getPageHeight() + "\n"
                    + "Parâmetros: " + parameters + "\n"
                    + "Campos: " + fields + "\n"
                    + "Variáveis: " + variables + "\n"
                    + "Bandas: " + bands + "\n\n"
                    + "O modelo foi lido sem modificar o recurso empacotado.";

            modalManager.alert("Análise do JRXML", text, "info", null);
        } catch (Exception ex) {
            modalManager.alert(
                    "Falha na análise",
                    template.fileName() + "\n\n" + compact(ex.getMessage()),
                    "error",
                    ex
            );
        }
    }

    private void exportTemplate(ReportTemplate template) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar JRXML");
        chooser.setInitialFileName(template.fileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JRXML", "*.jrxml"));

        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) {
            return;
        }

        try (InputStream in = template.resource().getInputStream()) {
            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            modalManager.alert(
                    "Exportação concluída",
                    "O template foi exportado para:\n" + target.getAbsolutePath(),
                    "info",
                    null
            );
        } catch (Exception ex) {
            modalManager.alert(
                    "Erro na exportação",
                    ex.getMessage() == null ? "Não foi possível exportar o template." : ex.getMessage(),
                    "error",
                    ex
            );
        }
    }

    private void importAndAnalyze() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar JRXML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JRXML", "*.jrxml"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }

        try (InputStream in = Files.newInputStream(file.toPath())) {
            JasperDesign design = JRXmlLoader.load(in);
            JasperCompileManager.compileReport(design);

            long parameters = design.getParametersList() == null ? 0 : design.getParametersList().size();
            long fields = design.getFieldsList() == null ? 0 : design.getFieldsList().size();

            modalManager.alert(
                    "JRXML analisado com sucesso",
                    "Ficheiro: " + file.getName()
                            + "\n\nCompilação: OK"
                            + "\nParâmetros: " + parameters
                            + "\nCampos: " + fields
                            + "\n\nO ficheiro foi apenas analisado; ainda não foi publicado no Kubata.",
                    "info",
                    null
            );
        } catch (Exception ex) {
            modalManager.alert(
                    "JRXML inválido",
                    file.getName() + "\n\n" + compact(ex.getMessage()),
                    "error",
                    ex
            );
        }
    }

    private void openXmlPreview(ReportTemplate template) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add("kubata-jrxml-editor");

        try (InputStream in = template.resource().getInputStream()) {
            area.setText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            area.setText("Erro ao abrir o JRXML: " + ex.getMessage());
        }

        VBox root = new VBox(area);
        root.setPadding(new Insets(4));
        VBox.setVgrow(area, Priority.ALWAYS);

        modalManager.showModal(
                root,
                new ModalManager.ModalConfig()
                        .size(1050, 720)
                        .minSize(760, 520)
                        .title("JRXML · " + template.fileName())
                        .icon(Feather.FILE_TEXT)
        );
    }

    private String inferModule(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        if (normalized.contains("/admin/")) return "Administrator";
        if (normalized.contains("/faturacao/")) return "Facturação";
        if (normalized.contains("/rh/")) return "RH";
        if (normalized.contains("/kubata-inventario/")) return "Inventário";
        if (normalized.contains("/kubata-vendas/")) return "Vendas";
        if (normalized.contains("/kubata-compras/")) return "Compras";
        if (normalized.contains("/kubata-financeiro/")) return "Financeiro";
        if (normalized.contains("/kubata-contabilidade/")) return "Contabilidade";
        if (normalized.contains("/kubata-fiscal/")) return "Fiscal";
        return "Comum";
    }

    private String compact(String message) {
        if (message == null || message.isBlank()) {
            return "Erro sem mensagem detalhada.";
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private record ValidationResult(String status, String message) {}

    private record ReportTemplate(
            String fileName,
            String displayName,
            String module,
            long size,
            String validationLabel,
            String message,
            Resource resource
    ) {
        ReportTemplate withValidation(String status, String reason) {
            return new ReportTemplate(
                    fileName, displayName, module, size, status, reason, resource
            );
        }

        String sizeLabel() {
            if (size < 0) return "N/D";
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", size / 1024.0);
            return String.format(Locale.ROOT, "%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
