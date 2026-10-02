package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import ao.allon.kubata.core.service.AuditService;
import ao.allon.kubata.core.service.SecurityService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.net.InetAddress;

/**
 * Gestão de adm_parametro_sistema.
 *
 * Permite parâmetros globais ou por empresa, com:
 * - pesquisa e filtragem;
 * - criação, edição e remoção com validação;
 * - persistência assíncrona;
 * - auditoria de alterações;
 * - protecção de parâmetros não editáveis;
 * - carregamento real das empresas e dos parâmetros.
 */
@Component
public class ParametrosSistemaView extends VBox {

    private static final List<String> TYPES = List.of(
            "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE"
    );

    private final ParametroSistemaRepository parametroRepository;
    private final EmpresaRepository empresaRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final AuditService auditService;
    private final SecurityService securityService;

    private final ComboBox<EmpresaScope> scopeCombo = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> groupFilter = new ComboBox<>();
    private final ObservableList<ParametroSistema> data = FXCollections.observableArrayList();
    private final AdvancedTableView<ParametroSistema> table = AdvancedTableView
            .<ParametroSistema>builder()
            .data(data)
            .entityName("Parâmetro")
            .onEdit(this::editFromContext)
            .onDelete(this::deleteFromContext)
            .onRefresh(this::reload)
            .build();

    private final Label totalLabel = new Label("0 parâmetros");
    private final Label editableLabel = new Label("0 editáveis");
    private final Label selectedKeyLabel = new Label("Nenhum parâmetro seleccionado");
    private final Label selectedValueLabel = new Label("Seleccione um parâmetro para ver os detalhes.");
    private final Label selectedMetaLabel = new Label("");
    private final Label protectedLabel = new Label("0 protegidos");
    private final Label groupCountLabel = new Label("0 grupos");
    private final Label scopeCountLabel = new Label("0 âmbitos");

    public ParametrosSistemaView(ParametroSistemaRepository parametroRepository,
                                 EmpresaRepository empresaRepository,
                                 PersistenceService persistenceService,
                                 SessionManager sessionManager,
                                 ModalManager modalManager,
                                 AuditService auditService,
                                 SecurityService securityService) {
        this.parametroRepository = parametroRepository;
        this.empresaRepository = empresaRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.auditService = auditService;
        this.securityService = securityService;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();

        Platform.runLater(() -> loadEmpresaScopes());
    }

    private void buildUi() {
        setSpacing(0);
        getStyleClass().addAll("application-view", "kubata-parameters-page");

        VBox hero = new VBox(12);
        hero.setPadding(new Insets(18, 20, 14, 20));
        hero.getStyleClass().add("kubata-parameters-hero");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-parameters-hero-icon");
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.getChildren().add(IconUtils.icon(Feather.SETTINGS, 22));

        VBox titleBox = new VBox(3);
        Label eyebrow = new Label("INÍCIO · CONFIGURAÇÃO CENTRAL");
        eyebrow.getStyleClass().add("kubata-parameters-eyebrow");

        Label title = new Label("Parâmetros do Sistema");
        title.getStyleClass().add("kubata-parameters-title");

        Label subtitle = new Label(
                "Configure parâmetros globais ou por empresa com validação, controlo de permissões e auditoria."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-parameters-subtitle");
        titleBox.getChildren().addAll(eyebrow, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> reload());

        Button novo = new Button(
                "Novo parâmetro",
                IconUtils.icon(Feather.PLUS, 13)
        );
        novo.getStyleClass().add("button-primary");
        novo.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.CRIAR)) {
                showPermissionDenied("PARAMETROS/CRIAR");
                return;
            }
            editParametro(null);
        });

        top.getChildren().addAll(iconBox, titleBox, spacer, refresh, novo);

        HBox context = new HBox(8);
        context.setAlignment(Pos.CENTER_LEFT);
        context.getStyleClass().add("kubata-parameters-contextbar");

        Label contextTitle = new Label(
                "ÂMBITO ACTUAL",
                IconUtils.icon(Feather.LAYERS, 11)
        );
        contextTitle.getStyleClass().add("kubata-parameters-context-title");

        scopeCombo.setPrefWidth(285);
        scopeCombo.setPromptText("Seleccionar âmbito");
        scopeCombo.setOnAction(e -> reload());

        Region contextSpacer = new Region();
        HBox.setHgrow(contextSpacer, Priority.ALWAYS);

        Label audit = new Label(
                "ALTERAÇÕES AUDITADAS · PARÂMETROS PROTEGIDOS NÃO PODEM SER EDITADOS",
                IconUtils.icon(Feather.SHIELD, 10)
        );
        audit.getStyleClass().add("kubata-parameters-audit-note");

        context.getChildren().addAll(contextTitle, scopeCombo, contextSpacer, audit);
        hero.getChildren().addAll(top, context);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14, 20, 20, 20));
        content.setFillWidth(true);

        content.getChildren().add(buildSummaryCards());

        HBox filters = buildFilters();
        content.getChildren().add(filters);

        VBox tableSection = new VBox(9);
        tableSection.getStyleClass().add("kubata-parameters-table-panel");
        tableSection.setPadding(new Insets(12));

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox heading = new VBox(2);
        Label tableTitle = new Label("Catálogo de parâmetros");
        tableTitle.getStyleClass().add("kubata-parameters-section-title");

        Label tableSubtitle = new Label(
                "Pesquise por chave, valor, grupo ou descrição. Duplo clique abre a edição quando permitido."
        );
        tableSubtitle.getStyleClass().add("kubata-parameters-section-note");
        heading.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);

        tableHeader.getChildren().addAll(heading, tableSpacer, totalLabel);

        tableSection.getChildren().addAll(tableHeader, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        content.getChildren().add(tableSection);
        VBox.setVgrow(content, Priority.ALWAYS);

        getChildren().addAll(hero, content);
    }

    private HBox buildFilters() {
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("kubata-parameters-filterbar");

        Label searchLabel = new Label("Pesquisa");
        searchLabel.getStyleClass().add("kubata-parameters-filter-label");

        searchField.setPromptText("Chave, valor, grupo ou descrição...");
        searchField.setPrefWidth(330);
        searchField.getStyleClass().add("kubata-parameters-search");

        Label groupLabel = new Label("Grupo");
        groupLabel.getStyleClass().add("kubata-parameters-filter-label");

        groupFilter.setPromptText("Todos os grupos");
        groupFilter.setPrefWidth(190);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            groupFilter.setValue("Todos os grupos");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        editableLabel.getStyleClass().add("kubata-parameters-counter");
        protectedLabel.getStyleClass().add("kubata-parameters-counter");
        scopeCountLabel.getStyleClass().add("kubata-parameters-counter");

        filters.getChildren().addAll(searchLabel, searchField, groupLabel, groupFilter, clear, spacer);
        return filters;
    }

    private HBox buildSummaryCards() {
        HBox row = new HBox(10);
        row.getChildren().addAll(
                parameterMetric("PARÂMETROS VISÍVEIS", totalLabel, Feather.LIST),
                parameterMetric("EDITÁVEIS", editableLabel, Feather.EDIT_3),
                parameterMetric("PROTEGIDOS", protectedLabel, Feather.LOCK),
                parameterMetric("GRUPOS", groupCountLabel, Feather.TAG),
                parameterMetric("ÂMBITOS", scopeCountLabel, Feather.BRIEFCASE)
        );
        for (javafx.scene.Node node : row.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        return row;
    }

    private VBox parameterMetric(String caption, Label value, Feather icon) {
        HBox line = new HBox(9);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-parameters-metric-icon");
        iconBox.setPrefSize(34, 34);
        iconBox.setMinSize(34, 34);
        iconBox.setMaxSize(34, 34);
        iconBox.getChildren().add(IconUtils.icon(icon, 14));

        VBox text = new VBox(1);
        Label label = new Label(caption);
        label.getStyleClass().add("kubata-parameters-metric-caption");
        value.getStyleClass().add("kubata-parameters-metric-value");
        text.getChildren().addAll(label, value);

        line.getChildren().addAll(iconBox, text);

        VBox card = new VBox(line);
        card.getStyleClass().add("kubata-parameters-metric");
        return card;
    }

    private VBox buildDetailsPane() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.INFO, 14));
        selectedKeyLabel.getStyleClass().add("h4");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEditDetails = new Button(
                "Editar seleccionado",
                IconUtils.icon(Feather.EDIT, 12)
        );
        btnEditDetails.getStyleClass().add("button-outlined");
        btnEditDetails.setOnAction(e -> {
            ParametroSistema selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showWarning("Seleccione um parâmetro.");
                return;
            }
            if (!Boolean.TRUE.equals(selected.getEditavel())) {
                showWarning("Este parâmetro está protegido.");
                return;
            }
            if (!can(PermissaoPerfil.Operacao.EDITAR)) {
                showPermissionDenied("PARAMETROS/EDITAR");
                return;
            }
            editParametro(selected);
        });

        header.getChildren().addAll(icon, selectedKeyLabel, spacer, btnEditDetails);

        selectedValueLabel.setWrapText(true);
        selectedMetaLabel.setWrapText(true);
        selectedMetaLabel.getStyleClass().add("text-muted");

        card.getChildren().addAll(
                header,
                new Separator(),
                selectedValueLabel,
                selectedMetaLabel
        );

        return card;
    }

    private void loadEmpresaScopes() {
        persistenceService.executeAsync(
                () -> {
                    List<Empresa> empresas = empresaRepository.findAll();
                    Platform.runLater(() -> {
                        EmpresaScope previous = scopeCombo.getSelectionModel().getSelectedItem();

                        scopeCombo.getItems().clear();
                        scopeCombo.getItems().add(
                                new EmpresaScope(null, "Global (todas as empresas)")
                        );

                        empresas.stream()
                                .filter(Objects::nonNull)
                                .sorted((a, b) -> nullToEmpty(a.getNome())
                                        .compareToIgnoreCase(nullToEmpty(b.getNome())))
                                .forEach(e -> scopeCombo.getItems().add(
                                        new EmpresaScope(e.getId(), e.getNome())
                                ));

                        if (previous == null) {
                            scopeCombo.getSelectionModel().selectFirst();
                        } else {
                            scopeCombo.getItems().stream()
                                    .filter(item -> Objects.equals(item.empresaId, previous.empresaId))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            scopeCombo.getSelectionModel()::select,
                                            () -> scopeCombo.getSelectionModel().selectFirst()
                                    );
                        }

                        reload();
                    });
                },
                "PARAMETROS_SCOPE_READ",
                "PARAMETRO_SISTEMA",
                "Carregamento dos âmbitos reais das empresas",
                null
        );
    }

    private void reload() {
        EmpresaScope scope = scopeCombo.getSelectionModel().getSelectedItem();
        if (scope == null) {
            return;
        }

        persistenceService.executeAsync(() -> {
            try {
                List<ParametroSistema> list = scope.empresaId == null
                        ? parametroRepository.findAllByEmpresaIsNullOrderByGrupoAscChaveAsc()
                        : parametroRepository.findAllByEmpresa_IdOrderByGrupoAscChaveAsc(scope.empresaId);

                Platform.runLater(() -> {
                    data.setAll(list);
                    refreshGroupFilter();
                    applyFilters();
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        modalManager.alert(
                                "Erro ao carregar parâmetros",
                                safeMessage(ex),
                                "error",
                                ex
                        )
                );
            }
        }, "PARAMETROS_READ", "PARAMETRO_SISTEMA",
                "Carregamento de parâmetros do sistema", null);
    }

    private void refreshGroupFilter() {
        String previous = groupFilter.getValue();

        List<String> groups = data.stream()
                .map(ParametroSistema::getGrupo)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        groupFilter.getItems().clear();
        groupFilter.getItems().add("Todos os grupos");
        groupFilter.getItems().addAll(groups);

        if (previous != null && groupFilter.getItems().contains(previous)) {
            groupFilter.setValue(previous);
        } else {
            groupFilter.setValue("Todos os grupos");
        }
    }

    private void applyFilters() {
        String search = Optional.ofNullable(searchField.getText())
                .orElse("")
                .trim()
                .toLowerCase(Locale.ROOT);

        String group = groupFilter.getValue();
        boolean allGroups = group == null
                || group.isBlank()
                || "Todos os grupos".equalsIgnoreCase(group);

        table.setFilter(parameter -> {
            if (parameter == null) {
                return false;
            }

            boolean groupMatches = allGroups
                    || group.equalsIgnoreCase(nullToEmpty(parameter.getGrupo()).trim());

            if (!groupMatches) {
                return false;
            }

            if (search.isBlank()) {
                return true;
            }

            return contains(parameter.getChave(), search)
                    || contains(parameter.getValor(), search)
                    || contains(parameter.getGrupo(), search)
                    || contains(parameter.getDescricao(), search);
        });

        updateCounters();
    }

    private void updateCounters() {
        int total = table.getItems().size();

        long editable = table.getItems().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEditavel()))
                .count();

        long protectedCount = total - editable;

        long groups = data.stream()
                .map(ParametroSistema::getGrupo)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .count();

        int scopes = scopeCombo.getItems().size();

        totalLabel.setText(total + (total == 1 ? " parâmetro" : " parâmetros"));
        editableLabel.setText(String.valueOf(editable));
        protectedLabel.setText(String.valueOf(protectedCount));
        groupCountLabel.setText(String.valueOf(groups));
        scopeCountLabel.setText(String.valueOf(scopes));
    }

    private void updateDetails(ParametroSistema selected) {
        if (selected == null) {
            selectedKeyLabel.setText("Nenhum parâmetro seleccionado");
            selectedValueLabel.setText("Seleccione um parâmetro para ver os detalhes.");
            selectedMetaLabel.setText("");
            return;
        }

        selectedKeyLabel.setText(nullToEmpty(selected.getChave()));
        selectedValueLabel.setText(
                "Valor: " + (
                        selected.getValor() == null || selected.getValor().isBlank()
                                ? "—"
                                : selected.getValor()
                )
        );

        EmpresaScope selectedScope = scopeCombo.getSelectionModel().getSelectedItem();
        String scope = selectedScope == null || selectedScope.empresaId == null
                ? "Global"
                : "Empresa: " + selectedScope.label;

        String updated = formatDateTime(selected.getAtualizadoEm());
        String user = nullToEmpty(selected.getAtualizadoPor());

        selectedMetaLabel.setText(
                "Tipo: " + nullToEmpty(selected.getTipoValor())
                        + "   •   Grupo: " + nullToEmpty(selected.getGrupo())
                        + "   •   " + scope
                        + "   •   Editável: " + (
                        Boolean.TRUE.equals(selected.getEditavel()) ? "Sim" : "Não")
                        + "   •   Última actualização: " + updated
                        + (user.isBlank() ? "" : "   •   Por: " + user)
        );
    }

    private void editParametro(ParametroSistema existing) {
        EmpresaScope scope = scopeCombo.getSelectionModel().getSelectedItem();
        if (scope == null) {
            showWarning("Seleccione um âmbito antes de continuar.");
            return;
        }


        TextField keyField = new TextField();
        keyField.setPromptText("Ex.: IVA_PADRAO");
        keyField.setPrefWidth(350);

        TextField valueField = new TextField();
        valueField.setPromptText("Valor do parâmetro");
        valueField.setPrefWidth(350);

        ComboBox<String> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(TYPES)
        );
        typeCombo.setPrefWidth(180);

        ComboBox<String> booleanCombo = new ComboBox<>(
                FXCollections.observableArrayList("true", "false")
        );
        booleanCombo.setPrefWidth(180);

        TextField groupField = new TextField();
        groupField.setPromptText("Ex.: SISTEMA, FISCAL, FINANCEIRO");
        groupField.setPrefWidth(350);

        TextArea descField = new TextArea();
        descField.setPromptText("Descrição funcional do parâmetro");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setPrefWidth(350);

        CheckBox editableCheck = new CheckBox("Permitir edição deste parâmetro");
        editableCheck.setSelected(true);

        if (existing != null) {
            keyField.setText(existing.getChave());
            keyField.setDisable(true);

            valueField.setText(existing.getValor());
            typeCombo.setValue(
                    TYPES.contains(existing.getTipoValor())
                            ? existing.getTipoValor()
                            : "STRING"
            );
            groupField.setText(existing.getGrupo());
            descField.setText(existing.getDescricao());
            editableCheck.setSelected(Boolean.TRUE.equals(existing.getEditavel()));
        } else {
            typeCombo.setValue("STRING");
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(4));

        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(100);

        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);

        grid.add(new Label("Chave"), 0, 0);
        grid.add(keyField, 1, 0);

        grid.add(new Label("Tipo"), 0, 1);
        grid.add(typeCombo, 1, 1);

        grid.add(new Label("Valor"), 0, 2);
        grid.add(valueField, 1, 2);

        grid.add(new Label("Valor booleano"), 0, 3);
        grid.add(booleanCombo, 1, 3);
        booleanCombo.setVisible(false);
        booleanCombo.setManaged(false);

        grid.add(new Label("Grupo"), 0, 4);
        grid.add(groupField, 1, 4);

        grid.add(new Label("Descrição"), 0, 5);
        grid.add(descField, 1, 5);

        grid.add(editableCheck, 1, 6);

        Runnable updateValueControl = () -> {
            boolean isBoolean = "BOOLEAN".equals(typeCombo.getValue());

            booleanCombo.setVisible(isBoolean);
            booleanCombo.setManaged(isBoolean);

            valueField.setDisable(isBoolean);

            if (isBoolean) {
                String current = valueField.getText();
                if ("true".equalsIgnoreCase(current) || "false".equalsIgnoreCase(current)) {
                    booleanCombo.setValue(current.toLowerCase(Locale.ROOT));
                } else if (booleanCombo.getValue() == null) {
                    booleanCombo.setValue("false");
                }
            }
        };

        typeCombo.setOnAction(e -> updateValueControl.run());

        if ("BOOLEAN".equals(typeCombo.getValue())) {
            booleanCombo.setValue(
                    "true".equalsIgnoreCase(valueField.getText()) ? "true" : "false"
            );
        }
        updateValueControl.run();

        Label hint = new Label(
                "Tipos suportados: STRING, INTEGER, DECIMAL, BOOLEAN e DATE. "
                        + "Datas devem usar o formato YYYY-MM-DD."
        );
        hint.getStyleClass().add("text-muted");
        hint.setWrapText(true);

        VBox form = new VBox(10, hint, grid);
        form.setPrefWidth(560);

        modalManager.showConfirmModal(
                form,
                existing == null ? "Novo parâmetro do sistema" : "Editar parâmetro do sistema",
                () -> saveFromForm(
                        existing,
                        scope,
                        keyField,
                        valueField,
                        typeCombo,
                        booleanCombo,
                        groupField,
                        descField,
                        editableCheck
                ),
                null
        );
    }

    private void saveFromForm(ParametroSistema existing,
                               EmpresaScope scope,
                               TextField keyField,
                               TextField valueField,
                               ComboBox<String> typeCombo,
                               ComboBox<String> booleanCombo,
                               TextField groupField,
                               TextArea descField,
                               CheckBox editableCheck) {

        if (existing != null && !Boolean.TRUE.equals(existing.getEditavel())) {
            showWarning("O parâmetro seleccionado está protegido e não pode ser alterado.");
            return;
        }

        String key = nullToEmpty(keyField.getText()).trim().toUpperCase(Locale.ROOT);
        String type = typeCombo.getValue();
        String value = "BOOLEAN".equals(type)
                ? booleanCombo.getValue()
                : nullToEmpty(valueField.getText()).trim();

        String group = nullToEmpty(groupField.getText()).trim().toUpperCase(Locale.ROOT);
        String description = nullToEmpty(descField.getText()).trim();

        if (!validateForm(key, value, type, group)) {
            return;
        }

        final Map<String, String> before = existing != null
                ? snapshot(existing, scope)
                : null;

        final String operationDescription =
                (existing == null ? "Criação" : "Alteração")
                        + " do parâmetro " + key;

        persistenceService.executeAsync(
                () -> {
                    try {
                        if (existing == null) {
                            if (scope.empresaId == null
                                    && parametroRepository.findByChaveAndEmpresaIdIsNull(key).isPresent()) {
                                throw new IllegalStateException(
                                        "Já existe um parâmetro global com a chave: " + key
                                );
                            }

                            if (scope.empresaId != null
                                    && parametroRepository.findByEmpresa_IdAndChave(
                                    scope.empresaId, key).isPresent()) {
                                throw new IllegalStateException(
                                        "Já existe um parâmetro nesta empresa com a chave: " + key
                                );
                            }
                        }

                        ParametroSistema target = existing != null
                                ? existing
                                : ParametroSistema.builder().build();

                        if (existing == null) {
                            target.setChave(key);

                            Empresa empresa = scope.empresaId == null
                                    ? null
                                    : empresaRepository.findById(scope.empresaId)
                                            .orElseThrow(() -> new IllegalStateException(
                                                    "A empresa seleccionada já não existe."
                                            ));

                            target.setEmpresa(empresa);
                        }

                        target.setValor(value);
                        target.setTipoValor(type);
                        target.setGrupo(group.isBlank() ? null : group);
                        target.setDescricao(description.isBlank() ? null : description);
                        target.setEditavel(editableCheck.isSelected());
                        target.setAtualizadoEm(LocalDateTime.now());

                        var user = sessionManager.getUser();
                        target.setAtualizadoPor(
                                truncate(
                                        user != null && user.getEmail() != null
                                                ? user.getEmail()
                                                : user != null && user.getNome() != null
                                                        ? user.getNome()
                                                        : "SYSTEM",
                                        50
                                )
                        );

                        ParametroSistema saved = parametroRepository.save(target);
                        auditChange(before, saved, scope.empresaId);
                    } catch (RuntimeException ex) {
                        throw ex;
                    }
                },
                "PARAMETRO_SAVE",
                "PARAMETRO_SISTEMA",
                operationDescription,
                this::reload
        );
    }

    private boolean validateForm(String key,
                                 String value,
                                 String type,
                                 String group) {
        if (key.isBlank()) {
            showWarning("A chave do parâmetro é obrigatória.");
            return false;
        }

        if (!key.matches("[A-Z][A-Z0-9_.-]{1,99}")) {
            showWarning(
                    "A chave deve começar por uma letra e conter apenas "
                            + "A-Z, 0-9, ponto, hífen ou underscore."
            );
            return false;
        }

        if (type == null || !TYPES.contains(type)) {
            showWarning("Seleccione um tipo de valor válido.");
            return false;
        }

        if (value.length() > 1000) {
            showWarning("O valor não pode ultrapassar 1000 caracteres.");
            return false;
        }

        try {
            switch (type) {
                case "INTEGER" -> Integer.parseInt(value);
                case "DECIMAL" -> new BigDecimal(value.replace(',', '.'));
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException("boolean");
                    }
                }
                case "DATE" -> LocalDate.parse(value);
                case "STRING" -> {
                    // Qualquer texto até ao limite da coluna.
                }
                default -> throw new IllegalArgumentException("tipo");
            }
        } catch (NumberFormatException | DateTimeParseException ex) {
            showWarning(
                    switch (type) {
                        case "INTEGER" -> "O valor deve ser um número inteiro válido.";
                        case "DECIMAL" -> "O valor deve ser um número decimal válido.";
                        case "DATE" -> "A data deve estar no formato YYYY-MM-DD.";
                        default -> "O valor introduzido não é válido.";
                    }
            );
            return false;
        } catch (IllegalArgumentException ex) {
            showWarning("O valor booleano deve ser true ou false.");
            return false;
        }

        if (group.length() > 50) {
            showWarning("O grupo não pode ultrapassar 50 caracteres.");
            return false;
        }

        return true;
    }

    private void deleteSelected() {
        ParametroSistema selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Seleccione um parâmetro para remover.");
            return;
        }

        deleteParameter(selected);
    }

    private void auditChange(Map<String, String> before,
                             ParametroSistema saved,
                             Long empresaId) {
        var user = sessionManager.getUser();
        auditService.logAction(
                user,
                user != null && user.getNome() != null ? user.getNome() : "SYSTEM",
                AuditLog.AuditActionType.CONFIG_CHANGE,
                "PARAMETRO_SISTEMA",
                String.valueOf(saved.getId()),
                "Parâmetro " + saved.getChave(),
                before,
                snapshot(saved, empresaId),
                "ADMINISTRATOR",
                localAddress(),
                null,
                null,
                true,
                AuditLog.AGTComplianceLevel.HIGH
        );
    }

    private void auditDelete(ParametroSistema deleted,
                             Map<String, String> before) {
        var user = sessionManager.getUser();
        auditService.logAction(
                user,
                user != null && user.getNome() != null ? user.getNome() : "SYSTEM",
                AuditLog.AuditActionType.CONFIG_CHANGE,
                "PARAMETRO_SISTEMA",
                String.valueOf(deleted.getId()),
                "Remoção do parâmetro " + deleted.getChave(),
                before,
                null,
                "ADMINISTRATOR",
                localAddress(),
                null,
                null,
                true,
                AuditLog.AGTComplianceLevel.HIGH
        );
    }

    private void editFromContext(ParametroSistema parameter) {
        if (parameter == null) {
            return;
        }
        if (!can(PermissaoPerfil.Operacao.EDITAR)) {
            showPermissionDenied("PARAMETROS/EDITAR");
            return;
        }
        if (!Boolean.TRUE.equals(parameter.getEditavel())) {
            showWarning("O parâmetro seleccionado está protegido e não pode ser editado.");
            return;
        }
        editParametro(parameter);
    }

    private void deleteFromContext(ParametroSistema parameter) {
        if (parameter == null) {
            return;
        }
        if (!can(PermissaoPerfil.Operacao.APAGAR) && !isElevated()) {
            showPermissionDenied("PARAMETROS/APAGAR");
            return;
        }
        if (!Boolean.TRUE.equals(parameter.getEditavel())) {
            showWarning("Este parâmetro está protegido e não pode ser removido.");
            return;
        }

        performDelete(parameter);
    }

    private void deleteParameter(ParametroSistema selected) {
        if (!Boolean.TRUE.equals(selected.getEditavel())) {
            showWarning("Este parâmetro está protegido e não pode ser removido.");
            return;
        }

        String key = nullToEmpty(selected.getChave());

        modalManager.showConfirmModal(
                new VBox(
                        8,
                        new Label("Tem a certeza que pretende remover este parâmetro?"),
                        new Label("Chave: " + key),
                        new Label(
                                "Esta operação remove o registo da base de dados e "
                                        + "fica registada na auditoria."
                        )
                ),
                "Remover parâmetro",
                () -> performDelete(selected),
                null
        );
    }

    private void performDelete(ParametroSistema selected) {
        EmpresaScope selectedScope = scopeCombo.getSelectionModel().getSelectedItem();
        String key = nullToEmpty(selected.getChave());
        Map<String, String> before = snapshot(selected, selectedScope);

        persistenceService.deleteAsync(
                parametroRepository,
                selected,
                selected.getId(),
                "PARAMETRO_SISTEMA",
                "Remoção do parâmetro " + key,
                () -> {
                    auditDelete(selected, before);
                    reload();
                }
        );
    }

    private boolean can(PermissaoPerfil.Operacao operation) {
        var user = sessionManager.getUser();
        if (user == null) {
            return false;
        }

        if (isElevated()) {
            return true;
        }

        return securityService.hasPermission(
                user,
                "ADMINISTRATOR",
                "PARAMETROS",
                operation
        );
    }

    private boolean isElevated() {
        var user = sessionManager.getUser();
        return user != null
                && (user.getRole() == Role.ADMIN || user.isSuperadmin());
    }

    private void showPermissionDenied(String permission) {
        modalManager.alert(
                "Permissão insuficiente",
                "Necessita da permissão " + permission + " ou de um papel administrativo.",
                "warning",
                null
        );
    }

    private void showWarning(String message) {
        modalManager.alert("Validação", message, "warning", null);
    }

    private static String safeMessage(Throwable ex) {
        return ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Ocorreu um erro ao processar a operação."
                : ex.getMessage();
    }

    private static String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "LOCAL";
        }
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null
                ? "—"
                : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String truncate(String value, int maxLength) {
        String normalized = nullToEmpty(value);
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

    private static Map<String, String> snapshot(ParametroSistema p, EmpresaScope scope) {
        Long empresaId = scope == null ? null : scope.empresaId;
        return snapshot(p, empresaId);
    }

    private static Map<String, String> snapshot(ParametroSistema p, Long empresaId) {
        Map<String, String> snapshot = new HashMap<>();
        snapshot.put("chave", p.getChave());
        snapshot.put("valor", p.getValor());
        snapshot.put("tipo", p.getTipoValor());
        snapshot.put("grupo", p.getGrupo());
        snapshot.put("descricao", p.getDescricao());
        snapshot.put("editavel", String.valueOf(p.getEditavel()));
        return snapshot;
    }

    private record EmpresaScope(Long empresaId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
