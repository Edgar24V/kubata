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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestão dedicada de {@code adm_parametro_sistema} (global ou por empresa), com auditoria de configuração (AGT/SAFT relevante).
 */
@Component
public class ParametrosSistemaView extends VBox {

    private final ParametroSistemaRepository parametroRepository;
    private final EmpresaRepository empresaRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final AuditService auditService;
    private final SecurityService securityService;

    private final ComboBox<EmpresaScope> scopeCombo = new ComboBox<>();
    private final AdvancedTableView<ParametroSistema> table = new AdvancedTableView<>();
    private final ObservableList<ParametroSistema> data = FXCollections.observableArrayList();

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
        Platform.runLater(() -> {
            fillEmpresaScopeCombo();
            reload();
        });
    }

    private void fillEmpresaScopeCombo() {
        scopeCombo.getItems().clear();
        scopeCombo.getItems().add(new EmpresaScope(null, "Global (todas as empresas)"));
        empresaRepository.findAll().forEach(e -> scopeCombo.getItems().add(new EmpresaScope(e.getId(), e.getNome())));
        scopeCombo.getSelectionModel().selectFirst();
    }

    private void buildUi() {
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("header-box");

        Label title = new Label("Parâmetros do sistema", IconUtils.icon(Feather.SETTINGS, 18));
        title.getStyleClass().add("h3");

        scopeCombo.setPromptText("Âmbito");
        scopeCombo.getItems().add(new EmpresaScope(null, "Global (todas as empresas)"));
        scopeCombo.getSelectionModel().selectFirst();
        scopeCombo.setOnAction(e -> reload());

        Button btnNovo = new Button("Novo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-success");
        btnNovo.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.CRIAR)) {
                modalManager.alert("Permissão", "Necessita PARAMETROS/CRIAR ou papel ADMIN.", "warning", null);
                return;
            }
            editParametro(null);
        });

        Button btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEditar.getStyleClass().add("button-primary");
        btnEditar.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.EDITAR)) {
                modalManager.alert("Permissão", "Necessita PARAMETROS/EDITAR ou papel ADMIN.", "warning", null);
                return;
            }
            ParametroSistema p = table.getSelectionModel().getSelectedItem();
            if (p == null) {
                modalManager.alert("Aviso", "Seleccione um parâmetro.", "warning", null);
            } else {
                editParametro(p);
            }
        });

        Button btnApagar = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnApagar.getStyleClass().add("button-danger");
        btnApagar.setOnAction(e -> {
            if (!can(PermissaoPerfil.Operacao.APAGAR) && !isElevated()) {
                modalManager.alert("Permissão", "Remoção reservada a administradores.", "warning", null);
                return;
            }
            deleteSelected();
        });

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> reload());

        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(title, spacer, new Label("Âmbito:"), scopeCombo, btnNovo, btnEditar, btnApagar, btnRefresh);

        TableUtils.standardize(table);
        table.setData(data);
        table.getColumns().add(TableUtils.createTextColumn("Chave", c -> new SimpleStringProperty(c.getValue().getChave())));
        table.getColumns().add(TableUtils.createTextColumn("Valor", c -> new SimpleStringProperty(c.getValue().getValor())));
        table.getColumns().add(TableUtils.createTextColumn("Tipo", c -> new SimpleStringProperty(c.getValue().getTipoValor())));
        table.getColumns().add(TableUtils.createTextColumn("Grupo", c -> new SimpleStringProperty(c.getValue().getGrupo())));
        table.getColumns().add(TableUtils.createTextColumn("Editável", c -> new SimpleStringProperty(Boolean.TRUE.equals(c.getValue().getEditavel()) ? "Sim" : "Não")));

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(toolbar, table);
    }

    private void reload() {
        EmpresaScope sc = scopeCombo.getSelectionModel().getSelectedItem();
        if (sc == null) {
            return;
        }
        persistenceService.executeAsync(() -> {
            try {
                List<ParametroSistema> list = sc.empresaId == null
                        ? parametroRepository.findAllByEmpresaIsNullOrderByGrupoAscChaveAsc()
                        : parametroRepository.findAllByEmpresa_IdOrderByGrupoAscChaveAsc(sc.empresaId);
                Platform.runLater(() -> data.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() ->
                    modalManager.alert("Erro", "Erro ao carregar parâmetros: " + e.getMessage(), "error", e));
            }
        }, "READ", "PARAMETRO_SISTEMA", "Carregamento de parâmetros", null);
    }

    private void editParametro(ParametroSistema existing) {
        EmpresaScope sc = scopeCombo.getSelectionModel().getSelectedItem();
        if (sc == null) {
            return;
        }

        TextField txtChave = new TextField();
        TextField txtValor = new TextField();
        ComboBox<String> cmbTipo = new ComboBox<>(FXCollections.observableArrayList("STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE"));
        TextField txtGrupo = new TextField();
        TextArea txtDesc = new TextArea();
        txtDesc.setPrefRowCount(2);
        CheckBox chkEditavel = new CheckBox("Editável");
        chkEditavel.setSelected(true);

        final Empresa empresaRef = sc.empresaId != null
                ? empresaRepository.findById(sc.empresaId).orElse(null)
                : null;

        if (existing != null) {
            txtChave.setText(existing.getChave());
            txtChave.setDisable(true);
            txtValor.setText(existing.getValor());
            cmbTipo.setValue(Optional.ofNullable(existing.getTipoValor()).orElse("STRING"));
            txtGrupo.setText(existing.getGrupo());
            txtDesc.setText(existing.getDescricao());
            chkEditavel.setSelected(Boolean.TRUE.equals(existing.getEditavel()));
        } else {
            cmbTipo.setValue("STRING");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Chave"), 0, 0);
        grid.add(txtChave, 1, 0);
        grid.add(new Label("Valor"), 0, 1);
        grid.add(txtValor, 1, 1);
        grid.add(new Label("Tipo"), 0, 2);
        grid.add(cmbTipo, 1, 2);
        grid.add(new Label("Grupo"), 0, 3);
        grid.add(txtGrupo, 1, 3);
        grid.add(new Label("Descrição"), 0, 4);
        grid.add(txtDesc, 1, 4);
        grid.add(chkEditavel, 1, 5);

        modalManager.showConfirmModal(grid, existing == null ? "Novo parâmetro" : "Editar parâmetro", () -> {
            if (txtChave.getText() == null || txtChave.getText().isBlank()) {
                modalManager.alert("Erro", "A chave do parâmetro é obrigatória.", "error", null);
                return;
            }
            
            ParametroSistema p = existing != null ? existing : ParametroSistema.builder().build();
            if (existing == null) {
                p.setChave(txtChave.getText().trim());
                p.setEmpresa(empresaRef);
            }
            p.setValor(txtValor.getText());
            p.setTipoValor(cmbTipo.getValue());
            p.setGrupo(txtGrupo.getText());
            p.setDescricao(txtDesc.getText());
            p.setEditavel(chkEditavel.isSelected());
            p.setAtualizadoEm(LocalDateTime.now());
            if (sessionManager.getUser() != null) {
                p.setAtualizadoPor(sessionManager.getUser().getEmail());
            }
            
            if (existing == null && sc.empresaId == null
                    && parametroRepository.findByChaveAndEmpresaIdIsNull(p.getChave()).isPresent()) {
                modalManager.alert("Erro", "Já existe um parâmetro global com esta chave.", "error", null);
                return;
            }
            if (existing == null && sc.empresaId != null
                    && parametroRepository.findByEmpresa_IdAndChave(sc.empresaId, p.getChave()).isPresent()) {
                modalManager.alert("Erro", "Já existe um parâmetro para esta empresa com esta chave.", "error", null);
                return;
            }
            
            Map<String, String> before = existing != null ? snapshot(existing) : null;
            persistenceService.saveAsync(parametroRepository, p, "PARAMETRO_SISTEMA",
                    "Parâmetro " + p.getChave(), saved -> {
                        var user = sessionManager.getUser();
                        auditService.logAction(user, user != null ? user.getNome() : "SYSTEM",
                                AuditLog.AuditActionType.CONFIG_CHANGE,
                                "PARAMETRO_SISTEMA",
                                String.valueOf(saved.getId()),
                                "Parâmetro " + saved.getChave(),
                                before,
                                snapshot(saved),
                                "ADMINISTRATOR",
                                "127.0.0.1", null, null,
                                true,
                                AuditLog.AGTComplianceLevel.HIGH);
                        Platform.runLater(this::reload);
                    });
        }, null);
    }

    private boolean can(PermissaoPerfil.Operacao op) {
        var u = sessionManager.getUser();
        if (u == null) {
            return false;
        }
        if (u.getRole() == Role.ADMIN || u.isSuperadmin()) {
            return true;
        }
        return securityService.hasPermission(u, "ADMINISTRATOR", "PARAMETROS", op);
    }

    private boolean isElevated() {
        var u = sessionManager.getUser();
        return u != null && (u.getRole() == Role.ADMIN || u.isSuperadmin());
    }

    private static Map<String, String> snapshot(ParametroSistema p) {
        Map<String, String> m = new HashMap<>();
        m.put("chave", p.getChave());
        m.put("valor", p.getValor());
        m.put("tipo", p.getTipoValor());
        m.put("grupo", p.getGrupo());
        m.put("descricao", p.getDescricao());
        m.put("editavel", String.valueOf(p.getEditavel()));
        return m;
    }

    private void deleteSelected() {
        ParametroSistema p = table.getSelectionModel().getSelectedItem();
        if (p == null) {
            modalManager.alert("Aviso", "Seleccione um parâmetro.", "warning", null);
            return;
        }
        if (!Boolean.TRUE.equals(p.getEditavel())) {
            modalManager.alert("Bloqueado", "Este parâmetro não é removível pela política do sistema.", "warning", null);
            return;
        }
        modalManager.showConfirmModal(new Label("Remover o parâmetro " + p.getChave() + "?"),
                "Confirmar", () -> persistenceService.deleteAsync(parametroRepository, p, p.getId(),
                        "PARAMETRO_SISTEMA", "Remoção: " + p.getChave(), () -> Platform.runLater(this::reload)), null);
    }

    private record EmpresaScope(Long empresaId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
