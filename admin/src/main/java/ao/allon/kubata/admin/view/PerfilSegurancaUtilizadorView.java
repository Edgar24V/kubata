package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.service.UserSecurityProfileService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PerfilSegurancaUtilizadorView {

    private static final List<String> MODULES = List.of(
            "FATURACAO",
            "VENDAS",
            "COMPRAS",
            "INVENTARIO",
            "FINANCEIRO",
            "CONTABILIDADE",
            "FISCAL",
            "RH"
    );

    private static final List<String> CRITICAL_OPERATIONS = List.of(
            "CRIAR_FACTURA",
            "ANULAR_FACTURA",
            "ALTERAR_PRECO",
            "FORCAR_CREDITO",
            "APROVAR_PAGAMENTO",
            "EXPORTAR_SAFT",
            "MOVIMENTO_FINANCEIRO"
    );

    private final UserSecurityProfileService service;
    private final EmpresaRepository empresaRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;

    public PerfilSegurancaUtilizadorView(
            UserSecurityProfileService service,
            EmpresaRepository empresaRepository,
            SessionManager sessionManager,
            ModalManager modalManager) {
        this.service = service;
        this.empresaRepository = empresaRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
    }

    public void open(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        if (!service.isCommonUser(user)) {
            modalManager.showErrorModal(
                    "Perfil de segurança",
                    "Esta funcionalidade destina-se apenas a utilizadores comuns. "
                            + "Contas Administrador e Superadministrador mantêm o fluxo de segurança administrativo.",
                    null
            );
            return;
        }

        UserSecurityProfile profile =
                service.loadForAdministration(sessionManager.getUser(), user.getId());

        VBox content = new VBox(12);
        content.setPadding(new Insets(4));

        CheckBox loginEnabled = check("Permitir login desta conta", profile.isLoginEnabled());
        CheckBox requireMfa = check("Exigir MFA para esta conta", profile.isRequireMfa());
        CheckBox allowRecovery = check(
                "Permitir código de recuperação MFA",
                profile.isAllowRecoveryCode()
        );

        Spinner<Integer> maxAttempts =
                spinner(profile.getMaxLoginAttempts(), 1, 50, 1);
        Spinner<Integer> lockoutMinutes =
                spinner(profile.getLockoutMinutes(), 1, 1440, 1);
        Spinner<Integer> sessionTimeout =
                spinner(profile.getSessionTimeoutMinutes(), 5, 10080, 5);
        Spinner<Integer> maxSessions =
                spinner(profile.getMaxConcurrentSessions(), 0, 100, 1);
        Spinner<Integer> passwordLength =
                spinner(profile.getPasswordMinLength(), 8, 128, 1);
        Spinner<Integer> passwordExpiry =
                spinner(profile.getPasswordExpiryDays(), 0, 3650, 30);

        CheckBox requireUpper = check("Maiúscula", profile.isPasswordRequireUpper());
        CheckBox requireLower = check("Minúscula", profile.isPasswordRequireLower());
        CheckBox requireDigit = check("Número", profile.isPasswordRequireDigit());
        CheckBox requireSymbol = check("Símbolo", profile.isPasswordRequireSymbol());

        TextField loginStart = new TextField(formatTime(profile.getLoginStart()));
        loginStart.setPromptText("HH:mm");
        loginStart.setPrefWidth(95);

        TextField loginEnd = new TextField(formatTime(profile.getLoginEnd()));
        loginEnd.setPromptText("HH:mm");
        loginEnd.setPrefWidth(95);

        TextField operationLimit =
                new TextField(decimal(profile.getFinancialOperationLimit()));
        operationLimit.setPromptText("Sem limite");
        operationLimit.setPrefWidth(140);

        TextField dailyLimit =
                new TextField(decimal(profile.getFinancialDailyLimit()));
        dailyLimit.setPromptText("Sem limite");
        dailyLimit.setPrefWidth(140);

        TextField currency = new TextField(
                profile.getFinancialCurrency() == null
                        ? "AOA"
                        : profile.getFinancialCurrency()
        );
        currency.setPrefWidth(80);

        Map<Long, CheckBox> companyChecks = new java.util.LinkedHashMap<>();
        VBox companyBox = new VBox(6);
        List<Empresa> empresas = empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getAtiva()))
                .sorted(java.util.Comparator.comparing(
                        e -> e.getNome() == null ? "" : e.getNome()
                ))
                .toList();

        for (Empresa empresa : empresas) {
            CheckBox check = new CheckBox(
                    empresa.getNome() == null ? "Empresa #" + empresa.getId() : empresa.getNome()
            );
            check.setSelected(profile.getAllowedCompanyIds().contains(empresa.getId()));
            companyChecks.put(empresa.getId(), check);
            companyBox.getChildren().add(check);
        }

        Label companyHint = hint("Nenhuma empresa seleccionada = sem restrição adicional de empresa.");
        ScrollPane companyScroll = new ScrollPane(companyBox);
        companyScroll.setFitToWidth(true);
        companyScroll.setPrefViewportHeight(95);
        companyScroll.setMaxHeight(120);

        Map<String, CheckBox> moduleChecks = new java.util.LinkedHashMap<>();
        GridPane modulesGrid = new GridPane();
        modulesGrid.setHgap(12);
        modulesGrid.setVgap(7);

        for (int i = 0; i < MODULES.size(); i++) {
            String module = MODULES.get(i);
            CheckBox check = new CheckBox(module);
            check.setSelected(profile.getAllowedModules().stream()
                    .anyMatch(value -> value.equalsIgnoreCase(module)));
            moduleChecks.put(module, check);
            modulesGrid.add(check, i % 3, i / 3);
        }

        Label moduleHint = hint("Os módulos seleccionados são uma restrição adicional. As funções (Ver, Criar, Editar, Apagar, Imprimir, Exportar, etc.) continuam a ser concedidas pelos Perfis de Acesso/RBAC.");

        TextArea ipRules = textArea(profile.getAllowedIpRanges(), "Uma regra por linha. Ex.: 192.168.1.0/24");
        TextArea criticalOps = textArea(
                profile.getAllowedCriticalOperations(),
                "Uma operação por linha."
        );

        Map<DayOfWeek, CheckBox> days = new EnumMap<>(DayOfWeek.class);
        GridPane daysGrid = new GridPane();
        daysGrid.setHgap(12);
        daysGrid.setVgap(7);
        String[] labels = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        DayOfWeek[] values = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        };
        for (int i = 0; i < values.length; i++) {
            CheckBox check = new CheckBox(labels[i]);
            check.setSelected(profile.getAllowedWeekDays().contains(values[i]));
            days.put(values[i], check);
            daysGrid.add(check, i, 0);
        }

        VBox authentication = section(
                "Autenticação",
                row(loginEnabled, requireMfa, allowRecovery),
                row(label("Máx. tentativas", maxAttempts), label("Bloqueio (min)", lockoutMinutes)),
                row(label("Timeout sessão (min)", sessionTimeout),
                        label("Sessões simultâneas (0 = ilimitado)", maxSessions))
        );

        VBox password = section(
                "Palavra-passe",
                row(label("Comprimento mínimo", passwordLength),
                        label("Expiração (dias; 0 = nunca)", passwordExpiry)),
                row(requireUpper, requireLower, requireDigit, requireSymbol)
        );

        VBox schedule = section(
                "Horário e dias",
                row(label("Início", loginStart), label("Fim", loginEnd)),
                daysGrid,
                hint("Início = fim significa 24 horas. Dias vazios significam todos os dias.")
        );

        VBox organization = section(
                "Empresas autorizadas",
                companyScroll,
                companyHint
        );

        VBox modules = section(
                "Módulos permitidos como restrição adicional",
                modulesGrid,
                moduleHint
        );

        VBox network = section(
                "IPs autorizados",
                ipRules,
                hint("Vazio = qualquer IP. Suporta IP exacto e CIDR IPv4/IPv6.")
        );

        VBox financial = section(
                "Limites financeiros",
                row(label("Por operação", operationLimit),
                        label("Diário", dailyLimit),
                        label("Moeda", currency)),
                hint("Deixe os limites vazios para não impor limite financeiro.")
        );

        VBox critical = section(
                "Operações críticas",
                criticalOps,
                hint("Vazio = nenhuma restrição adicional. Os códigos serão comparados sem distinção de maiúsculas.")
        );

        content.getChildren().addAll(
                header(user),
                authentication,
                password,
                schedule,
                organization,
                modules,
                network,
                financial,
                critical
        );

        Button cancel = new Button(
                "Cancelar",
                IconUtils.icon(Feather.X, 12)
        );
        cancel.getStyleClass().add("button-outlined");

        Button save = new Button(
                "Guardar política",
                IconUtils.icon(Feather.SAVE, 12)
        );
        save.getStyleClass().add("button-primary");

        HBox actions = new HBox(8, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        cancel.setOnAction(e -> modalManager.hideModal());
        save.setOnAction(e -> {
            try {
                UserSecurityProfile draft = new UserSecurityProfile();
                draft.setLoginEnabled(loginEnabled.isSelected());
                draft.setRequireMfa(requireMfa.isSelected());
                draft.setAllowRecoveryCode(allowRecovery.isSelected());
                draft.setMaxLoginAttempts(maxAttempts.getValue());
                draft.setLockoutMinutes(lockoutMinutes.getValue());
                draft.setSessionTimeoutMinutes(sessionTimeout.getValue());
                draft.setMaxConcurrentSessions(maxSessions.getValue());
                draft.setPasswordMinLength(passwordLength.getValue());
                draft.setPasswordRequireUpper(requireUpper.isSelected());
                draft.setPasswordRequireLower(requireLower.isSelected());
                draft.setPasswordRequireDigit(requireDigit.isSelected());
                draft.setPasswordRequireSymbol(requireSymbol.isSelected());
                draft.setPasswordExpiryDays(passwordExpiry.getValue());
                draft.setLoginStart(parseTime(loginStart.getText(), "Início do login"));
                draft.setLoginEnd(parseTime(loginEnd.getText(), "Fim do login"));

                draft.setAllowedCompanyIds(companyChecks.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));

                draft.setAllowedModules(moduleChecks.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));

                draft.setAllowedIpRanges(lines(ipRules.getText()));
                draft.setAllowedWeekDays(days.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
                draft.setAllowedCriticalOperations(lines(criticalOps.getText()));

                draft.setFinancialOperationLimit(decimalValue(operationLimit.getText()));
                draft.setFinancialDailyLimit(decimalValue(dailyLimit.getText()));
                draft.setFinancialCurrency(currency.getText());

                service.saveProfile(
                        sessionManager.getUser(),
                        user.getId(),
                        draft,
                        "127.0.0.1"
                );

                modalManager.hideModal();
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "Perfil de segurança",
                        "Não foi possível guardar a política individual.",
                        ex
                );
            }
        });

        VBox wrapper = new VBox(10, content, new Separator(), actions);
        VBox.setVgrow(content, Priority.ALWAYS);

        modalManager.showModal(
                wrapper,
                new ModalManager.ModalConfig()
                        .title("Perfil de segurança")
                        .subtitle(user.getNome() + " · política individual")
                        .icon(Feather.SHIELD)
                        .tone(ModalManager.ModalTone.INFO)
                        .size(820, 780)
                        .minSize(700, 620)
                        .maximizable(true)
                        .minimizable(false)
        );
    }

    private Label header(User user) {
        Label label = new Label(
                "Política individual · " + user.getEmail()
                        + "\nAplicável apenas a utilizadores comuns. Não concede funções; restringe o acesso já definido pelos Perfis de Acesso/RBAC."
        );
        label.setWrapText(true);
        label.getStyleClass().add("kubata-users-modal-row");
        label.setPadding(new Insets(8));
        return label;
    }

    private VBox section(String title, Node... nodes) {
        VBox box = new VBox(8);
        box.getStyleClass().add("kubata-users-modal-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-users-section-title");
        box.getChildren().add(titleLabel);
        box.getChildren().addAll(nodes);
        return box;
    }

    private HBox row(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox label(String title, Node control) {
        Label label = new Label(title);
        label.setPrefWidth(185);
        return new HBox(6, label, control);
    }

    private CheckBox check(String text, boolean selected) {
        CheckBox check = new CheckBox(text);
        check.setSelected(selected);
        return check;
    }

    private Spinner<Integer> spinner(int value, int min, int max, int step) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value, step);
        Spinner<Integer> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
    }

    private TextArea textArea(Set<String> values, String prompt) {
        TextArea area = new TextArea(String.join(System.lineSeparator(), values));
        area.setPromptText(prompt);
        area.setWrapText(false);
        area.setPrefRowCount(3);
        return area;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("kubata-users-modal-row");
        return label;
    }

    private String formatTime(LocalTime value) {
        return value == null ? "" : value.toString();
    }

    private LocalTime parseTime(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " é obrigatório.");
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(label + " deve usar o formato HH:mm.");
        }
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal decimalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(value.trim().replace(",", "."));
            if (decimal.signum() < 0) {
                throw new IllegalArgumentException("Os limites financeiros não podem ser negativos.");
            }
            return decimal;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor financeiro inválido.");
        }
    }

    private Set<String> lines(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String line : value.split("\\R")) {
            if (!line.isBlank()) {
                result.add(line.trim());
            }
        }
        return result;
   }
}
