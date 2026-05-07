package ao.allon.kubata.admin.ui.wizard;

import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.repository.EmpresaRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmpresaWizardView extends VBox {

    private final ModalManager modalManager;
    private final EmpresaRepository empresaRepository;
    private Empresa empresa;
    private int currentStep = 0;
    private final List<WizardStep> steps = new ArrayList<>();

    private Label lblStepTitle;
    private Label lblStepDescription;
    private StackPane stepContentContainer;
    private HBox progressIndicator;
    private ProgressBar progressBar;
    private Button btnPrev;
    private Button btnNext;
    private Button btnHelp;

    public EmpresaWizardView(ModalManager modalManager, EmpresaRepository empresaRepository) {
        this.modalManager = modalManager;
        this.empresaRepository = empresaRepository;
        buildUI();
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);
        setPrefSize(950, 700);
        setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // --- Header ---
        VBox header = new VBox(15);
        header.setPadding(new Insets(25, 35, 20, 35));
        header.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12 12 0 0;");

        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleText = new VBox(5);
        lblStepTitle = new Label("Configuração Inicial");
        lblStepTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        lblStepDescription = new Label("Siga os passos para configurar sua nova empresa.");
        lblStepDescription.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        titleText.getChildren().addAll(lblStepTitle, lblStepDescription);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        btnHelp = new Button("", IconUtils.icon(Feather.HELP_CIRCLE, 18));
        btnHelp.getStyleClass().add("outlined");
        btnHelp.setStyle("-fx-shape: 'M150 0 L75 200 L225 200 Z'; -fx-padding: 8;");
        btnHelp.setOnAction(e -> showContextHelp());
        
        titleBox.getChildren().addAll(titleText, spacer, btnHelp);

        // Barra de Progresso Visual
        VBox progressBox = new VBox(8);
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setStyle("-fx-accent: #27ae60;");
        
        progressIndicator = new HBox(10);
        progressIndicator.setAlignment(Pos.CENTER_LEFT);
        
        progressBox.getChildren().addAll(progressBar, progressIndicator);
        header.getChildren().addAll(titleBox, progressBox);
        
        // --- Área de Conteúdo Central ---
        stepContentContainer = new StackPane();
        VBox.setVgrow(stepContentContainer, Priority.ALWAYS);
        stepContentContainer.setPadding(new Insets(30, 45, 30, 45));
        stepContentContainer.setStyle("-fx-background-color: white;");

        // --- Footer com Navegação ---
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(20, 35, 25, 35));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 0 0 12 12; -fx-border-color: #ecf0f1; -fx-border-width: 1 0 0 0;");
        
        btnPrev = new Button("Anterior", IconUtils.icon(Feather.ARROW_LEFT, 16));
        btnPrev.getStyleClass().add("outlined");
        btnPrev.setPrefWidth(120);
        btnPrev.setPrefHeight(40);
        btnPrev.setOnAction(e -> prevStep());

        btnNext = new Button("Próximo", IconUtils.icon(Feather.ARROW_RIGHT, 16));
        btnNext.getStyleClass().add("accent");
        btnNext.setContentDisplay(ContentDisplay.RIGHT);
        btnNext.setPrefWidth(120);
        btnNext.setPrefHeight(40);
        btnNext.setOnAction(e -> nextStep());

        footer.getChildren().addAll(btnPrev, btnNext);

        getChildren().addAll(header, stepContentContainer, footer);
    }

    public void start(Empresa empresa) {
        this.empresa = empresa;
        this.currentStep = 0;
        initializeSteps();
        updateUI();
        
        modalManager.setPersistent(true);
        modalManager.showModal(this, new ModalManager.ModalConfig()
                .title("Assistente de Instalação Kubata ERP")
                .autoSize()
                .resizable(false));
    }

    private void initializeSteps() {
        steps.clear();
        steps.add(new WelcomeStep());              // 1
        steps.add(new BasicDataStep());            // 2
        steps.add(new SectorsStep());               // 3
        steps.add(new UsersPermissionsStep());     // 4
        steps.add(new FiscalStep());                // 5
        steps.add(new BrandingStep());              // 6
        steps.add(new ImportDataStep());            // 7
        steps.add(new BackupSecurityStep());        // 8
        steps.add(new SummaryStep());               // 9
        
        progressIndicator.getChildren().clear();
        for (int i = 0; i < steps.size(); i++) {
            Circle dot = new Circle(5);
            dot.setFill(Color.LIGHTGRAY);
            progressIndicator.getChildren().add(dot);
        }
    }

    private void updateUI() {
        WizardStep step = steps.get(currentStep);
        lblStepTitle.setText(step.getTitle());
        lblStepDescription.setText(step.getDescription());
        
        stepContentContainer.getChildren().setAll(step.getContent());
        
        btnPrev.setDisable(currentStep == 0);
        btnNext.setText(currentStep == steps.size() - 1 ? "Finalizar" : "Próximo");
        
        // Atualiza indicadores de progresso
        double progress = (double) (currentStep) / (steps.size() - 1);
        progressBar.setProgress(progress);
        
        for (int i = 0; i < progressIndicator.getChildren().size(); i++) {
            Circle dot = (Circle) progressIndicator.getChildren().get(i);
            if (i == currentStep) {
                dot.setFill(Color.web("#27ae60")); // Verde ativo
                dot.setRadius(7);
            } else if (i < currentStep) {
                dot.setFill(Color.web("#2ecc71")); // Verde concluído
                dot.setRadius(5);
            } else {
                dot.setFill(Color.web("#bdc3c7")); // Cinza pendente
                dot.setRadius(5);
            }
        }
    }

    private void nextStep() {
        WizardStep step = steps.get(currentStep);
        if (step.validate()) {
            step.savePartial(); // Salva os dados desta etapa antes de prosseguir
            logActivity("Concluiu etapa: " + step.getTitle());
            
            if (currentStep < steps.size() - 1) {
                currentStep++;
                updateUI();
            } else {
                finish();
            }
        }
    }

    private void prevStep() {
        if (currentStep > 0) {
            currentStep--;
            updateUI();
        }
    }

    private void finish() {
        try {
            empresaRepository.save(empresa);
            logActivity("Assistente finalizado com sucesso.");
            modalManager.setPersistent(false);
            modalManager.hideModal();
            modalManager.alert("Sucesso", "A empresa " + empresa.getNome() + " foi configurada com sucesso e está pronta para uso.", "success", null);
        } catch (Exception e) {
            modalManager.alert("Erro", "Falha ao salvar configurações finais: " + e.getMessage(), "error", null);
        }
    }

    private void showContextHelp() {
        WizardStep step = steps.get(currentStep);
        modalManager.alert("Ajuda: " + step.getTitle(), step.getHelpText(), "info", null);
    }

    private void logActivity(String action) {
        // TODO: Persistir log de auditoria via AcessoService
        System.out.println("[WIZARD LOG] " + empresa.getNome() + ": " + action);
    }

    // --- Interfaces e Classes de Etapas ---

    private interface WizardStep {
        String getTitle();
        String getDescription();
        Node getContent();
        boolean validate();
        void savePartial();
        String getHelpText();
    }

    // 1. Tela de Boas-vindas
    private class WelcomeStep implements WizardStep {
        public String getTitle() { return "1. Bem-vindo"; }
        public String getDescription() { return "Introdução ao assistente de configuração Kubata ERP."; }
        public Node getContent() {
            VBox box = new VBox(25);
            box.setAlignment(Pos.CENTER);
            
            Label lblWelcome = new Label("Bem-vindo ao Kubata ERP!");
            lblWelcome.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            
            Label lblText = new Label("Este assistente irá guiá-lo na configuração inicial da sua empresa. \n" +
                                     "O processo leva cerca de 10 a 15 minutos e garante que o sistema \n" +
                                     "esteja pronto para emitir faturas e gerir o seu negócio conforme a lei angolana.");
            lblText.setStyle("-fx-font-size: 16px; -fx-text-alignment: center; -fx-line-spacing: 5;");
            lblText.setWrapText(true);
            
            VBox list = new VBox(10);
            list.setAlignment(Pos.CENTER_LEFT);
            list.setMaxWidth(500);
            list.getChildren().addAll(
                new Label("✓ Dados Fiscais e Morada"),
                new Label("✓ Estrutura Organizacional"),
                new Label("✓ Utilizadores e Permissões"),
                new Label("✓ Configurações da AGT"),
                new Label("✓ Importação de Inventário")
            );
            list.setStyle("-fx-background-color: #f1f8e9; -fx-padding: 20px; -fx-background-radius: 8px;");

            box.getChildren().addAll(lblWelcome, lblText, list);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "Esta etapa apresenta os objetivos do assistente. Clique em Próximo para iniciar."; }
    }

    // 2. Dados Básicos
    private class BasicDataStep implements WizardStep {
        private TextField txtNome, txtNif, txtEmail, txtTelefone, txtMorada;
        private final ValidationSupport validationSupport = new ValidationSupport();

        public String getTitle() { return "2. Dados Básicos"; }
        public String getDescription() { return "Informações essenciais de identificação e contacto."; }
        public Node getContent() {
            GridPane grid = new GridPane();
            grid.setHgap(20); grid.setVgap(15);
            grid.setAlignment(Pos.CENTER);

            txtNome = new TextField(empresa.getNome());
            txtNif = new TextField(empresa.getNif());
            txtEmail = new TextField(empresa.getEmail());
            txtTelefone = new TextField(empresa.getTelefone());
            txtMorada = new TextField(empresa.getMorada());

            validationSupport.registerValidator(txtNome, Validator.createEmptyValidator("O nome é obrigatório"));
            validationSupport.registerValidator(txtNif, Validator.createEmptyValidator("O NIF é obrigatório"));
            validationSupport.registerValidator(txtEmail, Validator.createRegexValidator("Email inválido", "^[A-Za-z0-9+_.-]+@(.+)$", org.controlsfx.validation.Severity.ERROR));

            grid.add(new Label("Nome/Razão Social: *"), 0, 0); grid.add(txtNome, 1, 0, 3, 1);
            grid.add(new Label("NIF: *"), 0, 1); grid.add(txtNif, 1, 1);
            grid.add(new Label("E-mail:"), 2, 1); grid.add(txtEmail, 3, 1);
            grid.add(new Label("Telefone:"), 0, 2); grid.add(txtTelefone, 1, 2);
            grid.add(new Label("Endereço:"), 2, 2); grid.add(txtMorada, 3, 2);

            grid.getChildren().forEach(n -> {
                if (n instanceof TextField) ((TextField)n).setPrefWidth(250);
            });
            
            return grid;
        }
        public boolean validate() {
            if (validationSupport.isInvalid()) {
                modalManager.alert("Erro de Validação", "Por favor, corrija os erros nos campos destacados.", "error", null);
                return false;
            }
            return true;
        }
        public void savePartial() {
            empresa.setNome(txtNome.getText().trim());
            empresa.setNif(txtNif.getText().trim());
            empresa.setEmail(txtEmail.getText().trim());
            empresa.setTelefone(txtTelefone.getText().trim());
            empresa.setMorada(txtMorada.getText().trim());
        }
        public String getHelpText() { return "Preencha os dados conforme constam no certificado de NIF da sua empresa."; }
    }

    // 3. Setores e Departamentos
    private class SectorsStep implements WizardStep {
        public String getTitle() { return "3. Estrutura Organizacional"; }
        public String getDescription() { return "Defina os principais setores e departamentos da empresa."; }
        public Node getContent() {
            VBox box = new VBox(15);
            box.setAlignment(Pos.CENTER);
            
            ListView<String> lvSetores = new ListView<>();
            lvSetores.getItems().addAll("Administração", "Financeiro", "Vendas", "Armazém", "RH");
            lvSetores.setPrefHeight(200);
            lvSetores.setMaxWidth(400);
            
            HBox addBox = new HBox(10);
            addBox.setAlignment(Pos.CENTER);
            TextField txtNovo = new TextField();
            Button btnAdd = new Button("Adicionar", IconUtils.icon(Feather.PLUS, 14));
            btnAdd.setOnAction(e -> {
                if (!txtNovo.getText().isBlank()) {
                    lvSetores.getItems().add(txtNovo.getText().trim());
                    txtNovo.clear();
                }
            });
            addBox.getChildren().addAll(txtNovo, btnAdd);
            
            box.getChildren().addAll(new Label("Setores/Departamentos:"), lvSetores, addBox);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() { /* TODO: Salvar setores em tabela relacionada */ }
        public String getHelpText() { return "Defina a estrutura para facilitar a alocação de custos e funcionários futuramente."; }
    }

    // 4. Usuários Administrativos
    private class UsersPermissionsStep implements WizardStep {
        public String getTitle() { return "4. Utilizadores & Acessos"; }
        public String getDescription() { return "Registe os utilizadores que terão acesso ao sistema."; }
        public Node getContent() {
            VBox box = new VBox(15);
            box.setAlignment(Pos.CENTER);
            
            TableView<String[]> tvUsers = new TableView<>();
            TableColumn<String[], String> colNome = new TableColumn<>("Nome");
            TableColumn<String[], String> colPerfil = new TableColumn<>("Perfil");
            tvUsers.getColumns().addAll(colNome, colPerfil);
            tvUsers.setPrefHeight(200);
            tvUsers.setMaxWidth(500);
            
            Label lblTip = new Label("Dica: Pode definir permissões granulares no módulo Administrator após o login.");
            lblTip.setStyle("-fx-font-style: italic; -fx-text-fill: #95a5a6;");
            
            box.getChildren().addAll(tvUsers, lblTip);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "Adicione pelo menos um utilizador administrador para além do master."; }
    }

    // 5. Tributação e Regime Fiscal
    private class FiscalStep implements WizardStep {
        private ComboBox<String> cbRegime;
        private final ValidationSupport validationSupport = new ValidationSupport();

        public String getTitle() { return "5. Configuração Fiscal (AGT)"; }
        public String getDescription() { return "Configure o enquadramento tributário para emissão de faturas."; }
        public Node getContent() {
            VBox box = new VBox(20);
            box.setAlignment(Pos.CENTER);
            
            cbRegime = new ComboBox<>();
            cbRegime.getItems().addAll("Regime Geral", "Regime Simplificado", "Regime de Exclusão");
            cbRegime.setPromptText("Selecione o Regime de IVA");
            cbRegime.setPrefWidth(300);
            
            validationSupport.registerValidator(cbRegime, Validator.createEmptyValidator("Selecione o regime fiscal"));

            CheckBox chkSaft = new CheckBox("Gerar ficheiro SAFT-AO automaticamente");
            chkSaft.setSelected(true);
            
            box.getChildren().addAll(new Label("Enquadramento Tributário:"), cbRegime, chkSaft);
            return box;
        }
        public boolean validate() {
            if (validationSupport.isInvalid()) {
                modalManager.alert("Erro de Validação", "Selecione um regime fiscal.", "error", null);
                return false;
            }
            return true;
        }
        public void savePartial() {
            empresa.setRegimeFiscal(cbRegime.getValue());
        }
        public String getHelpText() { return "Consulte o seu contabilista para confirmar o regime de IVA correto."; }
    }

    // 6. Logotipo e Identidade
    private class BrandingStep implements WizardStep {
        public String getTitle() { return "6. Logótipo & Branding"; }
        public String getDescription() { return "Personalize a aparência dos seus documentos e faturas."; }
        public Node getContent() {
            VBox box = new VBox(20);
            box.setAlignment(Pos.CENTER);
            
            Rectangle rectLogo = new Rectangle(200, 120, Color.web("#f5f6f7"));
            rectLogo.setArcWidth(10); rectLogo.setArcHeight(10);
            rectLogo.setStroke(Color.web("#dcdde1"));
            
            Button btnUpload = new Button("Selecionar Logótipo", IconUtils.icon(Feather.UPLOAD, 14));
            
            HBox colors = new HBox(15);
            colors.setAlignment(Pos.CENTER);
            colors.getChildren().addAll(new Label("Cor dos Documentos:"), new ColorPicker(Color.web("#27ae60")));
            
            box.getChildren().addAll(new Label("Pré-visualização do Logótipo:"), rectLogo, btnUpload, colors);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "O logótipo será impresso em todas as faturas e guias emitidas pelo sistema."; }
    }

    // 7. Importação de Dados
    private class ImportDataStep implements WizardStep {
        public String getTitle() { return "7. Importação Inicial"; }
        public String getDescription() { return "Importe os seus dados de produtos, clientes e fornecedores."; }
        public Node getContent() {
            VBox box = new VBox(15);
            box.setAlignment(Pos.CENTER);
            
            Button btnImportProd = new Button("Importar Produtos (Excel/CSV)", IconUtils.icon(Feather.DATABASE, 14));
            Button btnImportCli = new Button("Importar Clientes (Excel/CSV)", IconUtils.icon(Feather.USERS, 14));
            
            Label lblInfo = new Label("Descarregue os nossos modelos de planilha para garantir a compatibilidade.");
            Hyperlink link = new Hyperlink("Descarregar Modelos");
            
            box.getChildren().addAll(btnImportProd, btnImportCli, lblInfo, link);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "Pode saltar esta etapa e importar os dados mais tarde nos respectivos módulos."; }
    }

    // 8. Backup e Segurança
    private class BackupSecurityStep implements WizardStep {
        public String getTitle() { return "8. Segurança & Backup"; }
        public String getDescription() { return "Defina as políticas de proteção de dados."; }
        public Node getContent() {
            VBox box = new VBox(20);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(0, 100, 0, 100));
            
            CheckBox chkBackupDiario = new CheckBox("Backup automático diário (Nuvem)");
            chkBackupDiario.setSelected(true);
            
            CheckBox chkMfa = new CheckBox("Ativar Autenticação de Dois Fatores (2FA) para Administradores");
            
            ComboBox<String> cbRetention = new ComboBox<>();
            cbRetention.getItems().addAll("Manter backups por 30 dias", "Manter backups por 1 ano", "Manter backups permanentemente");
            cbRetention.getSelectionModel().selectFirst();
            
            box.getChildren().addAll(chkBackupDiario, chkMfa, new Label("Retenção de Dados:"), cbRetention);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "A segurança dos dados é fundamental para a conformidade com a lei de proteção de dados."; }
    }

    // 9. Confirmação e Resumo
    private class SummaryStep implements WizardStep {
        public String getTitle() { return "9. Resumo & Conclusão"; }
        public String getDescription() { return "Confirme as configurações e ative o sistema."; }
        public Node getContent() {
            VBox box = new VBox(15);
            box.setAlignment(Pos.CENTER);
            
            Label lblDone = new Label("Tudo Pronto!");
            lblDone.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            
            VBox res = new VBox(10);
            res.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20px; -fx-background-radius: 8px; -fx-border-color: #dcdde1; -fx-border-width: 1px;");
            res.getChildren().addAll(
                new Label("Resumo das Configurações:"),
                new Label("✓ Empresa: " + empresa.getNome()),
                new Label("✓ NIF: " + empresa.getNif()),
                new Label("✓ Regime Fiscal: " + (empresa.getRegimeFiscal() != null ? empresa.getRegimeFiscal() : "Pendente")),
                new Label("✓ Backup e 2FA configurados")
            );
            
            Label lblFinal = new Label("Ao clicar em 'Finalizar', o sistema será preparado para o seu primeiro login.");
            
            box.getChildren().addAll(lblDone, res, lblFinal);
            return box;
        }
        public boolean validate() { return true; }
        public void savePartial() {}
        public String getHelpText() { return "Reveja os dados. Se algo estiver incorreto, utilize o botão 'Anterior'."; }
    }
}
