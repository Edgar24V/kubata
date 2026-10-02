package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.service.UserAdministrationService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Component
public class SessoesView extends VBox {

    private static final String SOURCE_IP = "127.0.0.1";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final UserSessionRepository repository;
    private final UserRepository userRepository;
    private final UserAdministrationService userAdministrationService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final NotificationService notificationService;

    private final ObservableList<UserSession> sessions = FXCollections.observableArrayList();
    private final FilteredList<UserSession> filteredSessions = new FilteredList<>(sessions, s -> true);
    private final TableView<UserSession> table = new TableView<>(filteredSessions);
    private final Label status = new Label();
    private final Label totalValue = new Label("0");
    private final Label usersValue = new Label("0");
    private final Label currentValue = new Label("0");
    private final Label latestValue = new Label("—");
    private final Label refreshValue = new Label("—");

    private TextField searchField;
    private ComboBox<String> contextFilter;
    private Label detailUser, detailSessionId, detailWorkstation, detailIp,
            detailContext, detailLogin, detailDuration, detailCurrent;

    private Button terminateSelectedButton;
    private Button terminateUserButton;

    public SessoesView(UserSessionRepository repository,
                       UserRepository userRepository,
                       UserAdministrationService userAdministrationService,
                       SessionManager sessionManager,
                       ModalManager modalManager,
                       NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.userAdministrationService = userAdministrationService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.notificationService = notificationService;

        setSpacing(0);
        getStyleClass().add("kubata-server-page");
        buildUi();
        load();
    }

    private void buildUi() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(20, 22, 16, 22));
        header.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(13);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-server-title-icon");
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.getChildren().add(IconUtils.icon(Feather.USERS, 22));

        VBox titles = new VBox(3);
        Label eyebrow = new Label("INFRAESTRUTURA · SEGURANÇA");
        eyebrow.getStyleClass().add("kubata-sessions-eyebrow");

        Label title = new Label("Sessões");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Centro de monitorização das sessões registadas, origem, contexto e controlo administrativo."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");
        titles.getChildren().addAll(eyebrow, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> load());

        line.getChildren().addAll(iconBox, titles, spacer, refresh);

        Label context = new Label(
                "CONTROLO DE SESSÕES · encerramento exacto por identificador e auditoria administrativa",
                IconUtils.icon(Feather.SHIELD, 11)
        );
        context.getStyleClass().add("kubata-sessions-context");

        header.getChildren().addAll(line, context);

        buildTable();

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 20, 20));
        content.setFillWidth(true);
        content.getChildren().addAll(buildMetrics(), buildFilters(), buildActionsBar());

        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        split.setDividerPositions(0.72);
        split.getStyleClass().add("kubata-sessions-split");

        VBox tableSection = new VBox(9);
        tableSection.setPadding(new Insets(12));
        tableSection.getStyleClass().add("kubata-server-panel");

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox tableTitles = new VBox(2);
        Label tableTitle = new Label("Sessões registadas");
        tableTitle.getStyleClass().add("kubata-server-panel-title");

        Label tableSubtitle = new Label(
                "Seleccione uma sessão para consultar os detalhes e operações disponíveis."
        );
        tableSubtitle.getStyleClass().add("kubata-server-note");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        tableHeader.getChildren().addAll(tableTitles, tableSpacer, status);

        tableSection.getChildren().addAll(tableHeader, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        split.getItems().addAll(tableSection, buildDetailsPane());
        VBox.setVgrow(split, Priority.ALWAYS);

        content.getChildren().add(split);
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        contextFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> {
                    updateActionState();
                    updateDetails(newValue);
                }
        );
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("kubata-sessions-filterbar");

        Label searchLabel = new Label("Pesquisa");
        searchLabel.getStyleClass().add("kubata-server-properties-label");

        searchField = new TextField();
        searchField.setPromptText("Utilizador, posto, IP ou contexto...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("kubata-sessions-search");

        Label contextLabel = new Label("Contexto");
        contextLabel.getStyleClass().add("kubata-server-properties-label");

        contextFilter = new ComboBox<>();
        contextFilter.getItems().add("TODOS");
        contextFilter.setValue("TODOS");
        contextFilter.setPrefWidth(175);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            contextFilter.setValue("TODOS");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label(
                "Filtros locais · não alteram os registos da base de dados",
                IconUtils.icon(Feather.INFO, 10)
        );
        hint.getStyleClass().add("kubata-server-note");

        bar.getChildren().addAll(
                searchLabel, searchField,
                contextLabel, contextFilter,
                clear, spacer, hint
        );
        return bar;
    }

    private HBox buildActionsBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("kubata-sessions-actions");

        terminateSelectedButton = new Button(
                "Terminar sessão seleccionada",
                IconUtils.icon(Feather.LOG_OUT, 13)
        );
        terminateSelectedButton.getStyleClass().add("button-outlined");
        terminateSelectedButton.setTooltip(
                new Tooltip("Termina apenas a sessão seleccionada.")
        );
        terminateSelectedButton.setOnAction(e -> terminateSelectedSession());

        terminateUserButton = new Button(
                "Terminar todas deste utilizador",
                IconUtils.icon(Feather.USER_X, 13)
        );
        terminateUserButton.getStyleClass().add("button-danger-outlined");
        terminateUserButton.setTooltip(
                new Tooltip("Termina todas as sessões registadas para o utilizador seleccionado.")
        );
        terminateUserButton.setOnAction(e -> terminateAllSelectedUserSessions());

        Label help = new Label(
                "A sessão do administrador actual deve ser encerrada pelo comando “Encerrar Sessão” do cabeçalho."
        );
        help.setWrapText(true);
        help.getStyleClass().add("kubata-server-note");
        HBox.setHgrow(help, Priority.ALWAYS);

        bar.getChildren().addAll(terminateSelectedButton, terminateUserButton, help);

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> updateActionState()
        );
        updateActionState();

        return bar;
    }

    private HBox buildMetrics() {
        HBox row = new HBox(10);
        row.getChildren().addAll(
                metricCard("SESSÕES", totalValue, Feather.USERS),
                metricCard("UTILIZADORES", usersValue, Feather.USER),
                metricCard("SESSÃO ACTUAL", currentValue, Feather.SHIELD),
                metricCard("ÚLTIMO LOGIN", latestValue, Feather.CLOCK),
                metricCard("ACTUALIZADO", refreshValue, Feather.REFRESH_CW)
        );
        for (Node node : row.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);
        return row;
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
        table.setFixedCellSize(40);
        table.getStyleClass().addAll("kubata-server-properties-table", "kubata-sessions-table");
        table.setPlaceholder(new Label("Nenhuma sessão corresponde aos filtros."));

        TableColumn<UserSession, String> id = textColumn(
                "ID", s -> s.getId() == null ? "—" : String.valueOf(s.getId()));
        id.setPrefWidth(70);
        id.setMaxWidth(85);

        TableColumn<UserSession, String> user = textColumn(
                "Utilizador", UserSession::getUsername);
        TableColumn<UserSession, String> workstation = textColumn(
                "Posto", UserSession::getWorkstation);
        TableColumn<UserSession, String> ip = textColumn(
                "IP", UserSession::getIpAddress);
        TableColumn<UserSession, String> context = textColumn(
                "Contexto", UserSession::getContext);
        TableColumn<UserSession, String> login = textColumn(
                "Login", s -> s.getLoginTime() == null ? "—" : s.getLoginTime().format(DATE_TIME));

        TableColumn<UserSession, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(data -> new SimpleStringProperty(
                isOwnSession(data.getValue()) ? "SESSÃO ACTUAL" : "ACTIVA"
        ));
        state.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll(
                        "kubata-session-status-badge",
                        "SESSÃO ACTUAL".equals(item)
                                ? "kubata-session-current"
                                : "kubata-session-active"
                );
                setText(null);
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<UserSession, Void> actions = new TableColumn<>("Acções");
        actions.setPrefWidth(82);
        actions.setMinWidth(82);
        actions.setMaxWidth(95);
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button("", IconUtils.icon(Feather.LOG_OUT, 11));
            {
                button.getStyleClass().add("button-icon-danger");
                button.setOnAction(e -> confirmTerminateSession(
                        getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                UserSession session = getTableView().getItems().get(getIndex());
                boolean own = isOwnSession(session);
                button.setDisable(own);
                button.setTooltip(new Tooltip(
                        own ? "A sessão actual está protegida."
                                : "Terminar esta sessão."
                ));
                setGraphic(button);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().setAll(id, user, workstation, ip, context, login, state, actions);
    }

    private TableColumn<UserSession, String> textColumn(
            String title,
            java.util.function.Function<UserSession, String> mapper) {
        TableColumn<UserSession, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                mapper.apply(data.getValue()) == null ? "—" : mapper.apply(data.getValue())
        ));
        return col;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(14));
        pane.getStyleClass().add("kubata-sessions-details");

        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-sessions-details-icon");
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);
        icon.getChildren().add(IconUtils.icon(Feather.USER, 17));

        VBox titleText = new VBox(2);
        detailUser = new Label("Nenhuma sessão seleccionada");
        detailUser.getStyleClass().add("kubata-sessions-details-title");

        Label caption = new Label("Detalhes da sessão");
        caption.getStyleClass().add("kubata-sessions-details-caption");
        titleText.getChildren().addAll(detailUser, caption);
        titleRow.getChildren().addAll(icon, titleText);

        VBox facts = new VBox(4);
        facts.getStyleClass().add("kubata-sessions-details-card");
        detailSessionId = detailRow(facts, "ID da sessão");
        detailWorkstation = detailRow(facts, "Posto");
        detailIp = detailRow(facts, "Endereço IP");
        detailContext = detailRow(facts, "Contexto");
        detailLogin = detailRow(facts, "Login");
        detailDuration = detailRow(facts, "Duração");
        detailCurrent = detailRow(facts, "Sessão actual");

        Label security = new Label(
                "O encerramento utiliza o ID da sessão e fica registado no histórico administrativo.",
                IconUtils.icon(Feather.SHIELD, 11)
        );
        security.setWrapText(true);
        security.getStyleClass().add("kubata-sessions-security-card");

        Button terminate = new Button("Terminar sessão", IconUtils.icon(Feather.LOG_OUT, 12));
        terminate.getStyleClass().add("button-danger-outlined");
        terminate.setOnAction(e -> terminateSelectedSession());

        Button terminateUser = new Button(
                "Terminar todas deste utilizador",
                IconUtils.icon(Feather.USER_X, 12)
        );
        terminateUser.getStyleClass().add("button-outlined");
        terminateUser.setOnAction(e -> terminateAllSelectedUserSessions());

        HBox actions = new HBox(7, terminate, terminateUser);
        actions.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(titleRow, facts, security, actions);
        return pane;
    }

    private Label detailRow(VBox parent, String title) {
        HBox row = new HBox(8);
        row.getStyleClass().add("kubata-sessions-details-row");

        Label key = new Label(title.toUpperCase());
        key.getStyleClass().add("kubata-sessions-details-key");
        key.setMinWidth(105);

        Label value = new Label("—");
        value.getStyleClass().add("kubata-sessions-details-value");
        HBox.setHgrow(value, Priority.ALWAYS);

        row.getChildren().addAll(key, value);
        parent.getChildren().add(row);
        return value;
    }

    private void updateDetails(UserSession selected) {
        if (selected == null) {
            detailUser.setText("Nenhuma sessão seleccionada");
            detailSessionId.setText("—");
            detailWorkstation.setText("—");
            detailIp.setText("—");
            detailContext.setText("—");
            detailLogin.setText("—");
            detailDuration.setText("—");
            detailCurrent.setText("—");
            return;
        }

        detailUser.setText(safe(selected.getUsername()));
        detailSessionId.setText(selected.getId() == null ? "—" : String.valueOf(selected.getId()));
        detailWorkstation.setText(safe(selected.getWorkstation()));
        detailIp.setText(safe(selected.getIpAddress()));
        detailContext.setText(safe(selected.getContext()));
        detailLogin.setText(formatDate(selected));
        detailDuration.setText(formatDuration(selected));
        detailCurrent.setText(isOwnSession(selected) ? "SIM · PROTEGIDA" : "NÃO");
    }

    private String formatDuration(UserSession session) {
        if (session == null || session.getLoginTime() == null) return "—";
        Duration duration = Duration.between(session.getLoginTime(), LocalDateTime.now());
        if (duration.isNegative()) return "—";

        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        return days > 0
                ? String.format("%dd %02dh %02dm", days, hours, minutes)
                : String.format("%02dh %02dm", hours, minutes);
    }

    private void applyFilters() {
        String query = searchField == null ? "" : searchField.getText();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        String context = contextFilter == null ? "TODOS" : contextFilter.getValue();

        filteredSessions.setPredicate(session -> {
            if (session == null) return false;

            boolean textMatch = normalized.isBlank()
                    || safe(session.getUsername()).toLowerCase().contains(normalized)
                    || safe(session.getWorkstation()).toLowerCase().contains(normalized)
                    || safe(session.getIpAddress()).toLowerCase().contains(normalized)
                    || safe(session.getContext()).toLowerCase().contains(normalized);

            return textMatch
                    && ("TODOS".equals(context)
                    || safe(session.getContext()).equalsIgnoreCase(context));
        });

        status.setText(filteredSessions.size() + " sessão(ões) visível(is)");
    }

    private void updateActionState() {
        UserSession selected = table.getSelectionModel().getSelectedItem();
        boolean enabled = selected != null && !isOwnSession(selected);
        terminateSelectedButton.setDisable(!enabled);
        terminateUserButton.setDisable(!enabled);
    }

    private boolean isOwnSession(UserSession session) {
        User actor = sessionManager.getUser();
        if (actor == null || session == null) {
            return false;
        }

        // Protege apenas a sessão desta instância. Sessões antigas do mesmo
        // utilizador podem ser encerradas pelo administrador sem bloquear a sessão actual.
        Long currentSessionId = sessionManager.getSessionId();
        if (currentSessionId != null && session.getId() != null) {
            return currentSessionId.equals(session.getId());
        }

        // Compatibilidade com sessões criadas antes do rastreio por ID.
        return actor.getNome() != null
                && actor.getNome().equalsIgnoreCase(session.getUsername());
    }

    private void terminateSelectedSession() {
        UserSession selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            confirmTerminateSession(selected);
        }
    }

    private void confirmTerminateSession(UserSession selected) {
        if (selected == null || isOwnSession(selected)) {
            return;
        }

        VBox content = new VBox(10);
        content.getStyleClass().add("kubata-session-confirm-card");

        Label title = new Label("Terminar sessão seleccionada");
        title.getStyleClass().add("h4");

        Label message = new Label(
                "Esta operação vai encerrar imediatamente a sessão de " +
                        safe(selected.getUsername()) + "."
        );
        message.setWrapText(true);

        Label details = new Label(
                "Posto: " + safe(selected.getWorkstation())
                        + "\nIP: " + safe(selected.getIpAddress())
                        + "\nLogin: " + formatDate(selected)
                        + "\n\nApenas esta sessão será removida."
        );
        details.setWrapText(true);
        details.getStyleClass().add("text-muted");

        content.getChildren().addAll(title, message, details);

        modalManager.showConfirmModal(
                content,
                "Terminar sessão",
                () -> executeTerminateSelected(selected),
                null
        );
    }

    private void executeTerminateSelected(UserSession selected) {
        User actor = sessionManager.getUser();
        if (actor == null) {
            modalManager.alert(
                    "Sessão administrativa inválida",
                    "A autenticação administrativa já não está disponível.",
                    "error",
                    null
            );
            return;
        }

        modalManager.showLoadingModal(
                "A terminar sessão",
                "A remover a sessão seleccionada e a registar a operação..."
        );

        Thread worker = new Thread(() -> {
            try {
                boolean terminated = userAdministrationService.terminarSessao(
                        actor,
                        selected.getId(),
                        SOURCE_IP
                );

                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    if (terminated) {
                        notificationService.showSuccess(
                                "Sessão terminada",
                                "A sessão de " + safe(selected.getUsername()) + " foi terminada."
                        );
                    } else {
                        notificationService.showInfo(
                                "Sessão já encerrada",
                                "A sessão seleccionada já não está activa. A lista foi actualizada."
                        );
                    }
                    load();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    modalManager.alert(
                            "Não foi possível terminar a sessão",
                            safeError(ex),
                            "error",
                            ex
                    );
                });
            }
        }, "kubata-admin-terminate-session");
        worker.setDaemon(true);
        worker.start();
    }

    private void terminateAllSelectedUserSessions() {
        UserSession selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || isOwnSession(selected)) {
            return;
        }

        String username = safe(selected.getUsername());
        VBox content = new VBox(10);
        content.getStyleClass().add("kubata-session-confirm-card");

        Label title = new Label("Terminar todas as sessões deste utilizador");
        title.getStyleClass().add("h4");

        Label message = new Label(
                "Todas as sessões actualmente registadas para " + username + " serão terminadas."
        );
        message.setWrapText(true);

        Label warning = new Label(
                "Esta operação pode interromper o trabalho do utilizador noutros computadores. "
                        + "Use-a apenas quando for necessário."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("text-danger");

        content.getChildren().addAll(title, message, warning);

        modalManager.showConfirmModal(
                content,
                "Terminar todas as sessões",
                () -> executeTerminateAllUserSessions(selected),
                null
        );
    }

    private void executeTerminateAllUserSessions(UserSession selected) {
        User actor = sessionManager.getUser();
        if (actor == null) {
            return;
        }

        modalManager.showLoadingModal(
                "A terminar sessões",
                "A encerrar todas as sessões do utilizador seleccionado..."
        );

        Thread worker = new Thread(() -> {
            try {
                User target = userRepository.findAllByNomeIgnoreCase(
                                safe(selected.getUsername())
                        )
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "O utilizador associado à sessão já não foi encontrado."
                        ));

                long count = userAdministrationService.terminarSessoes(
                        actor,
                        target.getId(),
                        SOURCE_IP
                );

                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    notificationService.showSuccess(
                            "Sessões terminadas",
                            count + (count == 1
                                    ? " sessão foi terminada."
                                    : " sessões foram terminadas.")
                    );
                    load();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    modalManager.alert(
                            "Não foi possível terminar as sessões",
                            safeError(ex),
                            "error",
                            ex
                    );
                });
            }
        }, "kubata-admin-terminate-user-sessions");
        worker.setDaemon(true);
        worker.start();
    }

    private void load() {
        Thread t = new Thread(() -> {
            try {
                var result = repository.findAll().stream()
                        .sorted(Comparator.comparing(
                                UserSession::getLoginTime,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .toList();

                Platform.runLater(() -> {
                    sessions.setAll(result);

                    long uniqueUsers = sessions.stream()
                            .map(UserSession::getUsername)
                            .filter(java.util.Objects::nonNull)
                            .map(String::toLowerCase)
                            .distinct()
                            .count();

                    long current = sessions.stream()
                            .filter(this::isOwnSession)
                            .count();

                    String latest = sessions.stream()
                            .map(UserSession::getLoginTime)
                            .filter(java.util.Objects::nonNull)
                            .findFirst()
                            .map(v -> v.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                            .orElse("—");

                    totalValue.setText(Integer.toString(sessions.size()));
                    usersValue.setText(Long.toString(uniqueUsers));
                    currentValue.setText(Long.toString(current));
                    latestValue.setText(latest);
                    refreshValue.setText(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("HH:mm:ss")));

                    contextFilter.getItems().setAll("TODOS");
                    sessions.stream()
                            .map(UserSession::getContext)
                            .filter(java.util.Objects::nonNull)
                            .map(String::trim)
                            .filter(v -> !v.isBlank())
                            .distinct()
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .forEach(contextFilter.getItems()::add);

                    applyFilters();
                    UserSession selected = table.getSelectionModel().getSelectedItem();
                    if (selected != null) updateDetails(selected);

                    String currentUser = sessionManager.getUser() == null
                            ? "Utilizador: —"
                            : "Utilizador actual: " + sessionManager.getUser().getEmail();

                    status.setText(
                            currentUser + " · " + filteredSessions.size()
                                    + " sessão(ões) visível(is)"
                    );
                    updateActionState();
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        status.setText("Erro ao carregar sessões: " + safeError(ex))
                );
            }
        }, "kubata-admin-sessions");
        t.setDaemon(true);
        t.start();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String formatDate(UserSession session) {
        return session == null || session.getLoginTime() == null
                ? "—"
                : session.getLoginTime().format(DATE_TIME);
    }

    private String safeError(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "A operação não pôde ser concluída.";
        }
        return ex.getMessage();
    }
}
