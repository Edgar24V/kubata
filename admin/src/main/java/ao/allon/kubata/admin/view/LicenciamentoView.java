package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.repository.UserRepository;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Painel de licenciamento por módulo (chave e validade) com alertas de expiração.
 */
@Component
public class LicenciamentoView extends VBox {

    private static final int ALERT_DAYS = 30;

    private final ModuloSistemaRepository moduloRepository;
    private final UserRepository userRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final SecurityService securityService;

    private final Label banner = new Label();
    private final Label lblUsers = new Label();
    private final AdvancedTableView<ModuloSistema> table = new AdvancedTableView<>();
    private final ObservableList<ModuloSistema> rows = FXCollections.observableArrayList();

    public LicenciamentoView(ModuloSistemaRepository moduloRepository,
                             UserRepository userRepository,
                             PersistenceService persistenceService,
                             SessionManager sessionManager,
                             ModalManager modalManager,
                             SecurityService securityService) {
        this.moduloRepository = moduloRepository;
        this.userRepository = userRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.securityService = securityService;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        Platform.runLater(this::reload);
    }

    private void buildUi() {
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("header-box");

        Label title = new Label("Licenciamento", IconUtils.icon(Feather.KEY, 18));
        title.getStyleClass().add("h3");

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> reload());

        Button btnEdit = new Button("Editar módulo", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEdit.getStyleClass().add("button-primary");
        btnEdit.setOnAction(e -> editSelected());

        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(title, spacer, lblUsers, btnEdit, btnRefresh);

        banner.setPadding(new Insets(8, 16, 8, 16));
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setVisible(false);
        banner.setManaged(false);

        TableUtils.standardize(table);
        table.setData(rows);
        table.getColumns().add(TableUtils.createTextColumn("Módulo", c -> new SimpleStringProperty(c.getValue().getNome())));
        table.getColumns().add(TableUtils.createTextColumn("Código", c -> new SimpleStringProperty(c.getValue().getCodigo())));
        table.getColumns().add(TableUtils.createTextColumn("Estado", c -> new SimpleStringProperty(c.getValue().getEstado().name())));
        table.getColumns().add(TableUtils.createTextColumn("Chave", c -> new SimpleStringProperty(blankToDash(c.getValue().getLicencaChave()))));
        table.getColumns().add(TableUtils.createTextColumn("Validade", c -> new SimpleStringProperty(formatValidity(c.getValue().getLicencaValidade()))));

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(toolbar, banner, table);
    }

    private static String blankToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String formatValidity(LocalDateTime dt) {
        if (dt == null) {
            return "—";
        }
        return dt.toLocalDate().toString();
    }

    private void reload() {
        List<ModuloSistema> all = moduloRepository.findAll();
        rows.setAll(all);
        lblUsers.setText("Utilizadores registados: " + userRepository.count());
        updateBanner(all);
    }

    private void updateBanner(List<ModuloSistema> all) {
        LocalDateTime now = LocalDateTime.now();
        long soon = all.stream()
                .filter(m -> m.getLicencaValidade() != null)
                .filter(m -> ChronoUnit.DAYS.between(now, m.getLicencaValidade()) <= ALERT_DAYS && m.getLicencaValidade().isAfter(now))
                .count();
        long expired = all.stream()
                .filter(m -> m.getLicencaValidade() != null && m.getLicencaValidade().isBefore(now))
                .count();

        if (expired > 0) {
            banner.setText("Atenção: existem " + expired + " módulo(s) com licença expirada. Actualize as chaves.");
            banner.getStyleClass().setAll("badge", "badge-warning");
            banner.setVisible(true);
            banner.setManaged(true);
        } else if (soon > 0) {
            banner.setText("Alerta: " + soon + " licença(s) expiram nos próximos " + ALERT_DAYS + " dias.");
            banner.getStyleClass().setAll("badge", "badge-info");
            banner.setVisible(true);
            banner.setManaged(true);
        } else {
            banner.setVisible(false);
            banner.setManaged(false);
        }
    }

    private void editSelected() {
        User u = sessionManager.getUser();
        if (!securityService.hasPermission(u, "ADMINISTRATOR", "LICENCAS", ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR)
                && !u.isSuperadmin() && u.getRole() != ao.allon.kubata.core.domain.Role.ADMIN) {
            modalManager.alert("Permissão", "Não tem permissão para editar licenças (LICENCAS / EDITAR).", "warning", null);
            return;
        }
        ModuloSistema m = table.getSelectionModel().getSelectedItem();
        if (m == null) {
            modalManager.alert("Aviso", "Seleccione um módulo.", "warning", null);
            return;
        }

        TextField txtChave = new TextField(m.getLicencaChave() != null ? m.getLicencaChave() : "");
        DatePicker dp = new DatePicker(m.getLicencaValidade() != null ? m.getLicencaValidade().toLocalDate() : null);

        VBox form = new VBox(10,
                new Label("Chave de licença"),
                txtChave,
                new Label("Validade"),
                dp);
        form.setPadding(new Insets(10));

        modalManager.showConfirmModal(form, "Licença — " + m.getNome(), () -> {
            m.setLicencaChave(txtChave.getText().isBlank() ? null : txtChave.getText().trim());
            m.setLicencaValidade(dp.getValue() != null ? dp.getValue().atStartOfDay() : null);
            persistenceService.saveAsync(moduloRepository, m, "MODULO_SISTEMA",
                    "Actualização de licença: " + m.getCodigo(), saved -> Platform.runLater(this::reload));
        }, null);
    }
}
