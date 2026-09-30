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
import javafx.scene.Node;
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
    private final Label totalValue = new Label("0");
    private final Label latestValue = new Label("—");

    public SessoesView(UserSessionRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        setSpacing(0);
        getStyleClass().addAll("application-view", "kubata-infra-page");
        buildUi();
        load();
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
        iconBox.getChildren().add(IconUtils.icon(Feather.USERS, 21));

        VBox titles = new VBox(2);
        Label title = new Label("Sessões do Sistema");
        title.getStyleClass().add("kubata-infra-title");

        Label subtitle = new Label(
                "Monitorização das sessões registadas, postos de trabalho, IPs e contexto de acesso."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-infra-subtitle");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Actualizar sessões", IconUtils.icon(Feather.REFRESH_CW, 13));
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> load());

        line.getChildren().addAll(iconBox, titles, spacer, refresh);

        Label context = new Label("SEGURANÇA · actividade de acesso do sistema");
        context.getStyleClass().add("kubata-infra-status");

        header.getChildren().addAll(line, context);

        buildTable();

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 20, 20));
        content.setFillWidth(true);

        content.getChildren().add(buildMetrics());

        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        VBox tableTitles = new VBox(2);
        Label tableTitle = new Label("Registo de sessões");
        tableTitle.getStyleClass().add("kubata-infra-section-title");
        Label tableSubtitle = new Label(
                "A listagem é ordenada pelo momento de login mais recente."
        );
        tableSubtitle.getStyleClass().add("kubata-infra-section-subtitle");
        tableTitles.getChildren().addAll(tableTitle, tableSubtitle);

        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        status.getStyleClass().add("kubata-infra-status");
        tableHeader.getChildren().addAll(tableTitles, tableSpacer, status);

        VBox section = new VBox(10, tableHeader, table);
        section.getStyleClass().add("kubata-infra-section");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(section, Priority.ALWAYS);

        content.getChildren().add(section);
        VBox.setVgrow(content, Priority.ALWAYS);

        getChildren().addAll(header, content);
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
