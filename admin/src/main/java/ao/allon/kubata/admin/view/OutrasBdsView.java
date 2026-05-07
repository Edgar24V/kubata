package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ConexaoAuxiliar;
import ao.allon.kubata.core.repository.ConexaoAuxiliarRepository;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Gestão de ligações JDBC auxiliares com teste de conectividade básico.
 */
@Component
public class OutrasBdsView extends VBox {

    private final ConexaoAuxiliarRepository repository;
    private final PersistenceService persistenceService;
    private final ModalManager modalManager;

    private final AdvancedTableView<ConexaoAuxiliar> table = new AdvancedTableView<>();
    private final ObservableList<ConexaoAuxiliar> data = FXCollections.observableArrayList();

    public OutrasBdsView(ConexaoAuxiliarRepository repository,
                         PersistenceService persistenceService,
                         ModalManager modalManager) {
        this.repository = repository;
        this.persistenceService = persistenceService;
        this.modalManager = modalManager;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        Platform.runLater(this::reload);
    }

    private void buildUi() {
        HBox tb = new HBox(10);
        tb.setPadding(new Insets(10, 16, 10, 16));
        tb.setAlignment(Pos.CENTER_LEFT);
        tb.getStyleClass().add("header-box");
        Label title = new Label("Outras bases de dados", IconUtils.icon(Feather.DATABASE, 18));
        title.getStyleClass().add("h3");
        javafx.scene.layout.Pane sp = new javafx.scene.layout.Pane();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnNovo = new Button("Nova", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-success");
        btnNovo.setOnAction(e -> edit(null));
        Button btnEdit = new Button("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEdit.setOnAction(e -> {
            ConexaoAuxiliar c = table.getSelectionModel().getSelectedItem();
            if (c == null) {
                modalManager.alert("Aviso", "Seleccione uma ligação.", "warning", null);
            } else {
                edit(c);
            }
        });
        Button btnTest = new Button("Testar ligação", IconUtils.icon(Feather.ZAP, IconUtils.SIZE_SMALL));
        btnTest.getStyleClass().add("button-outlined");
        btnTest.setOnAction(e -> testSelected());
        Button btnDel = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnDel.getStyleClass().add("button-danger");
        btnDel.setOnAction(e -> deleteSelected());
        Button btnRef = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRef.setOnAction(e -> reload());
        tb.getChildren().addAll(title, sp, btnNovo, btnEdit, btnTest, btnDel, btnRef);

        TableUtils.standardize(table);
        table.setData(data);
        table.getColumns().add(TableUtils.createTextColumn("Nome", c -> new SimpleStringProperty(c.getValue().getNome())));
        table.getColumns().add(TableUtils.createTextColumn("JDBC URL", c -> new SimpleStringProperty(c.getValue().getJdbcUrl())));
        table.getColumns().add(TableUtils.createTextColumn("Utilizador", c -> new SimpleStringProperty(c.getValue().getUsername())));
        TableColumn<ConexaoAuxiliar, Boolean> colA = new TableColumn<>("Activo");
        colA.setCellValueFactory(c -> new SimpleBooleanProperty(Boolean.TRUE.equals(c.getValue().getActivo())));
        table.getColumns().add(colA);

        Label warn = new Label("Passwords são armazenadas em Base64 (não é encriptação forte). Use credenciais de serviço com privilégios mínimos.");
        warn.getStyleClass().add("text-muted");
        warn.setWrapText(true);
        warn.setPadding(new Insets(0, 16, 8, 16));

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(tb, warn, table);
    }

    private void reload() {
        data.setAll(repository.findAll());
    }

    private void edit(ConexaoAuxiliar existing) {
        TextField nome = new TextField();
        TextField url = new TextField();
        TextField user = new TextField();
        PasswordField pass = new PasswordField();
        TextField driver = new TextField();
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(true);
        TextArea desc = new TextArea();
        desc.setPrefRowCount(2);

        if (existing != null) {
            nome.setText(existing.getNome());
            url.setText(existing.getJdbcUrl());
            user.setText(existing.getUsername() != null ? existing.getUsername() : "");
            if (existing.getPasswordEnc() != null && !existing.getPasswordEnc().isBlank()) {
                try {
                    pass.setText(new String(Base64.getDecoder().decode(existing.getPasswordEnc())));
                } catch (IllegalArgumentException ignored) {
                    pass.setText("");
                }
            }
            driver.setText(existing.getDriverClass() != null ? existing.getDriverClass() : "");
            activo.setSelected(Boolean.TRUE.equals(existing.getActivo()));
            desc.setText(existing.getDescricao());
        }

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        int r = 0;
        g.add(new Label("Nome"), 0, r);
        g.add(nome, 1, r++);
        g.add(new Label("JDBC URL"), 0, r);
        g.add(url, 1, r++);
        g.add(new Label("Utilizador"), 0, r);
        g.add(user, 1, r++);
        g.add(new Label("Password"), 0, r);
        g.add(pass, 1, r++);
        g.add(new Label("Driver (opcional)"), 0, r);
        g.add(driver, 1, r++);
        g.add(activo, 1, r++);
        g.add(new Label("Descrição"), 0, r);
        g.add(desc, 1, r++);

        modalManager.showConfirmModal(g, existing == null ? "Nova ligação" : "Editar ligação", () -> {
            if (nome.getText().isBlank() || url.getText().isBlank()) {
                modalManager.alert("Validação", "Nome e JDBC URL são obrigatórios.", "warning", null);
                return;
            }
            ConexaoAuxiliar c = existing != null ? existing : ConexaoAuxiliar.builder().build();
            if (existing == null) {
                c.setCriadoEm(LocalDateTime.now());
            }
            c.setNome(nome.getText().trim());
            c.setJdbcUrl(url.getText().trim());
            c.setUsername(user.getText().isBlank() ? null : user.getText().trim());
            c.setPasswordEnc(pass.getText().isBlank() ? null : Base64.getEncoder().encodeToString(pass.getText().getBytes()));
            c.setDriverClass(driver.getText().isBlank() ? null : driver.getText().trim());
            c.setActivo(activo.isSelected());
            c.setDescricao(desc.getText());
            c.setActualizadoEm(LocalDateTime.now());

            persistenceService.saveAsync(repository, c, "CONEXAO_AUXILIAR", "Ligação auxiliar: " + c.getNome(),
                    saved -> Platform.runLater(this::reload));
        }, null);
    }

    private void deleteSelected() {
        ConexaoAuxiliar c = table.getSelectionModel().getSelectedItem();
        if (c == null) {
            modalManager.alert("Aviso", "Seleccione uma ligação.", "warning", null);
            return;
        }
        modalManager.showConfirmModal(new Label("Remover a ligação " + c.getNome() + "?"),
                "Confirmar", () -> persistenceService.deleteAsync(repository, c, c.getId(), "CONEXAO_AUXILIAR",
                        "Remoção ligação " + c.getNome(), () -> Platform.runLater(this::reload)), null);
    }

    private void testSelected() {
        ConexaoAuxiliar c = table.getSelectionModel().getSelectedItem();
        if (c == null) {
            modalManager.alert("Aviso", "Seleccione uma ligação.", "warning", null);
            return;
        }
        try {
            String pwd = "";
            if (c.getPasswordEnc() != null && !c.getPasswordEnc().isBlank()) {
                pwd = new String(Base64.getDecoder().decode(c.getPasswordEnc()));
            }
            if (c.getDriverClass() != null && !c.getDriverClass().isBlank()) {
                Class.forName(c.getDriverClass());
            }
            try (Connection conn = DriverManager.getConnection(
                    c.getJdbcUrl(),
                    Optional.ofNullable(c.getUsername()).orElse(""),
                    pwd)) {
                modalManager.alert("Ligação", "Sucesso: " + conn.getMetaData().getDatabaseProductName(), "info", null);
            }
        } catch (Exception ex) {
            modalManager.alert("Falha na ligação", ex.getMessage(), "error", ex);
        }
    }
}
