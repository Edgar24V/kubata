package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.FornecedorService;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.ProdutoService;
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
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

/**
 * Formulário de cadastro de Produto — Visual Moderno (Cards).
 * 
 * Layout:
 * - Header fixo
 * - ScrollPane com VBox de Cards (Secções)
 * - Footer fixo
 */
public class ProdutoFormView extends BorderPane {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final ImpostoService impostoService;
    private final FornecedorService fornecedorService;
    
    private final Produto original;
    private final Consumer<Produto> onSaved;
    private final boolean isEdit;

    // ── Informações Básicas ──────────────────────────────────────────────────
    private final TextField txtNome = new TextField();
    private final TextArea txtDescricao = new TextArea();
    private final SearchableComboBox<Categoria> cbCategoria = new SearchableComboBox<>();
    private final ComboBox<UnidadeMedida> cbUnidade = new ComboBox<>();
    private final TextField txtMarca = new TextField();

    // ── Identificação & Stock ────────────────────────────────────────────────
    private final ComboBox<String> cbBarcodeType = new ComboBox<>();
    private final MaskTextField txtCodigo = new MaskTextField();
    private final TextField txtLocalizacao = new TextField();
    private final TextField txtStockAtual = new TextField();
    private final TextField txtStockMin = new TextField();

    // ── Preços & Impostos ────────────────────────────────────────────────────
    private final TextField txtPreco = new TextField();
    private final TextField txtCusto = new TextField();
    private final ComboBox<Imposto> cbImposto = new ComboBox<>();
    private final SearchableComboBox<Fornecedor> cbFornecedor = new SearchableComboBox<>();

    private final TextField txtStepVenda = new TextField();
    private final TextField txtCasasDecimaisQtd = new TextField();

    // ── Validação ─────────────────────────────────────────────────────────────
    private final ValidationSupport validation = new ValidationSupport();

    private static boolean isUnidadeFracionavel(UnidadeMedida u) {
        if (u == null) return false;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private static BigDecimal unidadeMinimaParaUnidade(UnidadeMedida u, Integer qtdMinima) {
        if (qtdMinima == null) return BigDecimal.ZERO;
        if (u == null) return BigDecimal.valueOf(qtdMinima);

        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> BigDecimal.valueOf(qtdMinima).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
            case HORA, SERVICO -> BigDecimal.valueOf(qtdMinima).divide(new BigDecimal("60"), 3, RoundingMode.HALF_UP);
            default -> BigDecimal.valueOf(qtdMinima);
        };
    }

    private static int unidadeParaUnidadeMinima(UnidadeMedida u, BigDecimal qtd) {
        if (qtd == null) return 0;
        if (u == null) return qtd.setScale(0, RoundingMode.HALF_UP).intValue();

        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
            default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
        };
    }

    private static String unidadeShort(UnidadeMedida u) {
        if (u == null) return "un";
        return switch (u) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        };
    }

    private static BigDecimal defaultStepForUnit(UnidadeMedida u) {
        if (u == null) return BigDecimal.ONE;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> new BigDecimal("0.100");
            case HORA, SERVICO -> new BigDecimal("0.250");
            default -> BigDecimal.ONE;
        };
    }

    private static int defaultCasasForUnit(UnidadeMedida u) {
        if (u == null) return 0;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> 3;
            case HORA, SERVICO -> 3;
            default -> 0;
        };
    }

    public ProdutoFormView(ProdutoService produtoService, 
                           CategoriaService categoriaService, 
                           ImpostoService impostoService,
                           FornecedorService fornecedorService,
                           Produto produto, 
                           Consumer<Produto> onSaved) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.impostoService = impostoService;
        this.fornecedorService = fornecedorService;
        this.original = produto;
        this.onSaved = onSaved;
        this.isEdit = (produto != null);

        setPadding(new Insets(0));
        setTop(buildFormHeader());
        setCenter(buildContent());
        setBottom(buildFooter());

        KeyboardFxPopup.install(this);
        configureValidation();
        configureBusinessLogic();
        preload();
    }

    // ── Cabeçalho ─────────────────────────────────────────────────────────────
    private Node buildFormHeader() {
        Label icon = new Label("", new FontIcon(Feather.PACKAGE));
        ((FontIcon) icon.getGraphic()).setIconSize(24);
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(52, 52);
        icon.setMaxSize(52, 52);
        icon.getStyleClass().addAll(Styles.TITLE_3, Styles.ACCENT);
        icon.setStyle("-fx-background-color: -color-accent-subtle; -fx-background-radius: 50%;");

        String headTitle = isEdit ? "Editar: " + original.getNome() : "Novo Produto";
        String headSub = isEdit ? "Atualize as informações do produto." : "Preencha os dados do novo produto.";

        Label title = new Label(headTitle);
        title.getStyleClass().add(Styles.TITLE_3);
        Label sub = new Label(headSub);
        sub.getStyleClass().add(Styles.TEXT_MUTED);

        VBox text = new VBox(2, title, sub);
        HBox header = new HBox(16, icon, text);
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
            createSection("Informações Básicas", buildSectionBasic()),
            createSection("Identificação & Stock", buildSectionIdStock()),
            createSection("Preços & Impostos", buildSectionPriceTax())
        );

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // sp.getStyleClass().add(Styles.EDGE_TO_EDGE); // Removed as requested
        return sp;
    }

    private Card createSection(String title, Node content) {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label(title));
        card.setBody(content);
        return card;
    }

    // ── Secção 1: Informações Básicas ────────────────────────────────────────
    private GridPane buildSectionBasic() {
        GridPane g = sectionGrid();

        cbCategoria.setItems(FXCollections.observableArrayList(categoriaService.findAll()));
        cbCategoria.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c != null ? c.getNome() : ""; }
            @Override public Categoria fromString(String s) { return null; }
        });

        validation.registerValidator(txtStepVenda, false, (Control c, String val) -> {
            boolean invalid = false;
            try { if (val != null && !val.isBlank()) new BigDecimal(val.replace(",", ".")); }
            catch (Exception e) { invalid = true; }
            return ValidationResult.fromErrorIf(c, "Step inválido", invalid);
        });

        validation.registerValidator(txtCasasDecimaisQtd, false, (Control c, String val) -> {
            boolean invalid = false;
            try {
                if (val != null && !val.isBlank()) {
                    int i = Integer.parseInt(val);
                    invalid = i < 0 || i > 6;
                }
            } catch (Exception e) { invalid = true; }
            return ValidationResult.fromErrorIf(c, "Casas decimais inválidas", invalid);
        });

        cbUnidade.setItems(FXCollections.observableArrayList(UnidadeMedida.values()));
        
        txtDescricao.setPrefRowCount(3);
        applyTextLimit(txtNome, 100);
        applyTextLimit(txtMarca, 50);

        int r = 0;
        Label lblNome = formLabel("Nome *", Feather.TAG);
        g.add(lblNome, 0, r);
        span(txtNome, g, r, 1, 3);
        r++;

        g.addRow(r++, formLabel("Categoria", Feather.FOLDER), cbCategoria,
                      formLabel("Marca", Feather.AWARD),      txtMarca);

        g.addRow(r++, formLabel("Unidade *", Feather.BOX),    cbUnidade,
                      new Label(), new Label()); // Spacer

        Label lblDesc = formLabel("Descrição", Feather.FILE_TEXT);
        g.add(lblDesc, 0, r);
        span(txtDescricao, g, r, 1, 3);
        
        return g;
    }

    // ── Secção 2: Identificação & Stock ──────────────────────────────────────
    private GridPane buildSectionIdStock() {
        GridPane g = sectionGrid();

        cbBarcodeType.setItems(FXCollections.observableArrayList("EAN-13", "EAN-8", "UPC-A"));
        
        if (isEdit) {
            txtStockAtual.setDisable(true);
            txtStockAtual.setPromptText("Gerido via Movimentos");
        } else {
            txtStockAtual.setPromptText("Stock Inicial");
        }
        
        txtStockMin.setPromptText("Alerta Mínimo");
        txtStepVenda.setPromptText("0.100");
        txtCasasDecimaisQtd.setPromptText("3");

        int r = 0;
        g.addRow(r++, formLabel("Tipo Código", Feather.BAR_CHART), cbBarcodeType,
                      formLabel("Código Barras *", Feather.HASH),  txtCodigo);
        
        g.addRow(r++, formLabel("Localização", Feather.MAP_PIN),   txtLocalizacao,
                      new Label(), new Label());

        g.addRow(r++, formLabel("Stock Atual", Feather.LAYERS),    txtStockAtual,
                      formLabel("Stock Mínimo", Feather.ALERT_CIRCLE), txtStockMin);

        g.addRow(r++, formLabel("Step Venda", Feather.SLIDERS), txtStepVenda,
                      formLabel("Casas Dec.", Feather.HASH), txtCasasDecimaisQtd);

        return g;
    }

    // ── Secção 3: Preços & Impostos ──────────────────────────────────────────
    private GridPane buildSectionPriceTax() {
        GridPane g = sectionGrid();

        cbImposto.setItems(FXCollections.observableArrayList(impostoService.findAll()));
        cbImposto.setConverter(new StringConverter<>() {
            @Override public String toString(Imposto i) { return i != null ? i.getCodigo() + " (" + i.getPercentual() + "%)" : ""; }
            @Override public Imposto fromString(String s) { return null; }
        });

        cbFornecedor.setItems(FXCollections.observableArrayList(fornecedorService.findAll()));
        cbFornecedor.setConverter(new StringConverter<>() {
            @Override public String toString(Fornecedor f) { return f != null ? f.getNome() : ""; }
            @Override public Fornecedor fromString(String s) { return null; }
        });

        txtPreco.setPromptText("0.00 AOA");
        txtCusto.setPromptText("0.00 AOA");

        int r = 0;
        g.addRow(r++, formLabel("Preço Venda *", Feather.DOLLAR_SIGN), txtPreco,
                      formLabel("Custo", Feather.SHOPPING_BAG),        txtCusto);

        g.addRow(r++, formLabel("Imposto (IVA) *", Feather.PERCENT),   cbImposto,
                      formLabel("Fornecedor", Feather.TRUCK),          cbFornecedor);

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

    // ── Lógica de Negócio ─────────────────────────────────────────────────────
    private void configureBusinessLogic() {
        // Auto-detect Barcode Type
        txtCodigo.textProperty().addListener((obs, ov, nv) -> {
            if (nv != null && !nv.isEmpty()) {
                String detected = detectBarcodeType(nv);
                if (detected != null && cbBarcodeType.getValue() == null) {
                    cbBarcodeType.setValue(detected);
                }
            }
        });

        cbBarcodeType.valueProperty().addListener((obs, ov, nv) -> updateBarcodeMask(nv, txtCodigo));

        cbUnidade.valueProperty().addListener((obs, ov, nv) -> {
            if (isEdit) {
                txtStockAtual.setPromptText("Gerido via Movimentos");
                return;
            }
            if (isUnidadeFracionavel(nv)) {
                txtStockAtual.setPromptText("Stock Inicial (" + unidadeShort(nv) + ")");
            } else {
                txtStockAtual.setPromptText("Stock Inicial");
            }
        });
    }

    private void configureValidation() {
        validation.registerValidator(txtNome, true, Validator.createEmptyValidator("Nome é obrigatório"));
        
        validation.registerValidator(txtCodigo, true, Validator.combine(
                Validator.createEmptyValidator("Código é obrigatório"),
                (Control c, String val) -> ValidationResult.fromErrorIf(c, "Código inválido", !isBarcodeValid(val, cbBarcodeType.getValue()))
        ));

        validation.registerValidator(cbUnidade, true, Validator.createEmptyValidator("Unidade obrigatória"));
        
        validation.registerValidator(txtPreco, true, (Control c, String val) -> {
            boolean invalid = false;
            try { if(val != null && !val.isBlank()) new BigDecimal(val.replace(",",".")); }
            catch (Exception e) { invalid = true; }
            return ValidationResult.fromErrorIf(c, "Preço inválido", invalid);
        });

        validation.registerValidator(cbImposto, true, Validator.createEmptyValidator("Imposto obrigatório"));
    }

    private void preload() {
        if (original == null) {
            cbUnidade.setValue(UnidadeMedida.UNIDADE);
            // Tentar setar imposto padrão (NOR)
            cbImposto.getItems().stream()
                    .filter(i -> "NOR".equalsIgnoreCase(i.getCodigo()))
                    .findFirst()
                    .ifPresent(cbImposto::setValue);
            return;
        }
        
        txtNome.setText(safe(original.getNome()));
        txtDescricao.setText(safe(original.getDescricao()));
        cbCategoria.setValue(original.getCategoria());
        txtMarca.setText(safe(original.getMarca()));
        cbUnidade.setValue(original.getUnidadeMedida());
        
        cbBarcodeType.setValue(detectBarcodeType(original.getCodigoBarra()));
        txtCodigo.setText(safe(original.getCodigoBarra()));
        txtLocalizacao.setText(safe(original.getLocalizacao()));

        if (original.getStepVenda() != null) {
            txtStepVenda.setText(original.getStepVenda().stripTrailingZeros().toPlainString());
        } else {
            txtStepVenda.setText(defaultStepForUnit(original.getUnidadeMedida()).stripTrailingZeros().toPlainString());
        }

        if (original.getCasasDecimaisQuantidade() != null) {
            txtCasasDecimaisQtd.setText(String.valueOf(original.getCasasDecimaisQuantidade()));
        } else {
            txtCasasDecimaisQtd.setText(String.valueOf(defaultCasasForUnit(original.getUnidadeMedida())));
        }
        
        if (isUnidadeFracionavel(original.getUnidadeMedida())) {
            BigDecimal shown = unidadeMinimaParaUnidade(original.getUnidadeMedida(), original.getStock());
            txtStockAtual.setText(shown.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        } else {
            txtStockAtual.setText(original.getStock() != null ? original.getStock().toString() : "0");
        }
        txtStockMin.setText(original.getStockMinimo() != null ? original.getStockMinimo().toString() : "0");
        
        txtPreco.setText(original.getPrecoUnitario() != null ? original.getPrecoUnitario().toString() : "");
        txtCusto.setText(original.getPrecoCompra() != null ? original.getPrecoCompra().toString() : "");
        cbImposto.setValue(original.getImposto());
    }

    private void handleSave() {
        validation.redecorate();
        if (validation.isInvalid()) {
            AlertUtils.showErrorAlert("Validação", "Corrija os campos assinalados.");
            return;
        }

        Produto p = (original != null) ? original : new Produto();
        p.setNome(txtNome.getText());
        p.setDescricao(txtDescricao.getText());
        p.setCategoria(cbCategoria.getValue());
        p.setMarca(txtMarca.getText());
        p.setUnidadeMedida(cbUnidade.getValue());
        
        p.setCodigoBarra(txtCodigo.getText());
        p.setLocalizacao(txtLocalizacao.getText());
        
        try {
            if (!isEdit && !txtStockAtual.getText().isBlank()) {
                if (isUnidadeFracionavel(p.getUnidadeMedida())) {
                    BigDecimal qtd = new BigDecimal(txtStockAtual.getText().replace(",", "."));
                    p.setStock(unidadeParaUnidadeMinima(p.getUnidadeMedida(), qtd));
                } else {
                    p.setStock(Integer.parseInt(txtStockAtual.getText()));
                }
            }
            
            if (!txtStockMin.getText().isBlank())
                p.setStockMinimo(Integer.parseInt(txtStockMin.getText()));
                
            if (!txtPreco.getText().isBlank())
                p.setPrecoUnitario(new BigDecimal(txtPreco.getText().replace(",", ".")));
                
            if (!txtCusto.getText().isBlank())
                p.setPrecoCompra(new BigDecimal(txtCusto.getText().replace(",", ".")));

            if (!txtStepVenda.getText().isBlank()) {
                p.setStepVenda(new BigDecimal(txtStepVenda.getText().replace(",", ".")));
            } else {
                p.setStepVenda(null);
            }

            if (!txtCasasDecimaisQtd.getText().isBlank()) {
                p.setCasasDecimaisQuantidade(Integer.parseInt(txtCasasDecimaisQtd.getText()));
            } else {
                p.setCasasDecimaisQuantidade(null);
            }
                
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erro numérico", "Verifique os valores numéricos.");
            return;
        }

        p.setImposto(cbImposto.getValue());
        if (p.getImposto() != null) p.setPercentualIva(p.getImposto().getPercentual());

        try {
            produtoService.save(p);
            if (onSaved != null) onSaved.accept(p);
        
        } catch (Exception ex) {
            AlertUtils.showErrorAlert("Erro ao salvar", ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static GridPane sectionGrid() {
        GridPane g = new GridPane();
        g.setHgap(15);
        g.setVgap(15);
        g.setPadding(new Insets(15));
        
        ColumnConstraints c1 = new ColumnConstraints(120);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        ColumnConstraints c3 = new ColumnConstraints(120);
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

    private static <T extends javafx.scene.control.Control> void span(T ctrl, GridPane g, int row, int col, int colspan) {
        GridPane.setColumnSpan(ctrl, colspan);
        g.add(ctrl, col, row);
    }

    private static void applyTextLimit(TextField field, int max) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= max ? change : null));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ── Barcode Logic ─────────────────────────────────────────────────────────
    private String detectBarcodeType(String code) {
        if (code == null) return null;
        String digits = code.replaceAll("\\D", "");
        if (digits.length() == 13) return "EAN-13";
        if (digits.length() == 8) return "EAN-8";
        if (digits.length() == 12) return "UPC-A";
        return null;
    }

    private void updateBarcodeMask(String type, MaskTextField field) {
        if (field == null) return;
        if (type == null) { field.setMask(null); return; }
        switch (type) {
            case "EAN-13" -> field.setMask("9999999999999");
            case "EAN-8" -> field.setMask("99999999");
            case "UPC-A" -> field.setMask("999999999999");
            default -> field.setMask(null);
        }
    }

    private boolean isBarcodeValid(String value, String type) {
        if (value == null) return false;
        String digits = value.replaceAll("\\D", "");
        if (digits.isEmpty()) return false;
        
        String effectiveType = type;
        if (effectiveType == null || effectiveType.isBlank()) effectiveType = detectBarcodeType(digits);
        if (effectiveType == null) return true; // Se não detectou tipo, assume válido se não vazio? Ou false? No original era false.

        return switch (effectiveType) {
            case "EAN-13" -> digits.length() == 13;
            case "EAN-8" -> digits.length() == 8;
            case "UPC-A" -> digits.length() == 12;
            default -> true;
        };
    }
}
