package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.service.DatabaseBackupService;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
// import atlantafx.base.controls.MaskTextField; // REMOVED
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ao.allon.kubata.faturacao.service.EmailSettingsService;
import java.io.ByteArrayInputStream;
import java.util.prefs.Preferences;
import java.nio.file.Files;
import java.io.IOException;

import java.io.File;

public class ConfiguracoesView extends VBox {

    private final EmpresaService empresaService;
    private final DatabaseBackupService backupService;
    private final ao.allon.kubata.faturacao.pdv.service.CustomerDisplayService customerDisplayService;
    private final EmailSettingsService emailSettingsService;

    private TextField txtNome;
    private TextField txtSlogan;
    private TextField txtNif;
    private TextField txtEndereco;
    private TextField txtCidade;
    private TextField txtEmail;
    private TextField txtTelefone;
    private TextField txtWebsite;
    private TextField txtConservatoria;
    private TextField txtCapitalSocial;
    
    // Dados Bancários
    private TextField txtBanco1;
    private TextField txtIban1;
    private TextField txtBanco2;
    private TextField txtIban2;
    
    private ComboBox<String> cbRegime;
    
    private ImageView logoPreview;
    private byte[] currentLogoBytes;

    public ConfiguracoesView(EmpresaService empresaService, DatabaseBackupService backupService, ao.allon.kubata.faturacao.pdv.service.CustomerDisplayService customerDisplayService, EmailSettingsService emailSettingsService) {
        this.empresaService = empresaService;
        this.backupService = backupService;
        this.customerDisplayService = customerDisplayService;
        this.emailSettingsService = emailSettingsService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("configuracoes-view");

        Label title = new Label("Configurações do Sistema");
        title.getStyleClass().add(Styles.TITLE_2);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        tabs.getTabs().addAll(
            createEmpresaTab(),
            createEmailTab(),
            createPdvDisplayTab(),
            createBackupTab(),
            createSobreTab()
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(title, tabs);
    }

    private Tab createPdvDisplayTab() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(20));
        Preferences prefs = Preferences.userRoot().node("ao/allon/kubata/pdv");

        CheckBox chkEnable = new CheckBox("Ativar Visor do Cliente (segunda tela)");
        chkEnable.setSelected(prefs.getBoolean("customerDisplay.enabled", true));

        ComboBox<String> cbMonitor = new ComboBox<>();
        var screens = javafx.stage.Screen.getScreens();
        for (int i = 0; i < screens.size(); i++) {
            var b = screens.get(i).getVisualBounds();
            cbMonitor.getItems().add("Monitor " + i + " - " + (int)b.getWidth() + "x" + (int)b.getHeight());
        }
        int selIdx = prefs.getInt("customerDisplay.monitorIndex", screens.size() > 1 ? 1 : 0);
        if (selIdx < 0 || selIdx >= screens.size()) selIdx = 0;
        cbMonitor.getSelectionModel().select(selIdx);

        Button btnTest = new Button("Testar Visor");
        btnTest.setGraphic(IconUtils.icon(Feather.PLAY, IconUtils.SIZE_SMALL));
        btnTest.getStyleClass().add(Styles.ACCENT);
        btnTest.setOnAction(e -> customerDisplayService.start());

        Button btnStop = new Button("Fechar Visor");
        btnStop.setGraphic(IconUtils.icon(Feather.X_CIRCLE, IconUtils.SIZE_SMALL));
        btnStop.getStyleClass().add(Styles.DANGER);
        btnStop.setOnAction(e -> customerDisplayService.stop());

        Button btnSalvar = new Button("Guardar Configurações");
        btnSalvar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnSalvar.setGraphic(new FontIcon(Feather.SAVE));
        btnSalvar.setOnAction(e -> {
            prefs.putBoolean("customerDisplay.enabled", chkEnable.isSelected());
            prefs.putInt("customerDisplay.monitorIndex", cbMonitor.getSelectionModel().getSelectedIndex());
            AlertUtils.showInfoAlert("Configurações", "Preferências do visor do cliente atualizadas.");
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.addRow(0, new Label("Estado"), chkEnable);
        grid.addRow(1, new Label("Monitor"), cbMonitor);

        HBox actions = new HBox(8, btnTest, btnStop, new Separator(), btnSalvar);
        actions.setAlignment(Pos.CENTER_LEFT);

        layout.getChildren().addAll(
                TileFactory.createSectionHeader("Visor do Cliente", "Configurações de dupla tela", Feather.MONITOR),
                grid,
                actions
        );
        return new Tab("PDV & Display", layout);
    }

    private Tab createEmpresaTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));

        // Seção de Branding (Logo e Identidade)
        HBox brandingBox = new HBox(20);
        brandingBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox logoSection = createLogoSection();
        
        VBox identitySection = new VBox(10);
        txtNome = new TextField();
        txtSlogan = new TextField();
        identitySection.getChildren().addAll(
            TileFactory.createFormField("Nome da Empresa", "Razão social ou nome comercial", txtNome),
            TileFactory.createFormField("Slogan", "Frase de efeito ou lema", txtSlogan)
        );
        HBox.setHgrow(identitySection, Priority.ALWAYS);
        
        brandingBox.getChildren().addAll(logoSection, identitySection);
        container.getChildren().add(brandingBox);
        
        container.getChildren().add(new Separator(Orientation.HORIZONTAL));

        // Colunas de Dados
        HBox columns = new HBox(20);
        
        // Coluna 1: Fiscal e Legal
        VBox col1 = new VBox(15);
        txtNif = new TextField();
        txtConservatoria = new TextField();
        txtCapitalSocial = new TextField();
        cbRegime = new ComboBox<>();
        cbRegime.getItems().addAll("Regime Geral", "Regime Simplificado", "Regime de Exclusão");
        cbRegime.setMaxWidth(Double.MAX_VALUE);
        
        col1.getChildren().addAll(
            TileFactory.createSectionHeader("Fiscal e Legal", "Dados jurídicos", Feather.FILE_TEXT),
            TileFactory.createFormField("NIF", "Número de Identificação Fiscal", txtNif),
            TileFactory.createFormField("Regime Fiscal", "Enquadramento tributário", cbRegime),
            TileFactory.createFormField("Conservatória", "Dados de registro comercial", txtConservatoria),
            TileFactory.createFormField("Capital Social", "Valor do capital social", txtCapitalSocial)
        );
        
        // Coluna 2: Localização e Contato
        VBox col2 = new VBox(15);
        txtEndereco = new TextField();
        txtCidade = new TextField();
        txtEmail = new TextField();
        txtTelefone = new TextField();
        txtWebsite = new TextField();
        
        col2.getChildren().addAll(
            TileFactory.createSectionHeader("Localização e Contato", "Endereço e comunicação", Feather.MAP_PIN),
            TileFactory.createFormField("Endereço", "Rua, Bairro, Nº", txtEndereco),
            TileFactory.createFormField("Cidade", "Cidade / Província", txtCidade),
            TileFactory.createFormField("Email", "Correio eletrónico principal", txtEmail),
            TileFactory.createFormField("Telefone", "Contacto telefónico", txtTelefone),
            TileFactory.createFormField("Website", "Página web oficial", txtWebsite)
        );
        
        // Coluna 3: Dados Bancários
        VBox col3 = new VBox(15);
        txtBanco1 = new TextField();
        txtIban1 = new TextField();
        txtBanco2 = new TextField();
        txtIban2 = new TextField();
        
        col3.getChildren().addAll(
            TileFactory.createSectionHeader("Dados Bancários", "Contas para documentos", Feather.CREDIT_CARD),
            TileFactory.createFormField("Banco Principal", "Nome do banco", txtBanco1),
            TileFactory.createFormField("IBAN Principal", "Número de IBAN", txtIban1),
            TileFactory.createFormField("Banco Secundário", "Nome do banco (opcional)", txtBanco2),
            TileFactory.createFormField("IBAN Secundário", "Número de IBAN (opcional)", txtIban2)
        );

        columns.getChildren().addAll(col1, col2, col3);
        HBox.setHgrow(col1, Priority.ALWAYS);
        HBox.setHgrow(col2, Priority.ALWAYS);
        HBox.setHgrow(col3, Priority.ALWAYS);
        
        container.getChildren().add(columns);

        // ScrollPane
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Actions Area
        Button btnSalvar = new Button("Salvar Alterações");
        btnSalvar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnSalvar.setGraphic(new FontIcon(Feather.SAVE));
        btnSalvar.setOnAction(e -> salvarEmpresa());

        HBox actions = new HBox(btnSalvar);
        actions.setPadding(new Insets(20));
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("actions-bar");

        VBox layout = new VBox(scroll, actions);
        
        loadEmpresaData();

        return new Tab("Dados da Empresa", layout);
    }

    private Tab createEmailTab() {
        EmailConfigView emailConfigView = new EmailConfigView(emailSettingsService);
        return new Tab("Email", emailConfigView);
    }

    private VBox createLogoSection() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        
        logoPreview = new ImageView();
        logoPreview.setFitWidth(120);
        logoPreview.setFitHeight(120);
        logoPreview.setPreserveRatio(true);
        // Estilo de borda para o logo
        logoPreview.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        
        Button btnUpload = new Button("Carregar Logo");
        btnUpload.setGraphic(new FontIcon(Feather.UPLOAD));
        btnUpload.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnUpload.setOnAction(e -> selectLogo());
        
        Button btnRemove = new Button("Remover");
        btnRemove.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnRemove.setGraphic(new FontIcon(Feather.TRASH));
        btnRemove.setOnAction(e -> clearLogo());
        
        HBox buttons = new HBox(5, btnUpload, btnRemove);
        buttons.setAlignment(Pos.CENTER);
        
        box.getChildren().addAll(logoPreview, buttons);
        return box;
    }

    private void selectLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Logotipo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                // Validação simples de tamanho (ex: max 2MB)
                if (bytes.length > 2 * 1024 * 1024) {
                    AlertUtils.showErrorAlert("Erro", "A imagem é muito grande. O tamanho máximo é 2MB.");
                    return;
                }
                this.currentLogoBytes = bytes;
                updateLogoPreview();
            } catch (IOException e) {
                AlertUtils.showErrorAlert("Erro", "Falha ao ler o arquivo: " + e.getMessage());
            }
        }
    }

    private void clearLogo() {
        this.currentLogoBytes = null;
        logoPreview.setImage(null);
    }

    private void updateLogoPreview() {
        if (currentLogoBytes != null && currentLogoBytes.length > 0) {
            logoPreview.setImage(new Image(new ByteArrayInputStream(currentLogoBytes)));
        } else {
            logoPreview.setImage(null);
        }
    }

    private void loadEmpresaData() {
        try {
            Empresa emp = empresaService.getDadosEmpresa();
            if (emp != null) {
                txtNome.setText(emp.getNome() != null ? emp.getNome() : "");
                txtSlogan.setText(emp.getSlogan() != null ? emp.getSlogan() : "");
                txtNif.setText(emp.getNif() != null ? emp.getNif() : "");
                txtEndereco.setText(emp.getEndereco() != null ? emp.getEndereco() : "");
                txtCidade.setText(emp.getCidade() != null ? emp.getCidade() : "");
                txtEmail.setText(emp.getEmail() != null ? emp.getEmail() : "");
                txtTelefone.setText(emp.getTelefone() != null ? emp.getTelefone() : "");
                txtWebsite.setText(emp.getWebsite() != null ? emp.getWebsite() : "");
                txtConservatoria.setText(emp.getConservatoria() != null ? emp.getConservatoria() : "");
                txtCapitalSocial.setText(emp.getCapitalSocial() != null ? emp.getCapitalSocial() : "");
                
                txtBanco1.setText(emp.getBanco1() != null ? emp.getBanco1() : "");
                txtIban1.setText(emp.getIban1() != null ? emp.getIban1() : "");
                txtBanco2.setText(emp.getBanco2() != null ? emp.getBanco2() : "");
                txtIban2.setText(emp.getIban2() != null ? emp.getIban2() : "");
                
                cbRegime.setValue(emp.getRegimeIva());
                
                this.currentLogoBytes = emp.getLogotipo();
                updateLogoPreview();
            }
        } catch (Exception e) {
            AlertUtils.showErrorAlert("Erro", "Falha ao carregar dados da empresa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void salvarEmpresa() {
        // Validações
        String nif = txtNif.getText();
        if (nif != null && !nif.isEmpty()) {
            String msg = ao.allon.kubata.faturacao.util.AngolaValidationUtils.validateNifMessage(nif);
            if (msg != null) {
                AlertUtils.showErrorAlert("Validação", msg);
                return;
            }
        }
        
        String telefone = txtTelefone.getText();
        if (telefone != null && !telefone.isEmpty()) {
            // Apenas um aviso se o telefone parecer inválido, mas permite salvar
            if (!ao.allon.kubata.faturacao.util.AngolaValidationUtils.isValidTelefone(telefone)) {
                 // Opcional: mostrar warning ou apenas logar
            }
        }

        Empresa emp = empresaService.getDadosEmpresa();
        if (emp == null) emp = new Empresa();
        
        emp.setNome(txtNome.getText());
        emp.setSlogan(txtSlogan.getText());
        emp.setNif(ao.allon.kubata.faturacao.util.AngolaValidationUtils.normalizeNif(txtNif.getText()));
        emp.setEndereco(txtEndereco.getText());
        emp.setCidade(txtCidade.getText());
        emp.setEmail(txtEmail.getText());
        emp.setTelefone(txtTelefone.getText());
        emp.setWebsite(txtWebsite.getText());
        emp.setConservatoria(txtConservatoria.getText());
        emp.setCapitalSocial(txtCapitalSocial.getText());
        
        emp.setBanco1(txtBanco1.getText());
        emp.setIban1(txtIban1.getText());
        emp.setBanco2(txtBanco2.getText());
        emp.setIban2(txtIban2.getText());
        
        emp.setRegimeIva(cbRegime.getValue());
        emp.setLogotipo(currentLogoBytes);

        try {
            empresaService.salvarDadosEmpresa(emp);
            AlertUtils.showInfoAlert("Sucesso", "Dados da empresa atualizados com sucesso.");
        } catch (Exception ex) {
            AlertUtils.showErrorAlert("Erro", "Falha ao salvar dados: " + ex.getMessage());
        }
    }

    private Tab createBackupTab() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Card backupCard = new Card();
        backupCard.setHeader(new Label("Cópia de Segurança"));
        backupCard.setBody(new Label("Realize backups regulares para proteger seus dados. O sistema realiza backups automáticos diariamente."));
        
        Button btnBackup = new Button("Realizar Backup Agora");
        btnBackup.setGraphic(IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnBackup.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnBackup.setOnAction(e -> performBackup());

        layout.getChildren().addAll(backupCard, btnBackup);

        return new Tab("Backup & Restore", layout);
    }

    private void performBackup() {
        try {
            backupService.performBackup();
            AlertUtils.showInfoAlert("Backup", "Cópia de segurança realizada com sucesso na pasta de backups.");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro de Backup", "Falha ao criar o backup do banco de dados.", e);
        }
    }

    private Tab createSobreTab() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label appName = new Label("Kubata ERP");
        appName.getStyleClass().add(Styles.TITLE_1);
        
        Label version = new Label("Versão 1.0.0 (Angola Edition)");
        version.getStyleClass().add(Styles.TEXT_MUTED);

        Label copyright = new Label("© 2024 Allon Software. Todos os direitos reservados.");
        
        layout.getChildren().addAll(appName, version, copyright);
        return new Tab("Sobre", layout);
    }
}
