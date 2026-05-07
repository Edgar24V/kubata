package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.domain.enums.NaturezaConta;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.core.service.PlanoContaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import net.sf.jasperreports.engine.JasperPrint;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class PlanoContasView extends BorderPane {

    private final PlanoContaService service;
    private final ContabilidadeService contabilidadeService;
    private final JasperReportService jasperReportService;
    
    private TreeView<PlanoConta> treeView;
    private CustomTextField searchField;
    private TextField codigoField;
    private TextField descricaoField;
    private ComboBox<ClasseConta> classeCombo;
    private ComboBox<NaturezaConta> naturezaCombo;
    private CheckBox movimentoCheck;
    private Label parentLabel;
    private PlanoConta currentParent;
    private PlanoConta currentConta;

    // KPI Labels
    private Label lblTotalContas;
    private Label lblContasRaiz;
    private Label lblContasMovimento;
    private Label lblContasAnaliticas;

    public PlanoContasView(PlanoContaService service, ContabilidadeService contabilidadeService, JasperReportService jasperReportService) {
        this.service = service;
        this.contabilidadeService = contabilidadeService;
        this.jasperReportService = jasperReportService;
        initializeUI();
        loadTree();
    }

    private void initializeUI() {
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.setFillWidth(true);

        // 1. Header com KPIs
        mainContent.getChildren().add(createHeader());
        mainContent.getChildren().add(createKPISection());

        // 2. Conteúdo principal: Árvore + Formulário
        HBox contentBox = new HBox(20);
        contentBox.setFillHeight(true);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        // Esquerda: Árvore com busca
        VBox leftPane = createTreeSection();
        leftPane.setPrefWidth(350);

        // Centro: Formulário
        VBox centerPane = createFormSection();
        centerPane.setPrefWidth(450);

        // Direita: Ações rápidas
        VBox rightPane = createActionsSection();
        rightPane.setPrefWidth(200);

        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(centerPane, Priority.NEVER);
        HBox.setHgrow(rightPane, Priority.NEVER);

        contentBox.getChildren().addAll(leftPane, centerPane, rightPane);
        mainContent.getChildren().add(contentBox);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        setCenter(scrollPane);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Plano de Contas");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Gestão contábil e estrutura de contas");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);
        return header;
    }

    private FlowPane createKPISection() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(15);
        pane.setAlignment(Pos.TOP_LEFT);
        pane.setPadding(new Insets(0, 0, 10, 0));

        lblTotalContas = new Label("0");
        lblContasRaiz = new Label("0");
        lblContasMovimento = new Label("0");
        lblContasAnaliticas = new Label("0");

        pane.getChildren().addAll(
            createKPICard("Total Contas", lblTotalContas, Feather.LIST, Styles.ACCENT),
            createKPICard("Contas Raiz", lblContasRaiz, Feather.GIT_BRANCH, Styles.WARNING),
            createKPICard("Contas Movimento", lblContasMovimento, Feather.ACTIVITY, Styles.SUCCESS),
            createKPICard("Contas Analíticas", lblContasAnaliticas, Feather.FILE_TEXT, Styles.TEXT_MUTED)
        );

        return pane;
    }

    private Card createKPICard(String titulo, Label valorLabel, Feather icono, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(150);
        card.setMaxWidth(180);

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(icono);
        icon.setIconSize(20);
        icon.getStyleClass().add(colorStyle);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(Styles.TEXT_MUTED);
        lblTitulo.setFont(Font.font("System", 11));
        lblTitulo.setWrapText(true);

        header.getChildren().addAll(icon, lblTitulo);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        content.getChildren().addAll(header, valorLabel);
        card.setBody(content);

        return card;
    }

    private VBox createTreeSection() {
        // Search
        searchField = new CustomTextField();
        searchField.setPromptText("Buscar conta...");
        searchField.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        searchField.textProperty().addListener((obs, ov, nv) -> filterTree(nv));

        // Tree
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                carregarDetalhes(newVal.getValue());
            } else {
                limparFormulario();
            }
        });

        Card card = new Card();
        card.setHeader(new Label("Estrutura de Contas"));
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setBody(treeView);

        VBox section = new VBox(10, searchField, card);
        VBox.setVgrow(card, Priority.ALWAYS);
        return section;
    }

    private VBox createFormSection() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setPadding(new Insets(15));

        codigoField = new TextField();
        codigoField.setPromptText("Código da conta");
        descricaoField = new TextField();
        descricaoField.setPromptText("Descrição da conta");
        classeCombo = new ComboBox<>();
        classeCombo.getItems().setAll(ClasseConta.values());
        classeCombo.setPromptText("Selecione a classe");
        naturezaCombo = new ComboBox<>();
        naturezaCombo.getItems().setAll(NaturezaConta.values());
        naturezaCombo.setPromptText("Selecione a natureza");
        movimentoCheck = new CheckBox("Conta de Movimento");
        parentLabel = new Label("Nenhum (Raiz)");

        form.addRow(0, new Label("Conta Pai:"), parentLabel);
        form.addRow(1, new Label("Código:"), codigoField);
        form.addRow(2, new Label("Descrição:"), descricaoField);
        form.addRow(3, new Label("Classe:"), classeCombo);
        form.addRow(4, new Label("Natureza:"), naturezaCombo);
        form.addRow(5, new Label("Tipo:"), movimentoCheck);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(col1, col2);

        Card card = new Card();
        card.setHeader(new Label("Detalhes da Conta"));
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setBody(form);

        VBox section = new VBox(card);
        return section;
    }

    private VBox createActionsSection() {
        Button btnSalvar = new Button("Salvar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSalvar.getStyleClass().addAll(Styles.SUCCESS);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setOnAction(e -> salvar());
        
        Button btnNovoFilho = new Button("Novo Filho", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovoFilho.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnNovoFilho.setMaxWidth(Double.MAX_VALUE);
        btnNovoFilho.setOnAction(e -> prepararNovoFilho());
        
        Button btnNovoRaiz = new Button("Nova Raiz", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        btnNovoRaiz.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnNovoRaiz.setMaxWidth(Double.MAX_VALUE);
        btnNovoRaiz.setOnAction(e -> prepararNovaRaiz());

        Button btnExcluir = new Button("Excluir", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
        btnExcluir.getStyleClass().addAll(Styles.DANGER);
        btnExcluir.setMaxWidth(Double.MAX_VALUE);
        btnExcluir.setOnAction(e -> excluir());

        Separator sep1 = new Separator();
        
        Button btnBalancete = new Button("Balancete", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnBalancete.getStyleClass().addAll(Styles.ACCENT);
        btnBalancete.setMaxWidth(Double.MAX_VALUE);
        btnBalancete.setOnAction(e -> gerarBalancete());
        
        Button btnDRE = new Button("DRE", IconUtils.icon(Feather.TRENDING_UP, IconUtils.SIZE_SMALL));
        btnDRE.getStyleClass().addAll(Styles.ACCENT);
        btnDRE.setMaxWidth(Double.MAX_VALUE);
        btnDRE.setOnAction(e -> gerarDRE());

        Card card = new Card();
        card.setHeader(new Label("Ações"));
        card.getStyleClass().add(Styles.ELEVATED_1);
        
        VBox actions = new VBox(10, btnSalvar, btnNovoFilho, btnNovoRaiz, btnExcluir, sep1, btnBalancete, btnDRE);
        actions.setPadding(new Insets(15));
        card.setBody(actions);

        VBox section = new VBox(card);
        return section;
    }

    private void filterTree(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            expandAll(treeView.getRoot(), true);
            return;
        }
        String lower = searchText.toLowerCase();
        filterTreeItem(treeView.getRoot(), lower);
    }

    private boolean filterTreeItem(TreeItem<PlanoConta> item, String searchText) {
        boolean matches = item.getValue() != null && (
            (item.getValue().getCodigo() != null && item.getValue().getCodigo().toLowerCase().contains(searchText)) ||
            (item.getValue().getDescricao() != null && item.getValue().getDescricao().toLowerCase().contains(searchText))
        );
        
        boolean childMatches = false;
        for (TreeItem<PlanoConta> child : item.getChildren()) {
            if (filterTreeItem(child, searchText)) {
                childMatches = true;
            }
        }
        
        boolean visible = matches || childMatches;
        item.setExpanded(childMatches);
        return visible;
    }

    private void expandAll(TreeItem<PlanoConta> item, boolean expand) {
        item.setExpanded(expand);
        for (TreeItem<PlanoConta> child : item.getChildren()) {
            expandAll(child, expand);
        }
    }

    private void updateKPIs(List<PlanoConta> todas) {
        int total = todas.size();
        int raiz = (int) todas.stream().filter(c -> c.getContaPai() == null).count();
        int movimento = (int) todas.stream().filter(c -> c.getMovimento() != null && c.getMovimento()).count();
        int analiticas = (int) todas.stream().filter(c -> c.getContaPai() != null && (c.getMovimento() == null || !c.getMovimento())).count();

        lblTotalContas.setText(String.valueOf(total));
        lblContasRaiz.setText(String.valueOf(raiz));
        lblContasMovimento.setText(String.valueOf(movimento));
        lblContasAnaliticas.setText(String.valueOf(analiticas));
    }

    private void loadTree() {
        TreeItem<PlanoConta> root = new TreeItem<>(new PlanoConta());
        root.setExpanded(true);
        
        List<PlanoConta> todas = service.findAll();
        Map<Long, TreeItem<PlanoConta>> itemMap = new HashMap<>();

        for (PlanoConta conta : todas) {
            TreeItem<PlanoConta> item = new TreeItem<>(conta);
            itemMap.put(conta.getId(), item);
        }

        for (PlanoConta conta : todas) {
            TreeItem<PlanoConta> item = itemMap.get(conta.getId());
            if (conta.getContaPai() != null) {
                TreeItem<PlanoConta> parentItem = itemMap.get(conta.getContaPai().getId());
                if (parentItem != null) {
                    parentItem.getChildren().add(item);
                } else {
                    root.getChildren().add(item);
                }
            } else {
                root.getChildren().add(item);
            }
        }
        
        sortTree(root);
        treeView.setRoot(root);
        
        // Atualizar KPIs
        updateKPIs(todas);
    }

    private void sortTree(TreeItem<PlanoConta> item) {
        item.getChildren().sort(Comparator.comparing(t -> t.getValue().getCodigo()));
        for (TreeItem<PlanoConta> child : item.getChildren()) {
            sortTree(child);
        }
    }

    private void addTreeItem(PlanoConta conta, TreeItem<PlanoConta> parentItem) {
        // Método não usado mais com a nova lógica
    }

    private void carregarDetalhes(PlanoConta conta) {
        currentConta = conta;
        currentParent = conta.getContaPai();
        
        codigoField.setText(conta.getCodigo());
        descricaoField.setText(conta.getDescricao());
        classeCombo.setValue(conta.getClasse());
        naturezaCombo.setValue(conta.getNatureza());
        movimentoCheck.setSelected(conta.getMovimento());
        
        if (currentParent != null) {
            parentLabel.setText(currentParent.toString());
        } else {
            parentLabel.setText("Nenhum (Raiz)");
        }
        
        codigoField.setEditable(false); // Código não deve mudar para evitar inconsistência, ou deve ter validação forte.
    }

    private void limparFormulario() {
        currentConta = null;
        codigoField.clear();
        descricaoField.clear();
        classeCombo.setValue(null);
        naturezaCombo.setValue(null);
        movimentoCheck.setSelected(false);
        codigoField.setEditable(true);
    }

    private void prepararNovoFilho() {
        TreeItem<PlanoConta> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selecione uma conta pai primeiro.");
            return;
        }
        
        currentParent = selected.getValue();
        currentConta = new PlanoConta(); // Novo objeto
        
        codigoField.clear();
        // Sugerir código: pai.codigo + "."
        codigoField.setText(currentParent.getCodigo() + ".");
        codigoField.setEditable(true);
        
        descricaoField.clear();
        classeCombo.setValue(currentParent.getClasse()); // Herda classe
        naturezaCombo.setValue(currentParent.getNatureza()); // Herda natureza
        movimentoCheck.setSelected(true); // Geralmente filhos são de movimento
        
        parentLabel.setText(currentParent.toString());
    }
    
    private void prepararNovaRaiz() {
        currentParent = null;
        currentConta = new PlanoConta();
        limparFormulario();
        parentLabel.setText("Nenhum (Raiz)");
    }

    private void salvar() {
        if (currentConta == null) currentConta = new PlanoConta();
        
        try {
            currentConta.setCodigo(codigoField.getText());
            currentConta.setDescricao(descricaoField.getText());
            currentConta.setClasse(classeCombo.getValue());
            currentConta.setNatureza(naturezaCombo.getValue());
            currentConta.setMovimento(movimentoCheck.isSelected());
            currentConta.setContaPai(currentParent);
            
            service.save(currentConta);
            loadTree(); // Recarregar árvore
            showAlert("Conta salva com sucesso!");
        } catch (Exception e) {
            showAlert("Erro ao salvar: " + e.getMessage());
        }
    }

    private void excluir() {
        if (currentConta == null || currentConta.getId() == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja excluir esta conta?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(currentConta.getId());
                loadTree();
                limparFormulario();
            } catch (Exception e) {
                showAlert("Erro ao excluir: " + e.getMessage());
            }
        }
    }

    private void gerarBalancete() {
        try {
            // Data de corte hoje
            LocalDate dataCorte = LocalDate.now();
            List<ContabilidadeService.BalanceteItemDTO> balancete = contabilidadeService.gerarBalancete(dataCorte);
            
            JasperPrint print = jasperReportService.prepararBalancete(balancete, dataCorte);
            jasperReportService.showReport(print);
        } catch (Exception e) {
            showAlert("Erro ao gerar balancete: " + e.getMessage());
        }
    }

    private void gerarDRE() {
        try {
            // Período: Mês Atual
            LocalDate inicio = LocalDate.now().withDayOfMonth(1);
            LocalDate fim = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            
            List<ContabilidadeService.BalanceteItemDTO> dre = contabilidadeService.gerarDRE(inicio, fim);
            
            JasperPrint print = jasperReportService.prepararDRE(dre, inicio, fim);
            jasperReportService.showReport(print);
        } catch (Exception e) {
            showAlert("Erro ao gerar DRE: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        AlertUtils.showInfoAlert("Informação", msg);
    }
}
