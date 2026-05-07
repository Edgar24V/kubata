package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.util.AngolaValidationUtils;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.SessionManager;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.time.format.DateTimeFormatter;

public class GestaoUsuariosView extends VBox {

    private final AcessoService acessoService;
    private final AdvancedTableView<User> table = new AdvancedTableView<>();
    private final User currentUser;
    private final ModalService modalService;
    private final JasperReportService jasperReportService;
    private final ObservableList<User> masterData = FXCollections.observableArrayList();
    private final SessionManager sessionManager;

    public GestaoUsuariosView(AcessoService acessoService, User currentUser, ModalService modalService, JasperReportService jasperReportService, SessionManager sessionManager) {
        this.acessoService = acessoService;
        this.currentUser = currentUser;
        this.modalService = modalService;
        this.jasperReportService = jasperReportService;
        this.sessionManager = sessionManager;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("gestao-usuarios-view");

        setupHeader();
        setupTable();
        loadData();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Usuários");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Administre o acesso ao sistema");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CustomTextField search = new CustomTextField();
        search.setPromptText("Pesquisar...");
        search.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        search.setPrefWidth(250);

        Button btnNovo = new Button("Novo", IconUtils.icon(Feather.USER_PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnNovo.setOnAction(e -> showUserDialog(null));

        Button btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEditar.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showUserDialog(selected);
            else AlertUtils.showWarningAlert("Editar", "Selecione um usuário.");
        });

        Button btnExcluir = new Button("Excluir", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnExcluir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnExcluir.setOnAction(e -> deleteSelected());

        Button btnAcessos = new Button("Acessos", IconUtils.icon(Feather.LOCK, IconUtils.SIZE_SMALL));
        btnAcessos.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAcessos.setOnAction(e -> openAccessModal());
        Tooltip tipAcessos = new Tooltip("Definir permissões por módulo e opção");
        btnAcessos.setTooltip(tipAcessos);
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            btnAcessos.setDisable(true);
            btnAcessos.setTooltip(new Tooltip("Apenas administradores podem gerir acessos"));
        }
        Button btnAtivar = new Button("Ativar/Inativar", IconUtils.icon(Feather.TOGGLE_LEFT, IconUtils.SIZE_SMALL));
        btnAtivar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtivar.setOnAction(e -> toggleAtivoSelected());

        Button btnReset = new Button("Redefinir Senha", IconUtils.icon(Feather.KEY, IconUtils.SIZE_SMALL));
        btnReset.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnReset.setOnAction(e -> resetPasswordSelected());

        Button btnRelatorio = new Button("Relatório", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnRelatorio.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRelatorio.setOnAction(e -> exportPermissionsSelected());

        header.getChildren().addAll(titleBox, spacer, search, btnNovo, btnEditar, btnExcluir, btnAtivar, btnReset, btnAcessos, btnRelatorio);
        getChildren().add(header);

        table.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            boolean acessosDisabled = nv == null || nv.getRole() == Role.ADMIN || currentUser == null || currentUser.getRole() != Role.ADMIN;
            btnAcessos.setDisable(acessosDisabled);
            if (nv != null && nv.getRole() == Role.ADMIN) {
                btnAcessos.setTooltip(new Tooltip("Administrador possui acesso total"));
            } else if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
                btnAcessos.setTooltip(tipAcessos);
            }
            boolean adminSelected = nv != null && nv.getRole() == Role.ADMIN;
            btnExcluir.setDisable(nv == null || adminSelected);
            btnAtivar.setDisable(nv == null || adminSelected);
            if (adminSelected) {
                btnExcluir.setTooltip(new Tooltip("Não é permitido excluir o Administrador"));
                btnAtivar.setTooltip(new Tooltip("Não é permitido inativar o Administrador"));
            } else {
                btnExcluir.setTooltip(null);
                btnAtivar.setTooltip(null);
            }
        });

        search.textProperty().addListener((obs, ov, nv) -> {
            String q = nv != null ? nv.trim().toLowerCase() : "";
            table.setFilter(u -> q.isEmpty()
                    || (u.getNome() != null && u.getNome().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    || (u.getNif() != null && u.getNif().toLowerCase().contains(q)));
        });
    }

    private void setupTable() {
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<User, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));

        TableColumn<User, String> colRole = new TableColumn<>("Função");
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole() != null ? c.getValue().getRole().name() : ""));
        colRole.setCellFactory(col -> new TableCell<>() {
            private final Label tag = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); return;
                }
                boolean isAdmin = "ADMIN".equals(item);
                tag.setText(isAdmin ? "ADMIN  Acesso Total" : item);
                tag.getStyleClass().clear();
                tag.getStyleClass().add(Styles.TEXT_BOLD);
                if (isAdmin) {
                    tag.getStyleClass().add(Styles.ACCENT);
                }
                setGraphic(tag);
                setText(null);
            }
        });

        TableColumn<User, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNif()));

        TableColumn<User, String> colTel = new TableColumn<>("Telefone");
        colTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefone()));

        TableColumn<User, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isEnabled() ? "Ativo" : "Inativo"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label tag = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                tag.setText(item);
                tag.getStyleClass().clear();
                tag.getStyleClass().add(Styles.TEXT_BOLD);
                if ("Ativo".equalsIgnoreCase(item)) tag.getStyleClass().add(Styles.SUCCESS);
                else tag.getStyleClass().add(Styles.DANGER);
                setGraphic(tag);
                setText(null);
            }
        });

        TableColumn<User, String> colUltimo = new TableColumn<>("Último Acesso");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colUltimo.setCellValueFactory(c -> {
            var ua = c.getValue().getUltimoAcesso();
            String s = ua != null ? ua.format(fmt) : "";
            return new SimpleStringProperty(s);
        });

        table.getColumns().addAll(colNome, colEmail, colRole, colNif, colTel, colStatus, colUltimo);
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    private void loadData() {
        masterData.setAll(acessoService.listarTodosUsuarios());
        table.setData(masterData);
    }

    private static class UserFormData {
        User user;
        String rawPassword;
        public UserFormData(User user, String rawPassword) {
            this.user = user;
            this.rawPassword = rawPassword;
        }
    }

    private void showUserDialog(User existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtNome = new TextField(existing != null ? existing.getNome() : "");
        TextField txtEmail = new TextField(existing != null ? existing.getEmail() : "");
        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText(existing != null ? "Deixe em branco para manter" : "Senha obrigatória");

        TextField txtNif = new TextField(existing != null ? existing.getNif() : "");
        TextField txtTelefone = new TextField(existing != null ? existing.getTelefone() : "");

        ComboBox<Role> cbRole = new ComboBox<>();
        cbRole.getItems().addAll(Role.values());
        cbRole.setValue(existing != null ? existing.getRole() : Role.USER);
        
        CheckBox cbAtivo = new CheckBox("Ativo");
        cbAtivo.setSelected(existing == null || existing.isEnabled());

        grid.add(new Label("Nome *"), 0, 0); grid.add(txtNome, 1, 0);
        grid.add(new Label("Email *"), 0, 1); grid.add(txtEmail, 1, 1);
        grid.add(new Label("Senha"), 0, 2); grid.add(txtSenha, 1, 2);
        grid.add(new Label("NIF"), 0, 3); grid.add(txtNif, 1, 3);
        grid.add(new Label("Telefone"), 0, 4); grid.add(txtTelefone, 1, 4);
        grid.add(new Label("Função"), 0, 5); grid.add(cbRole, 1, 5);
        grid.add(cbAtivo, 1, 6);

        txtNome.setPrefWidth(300);
        cbRole.setMaxWidth(Double.MAX_VALUE);

        modalService.create()
                .title(existing == null ? "Novo Usuário" : "Editar Usuário")
                .content(grid)
                .dynamicSize()
                .withConfirmButton("Salvar", () -> {
                    if (txtNome.getText().isBlank() || txtEmail.getText().isBlank()) {
                        AlertUtils.showWarningAlert("Validação", "Nome e Email são obrigatórios.");
                        return false;
                    }
                    if (existing == null && txtSenha.getText().isBlank()) {
                        AlertUtils.showWarningAlert("Validação", "Senha é obrigatória para novos usuários.");
                        return false;
                    }
                    if (!txtNif.getText().isBlank() && !AngolaValidationUtils.isValidNif(txtNif.getText())) {
                        AlertUtils.showWarningAlert("Validação", "NIF inválido.");
                        return false;
                    }
                    if (!txtTelefone.getText().isBlank() && !AngolaValidationUtils.isValidTelefone(txtTelefone.getText())) {
                        AlertUtils.showWarningAlert("Validação", "Telefone inválido (deve ter 9 dígitos e começar com 9).");
                        return false;
                    }
                    try {
                        User u = existing != null ? existing : new User();
                        u.setNome(txtNome.getText());
                        u.setEmail(txtEmail.getText());
                        u.setNif(txtNif.getText());
                        u.setTelefone(txtTelefone.getText());
                        u.setRole(cbRole.getValue());
                        u.setActive(cbAtivo.isSelected());
                        acessoService.salvarUsuario(u, txtSenha.getText());
                        loadData();
                        AlertUtils.showInfoAlert("Sucesso", "Usuário salvo com sucesso.");
                        return true;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Erro ao salvar usuário.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void deleteSelected() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Excluir", "Selecione um usuário.");
            return;
        }
        if (selected.getRole() == Role.ADMIN) {
            modalService.create()
                    .title("Operação Bloqueada")
                    .content(new Label("Não é permitido excluir o utilizador Administrador."))
                    .autoSize()
                    .withConfirmButton("OK", () -> {})
                    .buildAndShow();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja excluir o usuário " + selected.getNome() + "?");
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    acessoService.excluirUsuario(currentUser, selected);
                    loadData();
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro", "Erro ao excluir usuário.", e);
                }
            }
        });
    }

    private void openAccessModal() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Acessos", "Selecione um usuário.");
            return;
        }
        if (selected.getRole() == Role.ADMIN) {
            modalService.create()
                    .title("Administrador")
                    .content(new Label("O utilizador Administrador já possui acesso total. Não é necessário definir permissões."))
                    .autoSize()
                    .withConfirmButton("OK", () -> {})
                    .buildAndShow();
            return;
        }
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            AlertUtils.showWarningAlert("Permissão Negada", "Apenas administradores podem gerir acessos.");
            return;
        }
        java.util.Map<String, java.util.List<String>> catalog = buildModuleCatalog();
        java.util.Map<String, java.util.Set<String>> selections = new java.util.HashMap<>();
        java.util.Map<String, java.util.Map<String, CheckBox>> controls = new java.util.HashMap<>();
        try {
            java.lang.reflect.Method m = acessoService.getClass().getMethod("listarAcessosUsuario", ao.allon.kubata.core.domain.User.class);
            @SuppressWarnings("unchecked")
            java.util.List<ao.allon.kubata.core.domain.UserAccessPermission> perms =
                    (java.util.List<ao.allon.kubata.core.domain.UserAccessPermission>) m.invoke(acessoService, selected);
            for (var p : perms) {
                selections.computeIfAbsent(p.getModulo(), k -> new java.util.HashSet<>()).add(p.getOpcao());
            }
        } catch (Exception ignored) { }

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));
        HBox top = new HBox(8);
        Label lbl = new Label("Defina os módulos e opções permitidos para " + selected.getNome());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        ComboBox<String> presets = new ComboBox<>();
        presets.getItems().addAll("Padrão", "Loja", "Backoffice", "Diretoria", "Fiscal (AGT)");
        presets.setPromptText("Aplicar preset");
        presets.valueProperty().addListener((o, ov, nv) -> applyPreset(nv, catalog, selections, controls));
        CustomTextField filter = new CustomTextField();
        filter.setPromptText("Filtrar opções...");
        filter.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        top.getChildren().addAll(lbl, sp, presets, filter);
        content.getChildren().add(top);

        VBox modulesBox = new VBox(10);
        for (var e : catalog.entrySet()) {
            String modulo = e.getKey();
            Card card = new Card();
            card.getStyleClass().add(Styles.ELEVATED_1);
            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label mLabel = new Label(modulo);
            mLabel.getStyleClass().add(Styles.TEXT_BOLD);
            Region rs = new Region(); HBox.setHgrow(rs, Priority.ALWAYS);
            CheckBox all = new CheckBox("Tudo");
            header.getChildren().addAll(IconUtils.icon(Feather.PACKAGE, IconUtils.SIZE_SMALL), mLabel, rs, all);
            card.setHeader(header);

            FlowPane options = new FlowPane(10, 8);
            java.util.Map<String, CheckBox> map = new java.util.HashMap<>();
            for (String opt : e.getValue()) {
                CheckBox cb = new CheckBox(opt);
                boolean sel = selections.getOrDefault(modulo, java.util.Set.of()).contains(opt);
                cb.setSelected(sel);
                map.put(opt, cb);
                options.getChildren().add(cb);
            }
            all.selectedProperty().addListener((o, ov, nv) -> {
                for (var cb : map.values()) cb.setSelected(nv);
            });
            card.setBody(options);
            controls.put(modulo, map);
            modulesBox.getChildren().add(card);
        }
        ScrollPane scroll = new ScrollPane(modulesBox); scroll.setFitToWidth(true); scroll.getStyleClass().add(Styles.FLAT);
        content.getChildren().addAll(new Separator(), scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        filter.textProperty().addListener((obs, ov, nv) -> {
            String q = nv != null ? nv.trim().toLowerCase() : "";
            for (var modEntry : controls.entrySet()) {
                String modulo = modEntry.getKey();
                java.util.Map<String, CheckBox> map = modEntry.getValue();
                boolean any = false;
                for (var opt : map.entrySet()) {
                    boolean vis = q.isEmpty()
                            || modulo.toLowerCase().contains(q)
                            || opt.getKey().toLowerCase().contains(q);
                    opt.getValue().setVisible(vis);
                    if (vis) any = true;
                }
                for (javafx.scene.Node n : modulesBox.getChildren()) {
                    if (n instanceof Card c) {
                        javafx.scene.Node h = c.getHeader();
                        if (h instanceof HBox hb) {
                            for (javafx.scene.Node child : hb.getChildren()) {
                                if (child instanceof Label lab && lab.getText().equals(modulo)) {
                                    c.setVisible(any);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        modalService.create()
                .title("Acessos do Usuário")
                .content(content)
                .dynamicSize()
                .withConfirmButton("Salvar", () -> {
                    java.util.Map<String, java.util.Set<String>> out = new java.util.HashMap<>();
                    for (var modEntry : controls.entrySet()) {
                        java.util.Set<String> set = new java.util.HashSet<>();
                        for (var optEntry : modEntry.getValue().entrySet()) {
                            if (optEntry.getValue().isSelected()) set.add(optEntry.getKey());
                        }
                        if (!set.isEmpty()) out.put(modEntry.getKey(), set);
                    }
                    try {
                        java.lang.reflect.Method m = acessoService.getClass().getMethod("salvarAcessosUsuario", Long.class, java.util.Map.class);
                        m.invoke(acessoService, selected.getId(), out);
                        if (currentUser != null && selected.getId() != null && currentUser.getId() != null && selected.getId().equals(currentUser.getId())) {
                            sessionManager.notifyPermissionsChanged();
                        }
                        modalService.create()
                                .title("Sucesso")
                                .content(new Label("Acessos do usuário atualizados com sucesso."))
                                .autoSize()
                                .withConfirmButton("OK", () -> {})
                                .buildAndShow();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao salvar acessos.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void toggleAtivoSelected() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Status", "Selecione um usuário.");
            return;
        }
        if (selected.getRole() == Role.ADMIN) {
            modalService.create()
                    .title("Operação Bloqueada")
                    .content(new Label("Não é permitido inativar o utilizador Administrador."))
                    .autoSize()
                    .withConfirmButton("OK", () -> {})
                    .buildAndShow();
            return;
        }
        boolean novo = !selected.isEnabled();
        try {
            acessoService.alterarStatusUsuario(selected.getId(), novo);
            loadData();
            AlertUtils.showInfoAlert("Status", novo ? "Usuário ativado." : "Usuário inativado.");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao alterar status do usuário.", e);
        }
    }

    private void resetPasswordSelected() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Senha", "Selecione um usuário.");
            return;
        }
        PasswordField p1 = new PasswordField();
        PasswordField p2 = new PasswordField();
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        grid.add(new Label("Nova senha"), 0, 0); grid.add(p1, 1, 0);
        grid.add(new Label("Confirmar senha"), 0, 1); grid.add(p2, 1, 1);
        modalService.create()
                .title("Redefinir Senha")
                .content(grid)
                .dynamicSize()
                .withConfirmButton("Salvar", () -> {
                    String s1 = p1.getText(); String s2 = p2.getText();
                    if (s1 == null || s1.isBlank() || !s1.equals(s2)) {
                        AlertUtils.showWarningAlert("Validação", "As senhas não coincidem ou são inválidas.");
                        return false;
                    }
                    try {
                        acessoService.redefinirSenha(selected.getId(), s1);
                        AlertUtils.showInfoAlert("Sucesso", "Senha atualizada.");
                        return true;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao redefinir senha.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void exportPermissionsSelected() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Relatório", "Selecione um usuário.");
            return;
        }
        try {
            java.lang.reflect.Method m = acessoService.getClass().getMethod("listarAcessosUsuario", ao.allon.kubata.core.domain.User.class);
            @SuppressWarnings("unchecked")
            java.util.List<ao.allon.kubata.core.domain.UserAccessPermission> perms =
                    (java.util.List<ao.allon.kubata.core.domain.UserAccessPermission>) m.invoke(acessoService, selected);
            var print = jasperReportService.prepararPermissoesUsuario(selected, perms);
            jasperReportService.showReport(print);
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao gerar relatório de permissões.", e);
        }
    }

    private void applyPreset(String preset, java.util.Map<String, java.util.List<String>> catalog,
                             java.util.Map<String, java.util.Set<String>> selections,
                             java.util.Map<String, java.util.Map<String, CheckBox>> controls) {
        selections.clear();
        java.util.function.BiConsumer<String, String[]> add = (m, opts) -> {
            java.util.Set<String> set = selections.computeIfAbsent(m, k -> new java.util.HashSet<>());
            for (String o : opts) set.add(o);
        };
        if ("Loja".equals(preset)) {
            add.accept("FATURACAO", new String[]{"Emitir", "Anular", "Ver"});
            add.accept("CLIENTES", new String[]{"Criar", "Ver"});
            add.accept("RECIBOS", new String[]{"Emitir", "Ver"});
            add.accept("PDV", new String[]{"Abrir", "Fechar", "Vender", "Balança", "Editar Quantidade"});
        } else if ("Backoffice".equals(preset)) {
            add.accept("ESTOQUE", new String[]{"Ver", "Ajustar"});
            add.accept("PRODUTOS", new String[]{"Criar", "Editar", "Ver"});
            add.accept("COMPRAS", new String[]{"Emitir", "Receber", "Ver"});
            add.accept("FORNECEDORES", new String[]{"Criar", "Ver"});
        } else if ("Diretoria".equals(preset)) {
            add.accept("RELATORIOS", new String[]{"Financeiro", "Vendas", "Inventário"});
            add.accept("SAFT", new String[]{"Exportar"});
        } else if ("Fiscal (AGT)".equals(preset)) {
            add.accept("SAFT", new String[]{"Exportar", "Validar"});
            add.accept("IMPOSTOS", new String[]{"Tabelas", "Mapas"});
            add.accept("SERIES", new String[]{"Gerir"});
            add.accept("MOTIVOS_ISENCAO", new String[]{"Gerir"});
            add.accept("RETENCAO", new String[]{"Gerir"});
        }
        for (var mod : controls.entrySet()) {
            for (var opt : mod.getValue().entrySet()) {
                boolean sel = selections.getOrDefault(mod.getKey(), java.util.Set.of()).contains(opt.getKey());
                opt.getValue().setSelected(sel);
            }
        }
    }

    private java.util.Map<String, java.util.List<String>> buildModuleCatalog() {
        java.util.Map<String, java.util.List<String>> cat = new java.util.LinkedHashMap<>();
        cat.put("FATURACAO", java.util.List.of("Emitir", "Anular", "Ver", "Descontos", "Preços"));
        cat.put("RECIBOS", java.util.List.of("Emitir", "Anular", "Ver"));
        cat.put("CLIENTES", java.util.List.of("Criar", "Editar", "Ver"));
        cat.put("PRODUTOS", java.util.List.of("Criar", "Editar", "Ver"));
        cat.put("ESTOQUE", java.util.List.of("Ver", "Ajustar", "Transferir"));
        cat.put("COMPRAS", java.util.List.of("Emitir", "Receber", "Ver"));
        cat.put("LOGISTICA", java.util.List.of("Guias Remessa", "Guias Transporte"));
        cat.put("RELATORIOS", java.util.List.of("Financeiro", "Vendas", "Inventário"));
        cat.put("SAFT", java.util.List.of("Exportar", "Validar", "Assinar"));
        cat.put("IMPOSTOS", java.util.List.of("Tabelas", "Mapas"));
        cat.put("SERIES", java.util.List.of("Gerir"));
        cat.put("MOTIVOS_ISENCAO", java.util.List.of("Gerir"));
        cat.put("RETENCAO", java.util.List.of("Gerir"));
        cat.put("CONFIG", java.util.List.of("Empresa", "Email", "Backup"));
        cat.put("USUARIOS", java.util.List.of("Criar", "Editar", "Ver"));
        cat.put("PDV", java.util.List.of("Abrir", "Fechar", "Vender", "Balança", "Editar Quantidade", "Editar Preço", "Editar Desconto"));
        return cat;
    }
}
