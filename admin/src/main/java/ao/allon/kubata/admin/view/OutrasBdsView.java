package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ConexaoAuxiliar;
import ao.allon.kubata.core.repository.ConexaoAuxiliarRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Central de Outras Bases de Dados do Kubata.
 *
 * <p>Permite gerir ligações JDBC auxiliares, testar disponibilidade,
 * consultar metadados básicos e manter o catálogo de conexões organizado.</p>
 */
@Component
public class OutrasBdsView extends VBox {

    private final ConexaoAuxiliarRepository repository;
    private final PersistenceService persistenceService;
    private final ModalManager modalManager;

    private final TableView<ConexaoAuxiliar> table = new TableView<>();
    private final javafx.collections.ObservableList<ConexaoAuxiliar> data =
            FXCollections.observableArrayList();

    private final Label totalValue = new Label("0");
    private final Label activeValue = new Label("0");
    private final Label inactiveValue = new Label("0");
    private final Label selectedState = new Label("Nenhuma ligação seleccionada");
    private final Label selectedMeta = new Label("Seleccione uma ligação para consultar o estado.");

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private Button btnEdit;
    private Button btnDelete;
    private Button btnTest;
    private Button btnToggle;

    public OutrasBdsView(
            ConexaoAuxiliarRepository repository,
            PersistenceService persistenceService,
            ModalManager modalManager
    ) {
        this.repository = repository;
        this.persistenceService = persistenceService;
        this.modalManager = modalManager;

        setSpacing(0);
        getStyleClass().add("kubata-other-db-page");

        buildUi();
        Platform.runLater(this::reload);
    }

    private void buildUi() {
        VBox header = buildHeader();
        HBox filters = buildFilters();
        BorderPane body = buildBody();
        HBox footer = buildFooter();

        getChildren().addAll(header, filters, body, footer);
        VBox.setVgrow(body, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-other-db-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-other-db-title-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.DATABASE, 22)));

        VBox titleBox = new VBox(2);
        Label title = new Label("Outras Bases de Dados");
        title.getStyleClass().add("kubata-other-db-title");

        Label subtitle = new Label(
                "Ligaçōes JDBC auxiliares para integração com sistemas externos, migração, consulta e interoperabilidade."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-other-db-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newButton = new Button(
                "Nova ligação",
                IconUtils.icon(Feather.PLUS, 13)
        );
        newButton.getStyleClass().add("button-primary");
        newButton.setOnAction(e -> edit(null));

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> reload());

        titleLine.getChildren().addAll(icon, titleBox, spacer, newButton, refresh);

        HBox kpis = new HBox(
                10,
                kpi("LIGAÇÕES", Feather.DATABASE, totalValue),
                kpi("ACTIVAS", Feather.CHECK_CIRCLE, activeValue),
                kpi("INACTIVAS", Feather.PAUSE_CIRCLE, inactiveValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.setMinWidth(150);
        card.setPadding(new Insets(9, 13, 9, 13));
        card.getStyleClass().add("kubata-other-db-kpi");

        HBox line = new HBox(7);
        line.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label("", IconUtils.icon(icon, 13));
        ico.getStyleClass().add("kubata-other-db-kpi-icon");

        Label text = new Label(title);
        text.getStyleClass().add("kubata-other-db-kpi-title");

        value.getStyleClass().add("kubata-other-db-kpi-value");
        line.getChildren().addAll(ico, text);
        card.getChildren().addAll(line, value);
        return card;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-other-db-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome, descrição, URL, utilizador ou driver...");
        searchField.setPrefWidth(370);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todas",
                "Activas",
                "Inactivas"
        ));
        statusFilter.setValue("Todas");
        statusFilter.setPrefWidth(130);
        statusFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button(
                "Limpar",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("Todas");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Teste a ligação antes de a activar.");
        hint.getStyleClass().add("kubata-other-db-filter-hint");

        bar.getChildren().addAll(searchField, statusFilter, clear, spacer, hint);
        return bar;
    }

    private BorderPane buildBody() {
        TableView<ConexaoAuxiliar> view = table;
        view.setItems(data);
        view.setPlaceholder(new Label("Nenhuma ligação auxiliar encontrada."));
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.getStyleClass().add("kubata-other-db-table");

        TableColumn<ConexaoAuxiliar, String> name = new TableColumn<>("Ligação");
        name.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNome(), "—")));
        name.setPrefWidth(180);

        TableColumn<ConexaoAuxiliar, String> type = new TableColumn<>("Motor / JDBC");
        type.setCellValueFactory(c -> new SimpleStringProperty(
                detectDatabase(c.getValue().getJdbcUrl())
                        + " · " + safe(c.getValue().getDriverClass(), "Driver automático")
        ));
        type.setPrefWidth(220);

        TableColumn<ConexaoAuxiliar, String> host = new TableColumn<>("Destino");
        host.setCellValueFactory(c -> new SimpleStringProperty(summarizeUrl(c.getValue().getJdbcUrl())));
        host.setPrefWidth(235);

        TableColumn<ConexaoAuxiliar, String> user = new TableColumn<>("Utilizador");
        user.setCellValueFactory(c -> new SimpleStringProperty(
                safe(c.getValue().getUsername(), "integrado / vazio")
        ));
        user.setPrefWidth(150);

        TableColumn<ConexaoAuxiliar, String> status = new TableColumn<>("Estado");
        status.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActivo()) ? "Activa" : "Inactiva"
        ));
        status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll(
                        "kubata-other-db-badge",
                        "Activa".equals(item)
                                ? "kubata-other-db-badge-success"
                                : "kubata-other-db-badge-neutral"
                );
                setText(null);
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });
        status.setPrefWidth(105);

        view.getColumns().addAll(name, type, host, user, status);

        view.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> updateSelection(selected)
        );

        view.setRowFactory(tv -> {
            TableRow<ConexaoAuxiliar> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit(row.getItem());
                }
            });
            return row;
        });

        BorderPane center = new BorderPane();
        center.setCenter(view);

        VBox details = buildDetails();
        details.setPrefWidth(320);
        ScrollPane detailScroll = new ScrollPane(details);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.getStyleClass().add("kubata-other-db-detail-scroll");

        SplitPane split = new SplitPane(center, detailScroll);
        split.setDividerPositions(0.72);

        return new BorderPane(split);
    }

    private VBox buildDetails() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("kubata-other-db-details");

        Label title = new Label("Detalhes da ligação");
        title.getStyleClass().add("kubata-other-db-section-title");

        selectedState.getStyleClass().add("kubata-other-db-detail-title");
        selectedMeta.setWrapText(true);
        selectedMeta.getStyleClass().add("kubata-other-db-detail-text");

        VBox stateCard = new VBox(4, selectedState, selectedMeta);
        stateCard.getStyleClass().add("kubata-other-db-state-card");

        VBox actionBox = new VBox(7);
        Label actions = new Label("Operações");
        actions.getStyleClass().add("kubata-other-db-section-title");

        btnTest = actionButton("Testar ligação", Feather.ZAP);
        btnEdit = actionButton("Editar ligação", Feather.EDIT_2);
        btnToggle = actionButton("Activar / desactivar", Feather.POWER);
        btnDelete = actionButton("Remover ligação", Feather.TRASH_2);
        btnDelete.getStyleClass().add("button-danger-outlined");

        btnTest.setOnAction(e -> testSelected());
        btnEdit.setOnAction(e -> selected().ifPresent(this::edit));
        btnToggle.setOnAction(e -> toggleSelected());
        btnDelete.setOnAction(e -> deleteSelected());

        actionBox.getChildren().addAll(actions, btnTest, btnEdit, btnToggle, btnDelete);

        VBox security = new VBox(7);
        Label securityTitle = new Label("Segurança");
        securityTitle.getStyleClass().add("kubata-other-db-section-title");

        Label securityText = new Label(
                "Use contas de serviço com privilégio mínimo. A credencial existente é armazenada "
                        + "no formato suportado actualmente pela aplicação; Base64 não substitui cifragem forte ou um secret store."
        );
        securityText.setWrapText(true);
        securityText.getStyleClass().add("kubata-other-db-warning");

        security.getChildren().addAll(securityTitle, securityText);

        root.getChildren().addAll(
                title,
                stateCard,
                new Separator(),
                actionBox,
                new Separator(),
                security
        );

        updateSelection(null);
        return root;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 14, 8, 14));
        footer.getStyleClass().add("kubata-other-db-footer");

        Label left = new Label("Integrações JDBC auxiliares · último carregamento: —");
        left.setId("otherDbFooterStatus");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label("Evite guardar credenciais administrativas em ligações auxiliares.");
        right.getStyleClass().add("kubata-other-db-footer-hint");

        footer.getChildren().addAll(left, spacer, right);
        return footer;
    }

    private Button actionButton(String text, Feather icon) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(32);
        button.getStyleClass().add("button-outlined");
        return button;
    }

    private void reload() {
        try {
            List<ConexaoAuxiliar> rows = repository.findAll()
                    .stream()
                    .sorted(Comparator.comparing(
                            c -> safe(c.getNome(), ""),
                            String.CASE_INSENSITIVE_ORDER
                    ))
                    .toList();

            data.setAll(rows);
            updateKpis();
            applyFilters();

            if (table.getSelectionModel().getSelectedItem() == null && !data.isEmpty()) {
                table.getSelectionModel().selectFirst();
            }

            Label footer = (Label) lookup("#otherDbFooterStatus");
            if (footer != null) {
                footer.setText(data.size() + " ligação(ões) carregada(s) · " + LocalDateTime.now().toString().replace('T', ' '));
            }
        } catch (Exception ex) {
            modalManager.alert(
                    "Erro ao carregar",
                    message(ex, "Não foi possível carregar as ligações auxiliares."),
                    "error",
                    ex
            );
        }
    }

    private void updateKpis() {
        totalValue.setText(String.valueOf(data.size()));
        activeValue.setText(String.valueOf(
                data.stream().filter(c -> Boolean.TRUE.equals(c.getActivo())).count()
        ));
        inactiveValue.setText(String.valueOf(
                data.stream().filter(c -> !Boolean.TRUE.equals(c.getActivo())).count()
        ));
    }

    private void applyFilters() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String status = statusFilter == null ? "Todas" : statusFilter.getValue();

        table.setItems(data.filtered(c -> {
            boolean text = query.isBlank()
                    || contains(c.getNome(), query)
                    || contains(c.getDescricao(), query)
                    || contains(c.getJdbcUrl(), query)
                    || contains(c.getUsername(), query)
                    || contains(c.getDriverClass(), query);

            boolean state = switch (status) {
                case "Activas" -> Boolean.TRUE.equals(c.getActivo());
                case "Inactivas" -> !Boolean.TRUE.equals(c.getActivo());
                default -> true;
            };

            return text && state;
        }));
    }

    private void updateSelection(ConexaoAuxiliar c) {
        boolean enabled = c != null;
        if (btnTest != null) btnTest.setDisable(!enabled);
        if (btnEdit != null) btnEdit.setDisable(!enabled);
        if (btnToggle != null) btnToggle.setDisable(!enabled);
        if (btnDelete != null) btnDelete.setDisable(!enabled);

        if (!enabled) {
            selectedState.setText("Nenhuma ligação seleccionada");
            selectedMeta.setText("Seleccione uma ligação para consultar o estado, testar ou editar.");
            return;
        }

        selectedState.setText(safe(c.getNome(), "Ligação"));
        selectedMeta.setText(
                (Boolean.TRUE.equals(c.getActivo()) ? "Activa" : "Inactiva")
                        + " · " + detectDatabase(c.getJdbcUrl())
                        + " · " + summarizeUrl(c.getJdbcUrl())
        );
    }

    private Optional<ConexaoAuxiliar> selected() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private void edit(ConexaoAuxiliar existing) {
        TextField nome = new TextField();
        TextField url = new TextField();
        TextField user = new TextField();
        PasswordField pass = new PasswordField();
        TextField driver = new TextField();
        TextArea desc = new TextArea();
        CheckBox activo = new CheckBox("Ligação disponível para utilização");

        ComboBox<String> preset = new ComboBox<>(FXCollections.observableArrayList(
                "Automático",
                "PostgreSQL",
                "MySQL / MariaDB",
                "Microsoft SQL Server",
                "H2",
                "Oracle"
        ));
        preset.setValue("Automático");

        activo.setSelected(true);
        desc.setPrefRowCount(3);
        url.setPromptText("jdbc:postgresql://servidor:5432/base");
        user.setPromptText("utilizador de serviço");
        pass.setPromptText("Password da ligação");

        if (existing != null) {
            nome.setText(safe(existing.getNome(), ""));
            url.setText(safe(existing.getJdbcUrl(), ""));
            user.setText(safe(existing.getUsername(), ""));
            driver.setText(safe(existing.getDriverClass(), ""));
            desc.setText(safe(existing.getDescricao(), ""));
            activo.setSelected(Boolean.TRUE.equals(existing.getActivo()));

            if (existing.getPasswordEnc() != null && !existing.getPasswordEnc().isBlank()) {
                try {
                    pass.setText(new String(Base64.getDecoder().decode(existing.getPasswordEnc())));
                } catch (IllegalArgumentException ignored) {
                    // Credencial existente não é decodificável pelo formato actual.
                }
            }
        }

        preset.valueProperty().addListener((obs, old, selectedPreset) -> {
            if ("Automático".equals(selectedPreset)) return;
            String generated = driverFor(selectedPreset);
            if (generated != null) driver.setText(generated);
        });

        VBox root = new VBox(14);
        root.setPadding(new Insets(4));

        root.getChildren().add(section(
                Feather.DATABASE,
                "Ligação JDBC",
                "Configure o destino externo, o driver e a conta de serviço usada pelo Kubata."
        ));

        GridPane main = formGrid();
        addRow(main, 0, "Nome:*", nome);
        addRow(main, 1, "Motor:", preset);
        addRow(main, 2, "JDBC URL:*", url);
        addRow(main, 3, "Utilizador:", user);
        addRow(main, 4, "Password:", pass);
        addRow(main, 5, "Driver:", driver);
        addRow(main, 6, "Descrição:", desc);

        VBox state = new VBox(6);
        state.getStyleClass().add("kubata-other-db-form-card");
        state.getChildren().addAll(
                activo,
                new Label("Teste a conexão antes de a marcar como activa."),
                new Label("Boas práticas: base de dados dedicada, privilégios mínimos e credenciais próprias do serviço.")
        );
        state.getChildren().get(1).getStyleClass().add("kubata-other-db-form-note");
        state.getChildren().get(2).getStyleClass().add("kubata-other-db-form-note");

        root.getChildren().addAll(main, state);

        modalManager.showConfirmModal(
                root,
                existing == null ? "Nova ligação auxiliar" : "Editar ligação auxiliar",
                () -> {
                    try {
                        if (nome.getText().isBlank() || url.getText().isBlank()) {
                            throw new IllegalArgumentException("Nome e JDBC URL são obrigatórios.");
                        }

                        ConexaoAuxiliar target = existing != null
                                ? existing
                                : ConexaoAuxiliar.builder().build();

                        if (existing == null) {
                            target.setCriadoEm(LocalDateTime.now());
                        }

                        target.setNome(nome.getText().trim());
                        target.setJdbcUrl(url.getText().trim());
                        target.setUsername(user.getText().isBlank() ? null : user.getText().trim());
                        target.setPasswordEnc(
                                pass.getText().isBlank()
                                        ? null
                                        : Base64.getEncoder().encodeToString(pass.getText().getBytes())
                        );
                        target.setDriverClass(driver.getText().isBlank() ? null : driver.getText().trim());
                        target.setDescricao(desc.getText().isBlank() ? null : desc.getText().trim());
                        target.setActivo(activo.isSelected());
                        target.setActualizadoEm(LocalDateTime.now());

                        persistenceService.saveAsync(
                                repository,
                                target,
                                "CONEXAO_AUXILIAR",
                                "Ligação auxiliar: " + target.getNome(),
                                saved -> Platform.runLater(this::reload)
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Dados inválidos",
                                message(ex, "Verifique os dados da ligação."),
                                "warning",
                                ex
                        );
                    }
                },
                null
        );
    }

    private VBox section(Feather icon, String title, String text) {
        VBox box = new VBox(4);
        box.getStyleClass().add("kubata-other-db-section");

        HBox line = new HBox(8);
        line.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("kubata-other-db-section-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-other-db-section-title");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("kubata-other-db-section-text");

        line.getChildren().addAll(iconLabel, titleLabel);
        box.getChildren().addAll(line, textLabel);
        return box;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(9);
        grid.getStyleClass().add("kubata-other-db-form-grid");

        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);

        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labels, values);

        return grid;
    }

    private void addRow(GridPane grid, int row, String label, Control control) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("kubata-other-db-field-label");
        control.setMaxWidth(Double.MAX_VALUE);

        grid.add(labelNode, 0, row);
        grid.add(control, 1, row);
    }

    private void testSelected() {
        selected().ifPresent(this::testConnection);
    }

    private void testConnection(ConexaoAuxiliar c) {
        modalManager.alert(
                "Teste de ligação",
                "A testar «" + safe(c.getNome(), "Ligação") + "»…",
                "info",
                null
        );

        persistenceService.executeAsync(
                () -> {
                    long start = System.nanoTime();
                    String pwd = decodePassword(c.getPasswordEnc());

                    try {
                        if (c.getDriverClass() != null && !c.getDriverClass().isBlank()) {
                            Class.forName(c.getDriverClass());
                        }

                        try (Connection connection = DriverManager.getConnection(
                                c.getJdbcUrl(),
                                Optional.ofNullable(c.getUsername()).orElse(""),
                                pwd
                        )) {
                            connection.setReadOnly(true);

                            String database = safe(connection.getMetaData().getDatabaseProductName(), "BD");
                            String version = safe(connection.getMetaData().getDatabaseProductVersion(), "—");
                            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();

                            Platform.runLater(() -> modalManager.alert(
                                    "Ligação bem-sucedida",
                                    database + " · " + version + "\n"
                                            + "Latência de abertura: " + elapsed + " ms\n"
                                            + "Catálogo: " + safe(c.getNome(), "Ligação"),
                                    "info",
                                    null
                            ));
                        }
                    } catch (Exception ex) {
                        Platform.runLater(() -> modalManager.alert(
                                "Falha de conectividade",
                                message(ex, "Não foi possível estabelecer a ligação JDBC.") + "\n\n"
                                        + "Verifique URL, driver, rede, utilizador e permissões.",
                                "error",
                                ex
                        ));
                    }
                },
                "READ",
                "CONEXAO_AUXILIAR",
                "Teste de conectividade: " + safe(c.getNome(), "Ligação"),
                null
        );
    }

    private void toggleSelected() {
        selected().ifPresent(c -> {
            c.setActivo(!Boolean.TRUE.equals(c.getActivo()));
            c.setActualizadoEm(LocalDateTime.now());

            persistenceService.saveAsync(
                    repository,
                    c,
                    "CONEXAO_AUXILIAR",
                    "Alteração de estado da ligação: " + c.getNome(),
                    saved -> Platform.runLater(this::reload)
            );
        });
    }

    private void deleteSelected() {
        selected().ifPresent(c -> modalManager.showConfirmModal(
                confirmCard(c),
                "Remover ligação auxiliar",
                () -> persistenceService.deleteAsync(
                        repository,
                        c,
                        c.getId(),
                        "CONEXAO_AUXILIAR",
                        "Remoção ligação: " + c.getNome(),
                        () -> Platform.runLater(this::reload)
                ),
                null
        ));
    }

    private VBox confirmCard(ConexaoAuxiliar c) {
        VBox box = new VBox(7);
        box.setPadding(new Insets(4));

        Label title = new Label("Eliminar «" + safe(c.getNome(), "Ligação") + "»?");
        title.getStyleClass().add("kubata-other-db-confirm-title");

        Label text = new Label(
                "A ligação será removida do catálogo do Kubata. Esta operação não elimina "
                        + "a base de dados externa nem os seus dados."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-other-db-confirm-text");

        box.getChildren().addAll(title, text);
        return box;
    }

    private String detectDatabase(String url) {
        String value = safe(url, "").toLowerCase(Locale.ROOT);
        if (value.startsWith("jdbc:postgresql:")) return "PostgreSQL";
        if (value.startsWith("jdbc:mysql:")) return "MySQL";
        if (value.startsWith("jdbc:mariadb:")) return "MariaDB";
        if (value.startsWith("jdbc:sqlserver:")) return "SQL Server";
        if (value.startsWith("jdbc:h2:")) return "H2";
        if (value.startsWith("jdbc:oracle:")) return "Oracle";
        return "JDBC";
    }

    private String driverFor(String preset) {
        return switch (preset) {
            case "PostgreSQL" -> "org.postgresql.Driver";
            case "MySQL / MariaDB" -> "com.mysql.cj.jdbc.Driver";
            case "Microsoft SQL Server" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "H2" -> "org.h2.Driver";
            case "Oracle" -> "oracle.jdbc.OracleDriver";
            default -> null;
        };
    }

    private String summarizeUrl(String url) {
        String value = safe(url, "—");
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);

        return value.length() > 55
                ? value.substring(0, 52) + "…"
                : value;
    }

    private String decodePassword(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String message(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? fallback
                : ex.getMessage();
    }
}
