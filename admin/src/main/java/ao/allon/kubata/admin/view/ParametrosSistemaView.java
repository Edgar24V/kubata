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
        VBox toolbar = new VBox(10);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.getStyleClass().add("header-box");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(
                "Parâmetros do Sistema",
                IconUtils.icon(Feather.SETTINGS, 18)
        );
        title.getStyleClass().add("h3");

        Label scopeCaption = new Label("Âmbito:");
        scopeCaption.getStyleClass().add("text-muted");

        scopeCombo.setPrefWidth(250);
        scopeCombo.setPromptText("Seleccionar âmbito");
        scopeCombo.setOnAction(e -> reload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button(
                "Novo",
                IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-success");
        btnNovo.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.CRIAR)) {
                showPermissionDenied("PARAMETROS/CRIAR");
                return;
            }
            editParametro(null);
        });

        Button btnEditar = new Button(
                "Editar",
                IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL)
        );
        btnEditar.getStyleClass().add("button-primary");
        btnEditar.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.EDITAR)) {
                showPermissionDenied("PARAMETROS/EDITAR");
                return;
            }

            ParametroSistema selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showWarning("Seleccione um parâmetro para editar.");
                return;
            }
            if (!Boolean.TRUE.equals(selected.getEditavel())) {
                showWarning("O parâmetro seleccionado está protegido e não pode ser editado.");
                return;
            }

            editParametro(selected);
        });

        Button btnApagar = new Button(
                "Remover",
                IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL)
        );
        btnApagar.getStyleClass().add("button-danger");
        btnApagar.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.APAGAR) && !isElevated()) {
                showPermissionDenied("PARAMETROS/APAGAR");
                return;
            }
            deleteSelected();
        });

        Button btnRefresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> reload());

        top.getChildren().addAll(
                title,
                scopeCaption,
                scopeCombo,
                spacer,
                btnNovo,
                btnEditar,
                btnApagar,
                btnRefresh
        );

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Pesquisar por chave, valor, grupo ou descrição...");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        groupFilter.setPromptText("Todos os grupos");
        groupFilter.setPrefWidth(180);
        groupFilter.setOnAction(e -> applyFilters());

        filters.getChildren().addAll(
                new Label("Pesquisar:"),
                searchField,
                new Label("Grupo:"),
                groupFilter
        );

        HBox counters = new HBox(18);
        counters.setAlignment(Pos.CENTER_LEFT);
        totalLabel.getStyleClass().add("text-muted");
        editableLabel.getStyleClass().add("text-muted");
        counters.getChildren().addAll(totalLabel, editableLabel);

        toolbar.getChildren().addAll(top, filters, counters);

        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ParametroSistema, String> keyCol = TableUtils.createTextColumn(
                "Chave",
                c -> new SimpleStringProperty(nullToEmpty(c.getValue().getChave()))
        );
        keyCol.setPrefWidth(190);

        TableColumn<ParametroSistema, String> valueCol = TableUtils.createTextColumn(
                "Valor",
                c -> {
                    String value = c.getValue().getValor();
                    return new SimpleStringProperty(
                            value == null || value.isBlank() ? "—" : value
                    );
                }
        );
        valueCol.setPrefWidth(300);

        TableColumn<ParametroSistema, String> typeCol = TableUtils.createTextColumn(
                "Tipo",
                c -> new SimpleStringProperty(nullToEmpty(c.getValue().getTipoValor()))
        );
        typeCol.setPrefWidth(95);

        TableColumn<ParametroSistema, String> groupCol = TableUtils.createTextColumn(
                "Grupo",
                c -> new SimpleStringProperty(nullToEmpty(c.getValue().getGrupo()))
        );
        groupCol.setPrefWidth(135);

        TableColumn<ParametroSistema, String> editableCol = TableUtils.createTextColumn(
                "Editável",
                c -> new SimpleStringProperty(
                        Boolean.TRUE.equals(c.getValue().getEditavel()) ? "Sim" : "Não"
                )
        );
        editableCol.setPrefWidth(85);

        TableColumn<ParametroSistema, String> updatedCol = TableUtils.createTextColumn(
                "Actualizado em",
                c -> new SimpleStringProperty(formatDateTime(c.getValue().getAtualizadoEm()))
        );
        updatedCol.setPrefWidth(145);

        table.getColumns().addAll(
                keyCol,
                valueCol,
                typeCol,
                groupCol,
                editableCol,
                updatedCol
        );

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, selected) -> updateDetails(selected)
        );

        VBox details = buildDetailsPane();

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(toolbar, table, details);
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

        totalLabel.setText(total + (total == 1 ? " parâmetro" : " parâmetros"));
        editableLabel.setText(editable + (editable == 1 ? " editável" : " editáveis"));
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

        final Empresa empresaRef;
        if (scope.empresaId != null) {
            empresaRef = empresaRepository.findById(scope.empresaId).orElse(null);
            if (empresaRef == null) {
                showWarning("A empresa seleccionada já não está disponível.");
                return;
            }
        } else {
            empresaRef = null;
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
                        empresaRef,
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
                               Empresa empresaRef,
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

        if (existing == null) {
            if (scope.empresaId == null
                    && parametroRepository.findByChaveAndEmpresaIdIsNull(key).isPresent()) {
                showWarning("Já existe um parâmetro global com a chave: " + key);
                return;
            }

            if (scope.empresaId != null
                    && parametroRepository.findByEmpresa_IdAndChave(scope.empresaId, key).isPresent()) {
                showWarning("Já existe um parâmetro nesta empresa com a chave: " + key);
                return;
            }
        }

        ParametroSistema target = existing != null
                ? existing
                : ParametroSistema.builder().build();

        Map<String, String> before = existing != null
                ? snapshot(existing)
                : null;

        if (existing == null) {
            target.setChave(key);
            target.setEmpresa(empresaRef);
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

        final String descriptionForAudit =
                (existing == null ? "Criação" : "Alteração")
                        + " do parâmetro " + key;

        persistenceService.saveAsync(
                parametroRepository,
                target,
                "PARAMETRO_SISTEMA",
                descriptionForAudit,
                saved -> {
                    auditChange(existing, before, saved, scope.empresaId);
                    reload();
                }
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

    private void auditChange(ParametroSistema existing,
                             Map<String, String> before,
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
                             Map<String, String> before,
                             Long empresaId) {
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
        deleteParameter(parameter);
    }

    private void deleteParameter(ParametroSistema selected) {
        if (!Boolean.TRUE.equals(selected.getEditavel())) {
            showWarning("Este parâmetro está protegido e não pode ser removido.");
            return;
        }

        EmpresaScope selectedScope = scopeCombo.getSelectionModel().getSelectedItem();
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
                () -> {
                    Map<String, String> before = snapshot(selected, selectedScope);

                    persistenceService.deleteAsync(
                            parametroRepository,
                            selected,
                            selected.getId(),
                            "PARAMETRO_SISTEMA",
                            "Remoção do parâmetro " + key,
                            () -> {
                                auditDelete(selected, before,
                                        selectedScope == null ? null : selectedScope.empresaId);
                                reload();
                            }
                    );
                },
                null
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
