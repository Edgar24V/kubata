package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.UserSessionRepository;
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

import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Component
public class SessoesView extends VBox {

    private final UserSessionRepository repository;
    private final SessionManager sessionManager;
    private final ObservableList<UserSession> sessions = FXCollections.observableArrayList();
    private final TableView<UserSession> table = new TableView<>(sessions);
    private final Label status = new Label();

    public SessoesView(UserSessionRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        load();
    }

    private void buildUi() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(15, 18, 13, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-box");

        Label title = new Label("Sessões do Sistema", IconUtils.icon(Feather.USERS, 18));
        title.getStyleClass().add("h3");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> load());

        header.getChildren().addAll(title, spacer, refresh);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15, 18, 18, 18));

        Label note = new Label(
                "Visão operacional das sessões registadas. O encerramento remoto depende da estratégia de autenticação do módulo."
        );
        note.setWrapText(true);
        note.getStyleClass().add("text-muted");

        buildTable();

        status.getStyleClass().add("text-muted");
        content.getChildren().addAll(note, table, status);
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(header, content);
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhuma sessão registada."));

        TableColumn<UserSession, String> user = textColumn("Utilizador", s -> s.getUsername());
        TableColumn<UserSession, String> workstation = textColumn("Posto", s -> s.getWorkstation());
        TableColumn<UserSession, String> ip = textColumn("IP", s -> s.getIpAddress());
        TableColumn<UserSession, String> context = textColumn("Contexto", s -> s.getContext());
        TableColumn<UserSession, String> login = textColumn("Login", s ->
                s.getLoginTime() == null ? "—" :
                        s.getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        table.getColumns().setAll(user, workstation, ip, context, login);
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

    private void load() {
        Thread t = new Thread(() -> {
            try {
                var result = repository.findAll().stream()
                        .sorted(Comparator.comparing(
                                UserSession::getLoginTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();

                Platform.runLater(() -> {
                    sessions.setAll(result);
                    String current = sessionManager.getUser() == null
                            ? "Utilizador: —"
                            : "Utilizador actual: " + sessionManager.getUser().getEmail();
                    status.setText(current + " · " + sessions.size() + " sessão(ões) registada(s).");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Erro ao carregar sessões: " + ex.getMessage()));
            }
        }, "kubata-admin-sessions");
        t.setDaemon(true);
        t.start();
    }
}
