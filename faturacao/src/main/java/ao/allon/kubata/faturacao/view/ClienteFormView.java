package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.enums.TipoCliente;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.ui.components.KeyboardFxPopup;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.MaskTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;

/**
 * Formulário de criação / edição de cliente — Visual Moderno (Cards).
 *
 * Layout:
 * - Header fixo
 * - ScrollPane com VBox de Cards (Secções)
 * - Footer fixo
 */
public class ClienteFormView extends BorderPane {

    private final ClienteService  service;
    private final Cliente         original;
    private final java.util.function.Consumer<Cliente> onSaved;

    // ── Dados Pessoais ────────────────────────────────────────────────────────
    private final TextField          txtNome       = new TextField();
    private final MaskTextField      txtNif        = new MaskTextField("9999999999");
    private final TextField          txtBi         = new TextField();
    private final TextField          txtPassaporte  = new TextField();
    private final DatePicker         dpNascimento  = new DatePicker();
    private final ComboBox<String>   cbGenero      = new ComboBox<>();
    private final ComboBox<String>   cbEstadoCivil = new ComboBox<>();

    // ── Contactos ────────────────────────────────────────────────────────────
    private final MaskTextField      txtTelefone   = new MaskTextField("999999999");
    private final TextField          txtEmail      = new TextField();
    private final TextField          txtEndereco   = new TextField();
    private final ComboBox<String>   cbProvincia   = new ComboBox<>();
    private final TextField          txtMunicipio  = new TextField();

    // ── Dados Comerciais ─────────────────────────────────────────────────────
    private final ComboBox<TipoCliente> cbTipo     = new ComboBox<>();
    private final TextField          txtCategoria  = new TextField();
    private final TextField          txtLimite     = new TextField();
    private final ComboBox<String>   cbEstatuto    = new ComboBox<>();

    // ── Validação ─────────────────────────────────────────────────────────────
    private final ValidationSupport validation = new ValidationSupport();

    // ══════════════════════════════════════════════════════════════════════════
    public ClienteFormView(ClienteService service, Cliente cliente, java.util.function.Consumer<Cliente> onSaved) {
        this.service  = service;
        this.original = cliente;
        this.onSaved  = onSaved;

        setPadding(new Insets(0));
        setTop(buildFormHeader());
        setCenter(buildContent());
        setBottom(buildFooter());

        KeyboardFxPopup.install(this);
        configureValidation();
        configureBusinessLogic();
        preload();
    }

    // ── Cabeçalho do formulário ───────────────────────────────────────────────
    private Node buildFormHeader() {
        Label avatar = new Label(original == null ? "+" : getInitials(original.getNome()));
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(52, 52);
        avatar.setMaxSize(52, 52);
        avatar.getStyleClass().addAll(Styles.TITLE_3, Styles.ACCENT);
        avatar.setStyle("-fx-background-color: -color-accent-subtle; -fx-background-radius: 50%;");

        String headTitle = original == null ? "Novo Cliente" : "Editar: " + original.getNome();
        String headSub   = original == null
                ? "Preencha os dados do novo cliente."
                : "Actualize as informações do cliente.";

        Label title = new Label(headTitle);
        title.getStyleClass().add(Styles.TITLE_3);
        Label sub = new Label(headSub);
        sub.getStyleClass().add(Styles.TEXT_MUTED);

        VBox text = new VBox(2, title, sub);
        HBox header = new HBox(16, avatar, text);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.getStyleClass().add(Styles.BG_DEFAULT);
        header.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");
        return header;
    }

    // ── Conteúdo (Scrollable Cards) ──────────────────────────────────────────
    private Node buildContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.getChildren().addAll(
            createSection("Dados Pessoais", buildSectionPessoal()),
            createSection("Contactos", buildSectionContacto()),
            createSection("Dados Comerciais", buildSectionComercial())
        );

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // sp.getStyleClass().add(Styles.EDGE_TO_EDGE);
        return sp;
    }

    private Card createSection(String title, Node content) {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label(title));
        card.setBody(content);
        return card;
    }

    // ── Secção 1: Dados Pessoais ─────────────────────────────────────────────
    private GridPane buildSectionPessoal() {
        GridPane g = sectionGrid();

        cbGenero.setItems(FXCollections.observableArrayList("Masculino","Feminino","Outro"));
        cbGenero.setPromptText("Seleccionar…");
        cbEstadoCivil.setItems(FXCollections.observableArrayList("Solteiro(a)","Casado(a)","Divorciado(a)","Viúvo(a)","União de facto"));
        cbEstadoCivil.setPromptText("Seleccionar…");
        dpNascimento.setPromptText("dd/MM/aaaa");

        applyTextLimit(txtNome, 120);
        applyTextLimit(txtBi,   14);
        applyTextLimit(txtPassaporte, 20);

        txtNif.setPromptText("Ex: 5000000001");
        txtTelefone.setPromptText("Ex: 923456789");

        int r = 0;

        Label lblNome = formLabel("Nome *", Feather.USER);
        g.add(lblNome, 0, r);
        span(txtNome, g, r, 1, 3);
        r++;

        g.addRow(r++, formLabel("BI",     Feather.CREDIT_CARD),       txtBi,
                      formLabel("NIF *",  Feather.HASH),              txtNif);
        g.addRow(r++, formLabel("Passaporte", Feather.BOOK),          txtPassaporte,
                      formLabel("Data Nasc.", Feather.GIFT),          dpNascimento);
        g.addRow(r++, formLabel("Género", Feather.SMILE),             cbGenero,
                      formLabel("Estado Civil", Feather.HEART),       cbEstadoCivil);

        return g;
    }

    // ── Secção 2: Contactos ──────────────────────────────────────────────────
    private GridPane buildSectionContacto() {
        GridPane g = sectionGrid();

        cbProvincia.setItems(FXCollections.observableArrayList(
                "Luanda","Bengo","Benguela","Bié","Cabinda","Cuando Cubango",
                "Cuanza Norte","Cuanza Sul","Cunene","Huambo","Huíla","Lunda Norte",
                "Lunda Sul","Malanje","Moxico","Namibe","Uíge","Zaire"));
        cbProvincia.setPromptText("Seleccionar…");

        applyTextLimit(txtEmail, 120);
        applyTextLimit(txtEndereco, 180);
        applyTextLimit(txtMunicipio, 60);

        txtEmail.setPromptText("exemplo@empresa.ao");
        txtTelefone.setPromptText("9XXXXXXXX");

        int r = 0;
        g.addRow(r++, formLabel("Telefone", Feather.PHONE),          txtTelefone,
                      formLabel("Email",    Feather.MAIL),           txtEmail);

        Label lblEndereco = formLabel("Endereço", Feather.MAP_PIN);
        g.add(lblEndereco, 0, r);
        span(txtEndereco, g, r, 1, 3);
        r++;

        g.addRow(r++, formLabel("Província", Feather.MAP),           cbProvincia,
                      formLabel("Município",  Feather.NAVIGATION),    txtMunicipio);

        return g;
    }

    // ── Secção 3: Dados Comerciais ───────────────────────────────────────────
    private GridPane buildSectionComercial() {
        GridPane g = sectionGrid();

        cbTipo.setItems(FXCollections.observableArrayList(TipoCliente.values()));
        cbTipo.setPromptText("Seleccionar…");
        cbEstatuto.setItems(FXCollections.observableArrayList("Ativo","Inativo","Suspenso"));
        cbEstatuto.getSelectionModel().selectFirst();

        applyTextLimit(txtCategoria, 60);
        txtLimite.setPromptText("Ex: 50000.00");

        int r = 0;
        g.addRow(r++, formLabel("Tipo *",     Feather.BRIEFCASE),       cbTipo,
                      formLabel("Estatuto",   Feather.FLAG),            cbEstatuto);
        g.addRow(r++, formLabel("Categoria",  Feather.TAG),             txtCategoria,
                      formLabel("Limite Crédito", Feather.DOLLAR_SIGN), txtLimite);

        Label nota = new Label("ℹ️  O limite de crédito define o valor máximo de dívida permitido para este cliente.");
        nota.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.SMALL);
        nota.setWrapText(true);
        span(nota, g, r, 0, 4);

        return g;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        Button btnSalvar = new Button("Guardar", new FontIcon(Feather.SAVE));
        btnSalvar.getStyleClass().addAll(Styles.SUCCESS);
        btnSalvar.setDefaultButton(true);
        btnSalvar.setOnAction(e -> handleSave());

       

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10, spacer, btnSalvar);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 1 0 0 0; -fx-background-color: -color-bg-default;");
        return footer;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA DE NEGÓCIO & VALIDAÇÃO
    // ══════════════════════════════════════════════════════════════════════════
    private void configureBusinessLogic() {
        // NORMAS ANGOLANAS: Para cliente PARTICULAR, o BI é o NIF.
        /* Lógica removida a pedido do usuário: NIF agora é independente do BI.
        cbTipo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isParticular = (newVal == TipoCliente.PARTICULAR);
            if (isParticular) {
                txtNif.setEditable(false);
                txtNif.textProperty().bind(txtBi.textProperty());
            } else {
                txtNif.textProperty().unbind();
                txtNif.setEditable(true);
            }
        });
        */
        // Garante que NIF seja sempre editável e não vinculado
        txtNif.setEditable(true);
        txtNif.textProperty().unbind();
    }

    private void configureValidation() {
        validation.registerValidator(txtNome, true,
                Validator.combine(
                        Validator.createEmptyValidator("Nome é obrigatório"),
                        (javafx.scene.control.Control c, String s) -> ValidationResult.fromErrorIf(
                                c, "Mínimo 3 caracteres", s == null || s.trim().length() < 3)));

        validation.registerValidator(cbTipo, true,
                Validator.createEmptyValidator("Tipo de cliente é obrigatório"));

        validation.registerValidator(txtNif, true,
                (javafx.scene.control.Control c, String s) -> {
                    String msg = ao.allon.kubata.faturacao.util.AngolaValidationUtils.validateNifMessage(s);
                    return ValidationResult.fromErrorIf(c, msg != null ? msg : "", msg != null);
                });

        validation.registerValidator(txtTelefone, false,
                (javafx.scene.control.Control c, String s) -> ValidationResult.fromErrorIf(
                        c, "Telefone inválido",
                        s != null && !s.isBlank()
                                && !ao.allon.kubata.faturacao.util.AngolaValidationUtils.isValidTelefone(s)));
        
        // Validação condicional: Se for EMPRESA, NIF deve ser válido para empresa (se houver regra específica)
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRÉ-CARGA
    // ══════════════════════════════════════════════════════════════════════════
    private void preload() {
        if (original == null) {
            cbTipo.setValue(TipoCliente.PARTICULAR); // Default
            return;
        }
        txtNome.setText(safe(original.getNome()));
        txtBi.setText(safe(original.getBi()));
        
        // Se for particular, o NIF vai ser bindado ao BI, então cuidado para não sobrescrever
        cbTipo.setValue(original.getTipo());
        
        // NIF agora é sempre carregado independentemente do tipo
        txtNif.setText(ao.allon.kubata.faturacao.util.AngolaValidationUtils.formatNif(safe(original.getNif())));

        txtPassaporte.setText(safe(original.getPassaporte()));
        txtTelefone.setText(safe(original.getTelefone()));
        txtEmail.setText(safe(original.getEmail()));
        dpNascimento.setValue(original.getDataNascimento());
        cbGenero.setValue(original.getGenero());
        cbEstadoCivil.setValue(original.getEstadoCivil());
        txtEndereco.setText(safe(original.getEndereco()));
        cbProvincia.setValue(original.getProvincia());
        txtMunicipio.setText(safe(original.getMunicipio()));
        txtCategoria.setText(safe(original.getCategoriaCliente()));
        if (original.getLimiteCredito() != null) txtLimite.setText(original.getLimiteCredito().toPlainString());
        cbEstatuto.setValue(original.getEstatuto() != null ? original.getEstatuto() : "Ativo");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GUARDAR
    // ══════════════════════════════════════════════════════════════════════════
    private void handleSave() {
        validation.redecorate();
        if (validation.isInvalid()) {
            AlertUtils.showErrorAlert("Validação", "Corrija os campos assinalados antes de guardar.");
            return;
        }
        Cliente c = original != null ? original : new Cliente();
        c.setNome(txtNome.getText().trim());
        c.setNif(ao.allon.kubata.faturacao.util.AngolaValidationUtils.normalizeNif(txtNif.getText()));
        c.setBi(emptyOrNull(txtBi.getText()));
        c.setPassaporte(emptyOrNull(txtPassaporte.getText()));
        c.setTelefone(emptyOrNull(txtTelefone.getText()));
        c.setEmail(emptyOrNull(txtEmail.getText()));
        c.setTipo(cbTipo.getValue() != null ? cbTipo.getValue() : TipoCliente.PARTICULAR);
        c.setDataNascimento(dpNascimento.getValue());
        c.setGenero(emptyOrNull(cbGenero.getValue()));
        c.setEstadoCivil(emptyOrNull(cbEstadoCivil.getValue()));
        c.setEndereco(emptyOrNull(txtEndereco.getText()));
        c.setProvincia(emptyOrNull(cbProvincia.getValue()));
        c.setMunicipio(emptyOrNull(txtMunicipio.getText()));
        c.setCategoriaCliente(emptyOrNull(txtCategoria.getText()));
        try {
            String lim = txtLimite.getText();
            c.setLimiteCredito(lim != null && !lim.isBlank()
                    ? new java.math.BigDecimal(lim.replace(",","."))
                    : java.math.BigDecimal.ZERO);
        } catch (Exception ex) {
            c.setLimiteCredito(java.math.BigDecimal.ZERO);
        }
        c.setEstatuto(cbEstatuto.getValue());

        try {
            service.save(c);
            if (onSaved != null) onSaved.accept(c);
        } catch (IllegalArgumentException ex) {
            AlertUtils.showErrorAlert("Validação", ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.showErrorAlert("Erro ao guardar", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private static GridPane sectionGrid() {
        GridPane g = new GridPane();
        g.setHgap(15);
        g.setVgap(15);
        g.setPadding(new Insets(15));
        
        ColumnConstraints c1 = new ColumnConstraints(100);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        ColumnConstraints c3 = new ColumnConstraints(100);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2, c3, c4);
        return g;
    }

    private static Label formLabel(String text, Feather iconLiteral) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(14);
        
        Label l = new Label(text, icon);
        l.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.SMALL);
        l.setGraphicTextGap(6);
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    private static <T extends javafx.scene.control.Control> T span(T ctrl, GridPane g, int row, int col, int colspan) {
        GridPane.setColumnSpan(ctrl, colspan);
        g.add(ctrl, col, row);
        return ctrl;
    }

    private static void applyTextLimit(TextField field, int max) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= max ? change : null));
    }

    private static String safe(String s)         { return s == null ? "" : s; }
    private static String emptyOrNull(String s)  {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String getInitials(String nome) {
        if (nome == null || nome.isBlank()) return "+";
        String[] p = nome.trim().split("\\s+");
        return p.length >= 2
                ? ("" + p[0].charAt(0) + p[p.length-1].charAt(0)).toUpperCase()
                : nome.substring(0,1).toUpperCase();
    }
}
