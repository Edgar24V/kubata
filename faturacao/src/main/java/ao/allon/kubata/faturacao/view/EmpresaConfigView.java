package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;

import java.io.*;
import java.util.Base64;

/**
 * View para configuração dos dados da empresa.
 */
public class EmpresaConfigView extends VBox {

    private final EmpresaService empresaService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private TextField txtNome;
    private TextField txtNif;
    private TextField txtEndereco;
    private TextField txtCidade;
    private TextField txtTelefone;
    private TextField txtEmail;
    private TextField txtWebsite;
    private TextField txtRegimeIva;
    private TextField txtConservatoria;
    private TextField txtCapitalSocial;
    private TextField txtSoftwareValidation;
    private TextField txtBanco1;
    private TextField txtIban1;
    private TextField txtBanco2;
    private TextField txtIban2;
    private TextArea txtSlogan;
    private ImageView imgLogotipo;
    private byte[] logotipoData;

    public EmpresaConfigView(EmpresaService empresaService,
                             SessionManager sessionManager,
                             ModalService modalService) {
        this.empresaService = empresaService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupForm();
        loadData();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Configuração da Empresa");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Dados da empresa para emissão de documentos fiscais");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSalvar = new Button("Salvar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSalvar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnSalvar.setOnAction(e -> salvar());

        boolean canEdit = sessionManager.hasAccess("CONFIGURACOES", "Editar");
        btnSalvar.setDisable(!canEdit);

        header.getChildren().addAll(titleBox, spacer, btnSalvar);
        getChildren().add(header);
    }

    private void setupForm() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab Dados Gerais
        Tab tabGerais = new Tab("Dados Gerais", createTabGerais());

        // Tab Dados Bancários
        Tab tabBancarios = new Tab("Dados Bancários", createTabBancarios());

        // Tab Logotipo
        Tab tabLogotipo = new Tab("Logotipo", createTabLogotipo());

        tabPane.getTabs().addAll(tabGerais, tabBancarios, tabLogotipo);

        VBox.setVgrow(tabPane, Priority.ALWAYS);
        getChildren().add(tabPane);
    }

    private VBox createTabGerais() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        int row = 0;

        txtNome = new TextField();
        txtNome.setPromptText("Nome da Empresa");
        grid.add(new Label("Nome:*"), 0, row);
        grid.add(txtNome, 1, row);
        GridPane.setHgrow(txtNome, Priority.ALWAYS);

        row++;
        txtNif = new TextField();
        txtNif.setPromptText("NIF / NIPC");
        grid.add(new Label("NIF:*"), 0, row);
        grid.add(txtNif, 1, row);

        row++;
        txtEndereco = new TextField();
        txtEndereco.setPromptText("Endereço completo");
        grid.add(new Label("Endereço:*"), 0, row);
        grid.add(txtEndereco, 1, row);

        row++;
        txtCidade = new TextField();
        txtCidade.setPromptText("Cidade");
        grid.add(new Label("Cidade:*"), 0, row);
        grid.add(txtCidade, 1, row);

        row++;
        txtTelefone = new TextField();
        txtTelefone.setPromptText("Telefone");
        grid.add(new Label("Telefone:"), 0, row);
        grid.add(txtTelefone, 1, row);

        row++;
        txtEmail = new TextField();
        txtEmail.setPromptText("Email");
        grid.add(new Label("Email:"), 0, row);
        grid.add(txtEmail, 1, row);

        row++;
        txtWebsite = new TextField();
        txtWebsite.setPromptText("Website");
        grid.add(new Label("Website:"), 0, row);
        grid.add(txtWebsite, 1, row);

        row++;
        txtRegimeIva = new TextField();
        txtRegimeIva.setPromptText("Geral, Simplificado, etc.");
        grid.add(new Label("Regime IVA:*"), 0, row);
        grid.add(txtRegimeIva, 1, row);

        row++;
        txtConservatoria = new TextField();
        txtConservatoria.setPromptText("Conservatória do Registo Comercial");
        grid.add(new Label("Conservatória:"), 0, row);
        grid.add(txtConservatoria, 1, row);

        row++;
        txtCapitalSocial = new TextField();
        txtCapitalSocial.setPromptText("Capital Social");
        grid.add(new Label("Capital Social:"), 0, row);
        grid.add(txtCapitalSocial, 1, row);

        row++;
        txtSoftwareValidation = new TextField();
        txtSoftwareValidation.setPromptText("Número de validação do software");
        grid.add(new Label("Nº Validação Software:"), 0, row);
        grid.add(txtSoftwareValidation, 1, row);

        row++;
        txtSlogan = new TextArea();
        txtSlogan.setPromptText("Slogan da empresa");
        txtSlogan.setPrefRowCount(2);
        txtSlogan.setWrapText(true);
        grid.add(new Label("Slogan:"), 0, row);
        grid.add(txtSlogan, 1, row);

        VBox vbox = new VBox(grid);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private VBox createTabBancarios() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        // Banco 1
        Label lblBanco1 = new Label("Banco 1:");
        lblBanco1.setStyle("-fx-font-weight: bold;");
        grid.add(lblBanco1, 0, 0, 2, 1);

        txtBanco1 = new TextField();
        txtBanco1.setPromptText("Nome do Banco");
        grid.add(new Label("Banco:"), 0, 1);
        grid.add(txtBanco1, 1, 1);
        GridPane.setHgrow(txtBanco1, Priority.ALWAYS);

        txtIban1 = new TextField();
        txtIban1.setPromptText("IBAN");
        grid.add(new Label("IBAN:"), 0, 2);
        grid.add(txtIban1, 1, 2);

        // Separator
        grid.add(new Separator(), 0, 3, 2, 1);

        // Banco 2
        Label lblBanco2 = new Label("Banco 2:");
        lblBanco2.setStyle("-fx-font-weight: bold;");
        grid.add(lblBanco2, 0, 4, 2, 1);

        txtBanco2 = new TextField();
        txtBanco2.setPromptText("Nome do Banco");
        grid.add(new Label("Banco:"), 0, 5);
        grid.add(txtBanco2, 1, 5);

        txtIban2 = new TextField();
        txtIban2.setPromptText("IBAN");
        grid.add(new Label("IBAN:"), 0, 6);
        grid.add(txtIban2, 1, 6);

        VBox vbox = new VBox(grid);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private VBox createTabLogotipo() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));
        vbox.setAlignment(Pos.TOP_CENTER);

        imgLogotipo = new ImageView();
        imgLogotipo.setFitWidth(300);
        imgLogotipo.setFitHeight(200);
        imgLogotipo.setPreserveRatio(true);
        imgLogotipo.setStyle("-fx-border-color: #ccc; -fx-border-style: dashed;");

        Button btnSelecionar = new Button("Selecionar Imagem", IconUtils.icon(Feather.IMAGE, IconUtils.SIZE_SMALL));
        btnSelecionar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnSelecionar.setOnAction(e -> selecionarImagem());

        Button btnRemover = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnRemover.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnRemover.setOnAction(e -> removerImagem());

        HBox buttons = new HBox(10, btnSelecionar, btnRemover);
        buttons.setAlignment(Pos.CENTER);

        Label lblInfo = new Label("Formatos suportados: PNG, JPG, JPEG\nTamanho máximo recomendado: 500x300 pixels");
        lblInfo.getStyleClass().add(Styles.TEXT_MUTED);
        lblInfo.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(imgLogotipo, buttons, lblInfo);
        return vbox;
    }

    private void loadData() {
        Empresa empresa = empresaService.getDadosEmpresa();
        if (empresa != null) {
            txtNome.setText(empresa.getNome());
            txtNif.setText(empresa.getNif());
            txtEndereco.setText(empresa.getEndereco());
            txtCidade.setText(empresa.getCidade());
            txtTelefone.setText(empresa.getTelefone());
            txtEmail.setText(empresa.getEmail());
            txtWebsite.setText(empresa.getWebsite());
            txtRegimeIva.setText(empresa.getRegimeIva());
            txtConservatoria.setText(empresa.getConservatoria());
            txtCapitalSocial.setText(empresa.getCapitalSocial());
            txtSoftwareValidation.setText(empresa.getSoftwareValidationNumber());
            txtSlogan.setText(empresa.getSlogan());
            txtBanco1.setText(empresa.getBanco1());
            txtIban1.setText(empresa.getIban1());
            txtBanco2.setText(empresa.getBanco2());
            txtIban2.setText(empresa.getIban2());

            logotipoData = empresa.getLogotipo();
            if (logotipoData != null) {
                try {
                    Image img = new Image(new ByteArrayInputStream(logotipoData));
                    imgLogotipo.setImage(img);
                } catch (Exception e) {
                    System.err.println("Erro ao carregar logotipo: " + e.getMessage());
                }
            }
        }
    }

    private void salvar() {
        // Validação
        if (txtNome.getText().trim().isEmpty() ||
            txtNif.getText().trim().isEmpty() ||
            txtEndereco.getText().trim().isEmpty() ||
            txtCidade.getText().trim().isEmpty() ||
            txtRegimeIva.getText().trim().isEmpty()) {
            AlertUtils.showWarningAlert("Campos Obrigatórios", "Preencha todos os campos obrigatórios (*)");
            return;
        }

        Empresa empresa = empresaService.getDadosEmpresa();
        if (empresa == null) {
            empresa = new Empresa();
        }

        empresa.setNome(txtNome.getText().trim());
        empresa.setNif(txtNif.getText().trim());
        empresa.setEndereco(txtEndereco.getText().trim());
        empresa.setCidade(txtCidade.getText().trim());
        empresa.setTelefone(txtTelefone.getText().trim());
        empresa.setEmail(txtEmail.getText().trim());
        empresa.setWebsite(txtWebsite.getText().trim());
        empresa.setRegimeIva(txtRegimeIva.getText().trim());
        empresa.setConservatoria(txtConservatoria.getText().trim());
        empresa.setCapitalSocial(txtCapitalSocial.getText().trim());
        empresa.setSoftwareValidationNumber(txtSoftwareValidation.getText().trim());
        empresa.setSlogan(txtSlogan.getText().trim());
        empresa.setBanco1(txtBanco1.getText().trim());
        empresa.setIban1(txtIban1.getText().trim());
        empresa.setBanco2(txtBanco2.getText().trim());
        empresa.setIban2(txtIban2.getText().trim());
        empresa.setLogotipo(logotipoData);

        try {
            empresaService.salvarDadosEmpresa(empresa);
            AlertUtils.showInfoAlert("Sucesso", "Dados da empresa salvos com sucesso!");
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível salvar os dados da empresa.", ex);
        }
    }

    private void selecionarImagem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Logotipo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                if (data.length > 2 * 1024 * 1024) { // 2MB limit
                    AlertUtils.showWarningAlert("Arquivo muito grande", "O logotipo deve ter no máximo 2MB.");
                    return;
                }
                logotipoData = data;
                Image img = new Image(new ByteArrayInputStream(data));
                imgLogotipo.setImage(img);
            } catch (IOException e) {
                AlertUtils.showExceptionAlert("Erro", "Não foi possível ler a imagem.", e);
            }
        }
    }

    private void removerImagem() {
        logotipoData = null;
        imgLogotipo.setImage(null);
    }
}
