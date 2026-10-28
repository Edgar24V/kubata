package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.DemoDataService;
import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dados de Demonstração — criação de ambiente de testes para demonstrações
 * a clientes, no espirito do "Ambiente de Demonstração" do Primavera ERP.
 *
 * <p>O administrador pode gerar em um clique uma empresa fictícia com
 * exercício fiscal aberto, utilizadores de teste com perfis distintos
 * (administrador, vendedor, caixa, contabilista) e dados mestres de amostra
 * (categorias, artigos, clientes e fornecedores), e removê-los de igual forma
 * antes de colocar o sistema em produção.</p>
 */
@Component
public class DadosDemonstracaoView extends VBox {

    private final DemoDataService demoDataService;
    private final PersistenceService persistenceService;
    private final ModalManager modalManager;
    private final NotificationService notificationService;
    private final SessionManager sessionManager;

    private final CheckBox chkEmpresa = new CheckBox("Empresa de demonstração");
    private final CheckBox chkUtilizadores = new CheckBox("Utilizadores de teste");
    private final CheckBox chkMestres = new CheckBox("Dados mestres de amostra");

    private final Button btnCriar = new Button();
    private final Button btnLimpar = new Button();
    private final Button btnActualizar = new Button();

    private final Label lblEstadoEmpresa = new Label("—");
    private final Label lblEstadoUtilizadores = new Label("—");
    private final Label lblEstadoCategorias = new Label("—");
    private final Label lblEstadoArtigos = new Label("—");
    private final Label lblEstadoClientes = new Label("—");
    private final Label lblEstadoFornecedores = new Label("—");

    private final TextArea logArea = new TextArea();
    private final ProgressIndicator progressIndicator = new ProgressIndicator(-1);
    private final Label lblProgresso = new Label("Pronto para gerar o ambiente de demonstração.");

    public DadosDemonstracaoView(DemoDataService demoDataService,
                                 PersistenceService persistenceService,
                                 ModalManager modalManager,
                                 NotificationService notificationService,
                                 SessionManager sessionManager) {
        this.demoDataService = demoDataService;
        this.persistenceService = persistenceService;
        this.modalManager = modalManager;
        this.notificationService = notificationService;
        this.sessionManager = sessionManager;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
        refreshEstados();
    }

    // ── Construção da UI ──────────────────────────────────────────────────────

    private void buildUi() {
        HBox header = buildHeader();

        VBox content = new VBox(16);
        content.setPadding(new Insets(18, 20, 24, 20));
        content.setFillWidth(true);
        content.getChildren().addAll(
                buildIntroCard(),
                buildOptionsCard(),
                buildStatesCard(),
                buildProgressCard()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setMaxHeight(Double.MAX_VALUE);
        scroll.getStyleClass().add("application-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(header, scroll);
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-box");

        VBox titleBox = new VBox(2);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.ZAP, 18));
        Label title = new Label("Dados de Demonstração");
        title.getStyleClass().add("h3");
        titleLine.getChildren().addAll(icon, title);

        Label subtitle = new Label(
                "Crie um ambiente de testes completo — empresa fictícia, utilizadores de teste e dados de amostra — para demonstrar o sistema a clientes."
        );
        subtitle.getStyleClass().add("text-muted");
        subtitle.setWrapText(true);

        titleBox.getChildren().addAll(titleLine, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("DEMONSTRAÇÃO");
        badge.getStyleClass().add("demo-badge");

        header.getChildren().addAll(titleBox, spacer, badge);
        return header;
    }

    private VBox buildIntroCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        Label title = new Label("O que este assistente cria");
        title.getStyleClass().add("h4");

        Label description = new Label(
                "• Empresa de demonstração “" + DemoDataService.NOME_EMPRESA_DEMO
                        + "” (NIF " + DemoDataService.NIF_EMPRESA_DEMO + ") com exercício fiscal aberto;\n"
                        + "• Utilizadores de teste com funções distintas para testar cada módulo: administrador, vendedor, caixa e contabilista;\n"
                        + "• Dados mestres de amostra: categorias, artigos com stock, clientes e fornecedores.\n\n"
                        + "Todos os registos são marcados com o prefixo “DEMO”, pelo que podem ser removidos sem tocar nos dados reais. "
                        + "Palavra-passe única dos utilizadores de teste: " + DemoDataService.PALAVRA_PASSE_DEMO
        );
        description.setWrapText(true);
        description.getStyleClass().add("text-muted");

        card.getChildren().addAll(title, description);
        return card;
    }

    private VBox buildOptionsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        Label title = new Label("Opções de criação");
        title.getStyleClass().add("h4");

        chkEmpresa.setSelected(true);
        chkUtilizadores.setSelected(true);
        chkMestres.setSelected(true);
        chkEmpresa.setWrapText(true);
        chkUtilizadores.setWrapText(true);
        chkMestres.setWrapText(true);

        VBox options = new VBox(8, chkEmpresa, chkUtilizadores, chkMestres);

        Button btnSeleccionarTudo = new Button("Seleccionar tudo");
        btnSeleccionarTudo.getStyleClass().add("button-outlined");
        btnSeleccionarTudo.setOnAction(e -> {
            boolean seleccionar = !(chkEmpresa.isSelected() && chkUtilizadores.isSelected() && chkMestres.isSelected());
            chkEmpresa.setSelected(seleccionar);
            chkUtilizadores.setSelected(seleccionar);
            chkMestres.setSelected(seleccionar);
        });

        btnCriar.setText("Criar dados de demonstração");
        btnCriar.setGraphic(IconUtils.icon(Feather.PLAY, IconUtils.SIZE_SMALL));
        btnCriar.getStyleClass().add("button-primary");
        btnCriar.setOnAction(e -> confirmarCriacao());

        btnLimpar.setText("Remover dados de demonstração");
        btnLimpar.setGraphic(IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().add("button-outlined");
        btnLimpar.setOnAction(e -> confirmarRemocao());

        btnActualizar.setText("Actualizar");
        btnActualizar.setGraphic(IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnActualizar.getStyleClass().add("button-outlined");
        btnActualizar.setOnAction(e -> refreshEstados());

        HBox buttons = new HBox(10, btnCriar, btnLimpar, btnActualizar);
        buttons.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, options, btnSeleccionarTudo, buttons);
        return card;
    }

    private VBox buildStatesCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        Label title = new Label("Estado actual do ambiente de demonstração");
        title.getStyleClass().add("h4");

        HBox row1 = new HBox(12,
                stateChip("Empresa", lblEstadoEmpresa),
                stateChip("Utilizadores", lblEstadoUtilizadores),
                stateChip("Categorias", lblEstadoCategorias));
        HBox row2 = new HBox(12,
                stateChip("Artigos", lblEstadoArtigos),
                stateChip("Clientes", lblEstadoClientes),
                stateChip("Fornecedores", lblEstadoFornecedores));
        row1.setFillHeight(true);
        row2.setFillHeight(true);

        card.getChildren().addAll(title, row1, row2);
        return card;
    }

    private VBox buildProgressCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        Label title = new Label("Progresso da operação");
        title.getStyleClass().add("h4");

        progressIndicator.setPrefSize(18, 18);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        lblProgresso.getStyleClass().add("text-muted");
        lblProgresso.setWrapText(true);

        HBox statusRow = new HBox(8, progressIndicator, lblProgresso);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(10);
        logArea.getStyleClass().add("demo-log");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        card.getChildren().addAll(title, statusRow, logArea);
        return card;
    }

    // ── Acções ────────────────────────────────────────────────────────────────

    private void confirmarCriacao() {
        if (!chkEmpresa.isSelected() && !chkUtilizadores.isSelected() && !chkMestres.isSelected()) {
            notificationService.showWarning("Nada seleccionado",
                    "Seleccione pelo menos uma opção de criação.");
            return;
        }

        StringBuilder mensagem = new StringBuilder("Vão ser criados os seguintes dados de demonstração:\n");
        if (chkEmpresa.isSelected()) {
            mensagem.append("• Empresa de demonstração com exercício fiscal aberto\n");
        }
        if (chkUtilizadores.isSelected()) {
            mensagem.append("• 4 utilizadores de teste (administrador, vendedor, caixa, contabilista)\n");
        }
        if (chkMestres.isSelected()) {
            mensagem.append("• Categorias, artigos, clientes e fornecedores de amostra\n");
        }
        mensagem.append("\nContinuar?");

        VBox conteudo = new VBox(8);
        Label label = new Label(mensagem.toString());
        label.setWrapText(true);
        conteudo.getChildren().add(label);

        modalManager.showConfirmModal(conteudo, "Criar dados de demonstração", this::executarCriacao, null);
    }

    private void executarCriacao() {
        boolean comEmpresa = chkEmpresa.isSelected();
        boolean comUtilizadores = chkUtilizadores.isSelected();
        boolean comMestres = chkMestres.isSelected();

        setBusy(true, "A criar o ambiente de demonstração…");
        appendLog("── Início da criação dos dados de demonstração ──");

        persistenceService.executeAsync(
                () -> demoDataService.criarDadosDemo(comEmpresa, comUtilizadores, comMestres, this::appendLogAsync),
                "CRIAR_DEMO",
                "DEMONSTRACAO",
                "Criação de dados de demonstração",
                () -> Platform.runLater(() -> {
                    setBusy(false, "Ambiente de demonstração criado com sucesso.");
                    appendLog("── Criação concluída ──");
                    refreshEstados();
                    modalManager.alert(
                            "Dados de demonstração criados",
                            "O ambiente de demonstração está pronto a ser usado.\n\n"
                                    + "Utilizadores de teste (palavra-passe única "
                                    + DemoDataService.PALAVRA_PASSE_DEMO + "):\n"
                                    + "• admin.demo@kubata.ao (Administrador)\n"
                                    + "• vendedor.demo@kubata.ao (Vendedor)\n"
                                    + "• caixa.demo@kubata.ao (Caixa)\n"
                                    + "• contabilista.demo@kubata.ao (Contabilista)\n\n"
                                    + "Recomenda-se remover os dados de demonstração antes de ir para produção.",
                            "success",
                            null
                    );
                })
        );
    }

    private void confirmarRemocao() {
        VBox conteudo = new VBox(8);
        Label label = new Label(
                "Vão ser removidos TODOS os registos de demonstração "
                        + "(utilizadores de teste, artigos, categorias, clientes e fornecedores com o prefixo DEMO). "
                        + "A empresa de demonstração será desactivada.\n\nOs dados reais não são afectados. Continuar?"
        );
        label.setWrapText(true);
        conteudo.getChildren().add(label);

        modalManager.showConfirmModal(conteudo, "Remover dados de demonstração", this::executarRemocao, null);
    }

    private void executarRemocao() {
        setBusy(true, "A remover os dados de demonstração…");
        appendLog("── Início da remoção dos dados de demonstração ──");

        persistenceService.executeAsync(
                () -> demoDataService.limparDadosDemo(this::appendLogAsync),
                "REMOVER_DEMO",
                "DEMONSTRACAO",
                "Remoção de dados de demonstração",
                () -> Platform.runLater(() -> {
                    setBusy(false, "Dados de demonstração removidos.");
                    appendLog("── Remoção concluída ──");
                    refreshEstados();
                    notificationService.showSuccess("Remoção concluída",
                            "Os dados de demonstração foram removidos do sistema.");
                })
        );
    }

    // ── Estados e utilitários ─────────────────────────────────────────────────

    private void refreshEstados() {
        persistenceService.executeSilent(() -> {
            Map<String, Long> contagens = demoDataService.contagemDemonstracao();
            Platform.runLater(() -> {
                lblEstadoEmpresa.setText(estadoTexto(contagens.getOrDefault("empresa", 0L), "empresa"));
                lblEstadoUtilizadores.setText(contagens.getOrDefault("utilizadores", 0L) + " registos");
                lblEstadoCategorias.setText(contagens.getOrDefault("categorias", 0L) + " registos");
                lblEstadoArtigos.setText(contagens.getOrDefault("artigos", 0L) + " registos");
                lblEstadoClientes.setText(contagens.getOrDefault("clientes", 0L) + " registos");
                lblEstadoFornecedores.setText(contagens.getOrDefault("fornecedores", 0L) + " registos");
            });
        }, null);
    }

    private String estadoTexto(long quantidade, String rotulo) {
        return quantidade > 0 ? "Criada" : "Não criada";
    }

    private VBox stateChip(String titulo, Label valor) {
        VBox chip = new VBox(2);
        chip.setPadding(new Insets(10, 12, 10, 12));
        chip.getStyleClass().add("demo-state-chip");

        Label rotulo = new Label(titulo.toUpperCase());
        rotulo.getStyleClass().add("console-kpi-title");

        valor.getStyleClass().add("demo-state-value");

        chip.getChildren().addAll(rotulo, valor);
        HBox.setHgrow(chip, Priority.ALWAYS);
        chip.setMaxWidth(Double.MAX_VALUE);
        return chip;
    }

    private void setBusy(boolean ocupado, String mensagem) {
        btnCriar.setDisable(ocupado);
        btnLimpar.setDisable(ocupado);
        btnActualizar.setDisable(ocupado);
        chkEmpresa.setDisable(ocupado);
        chkUtilizadores.setDisable(ocupado);
        chkMestres.setDisable(ocupado);

        progressIndicator.setVisible(ocupado);
        progressIndicator.setManaged(ocupado);
        lblProgresso.setText(mensagem);
    }

    private void appendLog(String linha) {
        String texto = "[" + java.time.LocalTime.now().withNano(0) + "] " + linha + "\n";
        if (Platform.isFxApplicationThread()) {
            logArea.appendText(texto);
        } else {
            appendLogAsync(linha);
        }
    }

    private void appendLogAsync(String linha) {
        String texto = "[" + java.time.LocalTime.now().withNano(0) + "] " + linha + "\n";
        Platform.runLater(() -> logArea.appendText(texto));
    }
}
