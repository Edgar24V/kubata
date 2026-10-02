package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Centro de Ajuda integrado do Kubata Administrator.
 *
 * <p>É deliberadamente autónomo: a documentação fica disponível dentro da
 * aplicação, sem depender de internet ou de um navegador externo.</p>
 */
@Component
public class HelpCenterView extends BorderPane {

    private record Topic(
            String id,
            String title,
            String subtitle,
            Feather icon,
            List<String> keywords,
            Supplier<Node> content
    ) {}

    private final SessionManager sessionManager;
    private final VBox topicList = new VBox(4);
    private final VBox contentBox = new VBox(18);
    private final TextField searchField = new TextField();
    private final Label resultCount = new Label();

    private final List<Topic> topics = new ArrayList<>();
    private String selectedId;

    public HelpCenterView(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        getStyleClass().add("kubata-help-root");
        String css = getClass().getResource("/ao/allon/kubata/admin/ui/styles/help-center.css").toExternalForm();
        getStylesheets().add(css);
        setMinSize(0, 0);
        buildTopics();
        buildUI();
        selectTopic(topics.get(0).id());
    }

    private void buildUI() {
        VBox header = new VBox(4);
        header.getStyleClass().add("kubata-help-header");
        header.getChildren().addAll(
                titleRow(),
                new Label("Documentação integrada para aprender, administrar e trabalhar com segurança no Kubata Administrator.")
        );
        ((Label) header.getChildren().get(1)).getStyleClass().add("kubata-help-subtitle");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("kubata-help-search-row");

        searchField.setPromptText("Pesquisar por utilizadores, sessões, MFA, empresas, auditoria...");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter(newValue));

        resultCount.getStyleClass().add("kubata-help-result-count");
        searchRow.getChildren().addAll(searchField, resultCount);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox top = new VBox(14, header, searchRow);
        top.getStyleClass().add("kubata-help-top");
        setTop(top);

        topicList.getStyleClass().add("kubata-help-topic-list");

        ScrollPane navScroll = new ScrollPane(topicList);
        navScroll.setFitToWidth(true);
        navScroll.setFitToHeight(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.getStyleClass().add("kubata-help-nav-scroll");
        navScroll.setPrefWidth(285);

        VBox nav = new VBox(10);
        nav.getStyleClass().add("kubata-help-nav");
        Label navTitle = new Label("Navegação");
        navTitle.getStyleClass().add("kubata-help-nav-title");
        nav.getChildren().addAll(navTitle, navScroll);
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        ScrollPane contentScroll = new ScrollPane(contentBox);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("kubata-help-content-scroll");
        contentScroll.setPadding(new Insets(0));
        contentBox.setPadding(new Insets(0, 4, 24, 4));

        BorderPane body = new BorderPane();
        body.setLeft(nav);
        body.setCenter(contentScroll);
        body.getStyleClass().add("kubata-help-body");
        BorderPane.setMargin(nav, new Insets(18, 12, 18, 18));
        BorderPane.setMargin(contentScroll, new Insets(18, 18, 18, 12));
        setCenter(body);
    }

    private Node titleRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane(IconUtils.icon(Feather.HELP_CIRCLE, 27));
        icon.getStyleClass().add("kubata-help-hero-icon");

        VBox text = new VBox(2);
        Label title = new Label("Centro de Ajuda");
        title.getStyleClass().add("kubata-help-title");
        Label context = new Label("Kubata Administrator");
        context.getStyleClass().add("kubata-help-context");
        text.getChildren().addAll(title, context);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label user = new Label();
        user.getStyleClass().add("kubata-help-user-badge");
        if (sessionManager.getUser() != null) {
            user.setText("Sessão: " + sessionManager.getUser().getNome());
        } else {
            user.setText("Sessão não autenticada");
        }

        row.getChildren().addAll(icon, text, spacer, user);
        return row;
    }

    private void buildTopics() {
        topics.add(new Topic(
                "overview",
                "Como funciona o Administrator",
                "Visão geral do ambiente administrativo",
                Feather.HOME,
                List.of("administrator", "inicio", "consola", "ribbon", "menu", "navegação"),
                this::contentOverview
        ));
        topics.add(new Topic(
                "login",
                "Entrada e autenticação",
                "Login, password, MFA e bloqueios",
                Feather.LOG_IN,
                List.of("login", "senha", "password", "mfa", "totp", "bloqueio", "autenticação", "sessão"),
                this::contentLogin
        ));
        topics.add(new Topic(
                "navigation",
                "Navegação e Ribbon",
                "Como localizar cada função",
                Feather.GRID,
                List.of("ribbon", "abas", "grupos", "consola", "tab", "janela"),
                this::contentNavigation
        ));
        topics.add(new Topic(
                "users",
                "Utilizadores",
                "Criar, editar, activar e proteger contas",
                Feather.USERS,
                List.of("utilizadores", "utilizador", "conta", "role", "perfil", "password", "empresa", "filial"),
                this::contentUsers
        ));
        topics.add(new Topic(
                "profiles",
                "Perfis e permissões",
                "Como controlar o que cada pessoa pode fazer",
                Feather.SHIELD,
                List.of("perfis", "permissões", "acesso", "rbac", "módulos", "empresa"),
                this::contentProfiles
        ));
        topics.add(new Topic(
                "sessions",
                "Sessões e encerramento",
                "Sessão própria, sessões de outros utilizadores e Terminar todas",
                Feather.POWER,
                List.of("sessões", "terminar", "encerrar", "logout", "sair", "sessão própria", "terminar todas"),
                this::contentSessions
        ));
        topics.add(new Topic(
                "organization",
                "Empresas e exercícios",
                "Estrutura organizacional e períodos de trabalho",
                Feather.BRIEFCASE,
                List.of("empresa", "filial", "exercício", "ano", "organização"),
                this::contentOrganization
        ));
        topics.add(new Topic(
                "documents",
                "Fiscal, séries e documentos",
                "Numeração, integração e controlo documental",
                Feather.FILE_TEXT,
                List.of("fiscal", "agt", "séries", "documentos", "webhooks", "api"),
                this::contentDocuments
        ));
        topics.add(new Topic(
                "infra",
                "Infraestrutura e manutenção",
                "Centro, servidor, aplicações, moedas e manutenção",
                Feather.SERVER,
                List.of("infraestrutura", "servidor", "centro", "aplicações", "moedas", "câmbios", "manutenção"),
                this::contentInfrastructure
        ));
        topics.add(new Topic(
                "audit",
                "Auditoria, backup e relatórios",
                "Rastreabilidade e continuidade operacional",
                Feather.EYE,
                List.of("auditoria", "logs", "backup", "relatórios", "rastreabilidade"),
                this::contentAudit
        ));
        topics.add(new Topic(
                "troubleshooting",
                "Problemas comuns",
                "Procedimentos rápidos de diagnóstico",
                Feather.ALERT_TRIANGLE,
                List.of("erro", "problema", "falha", "login", "sessão", "backup", "mfa", "permissão"),
                this::contentTroubleshooting
        ));
    }

    private void applyFilter(String value) {
        String q = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        topicList.getChildren().clear();

        List<Topic> visible = topics.stream()
                .filter(t -> q.isBlank()
                        || t.title().toLowerCase(Locale.ROOT).contains(q)
                        || t.subtitle().toLowerCase(Locale.ROOT).contains(q)
                        || t.keywords().stream().anyMatch(k -> k.toLowerCase(Locale.ROOT).contains(q)))
                .toList();

        for (Topic topic : visible) {
            topicList.getChildren().add(topicButton(topic));
        }

        resultCount.setText(visible.size() + (visible.size() == 1 ? " tema" : " temas"));

        boolean selectedStillVisible = visible.stream().anyMatch(t -> t.id().equals(selectedId));
        if (!selectedStillVisible && !visible.isEmpty()) {
            selectTopic(visible.get(0).id());
        } else if (visible.isEmpty()) {
            showNoResults(q);
        } else {
            refreshTopicSelection();
        }
    }

    private Button topicButton(Topic topic) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMnemonicParsing(false);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setGraphic(IconUtils.icon(topic.icon(), 17));
        button.setText(topic.title());
        button.setTooltip(new Tooltip(topic.subtitle()));
        button.getStyleClass().add("kubata-help-topic-button");
        button.setOnAction(e -> selectTopic(topic.id()));
        button.setAccessibleText(topic.title() + ". " + topic.subtitle());
        return button;
    }

    private void refreshTopicSelection() {
        topicList.getChildren().forEach(node -> {
            if (node instanceof Button button) {
                String text = button.getText();
                boolean active = topics.stream().anyMatch(t -> t.id().equals(selectedId) && t.title().equals(text));
                button.getStyleClass().remove("kubata-help-topic-button-active");
                if (active) {
                    button.getStyleClass().add("kubata-help-topic-button-active");
                }
            }
        });
    }

    private void selectTopic(String id) {
        Topic topic = topics.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
        if (topic == null) return;

        selectedId = id;
        contentBox.getChildren().setAll(topic.content().get());
        refreshTopicSelection();
    }

    private void showNoResults(String q) {
        contentBox.getChildren().setAll(
                sectionCard(
                        Feather.SEARCH,
                        "Nenhum tema encontrado",
                        "Não encontrei um tópico correspondente a \"" + q + "\".",
                        paragraphs(
                                "Experimente procurar por termos como sessão, utilizador, MFA, perfil, permissões, auditoria, backup ou empresas."
                        )
                )
        );
    }

    private VBox page(String title, String subtitle, Feather icon) {
        VBox page = new VBox(18);
        Label eyebrow = new Label("GUIA DO KUBATA ADMINISTRATOR");
        eyebrow.getStyleClass().add("kubata-help-eyebrow");

        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        StackPane iconBox = new StackPane(IconUtils.icon(icon, 23));
        iconBox.getStyleClass().add("kubata-help-section-icon");

        VBox titles = new VBox(2);
        Label h = new Label(title);
        h.getStyleClass().add("kubata-help-section-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("kubata-help-section-subtitle");
        s.setWrapText(true);
        titles.getChildren().addAll(h, s);

        heading.getChildren().addAll(iconBox, titles);
        page.getChildren().addAll(eyebrow, heading);
        return page;
    }

    private VBox sectionCard(Feather icon, String title, String subtitle, List<Node> children) {
        VBox card = new VBox(10);
        card.getStyleClass().add("kubata-help-card");

        HBox heading = new HBox(9);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.getChildren().addAll(IconUtils.icon(icon, 17), label(title, "kubata-help-card-title"));

        Label sub = new Label(subtitle == null ? "" : subtitle);
        sub.setWrapText(true);
        sub.getStyleClass().add("kubata-help-card-subtitle");

        card.getChildren().addAll(heading, sub);
        card.getChildren().addAll(children);
        return card;
    }

    private List<Node> paragraphs(String... values) {
        List<Node> result = new ArrayList<>();
        for (String value : values) {
            Label p = new Label(value);
            p.setWrapText(true);
            p.getStyleClass().add("kubata-help-paragraph");
            result.add(p);
        }
        return result;
    }

    private Node label(String value, String style) {
        Label label = new Label(value);
        label.getStyleClass().add(style);
        return label;
    }

    private VBox step(int number, String title, String text) {
        HBox row = new HBox(11);
        row.setAlignment(Pos.TOP_LEFT);

        Label badge = new Label(String.valueOf(number));
        badge.getStyleClass().add("kubata-help-step-number");

        VBox body = new VBox(2);
        Label h = new Label(title);
        h.getStyleClass().add("kubata-help-step-title");
        Label p = new Label(text);
        p.setWrapText(true);
        p.getStyleClass().add("kubata-help-paragraph");
        body.getChildren().addAll(h, p);

        row.getChildren().addAll(badge, body);
        return new VBox(row);
    }

    private HBox fact(String label, String value, Feather icon) {
        HBox box = new HBox(9);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("kubata-help-fact");

        box.getChildren().add(IconUtils.icon(icon, 16));
        Label left = new Label(label);
        left.getStyleClass().add("kubata-help-fact-label");
        Label right = new Label(value);
        right.setWrapText(true);
        right.getStyleClass().add("kubata-help-fact-value");
        HBox.setHgrow(right, Priority.ALWAYS);
        box.getChildren().addAll(left, right);
        return box;
    }

    private VBox contentOverview() {
        VBox p = page(
                "Como funciona o Kubata Administrator",
                "O Administrator é a área de gestão, configuração, segurança e supervisão do ecossistema Kubata.",
                Feather.HOME
        );

        p.getChildren().add(sectionCard(
                Feather.LAYERS,
                "O que é o Administrator?",
                "É o centro administrativo do Kubata. O objetivo é manter a plataforma configurada, segura, rastreável e pronta para os módulos de negócio.",
                List.of(
                        fact("Consola", "Visão operacional do ambiente: sessões, bloqueios, logs, processos, manutenção e indicadores.", Feather.ACTIVITY),
                        fact("Gestão", "Dados mestres como empresas e exercícios fiscais.", Feather.BRIEFCASE),
                        fact("Segurança", "Utilizadores, perfis, permissões, licenciamento e políticas de acesso.", Feather.SHIELD),
                        fact("Fiscal", "Séries, integrações e configurações fiscais.", Feather.FILE_TEXT),
                        fact("Sistema", "Auditoria, backup, relatórios e extensibilidade.", Feather.SETTINGS),
                        fact("Infraestrutura", "Recursos técnicos, servidor, instâncias, centro da plataforma e monitorização.", Feather.SERVER)
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.ARROW_RIGHT,
                "Fluxo normal de trabalho",
                "Uma administração segura começa pela autenticação e termina com a revisão das alterações.",
                List.of(
                        step(1, "Entrar", "Faça login com a sua conta. A política da conta pode exigir MFA, validar horário, endereço IP, palavra-passe e limite de sessões."),
                        step(2, "Confirmar contexto", "Verifique o utilizador autenticado, a empresa/contexto aplicável e o módulo onde pretende trabalhar."),
                        step(3, "Executar", "Use o Ribbon para abrir a funcionalidade necessária. Cada área apresenta filtros, formulários, tabelas e ações adequadas à tarefa."),
                        step(4, "Confirmar alterações", "Depois de gravar, confirme a notificação de sucesso e, para operações críticas, consulte a auditoria quando necessário."),
                        step(5, "Encerrar", "No fim do trabalho, encerre a sessão pelo comando próprio do módulo. Não trate simplesmente o fecho da janela como substituto do logout.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.INFO,
                "Princípio fundamental",
                "As permissões definem o que cada conta pode executar.",
                paragraphs(
                        "Uma conta administrativa não deve ser utilizada como conta operacional comum. Crie utilizadores com o perfil e o contexto necessários para cada função.",
                        "Quando uma operação estiver indisponível, confirme primeiro o perfil, a empresa/contexto, o estado da conta e a política de segurança antes de alterar permissões."
                )
        ));

        return p;
    }

    private VBox contentLogin() {
        VBox p = page(
                "Entrada e autenticação",
                "O login não é apenas a comparação de email e palavra-passe: a política de segurança pode aplicar várias regras.",
                Feather.LOG_IN
        );

        p.getChildren().add(sectionCard(
                Feather.LOCK,
                "O que pode ser validado no login",
                "Dependendo da configuração da conta, o Kubata pode verificar:",
                paragraphs(
                        "• Conta activa e elegível para login.\n" +
                        "• Palavra-passe correcta e política de expiração.\n" +
                        "• Número máximo de tentativas e período de bloqueio.\n" +
                        "• MFA/TOTP ou código de recuperação, quando activo.\n" +
                        "• Horário e dias autorizados.\n" +
                        "• Endereço IP e restrições definidas para a conta.\n" +
                        "• Limite de sessões simultâneas.\n" +
                        "• Empresa e módulos permitidos para o contexto do utilizador."
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.CHECK_CIRCLE,
                "Quando aparece um erro de autenticação",
                "O modal de erro do Kubata explica o problema sem revelar dados sensíveis.",
                List.of(
                        step(1, "Leia o motivo", "Veja a secção “O que aconteceu”. Ela identifica a categoria da rejeição, por exemplo password expirada, MFA inválido ou limite de sessões."),
                        step(2, "Siga a resolução", "Use a secção “Como resolver”. Muitas situações podem ser corrigidas pelo próprio utilizador; outras exigem um administrador."),
                        step(3, "Evite tentativas repetidas", "Quando houver limite de tentativas, insistir no login pode prolongar o bloqueio."),
                        step(4, "Peça intervenção adequada", "Problemas de perfil, empresa, IP, MFA administrativo ou limites financeiros devem ser encaminhados para uma conta com a autorização necessária.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.SMARTPHONE,
                "MFA e recuperação",
                "Quando a conta tem MFA activo, a autenticação passa por uma segunda prova de identidade.",
                paragraphs(
                        "O código de recuperação deve ser tratado como informação secreta. Nunca o coloque em mensagens, logs, capturas de ecrã ou campos destinados a outros dados.",
                        "A recuperação e regeneração de códigos é uma operação administrativa controlada e auditada."
                )
        ));

        return p;
    }

    private VBox contentNavigation() {
        VBox p = page(
                "Navegação e Ribbon",
                "O cabeçalho organiza as operações por áreas. Os comandos permanecem consistentes para reduzir o tempo de aprendizagem.",
                Feather.GRID
        );

        p.getChildren().add(sectionCard(
                Feather.MENU,
                "Abas principais",
                "A organização actual do Administrator é:",
                List.of(
                        fact("Início", "Consola, Aplicação, Parâmetros e operações rápidas.", Feather.HOME),
                        fact("Gestão", "Empresa e exercícios fiscais.", Feather.BRIEFCASE),
                        fact("Segurança", "Utilizadores, Perfis e Licenciamento.", Feather.SHIELD),
                        fact("Fiscal", "Séries, API/Webhooks e Fiscal AGT.", Feather.FILE_TEXT),
                        fact("Sistema", "Extensibilidade, Auditoria, Backup e Relatórios.", Feather.SETTINGS),
                        fact("Infraestrutura", "Centro, Aplicações, Moedas, Sessões, Servidor, Instâncias e monitorização; disponível conforme o nível administrativo.", Feather.SERVER)
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.SEARCH,
                "Como encontrar uma função",
                "Existem três formas práticas.",
                List.of(
                        step(1, "Use as abas", "Comece pela área funcional mais próxima da tarefa."),
                        step(2, "Leia o nome do grupo", "Dentro de cada aba, os comandos estão agrupados por finalidade."),
                        step(3, "Use as dicas", "Passe o cursor pelos comandos para ver a descrição da ação no tooltip. O campo de pesquisa do cabeçalho também serve como ponto de entrada para pesquisas suportadas pela aplicação.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.GRID,
                "Janelas e separadores",
                "As funcionalidades podem abrir em separadores no centro da aplicação.",
                paragraphs(
                        "Uma funcionalidade aberta pode ser seleccionada novamente pelo seu separador, evitando abrir várias cópias da mesma área.",
                        "O separador da Consola é criado inicialmente para apresentar o estado administrativo do sistema."
                )
        ));

        return p;
    }

    private VBox contentUsers() {
        VBox p = page(
                "Gestão de utilizadores",
                "Utilizadores representam as identidades que entram no Kubata e recebem um conjunto controlado de permissões e contexto.",
                Feather.USERS
        );

        p.getChildren().add(sectionCard(
                Feather.USER_PLUS,
                "Criar um utilizador correctamente",
                "Evite criar contas genéricas ou partilhadas quando uma identidade individual for possível.",
                List.of(
                        step(1, "Defina a identidade", "Preencha nome, email e código de utilizador. O código funciona como referência administrativa."),
                        step(2, "Escolha o papel e tipo de conta", "Seleccione a função adequada sem atribuir privilégios administrativos por conveniência."),
                        step(3, "Associe o contexto", "Quando aplicável, escolha empresa e filial. O contexto deve corresponder à actividade real do utilizador."),
                        step(4, "Defina a password inicial", "Use uma password compatível com a política. Contas com password provisória devem alterá-la no fluxo previsto."),
                        step(5, "Atribua o perfil", "Associe apenas os perfis necessários e verifique as permissões antes de gravar."),
                        step(6, "Confirme a auditoria", "Depois da gravação, a operação administrativa fica disponível para rastreabilidade.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.USER_CHECK,
                "Estado da conta",
                "Uma conta activa pode autenticar-se quando todas as restantes políticas também forem satisfeitas.",
                paragraphs(
                        "Não confunda “utilizador activo” com “acesso garantido”. O login pode continuar a ser rejeitado por password expirada, MFA, horário, IP, empresa, módulo ou limite de sessões.",
                        "A desactivação de contas deve ser usada quando o acesso já não deve continuar autorizado."
                )
        ));

        return p;
    }

    private VBox contentProfiles() {
        VBox p = page(
                "Perfis e permissões",
                "O perfil é a forma organizada de dizer o que uma conta pode ver ou executar.",
                Feather.SHIELD
        );

        p.getChildren().add(sectionCard(
                Feather.LAYERS,
                "Como pensar em permissões",
                "A configuração deve ser baseada na função de trabalho e no princípio do menor privilégio.",
                List.of(
                        fact("Perfil", "Conjunto reutilizável de permissões.", Feather.SHIELD),
                        fact("Módulo", "Área do Kubata à qual a permissão se aplica.", Feather.GRID),
                        fact("Opção", "Operação concreta dentro do módulo.", Feather.CHECK),
                        fact("Empresa", "Contexto organizacional em que o acesso pode ser permitido.", Feather.BRIEFCASE),
                        fact("Operação crítica", "Ação sensível que pode exigir autorização adicional.", Feather.ALERT_TRIANGLE)
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.EDIT,
                "Alterar um perfil com segurança",
                "Uma alteração de permissão pode afectar vários utilizadores de uma só vez.",
                List.of(
                        step(1, "Abra Segurança > Perfis", "Localize o perfil e confirme a descrição antes de editar."),
                        step(2, "Revise o âmbito", "Confirme se o perfil é global ou limitado a empresa/contexto."),
                        step(3, "Revise permissões", "Conceda apenas as opções que fazem parte da função."),
                        step(4, "Valide o impacto", "Antes de gravar, pense nos utilizadores que usam esse perfil."),
                        step(5, "Registe e verifique", "Depois da alteração, confirme o estado final e consulte a auditoria quando a mudança for sensível.")
                )
        ));

        return p;
    }

    private VBox contentSessions() {
        VBox p = page(
                "Sessões e encerramento",
                "Uma sessão representa uma entrada activa de uma conta no ecossistema Kubata.",
                Feather.POWER
        );

        p.getChildren().add(sectionCard(
                Feather.LOG_OUT,
                "Como um utilizador comum encerra a própria sessão",
                "O utilizador deve sempre usar o comando de logout do módulo em que está a trabalhar.",
                List.of(
                        step(1, "Abra o menu do utilizador", "Procure o menu de perfil/cabeçalho no módulo operacional."),
                        step(2, "Escolha “Encerrar Sessão”", "Este é o comando destinado a terminar a sessão actual."),
                        step(3, "Confirme", "Quando for apresentada uma confirmação, confirme apenas se terminou o trabalho."),
                        step(4, "Volte ao login", "O módulo limpa o contexto local e regressa ao ecrã de autenticação."),
                        step(5, "Faça novo login quando necessário", "A próxima utilização deve começar por uma nova autenticação.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.LOCK,
                "Porque não se deve simplesmente fechar a janela",
                "Fechar a janela e terminar a sessão são conceitos diferentes.",
                paragraphs(
                        "O logout explícito permite que a aplicação execute o fluxo de encerramento, registe a operação e limpe o contexto local de autenticação.",
                        "Para o utilizador comum, a regra prática é simples: termine sempre a sessão pelo comando “Encerrar Sessão” disponibilizado no módulo.",
                        "No módulo Kubata Faturação, por exemplo, o comando encontra-se no menu do utilizador no cabeçalho e aparece como “Encerrar Sessão”. Depois da confirmação, a aplicação limpa a sessão local e regressa ao login."
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.USERS,
                "Quando um administrador termina sessões de outros utilizadores",
                "São operações diferentes do logout próprio.",
                List.of(
                        step(1, "Sessão individual", "Na administração de utilizadores/sessões, um administrador autorizado pode actuar sobre uma conta conforme as regras de segurança."),
                        step(2, "Terminar todas", "Na Consola existe a acção global “Terminar todas”. Ela termina as sessões dos outros utilizadores e preserva a sessão do administrador que executa a operação."),
                        step(3, "Confirmação", "A operação deve ser confirmada porque pode interromper pessoas a trabalhar noutros computadores."),
                        step(4, "Auditoria", "A operação global é registada para rastreabilidade administrativa.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.ALERT_TRIANGLE,
                "Limite de sessões simultâneas",
                "Quando uma conta atingiu o máximo configurado, um novo login pode ser rejeitado.",
                paragraphs(
                        "Nesse caso, encerre uma sessão antiga no dispositivo correspondente ou peça a um administrador autorizado para administrar as sessões. O fluxo de login pode também disponibilizar “Terminar sessão mais antiga” para uma conta administrativa autorizada.",
                        "A operação de terminar a sessão mais antiga remove apenas uma sessão antiga; não deve ser usada para terminar indiscriminadamente todas as sessões."
                )
        ));

        return p;
    }

    private VBox contentOrganization() {
        VBox p = page(
                "Empresas e exercícios",
                "Estas definições estabelecem o contexto organizacional e temporal em que o ERP trabalha.",
                Feather.BRIEFCASE
        );

        p.getChildren().add(sectionCard(
                Feather.BRIEFCASE,
                "Empresa e filial",
                "A empresa representa a entidade; a filial representa uma unidade operacional quando a estrutura a exigir.",
                List.of(
                        step(1, "Cadastre a empresa", "Preencha os dados de identificação e mantenha o estado da empresa correcto."),
                        step(2, "Associe filiais", "Use filiais quando a operação precisar de separar locais, equipas ou movimentos."),
                        step(3, "Relacione utilizadores", "Associe contas ao contexto necessário para evitar acesso indevido entre empresas."),
                        step(4, "Verifique permissões", "Um perfil pode ser tecnicamente válido e, ainda assim, não estar autorizado para a empresa em uso.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.CALENDAR,
                "Exercício fiscal",
                "O exercício organiza as operações por período.",
                paragraphs(
                        "Antes de lançar ou configurar documentos, confirme qual é o exercício em uso e se o período está aberto para a operação pretendida.",
                        "Não crie exercícios duplicados por tentativa. Confirme primeiro o catálogo existente e o estado do exercício."
                )
        ));

        return p;
    }

    private VBox contentDocuments() {
        VBox p = page(
                "Fiscal, séries e documentos",
                "A área Fiscal concentra configurações que influenciam a numeração e a integração documental.",
                Feather.FILE_TEXT
        );

        p.getChildren().add(sectionCard(
                Feather.LAYERS,
                "Séries de documentos",
                "Uma série organiza a numeração e o ciclo documental.",
                List.of(
                        step(1, "Defina a série", "Use um código claro e uma descrição que identifique a finalidade."),
                        step(2, "Associe ao contexto", "Confirme empresa, exercício ou restantes regras aplicáveis."),
                        step(3, "Valide a numeração", "Evite alterar séries sem perceber o impacto em documentos já emitidos."),
                        step(4, "Teste antes de produção", "Quando uma integração depender da série, valide o fluxo de ponta a ponta.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.GLOBE,
                "API e Webhooks",
                "Estas opções servem para integração entre o Kubata e sistemas externos.",
                paragraphs(
                        "Trate credenciais, endpoints e tokens de integração como segredos. Nunca coloque passwords, tokens ou chaves em descrições, mensagens de sistema ou logs.",
                        "Quando um webhook falhar, verifique primeiro o endereço configurado, disponibilidade do destino, formato do evento e registo de erro."
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.FILE_TEXT,
                "Fiscal AGT",
                "As configurações fiscais devem ser revistas com especial cuidado porque podem afectar documentos e obrigações do negócio.",
                paragraphs(
                        "Antes de alterar parâmetros fiscais em produção, confirme a empresa, exercício, série e o ambiente de destino."
                )
        ));

        return p;
    }

    private VBox contentInfrastructure() {
        VBox p = page(
                "Infraestrutura e manutenção",
                "A infraestrutura permite supervisionar a plataforma técnica sem misturar essas tarefas com a operação normal.",
                Feather.SERVER
        );

        p.getChildren().add(sectionCard(
                Feather.CPU,
                "Centro da plataforma",
                "O Centro agrega atalhos técnicos para observar o ambiente e localizar recursos.",
                List.of(
                        fact("Centro", "Visão geral administrativa da plataforma.", Feather.CPU),
                        fact("Aplicações", "Catálogo das aplicações instaladas.", Feather.PACKAGE),
                        fact("Moedas", "Moedas e câmbios disponíveis no sistema.", Feather.DOLLAR_SIGN),
                        fact("Sessões", "Consulta administrativa das sessões do sistema.", Feather.USERS),
                        fact("Monitor", "Indicadores e monitorização do ambiente.", Feather.ACTIVITY),
                        fact("Planos", "Operações de manutenção programada.", Feather.CALENDAR)
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.TOOL,
                "Modo de manutenção",
                "O modo de manutenção serve para controlar o acesso operacional durante intervenções planeadas.",
                List.of(
                        step(1, "Defina o motivo", "Introduza uma mensagem clara para os utilizadores afectados."),
                        step(2, "Active a manutenção", "O sistema passa a aplicar a regra de manutenção aos novos acessos operacionais."),
                        step(3, "Execute a intervenção", "Faça a manutenção necessária com o contexto administrativo apropriado."),
                        step(4, "Desactive", "Depois da intervenção, liberte o sistema para o uso normal."),
                        step(5, "Registe a operação", "Mantenha a rastreabilidade do motivo e do responsável.")
                )
        ));

        return p;
    }

    private VBox contentAudit() {
        VBox p = page(
                "Auditoria, backup e relatórios",
                "Estas áreas protegem a rastreabilidade e a continuidade operacional do ambiente.",
                Feather.EYE
        );

        p.getChildren().add(sectionCard(
                Feather.EYE,
                "Auditoria",
                "A auditoria ajuda a responder: quem fez, o quê, quando, em que contexto e com que resultado.",
                paragraphs(
                        "Use a auditoria para investigar alterações de utilizadores, permissões, sessões, segurança e outras operações administrativas.",
                        "Os registos não substituem políticas internas, mas fornecem uma fonte importante para análise posterior."
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.SAVE,
                "Backup",
                "Backup é uma medida de continuidade, não apenas uma cópia ocasional.",
                List.of(
                        step(1, "Defina o que deve ser protegido", "Confirme base de dados, ficheiros e restantes recursos relevantes."),
                        step(2, "Execute ou programe", "Use a funcionalidade de backup conforme a política da organização."),
                        step(3, "Verifique o resultado", "Um backup deve ser tratado como concluído somente depois de confirmar o seu estado."),
                        step(4, "Planeie restauração", "Uma cópia que nunca foi testada pode não ser suficiente para uma recuperação real.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.PRINTER,
                "Relatórios",
                "Use relatórios para transformar os dados administrativos em informação verificável e partilhável.",
                paragraphs(
                        "Antes de exportar, confirme filtros, empresa, exercício e período. Em documentos sensíveis, preserve o controlo de acesso e o contexto de emissão."
                )
        ));

        return p;
    }

    private VBox contentTroubleshooting() {
        VBox p = page(
                "Problemas comuns",
                "Use esta sequência antes de alterar configurações de segurança.",
                Feather.ALERT_TRIANGLE
        );

        p.getChildren().add(sectionCard(
                Feather.LOG_IN,
                "Não consigo entrar",
                "Diagnóstico recomendado:",
                List.of(
                        step(1, "Confirme a conta", "Verifique email/identificador e confirme que a conta não está desactivada."),
                        step(2, "Confirme a password", "Tenha atenção a passwords provisórias e expiradas."),
                        step(3, "Leia o modal", "O erro detalhado indica se o problema é MFA, sessões, horário, IP ou outra política."),
                        step(4, "Evite alterar permissões sem evidência", "Nem todo erro de login é um problema de perfil."),
                        step(5, "Escalone correctamente", "Quando a correcção exigir privilégios administrativos, peça a intervenção de um administrador autorizado.")
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.SHIELD,
                "Não vejo uma opção",
                "A ausência de um comando pode ser intencional.",
                paragraphs(
                        "Verifique a aba correcta, o perfil da conta, o módulo, a empresa/contexto e o estado do recurso. O Administrator usa autorização por função e contexto; não se deve conceder privilégios apenas para fazer um comando aparecer.",
                        "Em caso de dúvida, procure primeiro o tema correspondente neste Centro de Ajuda e só depois altere permissões."
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.USERS,
                "Uma conta atingiu o limite de sessões",
                "Há duas situações diferentes.",
                List.of(
                        fact("Utilizador comum", "Pode terminar a própria sessão no módulo onde está a trabalhar. Para outras sessões, precisa de administração.", Feather.LOG_OUT),
                        fact("Administrador autorizado", "Pode administrar sessões na área de sessões e, conforme a autorização, terminar a sessão mais antiga ou executar a operação global.", Feather.USERS)
                )
        ));

        p.getChildren().add(sectionCard(
                Feather.LIFE_BUOY,
                "Regra de ouro",
                "Quando uma correcção não é clara, preserve a evidência.",
                paragraphs(
                        "Anote a mensagem exacta do erro, o utilizador afectado, o módulo, a empresa/contexto e o momento aproximado. Evite partilhar passwords, códigos MFA, tokens ou outras credenciais."
                )
        ));

        return p;
    }
}
