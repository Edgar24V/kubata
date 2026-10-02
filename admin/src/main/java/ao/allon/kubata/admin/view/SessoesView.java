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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

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
    private final TableView<UserSession> table = new TableView<>(sessions);
    private final Label status = new Label();
    private final Label totalValue = new Label("0");
    private final Label latestValue = new Label("—");

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
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 22, 16, 22));
        header.getStyleClass().add("kubata-server-header");

        HBox line = new HBox(13);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().add("kubata-server-title-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.USERS, 22)));

        VBox titles = new VBox(2);
        Label title = new Label("Sessões do Sistema");
        title.getStyleClass().add("kubata-server-title");

        Label subtitle = new Label(
                "Consulte as sessões activas e termine sessões de outros utilizadores de forma controlada."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-server-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar sessões",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> load());

        line.getChildren().addAll(iconBox, titles, spacer, refresh);

        Label context = new Label(
                "SEGURANÇA · terminar sessões é uma operação administrativa auditada"
        );
        context.getStyleClass().add("kubata-server-status-bar");

        header.getChildren().addAll(line, context);

        buildTable();

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 20, 20));
        content.setFillWidth(true);

        content.getChildren().addAll(
                buildMetrics(),
                buildActionsBar()
        );

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox tableTitles = new VBox(2);
        Label tableTitle = new Label("Registo de sessões");
        tableTitle.getStyleClass().add("kubata-server-panel-title");
        Label tableSubtitle = new Label(
                "Seleccione uma sessão para disponibilizar as acções correspondentes."
        );
        tableSubtitle.getStyleClass().add("kubata-server-note");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        status.getStyleClass().add("kubata-server-status-value");
        tableHeader.getChildren().addAll(tableTitles, tableSpacer, status);

        VBox section = new VBox(10, tableHeader, table);
        section.getStyleClass().add("kubata-server-panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(section, Priority.ALWAYS);

        content.getChildren().add(section);
        VBox.setVgrow(content, Priority.ALWAYS);

        getChildren().addAll(header, content);
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
        HBox row = new HBox(12);
        row.getChildren().addAll(
                metricCard("SESSÕES REGISTADAS", totalValue, Feather.LIST),
                metricCard("ÚLTIMO LOGIN", latestValue, Feather.CLOCK)
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
        table.setPlaceholder(new Label("Nenhuma sessão registada."));

        TableColumn<UserSession, String> user =
                textColumn("Utilizador", UserSession::getUsername);
        TableColumn<UserSession, String> workstation =
                textColumn("Posto", UserSession::getWorkstation);
        TableColumn<UserSession, String> ip =
                textColumn("IP", UserSession::getIpAddress);
        TableColumn<UserSession, String> context =
                textColumn("Contexto", UserSession::getContext);
        TableColumn<UserSession, String> login = textColumn(
                "Login",
                s -> s.getLoginTime() == null ? "—" : s.getLoginTime().format(DATE_TIME)
        );

        TableColumn<UserSession, Void> actions = new TableColumn<>("Ações");
        actions.setPrefWidth(110);
        actions.setMinWidth(110);
        actions.setMaxWidth(130);
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button(
                    "Terminar",
                    IconUtils.icon(Feather.LOG_OUT, 11)
            );

            {
                button.getStyleClass().add("button-danger-outlined");
                button.setOnAction(e -> {
                    UserSession session = getTableView().getItems().get(getIndex());
                    confirmTerminateSession(session);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                UserSession session = getTableView().getItems().get(getIndex());
                boolean own = isOwnSession(session);
                button.setDisable(own);
                button.setTooltip(new Tooltip(
                        own
                                ? "Use “Encerrar Sessão” no cabeçalho para sair da sua própria sessão."
                                : "Terminar esta sessão."
                ));
                setGraphic(button);
            }
        });

        table.getColumns().setAll(user, workstation, ip, context, login, actions);
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> updateActionState()
        );
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

    private void updateActionState() {
        UserSession selected = table.getSelectionModel().getSelectedItem();
        boolean enabled = selected != null && !isOwnSession(selected);
        terminateSelectedButton.setDisable(!enabled);
        terminateUserButton.setDisable(!enabled);
    }

    private boolean isOwnSession(UserSession session) {
        User actor = sessionManager.getUser();
        return actor != null
                && session != null
                && actor.getNome() != null
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
                userAdministrationService.terminarSessao(
                        actor,
                        selected.getId(),
                        SOURCE_IP
                );

                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    notificationService.showSuccess(
                            "Sessão terminada",
                            "A sessão de " + safe(selected.getUsername()) + " foi terminada."
                    );
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
                    totalValue.setText(Integer.toString(sessions.size()));

                    String latest = sessions.stream()
                            .map(UserSession::getLoginTime)
                            .filter(java.util.Objects::nonNull)
                            .findFirst()
                            .map(v -> v.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                            .orElse("—");
                    latestValue.setText(latest);

                    String current = sessionManager.getUser() == null
                            ? "Utilizador: —"
                            : "Utilizador actual: " + sessionManager.getUser().getEmail();
                    status.setText(
                            current + " · " + sessions.size()
                                    + " sessão(ões) registada(s)."
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
