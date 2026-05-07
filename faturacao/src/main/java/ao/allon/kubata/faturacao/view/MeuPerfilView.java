package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserAccessPermission;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
import atlantafx.base.controls.CustomTextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MeuPerfilView extends BorderPane {

    private final SessionManager sessionManager;
    private final AcessoService acessoService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ao.allon.kubata.core.repository.UserRepository userRepository;

    private Label lblNome;
    private Label lblEmail;
    private Label lblTelefone;
    private Label lblNif;
    private Label lblRole;
    private Label lblStatus;
    private Label lblUltimoAcesso;
    private Label lblMfa;
    // Mapeamento de módulos para ícones e descrições
    private static final Map<String, ModuloInfo> MODULOS_INFO = createModulosInfo();
    
    private static Map<String, ModuloInfo> createModulosInfo() {
        Map<String, ModuloInfo> map = new HashMap<>();
        map.put("FATURACAO", new ModuloInfo(Feather.FILE_TEXT, "Faturação", "Gestão de faturas e documentos fiscais"));
        map.put("PDV", new ModuloInfo(Feather.SHOPPING_CART, "Ponto de Venda", "Frente de caixa e vendas diretas"));
        map.put("ESTOQUE", new ModuloInfo(Feather.PACKAGE, "Estoque/Inventário", "Controle de produtos e movimentações"));
        map.put("CLIENTES", new ModuloInfo(Feather.USERS, "Clientes", "Cadastro e gestão de clientes"));
        map.put("FORNECEDORES", new ModuloInfo(Feather.TRUCK, "Fornecedores", "Gestão de fornecedores"));
        map.put("RELATORIOS", new ModuloInfo(Feather.BAR_CHART_2, "Relatórios", "Relatórios e análises"));
        map.put("CONFIG", new ModuloInfo(Feather.SETTINGS, "Configurações", "Configurações do sistema"));
        map.put("USUARIOS", new ModuloInfo(Feather.USER_CHECK, "Usuários", "Gestão de usuários e permissões"));
        map.put("SAFT", new ModuloInfo(Feather.FILE_PLUS, "SAF-T AO", "Exportação SAFT para AGT"));
        map.put("IMPOSTOS", new ModuloInfo(Feather.PERCENT, "Impostos", "Configuração de impostos e taxas"));
        map.put("CAIXA", new ModuloInfo(Feather.DOLLAR_SIGN, "Caixa", "Gestão de caixa e fluxo"));
        map.put("BANCOS", new ModuloInfo(Feather.CREDIT_CARD, "Bancos", "Contas bancárias e transferências"));
        map.put("RECIBOS", new ModuloInfo(Feather.FILE, "Recibos", "Emissão de recibos"));
        map.put("DRE", new ModuloInfo(Feather.BAR_CHART, "DRE", "Demonstração do Resultado"));
        map.put("BALANCETE", new ModuloInfo(Feather.PIE_CHART, "Balancete", "Balancete contábil"));
        map.put("FINANCEIRO", new ModuloInfo(Feather.DOLLAR_SIGN, "Financeiro", "Resumo financeiro"));
        map.put("CONTAS_PAGAR", new ModuloInfo(Feather.ARROW_UP, "Contas a Pagar", "Gestão de despesas"));
        map.put("CONTAS_RECEBER", new ModuloInfo(Feather.ARROW_DOWN, "Contas a Receber", "Gestão de recebíveis"));
        return Collections.unmodifiableMap(map);
    }

    // Mapeamento de ações para ícones e descrições
    private static final Map<String, AcaoInfo> ACOES_INFO = createAcoesInfo();
    
    private static Map<String, AcaoInfo> createAcoesInfo() {
        Map<String, AcaoInfo> map = new HashMap<>();
        map.put("Ver", new AcaoInfo(Feather.EYE, "Visualizar", "Permite visualizar registros"));
        map.put("Criar", new AcaoInfo(Feather.PLUS_CIRCLE, "Criar", "Permite criar novos registros"));
        map.put("Editar", new AcaoInfo(Feather.EDIT, "Editar", "Permite modificar registros existentes"));
        map.put("Excluir", new AcaoInfo(Feather.TRASH_2, "Excluir", "Permite remover registros"));
        map.put("Exportar", new AcaoInfo(Feather.DOWNLOAD, "Exportar", "Permite exportar dados"));
        map.put("Admin", new AcaoInfo(Feather.SHIELD, "Administrar", "Acesso total ao módulo"));
        map.put("Abrir", new AcaoInfo(Feather.UNLOCK, "Abrir Caixa", "Permite abertura de caixa"));
        map.put("Fechar", new AcaoInfo(Feather.LOCK, "Fechar Caixa", "Permite fechamento de caixa"));
        map.put("Movimento", new AcaoInfo(Feather.MOVE, "Movimentar", "Permite movimentações de estoque"));
        map.put("Transferir", new AcaoInfo(Feather.SHUFFLE, "Transferir", "Permite transferências entre armazéns"));
        map.put("Ajustar", new AcaoInfo(Feather.SLIDERS, "Ajustar", "Permite ajustes de inventário"));
        return Collections.unmodifiableMap(map);
    }

    public MeuPerfilView(SessionManager sessionManager, 
                       AcessoService acessoService,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                       ao.allon.kubata.core.repository.UserRepository userRepository) {
        this.sessionManager = sessionManager;
        this.acessoService = acessoService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add(Styles.FLAT);
        scrollPane.setContent(buildContent());

        setCenter(scrollPane);
    }

    private Node buildContent() {
        FlowPane content = new FlowPane(20, 20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setRowValignment(VPos.TOP);

        VBox leftPanel = buildLeftPanel();
        VBox centerPanel = buildCenterPanel();
        VBox rightPanel = buildRightPanel();

        // Ajustar larguras para responsividade
        leftPanel.prefWidthProperty().bind(content.widthProperty().divide(3.5).subtract(20));
        leftPanel.setMinWidth(280);
        leftPanel.setMaxWidth(350);

        centerPanel.prefWidthProperty().bind(content.widthProperty().divide(2.0).subtract(20));
        centerPanel.setMinWidth(400);

        rightPanel.prefWidthProperty().bind(content.widthProperty().divide(4.5).subtract(20));
        rightPanel.setMinWidth(220);
        rightPanel.setMaxWidth(280);

        content.getChildren().addAll(leftPanel, centerPanel, rightPanel);
        return content;
    }

    private VBox buildLeftPanel() {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label("Perfil do Usuário"));
        card.getHeader().getStyleClass().add(Styles.TITLE_3);

        VBox avatarBox = new VBox(10);
        avatarBox.setAlignment(Pos.CENTER);
        avatarBox.setPadding(new Insets(20));

        StackPane avatarCircle = new StackPane();
        avatarCircle.setMinSize(100, 100);
        avatarCircle.setMaxSize(100, 100);
        avatarCircle.setStyle("-fx-background-color: -color-accent-emphasis; -fx-background-radius: 50;");

        User user = sessionManager.getUserObject();
        String iniciais = user != null && user.getNome() != null 
            ? Arrays.stream(user.getNome().split(" "))
                    .filter(s -> !s.isEmpty())
                    .limit(2)
                    .map(s -> String.valueOf(s.charAt(0)).toUpperCase())
                    .collect(Collectors.joining())
            : "U";

        Label lblAvatar = new Label(iniciais);
        lblAvatar.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: white;");
        avatarCircle.getChildren().add(lblAvatar);

        lblNome = new Label(user != null ? user.getNome() : "Usuário");
        lblNome.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        lblNome.setAlignment(Pos.CENTER);

        lblRole = new Label(user != null && user.getRole() != null ? formatRole(user.getRole()) : "-");
        lblRole.getStyleClass().add(Styles.TEXT_MUTED);

        lblStatus = new Label();
        updateStatusLabel(user);

        avatarBox.getChildren().addAll(avatarCircle, lblNome, lblRole, lblStatus);

        card.setBody(avatarBox);

        VBox box = new VBox(card);
        box.setPrefWidth(280);
        return box;
    }

    private VBox buildCenterPanel() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.TOP_CENTER);

        // Card de Informações Pessoais
        Card infoCard = buildInfoCard();

        // Card de Acessos/Permissões
        Card acessosCard = buildAcessosCard();

        box.getChildren().addAll(infoCard, acessosCard);
        VBox.setVgrow(acessosCard, Priority.ALWAYS);

        return box;
    }

    private Card buildInfoCard() {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label("Informações Pessoais"));
        card.getHeader().getStyleClass().add(Styles.TITLE_4);

        User user = sessionManager.getUserObject();

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));

        lblEmail = createInfoLabel(user != null ? user.getEmail() : "-");
        lblTelefone = createInfoLabel(user != null ? user.getTelefone() : "-");
        lblNif = createInfoLabel(user != null ? user.getNif() : "-");
        lblUltimoAcesso = createInfoLabel(user != null && user.getUltimoAcesso() != null 
            ? user.getUltimoAcesso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "Nunca");
        lblMfa = createInfoLabel(user != null && user.isMfaEnabled() ? "Ativado" : "Desativado");

        if (user != null && user.isMfaEnabled()) {
            lblMfa.setTextFill(Color.GREEN);
        } else {
            lblMfa.setTextFill(Color.GRAY);
        }

        int row = 0;
        grid.addRow(row++, createInfoRow("Email:", lblEmail, Feather.MAIL));
        grid.addRow(row++, createInfoRow("Telefone:", lblTelefone, Feather.PHONE));
        grid.addRow(row++, createInfoRow("NIF:", lblNif, Feather.CREDIT_CARD));
        grid.addRow(row++, createInfoRow("Último Acesso:", lblUltimoAcesso, Feather.CLOCK));
        grid.addRow(row++, createInfoRow("Autenticação 2FA:", lblMfa, Feather.SHIELD));

        Button btnEditar = new Button("Editar Dados", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        btnEditar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnEditar.setOnAction(e -> showEditarDadosDialog());

        Button btnSenha = new Button("Alterar Senha", IconUtils.icon(Feather.KEY, IconUtils.SIZE_SMALL));
        btnSenha.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnSenha.setOnAction(e -> showAlterarSenhaDialog());

        HBox buttons = new HBox(10, btnEditar, btnSenha);
        buttons.setPadding(new Insets(0, 15, 15, 15));

        VBox body = new VBox(grid, buttons);
        card.setBody(body);

        return card;
    }

    private Label createInfoLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add(Styles.TEXT_BOLD);
        return lbl;
    }

    private HBox createInfoRow(String label, Label value, Feather icon) {
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add(Styles.TEXT_MUTED);
        lblLabel.setMinWidth(120);

        FontIcon ico = IconUtils.icon(icon, IconUtils.SIZE_SMALL);
        ico.getStyleClass().add(Styles.TEXT_MUTED);

        HBox box = new HBox(8, ico, lblLabel, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Card buildAcessosCard() {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Meus Acessos no Sistema");
        title.getStyleClass().add(Styles.TITLE_4);
        
        Label lblCount = new Label();
        lblCount.getStyleClass().addAll("badge", "accent");
        header.getChildren().addAll(title, lblCount);
        card.setHeader(header);

        User user = sessionManager.getUserObject();
        
        // Verificar se usuário está disponível
        if (user == null) {
            VBox errorBox = new VBox(10);
            errorBox.setPadding(new Insets(30));
            errorBox.setAlignment(Pos.CENTER);
            
            FontIcon errorIcon = IconUtils.icon(Feather.ALERT_CIRCLE, 32);
            errorIcon.setFill(Color.ORANGE);
            
            Label lblError = new Label("Dados do usuário não disponíveis");
            lblError.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.TEXT_NORMAL);
            
            Label lblDesc = new Label("Não foi possível carregar suas informações de acesso.");
            lblDesc.getStyleClass().add(Styles.TEXT_MUTED);
            
            lblCount.setText("0");
            
            errorBox.getChildren().addAll(errorIcon, lblError, lblDesc);
            card.setBody(errorBox);
            return card;
        }
        
        if (user.getRole() == Role.ADMIN) {
            // Administrador - acesso total
            VBox adminBox = new VBox(15);
            adminBox.setPadding(new Insets(20));
            adminBox.setAlignment(Pos.CENTER);
            
            FontIcon shieldIcon = IconUtils.icon(Feather.SHIELD, 48);
            shieldIcon.setFill(Color.web("#10b981"));
            
            Label lblAdmin = new Label("Acesso Total ao Sistema");
            lblAdmin.getStyleClass().addAll(Styles.TITLE_3, Styles.TEXT_BOLD);
            lblAdmin.setTextFill(Color.web("#10b981"));
            
            Label lblDesc = new Label("Como administrador, você tem acesso irrestrito a todos os módulos e funcionalidades do sistema.");
            lblDesc.setWrapText(true);
            lblDesc.getStyleClass().add(Styles.TEXT_MUTED);
            lblDesc.setAlignment(Pos.CENTER);
            
            lblCount.setText("TOTAL");
            
            adminBox.getChildren().addAll(shieldIcon, lblAdmin, lblDesc);
            card.setBody(adminBox);
            return card;
        }

        // Carregar acessos reais do usuário
        List<UserAccessPermission> acessos;
        try {
            acessos = acessoService.listarAcessosUsuario(user);
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao carregar permissões de acesso", e);
            acessos = new ArrayList<>();
        }
        lblCount.setText(String.valueOf(acessos.size()));

        if (acessos.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setPadding(new Insets(30));
            emptyBox.setAlignment(Pos.CENTER);
            
            FontIcon lockIcon = IconUtils.icon(Feather.LOCK, 32);
            lockIcon.setFill(Color.GRAY);
            
            Label lblEmpty = new Label("Nenhum acesso específico configurado");
            lblEmpty.getStyleClass().add(Styles.TEXT_MUTED);
            
            emptyBox.getChildren().addAll(lockIcon, lblEmpty);
            card.setBody(emptyBox);
            return card;
        }

        // Agrupar acessos por módulo
        Map<String, List<UserAccessPermission>> acessosPorModulo = acessos.stream()
            .collect(Collectors.groupingBy(UserAccessPermission::getModulo));

        // Container scrollable para os módulos
        VBox modulosContainer = new VBox(10);
        modulosContainer.setPadding(new Insets(10));

        // Criar card para cada módulo
        for (Map.Entry<String, List<UserAccessPermission>> entry : acessosPorModulo.entrySet()) {
            String modulo = entry.getKey();
            List<UserAccessPermission> permissoes = entry.getValue();
            
            Node moduloCard = buildModuloAcessoCard(modulo, permissoes);
            modulosContainer.getChildren().add(moduloCard);
        }

        ScrollPane scrollPane = new ScrollPane(modulosContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setMinHeight(300);
        
        card.setBody(scrollPane);

        return card;
    }

    private Node buildModuloAcessoCard(String modulo, List<UserAccessPermission> permissoes) {
        ModuloInfo info = MODULOS_INFO.getOrDefault(modulo, 
            new ModuloInfo(Feather.FOLDER, modulo, "Módulo do sistema"));

        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");

        // Header do módulo
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon moduloIcon = IconUtils.icon(info.icone, 20);
        moduloIcon.setFill(Color.web("#2563eb"));
        
        VBox titleBox = new VBox(2);
        Label lblModulo = new Label(info.nome);
        lblModulo.getStyleClass().addAll(Styles.TEXT_BOLD, "text-medium");
        
        Label lblDesc = new Label(info.descricao);
        lblDesc.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        
        titleBox.getChildren().addAll(lblModulo, lblDesc);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Badge com quantidade de ações
        Label lblBadge = new Label(String.valueOf(permissoes.size()));
        lblBadge.getStyleClass().addAll("badge", "success");
        
        header.getChildren().addAll(moduloIcon, titleBox, spacer, lblBadge);

        // Grid de ações/permissões
        FlowPane permissoesBox = new FlowPane(8, 8);
        permissoesBox.setPadding(new Insets(5, 0, 0, 30));

        for (UserAccessPermission perm : permissoes) {
            Node acaoBadge = buildAcaoBadge(perm.getOpcao());
            permissoesBox.getChildren().add(acaoBadge);
        }

        card.getChildren().addAll(header, permissoesBox);
        return card;
    }

    private Node buildAcaoBadge(String opcao) {
        AcaoInfo info = ACOES_INFO.getOrDefault(opcao,
            new AcaoInfo(Feather.CHECK, opcao, "Permissão no sistema"));

        HBox badge = new HBox(6);
        badge.setPadding(new Insets(6, 10, 6, 10));
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setStyle("-fx-background-color: -color-success-subtle; -fx-background-radius: 6;");

        FontIcon icon = IconUtils.icon(info.icone, 14);
        icon.setFill(Color.web("#10b981"));

        Label lblAcao = new Label(info.nome);
        lblAcao.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #059669;");

        Tooltip tooltip = new Tooltip(info.descricao);
        Tooltip.install(badge, tooltip);

        badge.getChildren().addAll(icon, lblAcao);
        return badge;
    }

    private VBox buildRightPanel() {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label("Ações Rápidas"));
        card.getHeader().getStyleClass().add(Styles.TITLE_4);

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));

        Button btnAtualizar = new Button("Atualizar Dados", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnAtualizar.setMaxWidth(Double.MAX_VALUE);
        btnAtualizar.setOnAction(e -> refreshData());

        Button btnExportar = new Button("Exportar Perfil", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setMaxWidth(Double.MAX_VALUE);
        btnExportar.setOnAction(e -> exportarPerfil());

        Button btnSuporte = new Button("Suporte Técnico", IconUtils.icon(Feather.HELP_CIRCLE, IconUtils.SIZE_SMALL));
        btnSuporte.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnSuporte.setMaxWidth(Double.MAX_VALUE);
        btnSuporte.setOnAction(e -> AlertUtils.showInfoAlert("Suporte", "Entre em contato com o suporte técnico."));

        Separator sep = new Separator();

        Label lblInfo = new Label("Informações do Sistema");
        lblInfo.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        Label lblVersao = new Label("Versão: 2.1.0");
        lblVersao.getStyleClass().add(Styles.TEXT_SMALL);

        Label lblBuild = new Label("Build: 2025.03.01");
        lblBuild.getStyleClass().add(Styles.TEXT_SMALL);

        box.getChildren().addAll(
            btnAtualizar, btnExportar, btnSuporte,
            sep, lblInfo, lblVersao, lblBuild
        );

        card.setBody(box);

        VBox container = new VBox(card);
        container.setPrefWidth(220);
        return container;
    }

    private void updateStatusLabel(User user) {
        if (user == null) {
            lblStatus.setText("Status: Desconhecido");
            lblStatus.setTextFill(Color.GRAY);
            return;
        }

        if (user.getActive()) {
            lblStatus.setText("● Ativo");
            lblStatus.setTextFill(Color.GREEN);
        } else {
            lblStatus.setText("● Inativo");
            lblStatus.setTextFill(Color.RED);
        }
        lblStatus.getStyleClass().add(Styles.TEXT_BOLD);
    }

    private String formatRole(Role role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case USER -> "Usuário";
            case OPERATOR -> "Operador";
            case DIRETOR -> "Diretor";
            case GERENTE_FINANCEIRO -> "Gerente Financeiro";
            case CONTABILISTA -> "Contabilista";
            case OPERADOR_FATURACAO -> "Operador de Faturação";
            case CAIXA -> "Caixa";
            case SUPERVISOR_VENDAS -> "Supervisor de Vendas";
            case VENDEDOR -> "Vendedor";
            case ESTOQUE -> "Estoque";
            case COMPRAS -> "Compras";
            case LOGISTICA -> "Logística";
            case AUDITOR -> "Auditor";
            case SUPORTE_TI -> "Suporte TI";
            case RESPONSAVEL_FISCAL_AO -> "Responsável Fiscal (AO)";
            case VISITANTE -> "Visitante";
        };
    }

    private void refreshData() {
        try {
            User user = sessionManager.getUserObject();
            if (user == null) return;

            // Recarregar do banco
            userRepository.findById(user.getId()).ifPresent(u -> {
                lblNome.setText(u.getNome());
                lblEmail.setText(u.getEmail());
                lblTelefone.setText(u.getTelefone() != null ? u.getTelefone() : "-");
                lblNif.setText(u.getNif() != null ? u.getNif() : "-");
                lblUltimoAcesso.setText(u.getUltimoAcesso() != null 
                    ? u.getUltimoAcesso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "Nunca");
                lblMfa.setText(u.isMfaEnabled() ? "Ativado" : "Desativado");
                lblMfa.setTextFill(u.isMfaEnabled() ? Color.GREEN : Color.GRAY);
                updateStatusLabel(u);
                // Recarregar acessos
                setCenter(buildContent());
            });

            AlertUtils.showInfoAlert("Sucesso", "Dados atualizados com sucesso!");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao atualizar dados do perfil", e);
        }
    }

    private void exportarPerfil() {
        AlertUtils.showInfoAlert("Exportar", "Funcionalidade de exportação de perfil será implementada em breve.");
    }

    private void showEditarDadosDialog() {
        User user = sessionManager.getUserObject();
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Dados Pessoais");
        dialog.setHeaderText("Atualize suas informações");

        CustomTextField txtNome = new CustomTextField(user.getNome());
        txtNome.setPromptText("Nome completo");
        txtNome.setLeft(IconUtils.icon(Feather.USER, IconUtils.SIZE_SMALL));

        CustomTextField txtEmail = new CustomTextField(user.getEmail());
        txtEmail.setPromptText("Email");
        txtEmail.setLeft(IconUtils.icon(Feather.MAIL, IconUtils.SIZE_SMALL));

        CustomTextField txtTelefone = new CustomTextField(user.getTelefone() != null ? user.getTelefone() : "");
        txtTelefone.setPromptText("Telefone");
        txtTelefone.setLeft(IconUtils.icon(Feather.PHONE, IconUtils.SIZE_SMALL));

        CustomTextField txtNif = new CustomTextField(user.getNif() != null ? user.getNif() : "");
        txtNif.setPromptText("NIF");
        txtNif.setLeft(IconUtils.icon(Feather.CREDIT_CARD, IconUtils.SIZE_SMALL));

        VBox content = new VBox(10, 
            new Label("Nome:"), txtNome,
            new Label("Email:"), txtEmail,
            new Label("Telefone:"), txtTelefone,
            new Label("NIF:"), txtNif
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                user.setNome(txtNome.getText());
                user.setEmail(txtEmail.getText());
                user.setTelefone(txtTelefone.getText().isBlank() ? null : txtTelefone.getText());
                user.setNif(txtNif.getText().isBlank() ? null : txtNif.getText());
                
                try {
                    userRepository.save(user);
                    AlertUtils.showInfoAlert("Sucesso", "Dados atualizados com sucesso!");
                    refreshData();
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro", "Erro ao atualizar dados", e);
                }
            }
        });
    }

    private void showAlterarSenhaDialog() {
        User user = sessionManager.getUserObject();
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Alterar Senha");
        dialog.setHeaderText("Digite a nova senha");

        PasswordField txtSenhaAtual = new PasswordField();
        txtSenhaAtual.setPromptText("Senha atual");

        PasswordField txtNovaSenha = new PasswordField();
        txtNovaSenha.setPromptText("Nova senha (mín. 6 caracteres)");

        PasswordField txtConfirmar = new PasswordField();
        txtConfirmar.setPromptText("Confirmar nova senha");

        VBox content = new VBox(10,
            new Label("Senha Atual:"), txtSenhaAtual,
            new Label("Nova Senha:"), txtNovaSenha,
            new Label("Confirmar:"), txtConfirmar
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String senhaAtual = txtSenhaAtual.getText();
                String novaSenha = txtNovaSenha.getText();
                String confirmar = txtConfirmar.getText();

                if (senhaAtual.isBlank() || novaSenha.isBlank() || confirmar.isBlank()) {
                    AlertUtils.showErrorAlert("Erro", "Todos os campos são obrigatórios.");
                    return;
                }

                if (!novaSenha.equals(confirmar)) {
                    AlertUtils.showErrorAlert("Erro", "As senhas não coincidem.");
                    return;
                }

                if (novaSenha.length() < 6) {
                    AlertUtils.showErrorAlert("Erro", "A nova senha deve ter pelo menos 6 caracteres.");
                    return;
                }

                // Verificar senha atual
                try {
                    if (!passwordEncoder.matches(senhaAtual, user.getPassword())) {
                        AlertUtils.showErrorAlert("Erro", "Senha atual incorreta.");
                        return;
                    }
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro", "Erro ao verificar senha atual", e);
                    return;
                }

                try {
                    acessoService.redefinirSenha(user.getId(), novaSenha);
                    AlertUtils.showInfoAlert("Sucesso", "Senha alterada com sucesso!");
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro", "Erro ao alterar senha", e);
                }
            }
        });
    }

    // Classes auxiliares para mapeamento de informações
    private record ModuloInfo(Feather icone, String nome, String descricao) {}
    private record AcaoInfo(Feather icone, String nome, String descricao) {}
    
    // Classe para list view (caso precise futuramente)
    public static class AcessoItem {
        public final String modulo;
        public final String opcao;
        public final boolean temAcesso;

        public AcessoItem(String modulo, String opcao, boolean temAcesso) {
            this.modulo = modulo;
            this.opcao = opcao;
            this.temAcesso = temAcesso;
        }
    }
}
