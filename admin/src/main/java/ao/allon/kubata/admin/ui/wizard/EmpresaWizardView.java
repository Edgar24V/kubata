package ao.allon.kubata.admin.ui.wizard;

import ao.allon.kubata.admin.service.EmpresaSetupService;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Assistente central de instalação/configuração de empresa do Kubata.
 *
 * <p>O fluxo foi desenhado como um instalador empresarial: identificação,
 * localização, fiscal, contabilidade, módulos, organização, documentos,
 * segurança e revisão final.</p>
 */
@Component
public class EmpresaWizardView extends VBox {

    private final ModalManager modalManager;
    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final ExercicioFiscalRepository exercicioFiscalRepository;
    private final ParametroSistemaRepository parametroSistemaRepository;
    private final ModuleRegistry moduleRegistry;
    private final EmpresaSetupService empresaSetupService;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;

    private Empresa empresa;
    private boolean newCompany;
    private Runnable onCompleted;
    private int currentStep;
    private final List<WizardStep> steps = new ArrayList<>();
    private final Set<String> selectedModuleIds = new LinkedHashSet<>();
    private final Map<String, String> companyParameters = new LinkedHashMap<>();

    private boolean openFiscalYear = true;
    private boolean backupEnabled = true;
    private String backupFrequency = "DAILY";
    private int backupRetentionDays = 30;
    private boolean mfaAdminRequired;
    private boolean saftEnabled = true;

    private Label stepTitle;
    private Label stepDescription;
    private Label stepCounter;
    private Label helpLabel;
    private StackPane content;
    private HBox stepIndicators;
    private ProgressBar progressBar;
    private Button previousButton;
    private Button nextButton;

    public EmpresaWizardView(ModalManager modalManager,
                             EmpresaRepository empresaRepository,
                             UserRepository userRepository,
                             ExercicioFiscalRepository exercicioFiscalRepository,
                             ParametroSistemaRepository parametroSistemaRepository,
                             ModuleRegistry moduleRegistry,
                             EmpresaSetupService empresaSetupService,
                             PersistenceService persistenceService,
                             SessionManager sessionManager) {
        this.modalManager = modalManager;
        this.empresaRepository = empresaRepository;
        this.userRepository = userRepository;
        this.exercicioFiscalRepository = exercicioFiscalRepository;
        this.parametroSistemaRepository = parametroSistemaRepository;
        this.moduleRegistry = moduleRegistry;
        this.empresaSetupService = empresaSetupService;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("empresa-wizard");
        setPrefSize(1120, 760);

        VBox header = new VBox(10);
        header.getStyleClass().add("empresa-wizard-header");
        header.setPadding(new Insets(22, 26, 18, 26));

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("K");
        icon.getStyleClass().add("empresa-wizard-brand");

        VBox titles = new VBox(3);
        stepTitle = new Label();
        stepTitle.getStyleClass().add("empresa-wizard-title");

        stepDescription = new Label();
        stepDescription.getStyleClass().add("empresa-wizard-subtitle");
        stepDescription.setWrapText(true);

        titles.getChildren().addAll(stepTitle, stepDescription);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        stepCounter = new Label();
        stepCounter.getStyleClass().add("empresa-wizard-step-counter");

        Button help = new Button("", IconUtils.icon(Feather.HELP_CIRCLE, 17));
        help.getStyleClass().add("button-outlined");
        help.setTooltip(new Tooltip("Ajuda desta etapa"));
        help.setOnAction(e -> showHelp());

        titleRow.getChildren().addAll(icon, titles, spacer, stepCounter, help);

        stepIndicators = new HBox(7);
        stepIndicators.setAlignment(Pos.CENTER_LEFT);

        progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(7);
        progressBar.getStyleClass().add("empresa-wizard-progress");

        header.getChildren().addAll(titleRow, stepIndicators, progressBar);

        content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getStyleClass().add("empresa-wizard-content");
        content.setPadding(new Insets(24, 28, 24, 28));

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("empresa-wizard-footer");
        footer.setPadding(new Insets(14, 22, 16, 22));

        helpLabel = new Label();
        helpLabel.getStyleClass().add("empresa-wizard-help");
        helpLabel.setWrapText(true);
        HBox.setHgrow(helpLabel, Priority.ALWAYS);

        Button cancelButton = new Button("Cancelar", IconUtils.icon(Feather.X, 14));
        cancelButton.getStyleClass().add("button-outlined");
        cancelButton.setOnAction(e -> {
            modalManager.setPersistent(false);
            modalManager.hideModal();
        });

        previousButton = new Button("Anterior", IconUtils.icon(Feather.ARROW_LEFT, 14));
        previousButton.getStyleClass().add("button-outlined");
        previousButton.setOnAction(e -> previousStep());

        nextButton = new Button("Próximo", IconUtils.icon(Feather.ARROW_RIGHT, 14));
        nextButton.setContentDisplay(ContentDisplay.RIGHT);
        nextButton.getStyleClass().add("button-primary");
        nextButton.setOnAction(e -> nextStep());

        footer.getChildren().addAll(helpLabel, cancelButton, previousButton, nextButton);
        getChildren().addAll(header, content, footer);
    }

    public void start(Empresa empresa) {
        start(empresa, null);
    }

    public void start(Empresa empresa, Runnable onCompleted) {
        this.empresa = empresa == null ? new Empresa() : empresa;
        this.newCompany = this.empresa.getId() == null;
        this.onCompleted = onCompleted;
        this.currentStep = 0;
        this.companyParameters.clear();
        this.selectedModuleIds.clear();

        if (this.empresa.getPais() == null || this.empresa.getPais().isBlank()) {
            this.empresa.setPais("AO");
        }
        if (this.empresa.getMoedaBase() == null || this.empresa.getMoedaBase().isBlank()) {
            this.empresa.setMoedaBase("AOA");
        }
        if (this.empresa.getCasasDecimaisValor() == null) {
            this.empresa.setCasasDecimaisValor(2);
        }
        if (this.empresa.getCasasDecimaisQuantidade() == null) {
            this.empresa.setCasasDecimaisQuantidade(3);
        }
        if (this.empresa.getExercicioActual() == null) {
            this.empresa.setExercicioActual(LocalDate.now().getYear());
        }

        loadExistingModuleSelection();
        loadExistingParameters();

        steps.clear();
        steps.add(new WelcomeStep());
        steps.add(new IdentityStep());
        steps.add(new AddressStep());
        steps.add(new FiscalStep());
        steps.add(new AccountingStep());
        steps.add(new ModulesStep());
        steps.add(new OrganizationStep());
        steps.add(new DocumentsStep());
        steps.add(new SecurityStep());
        steps.add(new SummaryStep());

        buildIndicators();
        updateUI();

        modalManager.setPersistent(true);
        modalManager.showModal(this, new ModalManager.ModalConfig()
                .title(newCompany ? "Assistente de Instalação de Empresa" : "Assistente de Configuração de Empresa")
                .subtitle("Kubata Administrator · configuração central do ambiente empresarial")
                .size(1160, 820)
                .minSize(980, 700)
                .resizable(true)
                .closeOnOverlayClick(false)
                .closeOnEscape(false)
                .footerDivider(false));
    }

    private void loadExistingParameters() {
        if (empresa.getId() == null) {
            return;
        }

        parametroSistemaRepository.findAllByEmpresa_IdOrderByGrupoAscChaveAsc(empresa.getId())
                .forEach(parameter -> {
                    if (parameter.getChave() != null) {
                        companyParameters.put(parameter.getChave(), parameter.getValor());
                    }
                });

        backupEnabled = Boolean.parseBoolean(
                companyParameters.getOrDefault("BACKUP_EMPRESA_ENABLED", String.valueOf(backupEnabled))
        );
        backupFrequency = companyParameters.getOrDefault("BACKUP_EMPRESA_FREQUENCY", backupFrequency);
        try {
            backupRetentionDays = Integer.parseInt(
                    companyParameters.getOrDefault(
                            "BACKUP_EMPRESA_RETENTION_DAYS",
                            String.valueOf(backupRetentionDays)
                    )
            );
        } catch (NumberFormatException ignored) {
            backupRetentionDays = 30;
        }
        mfaAdminRequired = Boolean.parseBoolean(
                companyParameters.getOrDefault("MFA_ADMIN_REQUIRED", String.valueOf(mfaAdminRequired))
        );
        saftEnabled = Boolean.parseBoolean(
                companyParameters.getOrDefault("SAFT_AO_ENABLED", String.valueOf(saftEnabled))
        );
    }

    private void loadExistingModuleSelection() {
        if (empresa.getModulos() != null && !empresa.getModulos().isBlank()) {
            for (String raw : empresa.getModulos().split(",")) {
                String id = raw.trim().toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                selectedModuleIds.add(legacyModuleId(id));
            }
        }

        if (selectedModuleIds.isEmpty()) {
            moduleRegistry.getAllModules().stream()
                    .filter(KubataModule::isActive)
                    .sorted(Comparator.comparing(KubataModule::getModuleName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(module -> selectedModuleIds.add(module.getModuleId()));
        }
    }

    private String legacyModuleId(String id) {
        return switch (id) {
            case "estoque", "stock" -> "inventario";
            case "pos" -> "vendas";
            case "billing", "faturacao" -> "vendas";
            case "finance" -> "financeiro";
            case "accounting" -> "contabilidade";
            default -> id;
        };
    }

    private void buildIndicators() {
        stepIndicators.getChildren().clear();
        for (int i = 0; i < steps.size(); i++) {
            Circle dot = new Circle(5);
            dot.getStyleClass().add("empresa-wizard-dot");
            stepIndicators.getChildren().add(dot);
        }
    }

    private void updateUI() {
        WizardStep step = steps.get(currentStep);
        stepTitle.setText(step.title());
        stepDescription.setText(step.description());
        helpLabel.setText(step.help());

        Node node = step.content();
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("empresa-wizard-scroll");
        content.getChildren().setAll(scroll);

        stepCounter.setText("Etapa " + (currentStep + 1) + " de " + steps.size());
        progressBar.setProgress((double) currentStep / Math.max(1, steps.size() - 1));

        previousButton.setDisable(currentStep == 0);
        nextButton.setText(currentStep == steps.size() - 1 ? "Concluir instalação" : "Próximo");
        nextButton.setGraphic(IconUtils.icon(
                currentStep == steps.size() - 1 ? Feather.CHECK : Feather.ARROW_RIGHT, 14
        ));

        for (int i = 0; i < stepIndicators.getChildren().size(); i++) {
            Circle dot = (Circle) stepIndicators.getChildren().get(i);
            dot.getStyleClass().removeAll("current", "done");
            if (i == currentStep) {
                dot.getStyleClass().add("current");
                dot.setRadius(7);
            } else if (i < currentStep) {
                dot.getStyleClass().add("done");
                dot.setRadius(5);
            } else {
                dot.setRadius(5);
            }
        }
    }

    private void nextStep() {
        WizardStep step = steps.get(currentStep);
        if (!step.validate()) return;

        step.save();
        if (currentStep < steps.size() - 1) {
            currentStep++;
            updateUI();
        } else {
            finish();
        }
    }

    private void previousStep() {
        if (currentStep <= 0) return;
        currentStep--;
        updateUI();
    }

    private void finish() {
        WizardStep step = steps.get(currentStep);
        step.save();

        if (!validateBeforeFinish()) return;

        nextButton.setDisable(true);
        previousButton.setDisable(true);
        nextButton.setText("A preparar...");
        helpLabel.setText("A gravar a empresa e a preparar o ambiente empresarial…");

        String updatedBy = sessionManager.getUser() != null
                ? sessionManager.getUser().getNome()
                : "Administrador";

        persistenceService.executeAsync(
                () -> empresaSetupService.saveAndProvision(
                        empresa,
                        openFiscalYear,
                        backupEnabled,
                        backupFrequency,
                        backupRetentionDays,
                        mfaAdminRequired,
                        companyParameters,
                        updatedBy
                ),
                "SETUP_EMPRESA",
                "EMPRESA",
                (newCompany ? "Instalação" : "Reconfiguração") + " da empresa " + empresa.getNome(),
                () -> Platform.runLater(() -> {
                    modalManager.setPersistent(false);
                    modalManager.hideModal();
                    if (onCompleted != null) onCompleted.run();
                    modalManager.alert(
                            "Empresa pronta",
                            "A empresa “" + empresa.getNome()
                                    + "” foi " + (newCompany ? "instalada" : "reconfigurada")
                                    + " e está disponível no ecossistema Kubata.",
                            "success",
                            null
                    );
                })
        );
    }

    private boolean validateBeforeFinish() {
        if (empresa.getNome() == null || empresa.getNome().isBlank()) {
            modalManager.alert("Configuração incompleta", "Indique a razão social da empresa.", "warning", null);
            return false;
        }
        if (empresa.getIdentificador() == null || empresa.getIdentificador().isBlank()) {
            modalManager.alert("Configuração incompleta", "Indique o identificador curto da empresa.", "warning", null);
            return false;
        }
        if (empresa.getNif() == null || empresa.getNif().isBlank()) {
            modalManager.alert("Configuração incompleta", "Indique o NIF da empresa.", "warning", null);
            return false;
        }
        if (empresa.getMoedaBase() == null || empresa.getMoedaBase().isBlank()) {
            modalManager.alert("Configuração incompleta", "Seleccione a moeda base.", "warning", null);
            return false;
        }
        if (empresa.getExercicioActual() == null) {
            modalManager.alert("Configuração incompleta", "Indique o exercício actual.", "warning", null);
            return false;
        }

        String nif = empresa.getNif().trim();
        Optional<Empresa> duplicate = empresaRepository.findByNif(nif);
        if (duplicate.isPresent() && (empresa.getId() == null || !duplicate.get().getId().equals(empresa.getId()))) {
            modalManager.alert("NIF já registado", "Já existe outra empresa com o NIF " + nif + ".", "warning", null);
            return false;
        }

        if (selectedModuleIds.isEmpty()) {
            modalManager.alert("Módulos não configurados", "Seleccione pelo menos um módulo para esta empresa.", "warning", null);
            return false;
        }

        return true;
    }

    private void showHelp() {
        modalManager.info("Ajuda — " + steps.get(currentStep).title(), steps.get(currentStep).help());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private TextField text(String prompt, String value) {
        TextField field = new TextField(normalize(value));
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ComboBox<String> combo(String... values) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private Label section(String title, String description) {
        Label label = new Label(title);
        label.getStyleClass().add("empresa-wizard-section-title");
        if (description != null && !description.isBlank()) {
            label.setText(title + "  ·  " + description);
        }
        return label;
    }

    private HBox field(String label, Node editor) {
        Label l = new Label(label);
        l.getStyleClass().add("empresa-wizard-field-label");
        l.setMinWidth(145);
        HBox box = new HBox(12, l, editor);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(editor, Priority.ALWAYS);
        return box;
    }

    private VBox page(Node... nodes) {
        VBox root = new VBox(16);
        root.getStyleClass().add("empresa-wizard-page");
        root.getChildren().addAll(nodes);
        return root;
    }

    private VBox card(Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("empresa-wizard-card");
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("empresa-wizard-hint");
        return label;
    }

    private TextArea textArea(String value) {
        TextArea area = new TextArea(normalize(value));
        area.setWrapText(true);
        area.setPrefRowCount(4);
        return area;
    }

    private interface WizardStep {
        String title();
        String description();
        Node content();
        boolean validate();
        void save();
        String help();
    }

    private class WelcomeStep implements WizardStep {
        public String title() { return "1 · Boas-vindas e plano de instalação"; }
        public String description() { return "Prepare a empresa e siga o roteiro central do Kubata Administrator."; }
        public Node content() {
            VBox root = page(
                    section("Instalação empresarial Kubata", "um ambiente por empresa"),
                    new Label("Este assistente centraliza a configuração que será utilizada pelos módulos do ecossistema."),
                    hint("Ao concluir, o Kubata grava a empresa, configura o exercício seleccionado, cria os parâmetros por empresa e regista os módulos escolhidos. Pode voltar atrás para rever cada etapa."),
                    installationChecklist(),
                    card(
                            new Label("Antes de começar"),
                            new Label("Tenha disponível o NIF, dados legais, endereço, informações bancárias e a decisão sobre quais módulos a empresa irá utilizar.")
                    )
            );
            return root;
        }
        private Node installationChecklist() {
            VBox box = new VBox(8);
            box.getStyleClass().add("empresa-wizard-checklist");
            String[] items = {
                    "Identificação legal e NIF",
                    "Morada, contactos e localização",
                    "Fiscalidade e parametrização financeira",
                    "Módulos e funcionalidades por empresa",
                    "Organização e centros de operação",
                    "Documentos, logótipo e mensagens",
                    "Segurança, backup e conclusão"
            };
            for (String item : items) {
                box.getChildren().add(new Label("✓  " + item));
            }
            return box;
        }
        public boolean validate() { return true; }
        public void save() {}
        public String help() { return "Esta etapa explica o que será configurado. Nenhum dado é gravado aqui."; }
    }

    private class IdentityStep implements WizardStep {
        private TextField identifier;
        private TextField name;
        private TextField commercialName;
        private TextField nif;
        private ComboBox<String> taxpayerType;
        private DatePicker incorporationDate;
        private Spinner<Integer> startYear;
        public String title() { return "2 · Identificação da empresa"; }
        public String description() { return "Registe a identidade jurídica e comercial da empresa."; }
        public Node content() {
            identifier = text("Ex.: KBT", empresa.getIdentificador());
            identifier.setPrefWidth(130);
            name = text("Razão social / denominação", empresa.getNome());
            commercialName = text("Nome comercial", empresa.getNomeComercial());
            nif = text("NIF", empresa.getNif());
            taxpayerType = combo("Pessoa Colectiva", "Pessoa Singular", "Não Residente");
            taxpayerType.setValue(empresa.getTipoContribuinte() == null ? "Pessoa Colectiva" : empresa.getTipoContribuinte());
            incorporationDate = new DatePicker(empresa.getDataConstituicao());
            incorporationDate.setMaxWidth(Double.MAX_VALUE);
            startYear = new Spinner<>(1900, 2100, empresa.getAnoInicio() == null ? LocalDate.now().getYear() : empresa.getAnoInicio());
            startYear.setMaxWidth(Double.MAX_VALUE);

            VBox left = new VBox(
                    field("Identificador *", identifier),
                    field("NIF *", nif),
                    field("Tipo de contribuinte *", taxpayerType),
                    field("Data de constituição", incorporationDate)
            );
            VBox right = new VBox(
                    field("Razão social *", name),
                    field("Nome comercial", commercialName),
                    field("Ano início actividade", startYear)
            );

            HBox columns = new HBox(22, left, right);
            HBox.setHgrow(left, Priority.ALWAYS);
            HBox.setHgrow(right, Priority.ALWAYS);

            return page(
                    section("Identificação legal", "dados principais"),
                    columns,
                    hint("O identificador é usado como referência curta dentro do ecossistema. O NIF deve ser único.")
            );
        }
        public boolean validate() {
            if (normalize(identifier.getText()).length() < 2) {
                modalManager.alert("Identificador inválido", "Indique um identificador curto com pelo menos 2 caracteres.", "warning", null);
                return false;
            }
            if (normalize(name.getText()).isBlank()) {
                modalManager.alert("Nome obrigatório", "Indique a razão social da empresa.", "warning", null);
                return false;
            }
            if (normalize(nif.getText()).isBlank()) {
                modalManager.alert("NIF obrigatório", "Indique o NIF da empresa.", "warning", null);
                return false;
            }
            return true;
        }
        public void save() {
            empresa.setIdentificador(normalize(identifier.getText()).toUpperCase(Locale.ROOT));
            empresa.setNome(normalize(name.getText()));
            empresa.setNomeComercial(normalize(commercialName.getText()));
            empresa.setNif(normalize(nif.getText()));
            empresa.setTipoContribuinte(taxpayerType.getValue());
            empresa.setDataConstituicao(incorporationDate.getValue());
            empresa.setAnoInicio(startYear.getValue());
        }
        public String help() { return "Utilize exactamente os dados legais da empresa. O NIF não pode repetir-se noutra empresa."; }
    }

    private class AddressStep implements WizardStep {
        private TextField address;
        private TextField postalCode;
        private TextField locality;
        private ComboBox<String> province;
        private TextField municipality;
        private TextField fiscalDistrict;
        private TextField phone;
        private TextField mobile;
        private TextField fax;
        private TextField email;
        private TextField website;
        public String title() { return "3 · Morada e contactos"; }
        public String description() { return "Configure localização, contactos e presença digital."; }
        public Node content() {
            address = text("Morada completa", empresa.getMorada());
            postalCode = text("Código postal", empresa.getCodigoPostal());
            locality = text("Localidade", empresa.getLocalidade());
            province = combo("Bengo", "Benguela", "Bié", "Cabinda", "Cuando Cubango",
                    "Cuanza Norte", "Cuanza Sul", "Cunene", "Huambo", "Huíla",
                    "Luanda", "Lunda Norte", "Lunda Sul", "Malanje", "Moxico",
                    "Namibe", "Uíge", "Zaire");
            province.setValue(empresa.getProvincia() == null ? "Luanda" : empresa.getProvincia());
            municipality = text("Município", empresa.getMunicipio());
            fiscalDistrict = text("Bairro / zona fiscal", empresa.getBairroFiscal());
            phone = text("Telefone", empresa.getTelefone());
            mobile = text("Telemóvel", empresa.getTelemovel());
            fax = text("Fax", empresa.getFax());
            email = text("Email", empresa.getEmail());
            website = text("https://...", empresa.getWebsite());

            VBox left = new VBox(
                    field("Morada *", address),
                    field("Código postal", postalCode),
                    field("Localidade", locality),
                    field("Província", province),
                    field("Município", municipality)
            );
            VBox right = new VBox(
                    field("Bairro / zona fiscal", fiscalDistrict),
                    field("Telefone", phone),
                    field("Telemóvel", mobile),
                    field("Fax", fax),
                    field("Email", email),
                    field("Website", website)
            );
            HBox columns = new HBox(22, left, right);
            HBox.setHgrow(left, Priority.ALWAYS);
            HBox.setHgrow(right, Priority.ALWAYS);
            return page(
                    section("Localização", "endereço fiscal e operacional"),
                    columns,
                    hint("Preencha o endereço usado nos documentos e contactos oficiais.")
            );
        }
        public boolean validate() {
            if (normalize(address.getText()).isBlank()) {
                modalManager.alert("Morada obrigatória", "Indique a morada principal da empresa.", "warning", null);
                return false;
            }
            return true;
        }
        public void save() {
            empresa.setMorada(normalize(address.getText()));
            empresa.setCodigoPostal(normalize(postalCode.getText()));
            empresa.setLocalidade(normalize(locality.getText()));
            empresa.setProvincia(province.getValue());
            empresa.setMunicipio(normalize(municipality.getText()));
            empresa.setBairroFiscal(normalize(fiscalDistrict.getText()));
            empresa.setTelefone(normalize(phone.getText()));
            empresa.setTelemovel(normalize(mobile.getText()));
            empresa.setFax(normalize(fax.getText()));
            empresa.setEmail(normalize(email.getText()));
            empresa.setWebsite(normalize(website.getText()));
        }
        public String help() { return "A morada e os contactos são reutilizados pelos documentos e comunicações do Kubata."; }
    }

    private class FiscalStep implements WizardStep {
        private ComboBox<String> regime;
        private TextField cae;
        private TextField socialSecurityNif;
        private TextField fiscalNif;
        private TextField certificate;
        private TextField certificateVersion;
        private DatePicker certificateDate;
        private TextField certificateHash;
        public String title() { return "4 · Fiscalidade e conformidade"; }
        public String description() { return "Configure os dados fiscais que serão partilhados pelos módulos."; }
        public Node content() {
            regime = combo("Regime Geral", "Regime Simplificado", "Regime de Exclusão", "Isento", "Especial");
            regime.setValue(empresa.getRegimeFiscal() == null ? "Regime Geral" : empresa.getRegimeFiscal());
            cae = text("CAE", empresa.getCae());
            socialSecurityNif = text("NIF Segurança Social", empresa.getNifSegurancaSocial());
            fiscalNif = text("NIF Fiscal", empresa.getNifFiscal());
            certificate = text("Número do certificado AGT", empresa.getNumeroCertificadoAGT());
            certificateVersion = text("Versão", empresa.getVersaoCertificadoAGT());
            certificateDate = new DatePicker(empresa.getDataCertificadoAGT());
            certificateDate.setMaxWidth(Double.MAX_VALUE);
            certificateHash = text("Hash / impressão digital", empresa.getHashCertificadoAGT());

            CheckBox saft = new CheckBox("Preparar integração/controlo SAFT-AO");
            saft.setSelected(saftEnabled);
            saft.selectedProperty().addListener((obs, old, value) -> saftEnabled = value);

            return page(
                    section("Enquadramento tributário", "dados para faturação e reporting"),
                    field("Regime fiscal *", regime),
                    field("CAE", cae),
                    field("NIF Fiscal", fiscalNif),
                    field("NIF Segurança Social", socialSecurityNif),
                    new Separator(),
                    section("Certificação AGT", "quando aplicável"),
                    field("Nº certificado", certificate),
                    field("Versão certificado", certificateVersion),
                    field("Data", certificateDate),
                    field("Hash", certificateHash),
                    saft
            );
        }
        public boolean validate() {
            if (regime.getValue() == null || regime.getValue().isBlank()) {
                modalManager.alert("Regime fiscal", "Seleccione o regime fiscal da empresa.", "warning", null);
                return false;
            }
            return true;
        }
        public void save() {
            empresa.setRegimeFiscal(regime.getValue());
            empresa.setCae(normalize(cae.getText()));
            empresa.setNifFiscal(normalize(fiscalNif.getText()));
            empresa.setNifSegurancaSocial(normalize(socialSecurityNif.getText()));
            empresa.setNumeroCertificadoAGT(normalize(certificate.getText()));
            empresa.setVersaoCertificadoAGT(normalize(certificateVersion.getText()));
            empresa.setDataCertificadoAGT(certificateDate.getValue());
            empresa.setHashCertificadoAGT(normalize(certificateHash.getText()));
            companyParameters.put("SAFT_AO_ENABLED", String.valueOf(saftEnabled));
        }
        public String help() { return "Regime e dados fiscais são transversais ao ecossistema. Confirme os valores com a documentação oficial da empresa."; }
    }

    private class AccountingStep implements WizardStep {
        private Spinner<Integer> fiscalYear;
        private ComboBox<String> baseCurrency;
        private ComboBox<String> alternativeCurrency;
        private Spinner<Integer> moneyDecimals;
        private Spinner<Integer> quantityDecimals;
        private TextField bank;
        private TextField bankAccount;
        private TextField iban;
        private TextField shareCapital;
        private TextField expectedRevenue;
        private TextField nationalCapital;
        private TextField foreignCapital;
        private TextField publicCapital;
        public String title() { return "5 · Contabilidade, moeda e dados bancários"; }
        public String description() { return "Defina o exercício, moeda e informação financeira base."; }
        public Node content() {
            fiscalYear = new Spinner<>(2000, 2100, empresa.getExercicioActual());
            fiscalYear.setMaxWidth(Double.MAX_VALUE);
            baseCurrency = combo("AOA", "USD", "EUR", "ZAR");
            baseCurrency.setValue(empresa.getMoedaBase() == null ? "AOA" : empresa.getMoedaBase());
            alternativeCurrency = combo("AOA", "USD", "EUR", "ZAR");
            alternativeCurrency.setValue(empresa.getMoedaAlternativa());
            moneyDecimals = new Spinner<>(0, 6, empresa.getCasasDecimaisValor() == null ? 2 : empresa.getCasasDecimaisValor());
            quantityDecimals = new Spinner<>(0, 6, empresa.getCasasDecimaisQuantidade() == null ? 3 : empresa.getCasasDecimaisQuantidade());
            bank = text("Banco principal", empresa.getBanco());
            bankAccount = text("Conta bancária", empresa.getContaBancaria());
            iban = text("IBAN", empresa.getIban());
            shareCapital = text("Capital social", decimal(empresa.getCapitalSocial()));
            expectedRevenue = text("Volume de negócios previsto", decimal(empresa.getVolumeNegociosPrevisto()));
            nationalCapital = text("Capital nacional", decimal(empresa.getCapitalNacional()));
            foreignCapital = text("Capital estrangeiro", decimal(empresa.getCapitalEstrangeiro()));
            publicCapital = text("Capital público", decimal(empresa.getCapitalPublico()));

            GridPane grid = new GridPane();
            grid.setHgap(18);
            grid.setVgap(12);
            grid.add(field("Exercício actual *", fiscalYear), 0, 0);
            grid.add(field("Moeda base *", baseCurrency), 1, 0);
            grid.add(field("Moeda alternativa", alternativeCurrency), 0, 1);
            grid.add(field("Casas decimais valor", moneyDecimals), 1, 1);
            grid.add(field("Casas decimais quantidade", quantityDecimals), 0, 2);
            grid.add(field("Banco", bank), 1, 2);
            grid.add(field("Conta bancária", bankAccount), 0, 3);
            grid.add(field("IBAN", iban), 1, 3);
            grid.add(field("Capital social", shareCapital), 0, 4);
            grid.add(field("Volume previsto", expectedRevenue), 1, 4);
            grid.add(field("Capital nacional", nationalCapital), 0, 5);
            grid.add(field("Capital estrangeiro", foreignCapital), 1, 5);
            grid.add(field("Capital público", publicCapital), 0, 6);
            GridPane.setHgrow(grid.getChildren().get(1), Priority.ALWAYS);

            return page(
                    section("Configuração financeira", "parâmetros globais da empresa"),
                    grid,
                    hint("O exercício escolhido será usado para criar o primeiro exercício fiscal quando essa opção for activada na etapa de segurança.")
            );
        }
        public boolean validate() {
            if (fiscalYear.getValue() == null || baseCurrency.getValue() == null) {
                modalManager.alert("Configuração financeira", "Seleccione o exercício e a moeda base.", "warning", null);
                return false;
            }
            return validDecimal("Capital social", shareCapital)
                    && validDecimal("Volume de negócios previsto", expectedRevenue)
                    && validDecimal("Capital nacional", nationalCapital)
                    && validDecimal("Capital estrangeiro", foreignCapital)
                    && validDecimal("Capital público", publicCapital);
        }
        private boolean validDecimal(String label, TextField field) {
            if (normalize(field.getText()).isBlank()) return true;
            try {
                new BigDecimal(field.getText().trim().replace(",", "."));
                return true;
            } catch (NumberFormatException ex) {
                modalManager.alert("Valor inválido", label + " deve ser numérico.", "warning", null);
                return false;
            }
        }
        public void save() {
            empresa.setExercicioActual(fiscalYear.getValue());
            empresa.setMoedaBase(baseCurrency.getValue());
            empresa.setMoedaAlternativa(alternativeCurrency.getValue());
            empresa.setCasasDecimaisValor(moneyDecimals.getValue());
            empresa.setCasasDecimaisQuantidade(quantityDecimals.getValue());
            empresa.setBanco(normalize(bank.getText()));
            empresa.setContaBancaria(normalize(bankAccount.getText()));
            empresa.setIban(normalize(iban.getText()));
            empresa.setCapitalSocial(decimalValue(shareCapital));
            empresa.setVolumeNegociosPrevisto(decimalValue(expectedRevenue));
            empresa.setCapitalNacional(decimalValue(nationalCapital));
            empresa.setCapitalEstrangeiro(decimalValue(foreignCapital));
            empresa.setCapitalPublico(decimalValue(publicCapital));
        }
        public String help() { return "A moeda base e o exercício definem referências importantes para contabilidade, financeiro, vendas e compras."; }
    }

    private class ModulesStep implements WizardStep {
        private FlowPane cards;
        private Label selectedCount;
        public String title() { return "6 · Módulos e funcionalidades"; }
        public String description() { return "Escolha os módulos que estarão disponíveis para esta empresa."; }
        public Node content() {
            selectedCount = new Label();
            selectedCount.getStyleClass().add("empresa-wizard-module-summary");
            cards = new FlowPane(14, 14);
            cards.setPrefWrapLength(900);

            List<KubataModule> modules = moduleRegistry.getAllModules().stream()
                    .sorted(Comparator.comparing(KubataModule::getModuleName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            for (KubataModule module : modules) {
                cards.getChildren().add(moduleCard(module));
            }

            updateSelectedCount();

            return page(
                    section("Catálogo de módulos do Kubata", "configuração por empresa"),
                    selectedCount,
                    hint("A instalação global do módulo é gerida pelo Administrator. Aqui define-se quais módulos a empresa irá utilizar. Módulos não registados no runtime aparecem como indisponíveis e não podem ser seleccionados."),
                    cards
            );
        }
        private Node moduleCard(KubataModule module) {
            VBox box = new VBox(9);
            box.setPrefWidth(275);
            box.getStyleClass().add("empresa-module-card");

            HBox top = new HBox(9);
            top.setAlignment(Pos.CENTER_LEFT);
            Node icon = moduleIcon(module);
            Label name = new Label(module.getModuleName());
            name.getStyleClass().add("empresa-module-name");
            top.getChildren().addAll(icon, name);

            Label description = new Label(module.getModuleDescription());
            description.setWrapText(true);
            description.getStyleClass().add("empresa-wizard-hint");

            Label version = new Label("Versão " + module.getVersion());
            version.getStyleClass().add("empresa-module-meta");

            CheckBox check = new CheckBox(module.isActive() ? "Disponível para esta empresa" : "Módulo não activo");
            check.setSelected(selectedModuleIds.contains(module.getModuleId()));
            check.setDisable(!module.isActive());
            check.selectedProperty().addListener((obs, old, value) -> {
                if (value) selectedModuleIds.add(module.getModuleId());
                else selectedModuleIds.remove(module.getModuleId());
                updateSelectedCount();
            });

            box.getChildren().addAll(top, description, version, check);
            return box;
        }
        private Node moduleIcon(KubataModule module) {
            Feather feather = switch (module.getModuleId().toLowerCase(Locale.ROOT)) {
                case "inventario" -> Feather.BOX;
                case "vendas" -> Feather.SHOPPING_CART;
                case "compras" -> Feather.DOWNLOAD;
                case "financeiro" -> Feather.DOLLAR_SIGN;
                case "contabilidade" -> Feather.BAR_CHART_2;
                case "fiscal" -> Feather.FILE_TEXT;
                default -> Feather.LAYERS;
            };
            return IconUtils.icon(feather, 18);
        }
        private void updateSelectedCount() {
            if (selectedCount != null) {
                selectedCount.setText(selectedModuleIds.size() + " módulo(s) seleccionado(s) para esta empresa");
            }
        }
        public boolean validate() {
            if (selectedModuleIds.isEmpty()) {
                modalManager.alert("Módulos obrigatórios", "Seleccione pelo menos um módulo disponível.", "warning", null);
                return false;
            }
            return true;
        }
        public void save() {
            empresa.setModulos(selectedModuleIds.stream().sorted().collect(Collectors.joining(",")));
        }
        public String help() { return "Cada empresa pode ter um conjunto de módulos diferente. A lista apresentada vem do ModuleRegistry real do Kubata."; }
    }

    private class OrganizationStep implements WizardStep {
        private TextArea sectors;
        private TextArea activity;
        public String title() { return "7 · Estrutura operacional"; }
        public String description() { return "Defina a estrutura que será usada como base pelos módulos."; }
        public Node content() {
            sectors = textArea(empresa.getSetores());
            if (normalize(sectors.getText()).isBlank()) {
                sectors.setText("Administração\nFinanceiro\nVendas\nCompras\nArmazém\nRecursos Humanos");
            }
            sectors.setPrefRowCount(9);

            CheckBox active = new CheckBox("Empresa operacional");
            active.setSelected(empresa.getAtiva());
            active.selectedProperty().addListener((obs, old, value) -> empresa.setAtiva(value));

            activity = textArea(empresa.getDescricaoActividade());
            activity.setPromptText("Descreva resumidamente a actividade principal da empresa.");

            return page(
                    section("Estrutura organizacional", "informação reutilizável por RH, inventário, compras e financeiro"),
                    field("Sectores / departamentos", sectors),
                    field("Actividade principal", activity),
                    active,
                    hint("Utilize um sector por linha. A estrutura pode ser aprofundada posteriormente nos módulos especializados.")
            );
        }
        public boolean validate() { return true; }
        public void save() {
            empresa.setSetores(normalize(sectors.getText()).replace("\r\n", ",").replace("\n", ","));
            empresa.setDescricaoActividade(normalize(activity.getText()));
        }
        public String help() { return "A estrutura aqui criada serve como referência inicial. O cadastro detalhado de departamentos pode ser feito nos módulos especializados."; }
    }

    private class DocumentsStep implements WizardStep {
        private byte[] logoBytes;
        private String logoMimeType;
        private ImageView logoView;
        private TextArea footer;
        private TextArea invoiceMessage;
        public String title() { return "8 · Logótipo e documentos"; }
        public String description() { return "Configure a identidade visual e textos utilizados nos documentos."; }
        public Node content() {
            logoBytes = empresa.getLogotipo();
            logoMimeType = empresa.getLogotipoMimeType();

            StackPane logoBox = new StackPane();
            logoBox.getStyleClass().add("empresa-logo-box");
            logoView = new ImageView();
            logoView.setFitWidth(220);
            logoView.setFitHeight(120);
            logoView.setPreserveRatio(true);
            refreshLogoView(logoBox);

            Button choose = new Button("Seleccionar logótipo", IconUtils.icon(Feather.IMAGE, 14));
            choose.getStyleClass().add("button-outlined");
            choose.setOnAction(e -> chooseLogo(logoBox));

            footer = textArea(empresa.getRodapeDocumento());
            footer.setPromptText("Ex.: Documento emitido pelo sistema Kubata.");
            invoiceMessage = textArea(empresa.getMensagemFatura());
            invoiceMessage.setPromptText("Mensagem impressa nas faturas.");
            invoiceMessage.setPrefRowCount(4);

            VBox branding = card(
                    section("Identidade visual", "logótipo"),
                    logoBox,
                    choose
            );
            VBox docs = card(
                    section("Documentos", "textos padrão"),
                    field("Rodapé", footer),
                    field("Mensagem de fatura", invoiceMessage)
            );

            HBox body = new HBox(18, branding, docs);
            HBox.setHgrow(branding, Priority.ALWAYS);
            HBox.setHgrow(docs, Priority.ALWAYS);

            return page(body, hint("O logótipo será guardado na empresa e pode ser utilizado por todos os módulos de documentos que suportem a identidade empresarial."));
        }
        private void refreshLogoView(StackPane logoBox) {
            logoBox.getChildren().clear();
            if (logoBytes != null && logoBytes.length > 0) {
                try {
                    logoView.setImage(new Image(new ByteArrayInputStream(logoBytes)));
                    logoBox.getChildren().add(logoView);
                    return;
                } catch (Exception ignored) {}
            }
            logoBox.getChildren().add(new Label("Sem logótipo"));
        }
        private void chooseLogo(StackPane box) {
            if (getScene() == null || getScene().getWindow() == null) return;
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar logótipo da empresa");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file == null) return;
            try {
                logoBytes = Files.readAllBytes(file.toPath());
                logoMimeType = Files.probeContentType(file.toPath());
                refreshLogoView(box);
            } catch (Exception ex) {
                modalManager.alert("Logótipo", "Não foi possível carregar a imagem: " + ex.getMessage(), "error", ex);
            }
        }
        public boolean validate() { return true; }
        public void save() {
            empresa.setLogotipo(logoBytes);
            empresa.setLogotipoMimeType(logoMimeType);
            empresa.setRodapeDocumento(normalize(footer.getText()));
            empresa.setMensagemFatura(normalize(invoiceMessage.getText()));
        }
        public String help() { return "Configure uma identidade consistente para os documentos empresariais."; }
    }

    private class SecurityStep implements WizardStep {
        private CheckBox openYear;
        private CheckBox backup;
        private ComboBox<String> frequency;
        private ComboBox<String> retention;
        private CheckBox mfa;
        public String title() { return "9 · Segurança, backup e conclusão operacional"; }
        public String description() { return "Defina como o ambiente empresarial deve iniciar."; }
        public Node content() {
            openYear = new CheckBox("Criar e abrir automaticamente o exercício fiscal seleccionado");
            openYear.setSelected(openFiscalYear);

            backup = new CheckBox("Activar política de backup por empresa");
            backup.setSelected(backupEnabled);

            frequency = combo("DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY");
            frequency.setValue(backupFrequency);

            retention = combo("7", "30", "90", "180", "365");
            retention.setValue(String.valueOf(backupRetentionDays));

            mfa = new CheckBox("Exigir autenticação multifactor para administradores");
            mfa.setSelected(mfaAdminRequired);

            openYear.selectedProperty().addListener((obs, old, value) -> openFiscalYear = value);
            backup.selectedProperty().addListener((obs, old, value) -> backupEnabled = value);
            mfa.selectedProperty().addListener((obs, old, value) -> mfaAdminRequired = value);
            frequency.valueProperty().addListener((obs, old, value) -> backupFrequency = value);
            retention.valueProperty().addListener((obs, old, value) -> {
                try { backupRetentionDays = Integer.parseInt(value); } catch (Exception ignored) {}
            });

            return page(
                    section("Arranque do ambiente", "operações iniciais"),
                    card(openYear, hint("Quando activo, o Kubata cria o primeiro exercício fiscal da empresa, sem duplicar exercícios já existentes.")),
                    section("Protecção de dados", "backup"),
                    card(
                            backup,
                            field("Periodicidade", frequency),
                            field("Retenção (dias)", retention),
                            mfa
                    ),
                    hint("Estas opções são guardadas como parâmetros por empresa. Não alteram a política de backup de outras empresas.")
            );
        }
        public boolean validate() { return true; }
        public void save() {
            openFiscalYear = openYear.isSelected();
            backupEnabled = backup.isSelected();
            backupFrequency = frequency.getValue() == null ? "DAILY" : frequency.getValue();
            try { backupRetentionDays = Integer.parseInt(retention.getValue()); } catch (Exception ignored) {}
            mfaAdminRequired = mfa.isSelected();
        }
        public String help() { return "O Administrador centraliza a configuração. A política de backup desta etapa fica associada à empresa através dos parâmetros empresariais."; }
    }

    private class SummaryStep implements WizardStep {
        public String title() { return "10 · Revisão e instalação"; }
        public String description() { return "Revise o ambiente e conclua a instalação."; }
        public Node content() {
            VBox root = page(
                    section("Resumo final", "pronto para instalar"),
                    summaryLine("Empresa", empresa.getNome()),
                    summaryLine("NIF", empresa.getNif()),
                    summaryLine("Localização", String.join(" · ", nonBlank(empresa.getProvincia(), empresa.getMunicipio(), empresa.getLocalidade()))),
                    summaryLine("Regime fiscal", empresa.getRegimeFiscal()),
                    summaryLine("Exercício", String.valueOf(empresa.getExercicioActual())),
                    summaryLine("Moeda base", empresa.getMoedaBase()),
                    summaryLine("Módulos", selectedModuleNames()),
                    summaryLine("Backup", backupEnabled ? backupFrequency + " · " + backupRetentionDays + " dias" : "Desactivado"),
                    summaryLine("Exercício inicial", openFiscalYear ? "Será criado/aberto" : "Não criar automaticamente"),
                    summaryLine("Estado", empresa.getAtiva() ? "Activa" : "Inactiva"),
                    hint("Ao concluir, o Kubata validará a unicidade do NIF, gravará a empresa, criará o exercício quando seleccionado e persistirá os parâmetros de instalação.")
            );
            return root;
        }
        private Node summaryLine(String label, String value) {
            Label left = new Label(label);
            left.getStyleClass().add("empresa-summary-label");
            Label right = new Label(value == null || value.isBlank() ? "—" : value);
            right.getStyleClass().add("empresa-summary-value");
            right.setWrapText(true);
            HBox row = new HBox(14, left, right);
            row.getStyleClass().add("empresa-summary-row");
            HBox.setHgrow(right, Priority.ALWAYS);
            return row;
        }
        private String selectedModuleNames() {
            return moduleRegistry.getAllModules().stream()
                    .filter(m -> selectedModuleIds.contains(m.getModuleId()))
                    .sorted(Comparator.comparing(KubataModule::getModuleName, String.CASE_INSENSITIVE_ORDER))
                    .map(KubataModule::getModuleName)
                    .collect(Collectors.joining(", "));
        }
        public boolean validate() { return validateBeforeFinish(); }
        public void save() {
            empresa.setModulos(selectedModuleIds.stream().sorted().collect(Collectors.joining(",")));
        }
        public String help() { return "Revise todos os pontos. Utilize Anterior para corrigir qualquer informação antes da instalação."; }
    }

    private String selectedModuleNames() {
        return moduleRegistry.getAllModules().stream()
                .filter(m -> selectedModuleIds.contains(m.getModuleId()))
                .map(KubataModule::getModuleName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private BigDecimal decimalValue(TextField field) {
        if (field == null || normalize(field.getText()).isBlank()) return null;
        try {
            return new BigDecimal(field.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String[] nonBlank(String... values) {
        return Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toArray(String[]::new);
    }

    @Override
    public String toString() {
        return "EmpresaWizardView{" + (empresa == null ? "sem empresa" : empresa.getNome()) + "}";
    }
}
