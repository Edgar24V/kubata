package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.enums.TipoCliente;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Predicate;

/**
 * Ecrã principal de gestão de clientes — versão refactorizada.
 *
 * Melhorias:
 * - Header com KPI cards (total, ativos, inativos, suspensos)
 * - Toolbar condensada com ícones AtlantaFX / Ikonli
 * - Tabela com coluna de estatuto colorida (badge inline)
 * - Painel de detalhes redesenhado com avatar, tabs e histórico
 * - Paginação e filtros preservados
 */
public class ClientesView extends BorderPane {

    // ── Serviços ─────────────────────────────────────────────────────────────
    private final ClienteService  clienteService;
    private final FaturaService   faturaService;
    private final ReciboService   reciboService;
    private final SessionManager  sessionManager;
    private final ModalService    modalService;

    // ── UI / dados ────────────────────────────────────────────────────────────
    private final AdvancedTableView<Cliente> table = new AdvancedTableView<>();
    private final ObservableList<Cliente> masterData = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblTotalClientes;
    private Label lblClientesAtivos;
    private Label lblClientesInativos;
    private Label lblClientesEmpresa;

    // ─────────────────────────────────────────────────────────────────────────
    public ClientesView(ClienteService clienteService,
                        FaturaService   faturaService,
                        ReciboService   reciboService,
                        SessionManager  sessionManager,
                        ModalService    modalService) {
        this.clienteService = clienteService;
        this.faturaService  = faturaService;
        this.reciboService  = reciboService;
        this.sessionManager = sessionManager;
        this.modalService   = modalService;

        setPadding(new Insets(20));

        // Layout Principal
        setTop(createTopDashboard());
        setCenter(createMainContent());
        
        loadData();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DASHBOARD (KPIs)
    // ══════════════════════════════════════════════════════════════════════════
    private Node createTopDashboard() {
        HBox dashboard = new HBox(20);
        dashboard.setAlignment(Pos.CENTER_LEFT);
        dashboard.setPadding(new Insets(0, 0, 20, 0));

        lblTotalClientes = new Label("0");
        lblClientesAtivos = new Label("0");
        lblClientesInativos = new Label("0");
        lblClientesEmpresa = new Label("0");

        Card kpiTotal = createKPICard("Total Clientes", lblTotalClientes, Feather.USERS, Styles.ACCENT);
        Card kpiAtivos = createKPICard("Ativos", lblClientesAtivos, Feather.USER_CHECK, Styles.SUCCESS);
        Card kpiInativos = createKPICard("Inativos", lblClientesInativos, Feather.USER_X, Styles.DANGER);
        Card kpiEmpresas = createKPICard("Empresas", lblClientesEmpresa, Feather.HOME, Styles.WARNING);

        HBox.setHgrow(kpiTotal, Priority.ALWAYS);
        HBox.setHgrow(kpiAtivos, Priority.ALWAYS);
        HBox.setHgrow(kpiInativos, Priority.ALWAYS);
        HBox.setHgrow(kpiEmpresas, Priority.ALWAYS);

        dashboard.getChildren().addAll(kpiTotal, kpiAtivos, kpiInativos, kpiEmpresas);
        return dashboard;
    }

    private Card createKPICard(String titulo, Label valorLabel, Feather icono, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(180);
        
        VBox content = new VBox(8);
        content.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(icono);
        icon.setIconSize(24);
        icon.getStyleClass().add(colorStyle);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(Styles.TEXT_MUTED);
        lblTitulo.setWrapText(true);

        header.getChildren().addAll(icon, lblTitulo);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        content.getChildren().addAll(header, valorLabel);
        card.setBody(content);

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN CONTENT (Toolbar + Table)
    // ══════════════════════════════════════════════════════════════════════════
    private Node createMainContent() {
        VBox container = new VBox(15);
        VBox.setVgrow(container, Priority.ALWAYS);

        // Header com título, busca e botões
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        CustomTextField search = new CustomTextField();
        search.setPromptText("Pesquisar nome, NIF, email...");
        search.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        search.setPrefWidth(300);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button("Novo", IconUtils.icon(Feather.USER_PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnNovo.setOnAction(e -> showClienteForm(null));

        Button btnImport = new Button("Importar", IconUtils.icon(Feather.UPLOAD, IconUtils.SIZE_SMALL));
        btnImport.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnImport.setOnAction(e -> importCSV());

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExport.setOnAction(e -> exportCSV());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_ICON);
        btnAtualizar.setTooltip(new Tooltip("Atualizar Dados"));
        btnAtualizar.setOnAction(e -> loadData());

        header.getChildren().addAll(search, spacer, btnNovo, btnImport, btnExport, btnAtualizar);
        
        setupTable();
        table.setData(masterData);

        search.textProperty().addListener((obs, ov, nv) -> {
            String q = nv != null ? nv.trim().toLowerCase() : "";
            table.setFilter(c -> q.isEmpty()
                    || (c.getNome() != null && c.getNome().toLowerCase().contains(q))
                    || (c.getNif() != null && c.getNif().toLowerCase().contains(q))
                    || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                    || (c.getTelefone() != null && c.getTelefone().toLowerCase().contains(q)));
        });

        container.getChildren().addAll(header, table);
        return container;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CORPO — tabela (esquerda) + painel de detalhes (direita)
    // ══════════════════════════════════════════════════════════════════════════
    private void setupTable() {
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Cliente, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNome()));

        TableColumn<Cliente, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNif()));

        TableColumn<Cliente, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo() != null ? c.getValue().getTipo().name() : ""));
        colTipo.setCellFactory(col -> new TableCell<>() {
            private final Label tag = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setGraphic(null); return; }
                tag.setText(item);
                tag.getStyleClass().clear();
                tag.getStyleClass().add(Styles.TEXT_BOLD);
                tag.getStyleClass().add(Styles.ACCENT);
                setGraphic(tag);
                setText(null);
            }
        });

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEmail()));

        TableColumn<Cliente, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTelefone()));

        TableColumn<Cliente, String> colProvincia = new TableColumn<>("Província");
        colProvincia.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getProvincia()));

        TableColumn<Cliente, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEstatuto()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label tag = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setGraphic(null); return; }
                tag.setText(item);
                tag.getStyleClass().clear();
                tag.getStyleClass().add(Styles.TEXT_BOLD);
                if ("Ativo".equalsIgnoreCase(item)) tag.getStyleClass().add(Styles.SUCCESS);
                else if ("Inativo".equalsIgnoreCase(item)) tag.getStyleClass().add(Styles.DANGER);
                else if ("Suspenso".equalsIgnoreCase(item)) tag.getStyleClass().add(Styles.WARNING);
                else tag.getStyleClass().add(Styles.ACCENT);
                setGraphic(tag);
                setText(null);
            }
        });

        TableColumn<Cliente, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setSortable(false);
        colAcoes.setMinWidth(140);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnFaturas = new Button("", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
            private final Button btnHist    = new Button("", IconUtils.icon(Feather.CLOCK, IconUtils.SIZE_SMALL));
            private final Button btnEdit    = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button btnDel     = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            {
                btnFaturas.getStyleClass().addAll(Styles.BUTTON_ICON);
                btnHist.getStyleClass().addAll(Styles.BUTTON_ICON);
                btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON);
                btnDel.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER);
                btnFaturas.setTooltip(new Tooltip("Ver faturas"));
                btnHist.setTooltip(new Tooltip("Ver histórico"));
                btnEdit.setTooltip(new Tooltip("Editar"));
                btnDel.setTooltip(new Tooltip("Excluir"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Cliente c = getTableView().getItems().get(getIndex());
                btnFaturas.setOnAction(e -> showFaturasCliente(c));
                btnHist.setOnAction(e -> showHistoricoCliente(c));
                btnEdit.setOnAction(e -> showClienteForm(c));
                btnDel.setOnAction(e -> {
                    table.getSelectionModel().select(c);
                    confirmDelete();
                });
                setGraphic(new HBox(6, btnFaturas, btnHist, btnEdit, btnDel));
            }
        });

        table.getColumns().setAll(colNome, colNif, colTipo, colEmail, colTelefone, colProvincia, colStatus, colAcoes);
        VBox.setVgrow(table, Priority.ALWAYS);
        // table is added to container in createMainContent, so no need to add to children here if we just configure columns
        // But previously it was adding to children. Let's see createMainContent again.
        // createMainContent calls setupTable() then adds table to container.
        // So setupTable should ONLY configure the table, NOT add it to children.
    }

    private void showFaturasCliente(Cliente cliente) {
        if (cliente == null) return;
        ListView<String> list = new ListView<>();
        list.getItems().setAll(
                faturaService.findAll().stream()
                        .filter(f -> f.getCliente() != null && f.getCliente().getId().equals(cliente.getId()))
                        .map(f -> "Fatura " + f.getNumero() + " · " + f.getStatus() + " · " + f.getTotal() + " Kz")
                        .toList()
        );
        list.setPlaceholder(new Label("Sem faturas para este cliente."));
        modalService.create()
                .title("Faturas — " + safeStr(cliente.getNome()))
                .content(list)
                .autoSize()
                .buildAndShow();
    }

    private void showHistoricoCliente(Cliente cliente) {
        if (cliente == null) return;
        ListView<String> list = new ListView<>();
        var items = new java.util.ArrayList<String>();
        faturaService.findAll().stream()
                .filter(f -> f.getCliente() != null && f.getCliente().getId().equals(cliente.getId()))
                .forEach(f -> items.add("Fatura " + f.getNumero() + " · " + f.getStatus() + " · " + f.getTotal() + " Kz"));
        reciboService.findAll().stream()
                .filter(r -> r.getFatura() != null && r.getFatura().getCliente() != null
                        && r.getFatura().getCliente().getId().equals(cliente.getId()))
                .forEach(r -> items.add("Recibo " + r.getNumero() + " · " + r.getValor() + " Kz"));
        list.getItems().setAll(items);
        list.setPlaceholder(new Label("Sem histórico para este cliente."));
        modalService.create()
                .title("Histórico — " + safeStr(cliente.getNome()))
                .content(list)
                .autoSize()
                .buildAndShow();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DADOS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadData() {
        List<Cliente> lista = clienteService.findAll();
        masterData.setAll(lista);
        table.setFilter(c -> true);

        // Atualizar KPIs
        int total = lista.size();
        int ativos = (int) lista.stream().filter(c -> "Ativo".equalsIgnoreCase(c.getEstatuto())).count();
        int inativos = (int) lista.stream().filter(c -> "Inativo".equalsIgnoreCase(c.getEstatuto())).count();
        int empresas = (int) lista.stream().filter(c -> c.getTipo() == TipoCliente.EMPRESA).count();

        lblTotalClientes.setText(String.valueOf(total));
        lblClientesAtivos.setText(String.valueOf(ativos));
        lblClientesInativos.setText(String.valueOf(inativos));
        lblClientesEmpresa.setText(String.valueOf(empresas));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FORMULÁRIO (abre em modal)
    // ══════════════════════════════════════════════════════════════════════════
    private void showClienteForm(Cliente cliente) {
        ClienteFormView form = new ClienteFormView(clienteService, cliente, (c) -> {
            loadData();
            showToast("Sucesso", cliente == null ? "Cliente criado com sucesso." : "Cliente actualizado.");
        });
        form.setPrefSize(800, 600);
        modalService.create()
                .title(cliente == null ? "Novo Cliente" : "Editar Cliente — " + cliente.getNome())
                .content(form)
                .autoSize()
                .buildAndShow();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXCLUIR
    // ══════════════════════════════════════════════════════════════════════════
    private void confirmDelete() {
        Cliente c = table.getSelectionModel().getSelectedItem();
        if (c == null) { showInfo("Selecione um cliente para excluir."); return; }
        modalService.create()
                .title("Confirmar exclusão")
                .content(new Label("Tem a certeza que pretende excluir \"" + c.getNome() + "\"?\nEsta operação não pode ser revertida."))
                .autoSize()
                .withConfirmButton("Excluir", () -> {
                    try {
                        clienteService.delete(c.getId());
                        loadData();
                        showToast("Cliente excluído", c.getNome() + " foi removido com sucesso.");
                        return true;
                    } catch (Exception ex) {
                        showInfo("Erro: " + ex.getMessage());
                        return false;
                    }
                })
                .buildAndShow();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ACÇÕES EM LOTE
    // ══════════════════════════════════════════════════════════════════════════
    private void changeEstatutoSelected(String estatuto) {
        Cliente sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Selecione pelo menos um cliente."); return; }
        sel.setEstatuto(estatuto);
        clienteService.save(sel);
        loadData();
        showToast("Estatuto alterado", sel.getNome() + " → " + estatuto);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORTAÇÃO / IMPORTAÇÃO
    // ══════════════════════════════════════════════════════════════════════════
    private void exportCSV() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(System.getProperty("user.home"), "clientes_export.csv");
            java.util.List<String> lines = new java.util.ArrayList<>();
            lines.add("nome;nif;telefone;email;endereco;provincia;municipio;tipo;categoria;limite_credito;estatuto");
            for (Cliente c : table.getItems()) {
                lines.add(String.join(";",
                        safe(c.getNome()), safe(c.getNif()), safe(c.getTelefone()), safe(c.getEmail()),
                        safe(c.getEndereco()), safe(c.getProvincia()), safe(c.getMunicipio()),
                        c.getTipo() != null ? c.getTipo().name() : "",
                        safe(c.getCategoriaCliente()),
                        c.getLimiteCredito() != null ? c.getLimiteCredito().toString() : "",
                        safe(c.getEstatuto())));
            }
            java.nio.file.Files.write(path, lines);
            showToast("Exportação concluída", "Ficheiro guardado em " + path.getFileName());
        } catch (Exception e) {
            showInfo("Erro ao exportar: " + e.getMessage());
        }
    }

    private void importCSV() {
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Importar clientes CSV");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
            java.io.File file = chooser.showOpenDialog(getScene().getWindow());
            if (file == null) return;
            java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            if (lines.isEmpty()) return;
            int imported = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] p = line.split(";");
                if (p.length < 2) continue;
                Cliente c = new Cliente();
                c.setNome(p[0]);
                c.setNif(p[1]);
                if (p.length > 2) c.setTelefone(p[2]);
                if (p.length > 3) c.setEmail(p[3]);
                if (p.length > 4) c.setEndereco(p[4]);
                if (p.length > 5) c.setProvincia(p[5]);
                if (p.length > 6) c.setMunicipio(p[6]);
                c.setTipo(p.length > 7 && !p[7].isBlank() ? tryEnum(p[7]) : TipoCliente.PARTICULAR);
                if (p.length > 8) c.setCategoriaCliente(p[8]);
                if (p.length > 9 && !p[9].isBlank()) {
                    try { c.setLimiteCredito(new java.math.BigDecimal(p[9].replace(",","."))); }
                    catch (NumberFormatException ignored) {}
                }
                if (p.length > 10) c.setEstatuto(p[10]);
                try { clienteService.save(c); imported++; }
                catch (Exception ignored) {}
            }
            loadData();
            showToast("Importação concluída", imported + " clientes importados.");
        } catch (Exception e) {
            showInfo("Erro ao importar: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITÁRIOS
    // ══════════════════════════════════════════════════════════════════════════
    private static TipoCliente tryEnum(String s) {
        try { return TipoCliente.valueOf(s); } catch (Exception e) { return TipoCliente.PARTICULAR; }
    }
    private static String safe(String s)    { return s == null ? "" : s; }
    private static String safeStr(String s) { return s == null ? "" : s; }

    private void showInfo(String msg)  { AlertUtils.showInfoAlert("Informação", msg); }

    private void showToast(String title, String message) {
        Notifications.create()
                .title(title)
                .text(message)
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(3))
                .showInformation();
    }
}
