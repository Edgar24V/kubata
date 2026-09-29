package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import ao.allon.kubata.core.service.AcessoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Central de conformidade fiscal AGT / SAF-T AO.
 *
 * <p>A vista concentra as configurações operacionais que o Kubata utiliza
 * para facturação, exportação SAF-T, comunicação electrónica, contingência,
 * integridade e auditoria. As regras legais são apresentadas como referências
 * operacionais e não substituem a validação final junto da AGT.</p>
 */
@Component
public class FiscalAgtView extends VBox {

    private final ParametroSistemaRepository parametroRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;
    private final AcessoService acessoService;
    private final ModalManager modalManager;

    private final TextField txtSaft = new TextField();
    private final TextField txtIva = new TextField();
    private final ComboBox<String> cmbAmbiente = new ComboBox<>(
            FXCollections.observableArrayList("PRODUCAO", "TESTES")
    );
    private final ComboBox<String> cmbRegimeIva = new ComboBox<>(
            FXCollections.observableArrayList(
                    "Regime Geral",
                    "Regime Simplificado",
                    "Regime de Exclusão"
            )
    );

    private final TextField txtSoftwareNome = new TextField();
    private final TextField txtSoftwareVersao = new TextField();
    private final TextField txtSoftwareCert = new TextField();
    private final TextField txtSoftwareHash = new TextField();

    private final TextField txtEndpoint = new TextField();
    private final Spinner<Integer> spTimeout = new Spinner<>(5, 120, 30);
    private final CheckBox chkComunicacao = new CheckBox("Comunicação electrónica activa");
    private final CheckBox chkContingencia = new CheckBox("Permitir operação em contingência");
    private final CheckBox chkBackup = new CheckBox("Política de cópia de segurança configurada");
    private final TextField txtBackupPath = new TextField();
    private final TextField txtSaftExport = new TextField();

    private final Label complianceScore = new Label("0/8");
    private final Label complianceState = new Label("Configuração a validar");
    private final VBox checksBox = new VBox(8);
    private final Label lastSaved = new Label("Ainda não gravado nesta sessão.");

    private Button btnSave;
    private Button btnValidate;

    private boolean dataLoaded;

    public FiscalAgtView(
            ParametroSistemaRepository parametroRepository,
            PersistenceService persistenceService,
            SessionManager sessionManager,
            AcessoService acessoService,
            ModalManager modalManager
    ) {
        this.parametroRepository = parametroRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;
        this.acessoService = acessoService;
        this.modalManager = modalManager;

        setSpacing(0);
        getStyleClass().add("kubata-fiscal-agt-page");
        buildUi();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            load();
            refreshPermissions();
        }
    }

    private void buildUi() {
        getChildren().addAll(
                buildHeader(),
                buildComplianceStrip(),
                buildTabs(),
                buildFooter()
        );
        VBox.setVgrow(getChildren().get(2), Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-fiscal-agt-header");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-fiscal-agt-title-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.SHIELD, 22)));

        VBox titleBox = new VBox(2);
        Label title = new Label("Fiscal AGT");
        title.getStyleClass().add("kubata-fiscal-agt-title");

        Label subtitle = new Label(
                "Centro de conformidade para facturação, SAF-T AO, comunicação electrónica, "
                        + "contingência, integridade e auditoria."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-fiscal-agt-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnValidate = new Button(
                "Validar configuração",
                IconUtils.icon(Feather.CHECK_CIRCLE, 13)
        );
        btnValidate.getStyleClass().add("button-outlined");
        btnValidate.setOnAction(e -> validateConfiguration());

        btnSave = new Button(
                "Guardar alterações",
                IconUtils.icon(Feather.SAVE, 13)
        );
        btnSave.getStyleClass().add("button-primary");
        btnSave.setOnAction(e -> saveAll());

        line.getChildren().addAll(iconBox, titleBox, spacer, btnValidate, btnSave);
        header.getChildren().add(line);

        return header;
    }

    private HBox buildComplianceStrip() {
        HBox strip = new HBox(12);
        strip.setPadding(new Insets(0, 22, 16, 22));
        strip.getStyleClass().add("kubata-fiscal-agt-compliance-strip");

        VBox score = new VBox(2);
        score.setMinWidth(150);
        score.getStyleClass().add("kubata-fiscal-agt-score-card");

        Label scoreTitle = new Label("CONFORMIDADE LOCAL");
        scoreTitle.getStyleClass().add("kubata-fiscal-agt-kpi-label");
        complianceScore.getStyleClass().add("kubata-fiscal-agt-score");
        complianceState.getStyleClass().add("kubata-fiscal-agt-score-state");
        score.getChildren().addAll(scoreTitle, complianceScore, complianceState);

        VBox regime = miniInfo(
                "REGIME / AMBIENTE",
                () -> {
                    String regime = safe(cmbRegimeIva.getValue());
                    String ambiente = safe(cmbAmbiente.getValue());
                    return (regime.isBlank() ? "Não definido" : regime)
                            + " · "
                            + (ambiente.isBlank() ? "—" : ambiente);
                }
        );

        VBox software = miniInfo(
                "SOFTWARE",
                () -> {
                    String nome = safe(txtSoftwareNome.getText());
                    String versao = safe(txtSoftwareVersao.getText());
                    return (nome.isBlank() ? "Não identificado" : nome)
                            + (versao.isBlank() ? "" : " · " + versao);
                }
        );

        VBox communication = miniInfo(
                "COMUNICAÇÃO",
                () -> chkComunicacao.isSelected()
                        ? "Activada · configuração local"
                        : "Desactivada"
        );

        strip.getChildren().addAll(score, regime, software, communication);
        return strip;
    }

    private VBox miniInfo(String title, java.util.function.Supplier<String> valueSupplier) {
        VBox box = new VBox(3);
        box.getStyleClass().add("kubata-fiscal-agt-mini-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-fiscal-agt-kpi-label");
        Label value = new Label();
        value.getStyleClass().add("kubata-fiscal-agt-mini-value");
        value.setWrapText(true);
        value.textProperty().bind(
                new javafx.beans.binding.StringBinding() {
                    {
                        bind(cmbAmbiente.valueProperty(),
                                cmbRegimeIva.valueProperty(),
                                txtSoftwareNome.textProperty(),
                                txtSoftwareVersao.textProperty(),
                                chkComunicacao.selectedProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return valueSupplier.get();
                    }
                }
        );
        box.getChildren().addAll(titleLabel, value);
        return box;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("kubata-fiscal-agt-tabs");

        Tab overview = new Tab("Visão geral", buildOverview());
        Tab software = new Tab("Software & certificação", buildSoftwareTab());
        Tab electronic = new Tab("Facturação electrónica", buildElectronicTab());
        Tab saft = new Tab("SAF-T AO", buildSaftTab());
        Tab integrity = new Tab("Integridade & auditoria", buildIntegrityTab());

        tabs.getTabs().addAll(overview, software, electronic, saft, integrity);
        return tabs;
    }

    private ScrollPane buildOverview() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.CLIPBOARD,
                "Enquadramento fiscal",
                "Configure o contexto em que o sistema opera. O Decreto Presidencial n.º 71/25 "
                        + "estabelece regras sobre emissão, rectificação, anulação, conservação e "
                        + "arquivamento das facturas e documentos fiscalmente relevantes."
        ));

        GridPane grid = formGrid();
        addRow(grid, 0, "Regime de IVA", cmbRegimeIva);
        addRow(grid, 1, "Ambiente AGT", cmbAmbiente);
        addRow(grid, 2, "IVA padrão (%)", txtIva);

        Label currency = new Label(
                "Moeda operacional: Kz (AOA) para operações nacionais. "
                        + "Excepções de comércio internacional devem seguir o enquadramento aplicável."
        );
        currency.setWrapText(true);
        currency.getStyleClass().add("kubata-fiscal-agt-note");

        root.getChildren().addAll(grid, currency);

        root.getChildren().add(section(
                Feather.LAYERS,
                "Controlo de séries",
                "A numeração deve ser sequencial e cronológica por tipo de documento e exercício "
                        + "económico, podendo existir uma ou mais séries identificadas."
        ));

        root.getChildren().add(buildRuleCards(
                "Numeração sequencial",
                "O Kubata deve preservar a continuidade da sequência documental.",
                Feather.HASH,
                "Séries",
                "Gestão feita em Fiscal → Séries."
        ));

        root.getChildren().add(section(
                Feather.INFO,
                "Referência legal",
                "Base operacional: Decreto Presidencial n.º 71/25, de 20 de Março. "
                        + "A configuração do software não constitui certificação ou validação da AGT."
        ));

        return scroll(root);
    }

    private VBox buildSoftwareTab() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.CODE,
                "Identificação do software",
                "O regime exige software de facturação validado pela AGT para os casos abrangidos. "
                        + "Mantenha os identificadores usados pelo Kubata completos e auditáveis."
        ));

        GridPane grid = formGrid();
        addRow(grid, 0, "Nome do software", txtSoftwareNome);
        addRow(grid, 1, "Versão instalada", txtSoftwareVersao);
        addRow(grid, 2, "N.º certificação / validação", txtSoftwareCert);
        addRow(grid, 3, "Código hash", txtSoftwareHash);

        root.getChildren().add(grid);

        VBox warning = new VBox(6);
        warning.getStyleClass().add("kubata-fiscal-agt-warning-card");

        Label title = new Label("Regra de integridade");
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        Label text = new Label(
                "O software de facturação deve garantir numeração sequencial e cronológica "
                        + "e impedir a eliminação dos documentos depois de emitidos."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-fiscal-agt-card-text");

        warning.getChildren().addAll(title, text);
        root.getChildren().add(warning);

        Label hint = new Label(
                "Não introduza aqui uma certificação fictícia. O número/hash deve corresponder "
                        + "ao software efectivamente validado pela AGT, quando aplicável."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("kubata-fiscal-agt-note");
        root.getChildren().add(hint);

        return scroll(root);
    }

    private VBox buildElectronicTab() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.GLOBE,
                "Facturação electrónica",
                "Prepare o Kubata para emissão electrónica e transmissão das informações fiscalmente "
                        + "relevantes à AGT, de acordo com a fase de implementação aplicável ao contribuinte."
        ));

        GridPane grid = formGrid();
        addRow(grid, 0, "Endpoint AGT / gateway", txtEndpoint);

        spTimeout.setEditable(true);
        addRow(grid, 1, "Timeout de comunicação (s)", spTimeout);

        HBox flags = new HBox(
                16,
                chkComunicacao,
                chkContingencia
        );
        flags.setAlignment(Pos.CENTER_LEFT);
        flags.setPadding(new Insets(4, 0, 4, 0));

        root.getChildren().addAll(grid, flags);

        VBox contingency = new VBox(6);
        contingency.getStyleClass().add("kubata-fiscal-agt-info-card");

        Label title = new Label("Modo de contingência");
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        Label text = new Label(
                "Em caso de inoperacionalidade, a legislação prevê emissão em contingência "
                        + "em condições determinadas, incluindo posterior submissão/validação. "
                        + "O sistema deve identificar os documentos emitidos em contingência."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-fiscal-agt-card-text");

        contingency.getChildren().addAll(title, text);
        root.getChildren().add(contingency);

        return scroll(root);
    }

    private VBox buildSaftTab() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.FILE_TEXT,
                "Exportação SAF-T AO",
                "Defina a versão/esquema utilizado pelo exportador e os caminhos locais de trabalho. "
                        + "A exportação deve permanecer legível, íntegra e reproduzível."
        ));

        GridPane grid = formGrid();
        addRow(grid, 0, "Versão / esquema SAF-T", txtSaft);
        addRow(grid, 1, "Pasta de exportação SAF-T", txtSaftExport);

        root.getChildren().add(grid);

        root.getChildren().add(buildDeadlineCard());

        VBox technical = new VBox(7);
        technical.getStyleClass().add("kubata-fiscal-agt-info-card");

        Label title = new Label("Disponibilidade para fiscalização");
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        Label text = new Label(
                "O regime prevê acessibilidade e legibilidade dos dados pela AGT, inclusive "
                        + "mediante funções controladas e exportação de cópias exactas para suportes externos."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-fiscal-agt-card-text");

        technical.getChildren().addAll(title, text);
        root.getChildren().add(technical);

        return scroll(root);
    }

    private VBox buildIntegrityTab() {
        VBox root = contentRoot();

        root.getChildren().add(section(
                Feather.LOCK,
                "Integridade operacional",
                "O sistema deve proteger a integridade dos dados, detectar alterações não autorizadas, "
                        + "manter informação necessária à reconstituição do processamento e disponibilizar documentação técnica."
        ));

        VBox flags = new VBox(10);
        flags.getStyleClass().add("kubata-fiscal-agt-check-panel");

        CheckBox access = complianceCheck(
                "Controlo de acesso",
                "Funcionalidades fiscais protegidas por utilizador/perfil."
        );
        CheckBox tamper = complianceCheck(
                "Detecção de alterações",
                "Alterações relevantes devem ser identificáveis e auditáveis."
        );
        CheckBox backups = complianceCheck(
                "Cópias de segurança",
                "Existência de cópias de segurança dos dados fiscais."
        );
        CheckBox documentation = complianceCheck(
                "Documentação técnica",
                "Disponibilidade de documentação sobre funcionalidades, ciclos, controlo e dados."
        );

        // These checks represent the current application configuration/readiness,
        // rather than an external legal certification.
        access.setSelected(true);
        tamper.setSelected(true);
        backups.selectedProperty().bindBidirectional(chkBackup.selectedProperty());

        flags.getChildren().addAll(
                checkRow(access),
                checkRow(tamper),
                checkRow(backups),
                checkRow(documentation)
        );

        root.getChildren().add(flags);

        GridPane paths = formGrid();
        addRow(paths, 0, "Pasta de backups", txtBackupPath);
        root.getChildren().add(paths);

        VBox audit = new VBox(7);
        audit.getStyleClass().add("kubata-fiscal-agt-info-card");

        Label title = new Label("Auditoria");
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        Label text = new Label(
                "As alterações desta central devem ficar registadas no histórico da aplicação. "
                        + "A auditoria do Kubata não substitui os mecanismos técnicos e legais exigidos."
        );
        text.setWrapText(true);
        text.getStyleClass().add("kubata-fiscal-agt-card-text");

        audit.getChildren().addAll(title, text);
        root.getChildren().add(audit);

        root.getChildren().add(new Label("Diagnóstico de configuração"));
        checksBox.setFillWidth(true);
        root.getChildren().add(checksBox);

        return scroll(root);
    }

    private VBox buildDeadlineCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("kubata-fiscal-agt-deadline-card");

        Label title = new Label("Calendário de obrigações referido no diploma");
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        GridPane rows = new GridPane();
        rows.setHgap(18);
        rows.setVgap(7);

        addDeadline(rows, 0, "Inventário do exercício anterior", "Até 15 de Fevereiro");
        addDeadline(rows, 1, "Ficheiro de contabilidade SAF-T", "Até 10 de Abril");

        Label note = new Label(
                "Os prazos acima são os previstos no artigo 24.º para os ficheiros indicados; "
                        + "o sistema deve controlar o calendário aplicável ao contribuinte."
        );
        note.setWrapText(true);
        note.getStyleClass().add("kubata-fiscal-agt-note");

        card.getChildren().addAll(title, rows, note);
        return card;
    }

    private void addDeadline(GridPane grid, int row, String label, String date) {
        Label l = new Label(label);
        Label d = new Label(date);
        d.getStyleClass().add("kubata-fiscal-agt-deadline-value");
        grid.add(l, 0, row);
        grid.add(d, 1, row);
    }

    private VBox section(Feather icon, String title, String text) {
        VBox box = new VBox(4);
        box.getStyleClass().add("kubata-fiscal-agt-section");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 15));
        iconLabel.getStyleClass().add("kubata-fiscal-agt-section-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-fiscal-agt-section-title");

        heading.getChildren().addAll(iconLabel, titleLabel);

        Label body = new Label(text);
        body.setWrapText(true);
        body.getStyleClass().add("kubata-fiscal-agt-section-text");

        box.getChildren().addAll(heading, body);
        return box;
    }

    private VBox buildRuleCards(
            String title1,
            String text1,
            Feather icon1,
            String title2,
            String text2
    ) {
        VBox box = new VBox(8);
        box.getStyleClass().add("kubata-fiscal-agt-rule-card");

        HBox row1 = ruleRow(icon1, title1, text1);
        HBox row2 = ruleRow(Feather.SETTINGS, title2, text2);
        box.getChildren().addAll(row1, new Separator(), row2);

        return box;
    }

    private HBox ruleRow(Feather icon, String title, String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-fiscal-agt-rule-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 13)));

        VBox body = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-fiscal-agt-card-title");
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("kubata-fiscal-agt-card-text");

        body.getChildren().addAll(titleLabel, textLabel);
        HBox.setHgrow(body, Priority.ALWAYS);

        row.getChildren().addAll(iconBox, body);
        return row;
    }

    private CheckBox complianceCheck(String title, String subtitle) {
        CheckBox box = new CheckBox();
        box.setUserData(title + " — " + subtitle);
        return box;
    }

    private HBox checkRow(CheckBox check) {
        String value = String.valueOf(check.getUserData());
        String[] parts = value.split(" — ", 2);

        Label title = new Label(parts.length > 0 ? parts[0] : value);
        title.getStyleClass().add("kubata-fiscal-agt-card-title");

        Label subtitle = new Label(parts.length > 1 ? parts[1] : "");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-fiscal-agt-card-text");

        VBox text = new VBox(2, title, subtitle);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(10, check, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kubata-fiscal-agt-check-row");
        return row;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.getStyleClass().add("kubata-fiscal-agt-form-grid");

        ColumnConstraints label = new ColumnConstraints();
        label.setMinWidth(200);
        ColumnConstraints field = new ColumnConstraints();
        field.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, field);

        txtSaft.setPromptText("Ex.: SAF-T AO utilizado pelo exportador");
        txtIva.setPromptText("Ex.: 14");
        txtSoftwareNome.setPromptText("Nome comercial/técnico do software");
        txtSoftwareVersao.setPromptText("Ex.: 1.0.0");
        txtSoftwareCert.setPromptText("Número de validação/certificação, quando aplicável");
        txtSoftwareHash.setPromptText("Hash/código de identificação");
        txtEndpoint.setPromptText("https://...");
        txtBackupPath.setPromptText("Ex.: ./data/backups");
        txtSaftExport.setPromptText("Ex.: ./data/saft");

        for (TextField field : List.of(
                txtSaft, txtIva, txtSoftwareNome, txtSoftwareVersao,
                txtSoftwareCert, txtSoftwareHash, txtEndpoint, txtBackupPath, txtSaftExport
        )) {
            field.setMaxWidth(Double.MAX_VALUE);
        }

        cmbAmbiente.setMaxWidth(Double.MAX_VALUE);
        cmbRegimeIva.setMaxWidth(Double.MAX_VALUE);
        spTimeout.setMaxWidth(Double.MAX_VALUE);

        return grid;
    }

    private void addRow(GridPane grid, int row, String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("kubata-fiscal-agt-field-label");
        grid.add(l, 0, row);
        grid.add(control, 1, row);
    }

    private VBox contentRoot() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 22, 24, 22));
        root.getStyleClass().add("kubata-fiscal-agt-content");
        return root;
    }

    private ScrollPane scroll(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-fiscal-agt-scroll");
        return scroll;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 14, 8, 14));
        footer.getStyleClass().add("kubata-fiscal-agt-footer");

        lastSaved.getStyleClass().add("kubata-fiscal-agt-footer-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label legal = new Label(
                "Referência operacional · DP 71/25 · validar requisitos específicos com a AGT."
        );
        legal.getStyleClass().add("kubata-fiscal-agt-footer-text");

        footer.getChildren().addAll(lastSaved, spacer, legal);
        return footer;
    }

    private void load() {
        txtSaft.setText(val("SAFT_VERSAO"));
        txtIva.setText(val("IVA_PADRAO"));
        cmbAmbiente.setValue(
                val("AGT_AMBIENTE").isBlank() ? "PRODUCAO" : val("AGT_AMBIENTE")
        );
        txtSoftwareNome.setText(val("AGT_SOFTWARE_NOME"));
        txtSoftwareVersao.setText(val("AGT_SOFTWARE_VERSAO"));
        txtSoftwareCert.setText(val("AGT_SOFTWARE_CERT"));
        txtSoftwareHash.setText(val("AGT_SOFTWARE_HASH"));
        txtEndpoint.setText(val("AGT_ENDPOINT"));
        txtBackupPath.setText(val("AGT_BACKUP_DIR"));
        txtSaftExport.setText(val("SAFT_EXPORT_DIR"));
        cmbRegimeIva.setValue(
                val("FISCAL_REGIME_IVA").isBlank()
                        ? "Regime Geral"
                        : val("FISCAL_REGIME_IVA")
        );
        chkComunicacao.setSelected(Boolean.parseBoolean(val("AGT_COMUNICACAO_ATIVA")));
        chkContingencia.setSelected(Boolean.parseBoolean(val("AGT_CONTINGENCIA_ATIVA")));
        chkBackup.setSelected(Boolean.parseBoolean(val("AGT_BACKUP_CONFIGURADO")));

        String timeout = val("AGT_TIMEOUT_SEGUNDOS");
        if (!timeout.isBlank()) {
            try {
                spTimeout.getValueFactory().setValue(
                        Math.max(5, Math.min(120, Integer.parseInt(timeout)))
                );
            } catch (NumberFormatException ignored) {
            }
        }

        refreshCompliance();
    }

    private String val(String chave) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(chave)
                .map(ParametroSistema::getValor)
                .orElse("");
    }

    private void saveAll() {
        if (!hasEditPermission()) {
            deny("Não possui permissão para editar a configuração fiscal AGT.");
            return;
        }

        if (!validateFields()) {
            return;
        }

        persistenceService.executeAsync(
                () -> {
                    merge("SAFT_VERSAO", txtSaft.getText(), "STRING",
                            "Versão do esquema SAF-T AO exportado", "SISTEMA");

                    merge("IVA_PADRAO", txtIva.getText(), "DECIMAL",
                            "Taxa de IVA padrão (%)", "FISCAL");

                    merge("FISCAL_REGIME_IVA", cmbRegimeIva.getValue(), "STRING",
                            "Regime de IVA do contexto fiscal", "FISCAL");

                    merge("AGT_AMBIENTE", cmbAmbiente.getValue(), "STRING",
                            "Ambiente AGT", "FISCAL");

                    merge("AGT_SOFTWARE_NOME", txtSoftwareNome.getText(), "STRING",
                            "Nome do software de facturação", "FISCAL");

                    merge("AGT_SOFTWARE_VERSAO", txtSoftwareVersao.getText(), "STRING",
                            "Versão do software de facturação", "FISCAL");

                    merge("AGT_SOFTWARE_CERT", txtSoftwareCert.getText(), "STRING",
                            "Número de certificação ou validação AGT", "FISCAL");

                    merge("AGT_SOFTWARE_HASH", txtSoftwareHash.getText(), "STRING",
                            "Hash/código de identificação do software", "FISCAL");

                    merge("AGT_ENDPOINT", txtEndpoint.getText(), "STRING",
                            "Endpoint de integração AGT", "FISCAL");

                    merge("AGT_TIMEOUT_SEGUNDOS",
                            String.valueOf(spTimeout.getValue()),
                            "INTEGER",
                            "Timeout de comunicação AGT em segundos", "FISCAL");

                    merge("AGT_COMUNICACAO_ATIVA",
                            String.valueOf(chkComunicacao.isSelected()),
                            "BOOLEAN",
                            "Comunicação electrónica AGT activa", "FISCAL");

                    merge("AGT_CONTINGENCIA_ATIVA",
                            String.valueOf(chkContingencia.isSelected()),
                            "BOOLEAN",
                            "Operação em contingência habilitada", "FISCAL");

                    merge("AGT_BACKUP_CONFIGURADO",
                            String.valueOf(chkBackup.isSelected()),
                            "BOOLEAN",
                            "Política de backups fiscais configurada", "FISCAL");

                    merge("AGT_BACKUP_DIR", txtBackupPath.getText(), "STRING",
                            "Pasta de backups fiscais", "FISCAL");

                    merge("SAFT_EXPORT_DIR", txtSaftExport.getText(), "STRING",
                            "Pasta de exportação SAF-T AO", "SISTEMA");

                    registarAuditoria(
                            "UPDATE_FISCAL_AGT",
                            "Actualização da configuração fiscal AGT/SAF-T"
                    );
                },
                "UPDATE",
                "PARAMETRO_SISTEMA",
                "Gravação da configuração Fiscal AGT",
                () -> Platform.runLater(() -> {
                    load();
                    lastSaved.setText(
                            "Gravado em " + LocalDateTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            )
                    );
                    modalManager.alert(
                            "Configuração guardada",
                            "A configuração Fiscal AGT foi actualizada e registada na auditoria.",
                            "info",
                            null
                    );
                })
        );
    }

    private boolean validateFields() {
        List<String> errors = new ArrayList<>();

        if (cmbAmbiente.getValue() == null) {
            errors.add("Seleccione o ambiente AGT.");
        }
        if (cmbRegimeIva.getValue() == null) {
            errors.add("Seleccione o regime de IVA.");
        }

        String iva = safe(txtIva.getText());
        if (!iva.isBlank()) {
            try {
                double value = Double.parseDouble(iva.replace(',', '.'));
                if (value < 0 || value > 100) {
                    errors.add("A taxa de IVA deve estar entre 0 e 100.");
                }
            } catch (NumberFormatException ex) {
                errors.add("A taxa de IVA deve ser numérica.");
            }
        }

        if (spTimeout.getValue() == null || spTimeout.getValue() < 5 || spTimeout.getValue() > 120) {
            errors.add("O timeout deve estar entre 5 e 120 segundos.");
        }

        if (!errors.isEmpty()) {
            modalManager.alert(
                    "Configuração inválida",
                    String.join("\n", errors),
                    "warning",
                    null
            );
            return false;
        }

        return true;
    }

    private void validateConfiguration() {
        List<String> issues = new ArrayList<>();

        if (safe(txtSoftwareNome.getText()).isBlank()) {
            issues.add("Software: nome não identificado.");
        }
        if (safe(txtSoftwareVersao.getText()).isBlank()) {
            issues.add("Software: versão não identificada.");
        }
        if (safe(txtSoftwareCert.getText()).isBlank()) {
            issues.add("Software: certificação/validação não informada.");
        }
        if (safe(txtSoftwareHash.getText()).isBlank()) {
            issues.add("Software: hash/código não informado.");
        }
        if (chkComunicacao.isSelected() && safe(txtEndpoint.getText()).isBlank()) {
            issues.add("Comunicação: foi activada, mas não existe endpoint configurado.");
        }
        if (chkBackup.isSelected() && safe(txtBackupPath.getText()).isBlank()) {
            issues.add("Backup: marcado como configurado, mas sem pasta definida.");
        }
        if (safe(txtSaftExport.getText()).isBlank()) {
            issues.add("SAF-T: pasta de exportação ainda não definida.");
        }

        refreshCompliance();

        if (issues.isEmpty()) {
            modalManager.alert(
                    "Diagnóstico concluído",
                    "Não foram encontradas pendências básicas nesta central. "
                            + "Isto não constitui certificação nem confirmação de comunicação com a AGT.",
                    "info",
                    null
            );
        } else {
            modalManager.alert(
                    "Pendências encontradas",
                    String.join("\n", issues),
                    "warning",
                    null
            );
        }
    }

    private void refreshCompliance() {
        checksBox.getChildren().clear();

        List<ComplianceItem> items = List.of(
                new ComplianceItem(
                        "Ambiente e regime definidos",
                        cmbAmbiente.getValue() != null && cmbRegimeIva.getValue() != null
                ),
                new ComplianceItem(
                        "Software identificado",
                        !safe(txtSoftwareNome.getText()).isBlank()
                                && !safe(txtSoftwareVersao.getText()).isBlank()
                ),
                new ComplianceItem(
                        "Certificação / validação informada",
                        !safe(txtSoftwareCert.getText()).isBlank()
                ),
                new ComplianceItem(
                        "Hash / identificação informada",
                        !safe(txtSoftwareHash.getText()).isBlank()
                ),
                new ComplianceItem(
                        "Comunicação electrónica configurada",
                        !chkComunicacao.isSelected()
                                || !safe(txtEndpoint.getText()).isBlank()
                ),
                new ComplianceItem(
                        "Contingência parametrizada",
                        chkContingencia.isSelected()
                ),
                new ComplianceItem(
                        "Backup configurado",
                        chkBackup.isSelected()
                                && !safe(txtBackupPath.getText()).isBlank()
                ),
                new ComplianceItem(
                        "Exportação SAF-T preparada",
                        !safe(txtSaftExport.getText()).isBlank()
                )
        );

        int ok = 0;
        for (ComplianceItem item : items) {
            if (item.ok()) ok++;
            checksBox.getChildren().add(complianceRow(item));
        }

        complianceScore.setText(ok + "/" + items.size());
        complianceState.setText(
                ok == items.size()
                        ? "Pronto para revisão final"
                        : "Existem pendências de configuração"
        );
        complianceState.getStyleClass().removeAll(
                "kubata-fiscal-agt-score-good",
                "kubata-fiscal-agt-score-warning"
        );
        complianceState.getStyleClass().add(
                ok == items.size()
                        ? "kubata-fiscal-agt-score-good"
                        : "kubata-fiscal-agt-score-warning"
        );
    }

    private HBox complianceRow(ComplianceItem item) {
        Label icon = new Label(
                "",
                IconUtils.icon(
                        item.ok() ? Feather.CHECK_CIRCLE : Feather.ALERT_TRIANGLE,
                        14
                )
        );
        icon.getStyleClass().add(
                item.ok()
                        ? "kubata-fiscal-agt-check-ok"
                        : "kubata-fiscal-agt-check-warning"
        );

        Label label = new Label(item.label());
        label.getStyleClass().add("kubata-fiscal-agt-check-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label state = new Label(item.ok() ? "OK" : "PENDENTE");
        state.getStyleClass().add(
                item.ok()
                        ? "kubata-fiscal-agt-status-ok"
                        : "kubata-fiscal-agt-status-warning"
        );

        HBox row = new HBox(9, icon, label, spacer, state);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kubata-fiscal-agt-diagnostic-row");
        return row;
    }

    private record ComplianceItem(String label, boolean ok) {
    }

    private boolean hasEditPermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        return acessoService.temAcesso(
                user,
                "ADMINISTRATOR",
                "FISCAL_AGT",
                PermissaoPerfil.Operacao.EDITAR
        );
    }

    private void refreshPermissions() {
        boolean editable = hasEditPermission();

        if (btnSave != null) btnSave.setDisable(!editable);
        if (btnValidate != null) btnValidate.setDisable(!editable);

        setEditableState(editable);
    }

    private void setEditableState(boolean editable) {
        List<Control> controls = List.of(
                txtSaft, txtIva, cmbAmbiente, cmbRegimeIva,
                txtSoftwareNome, txtSoftwareVersao, txtSoftwareCert, txtSoftwareHash,
                txtEndpoint, spTimeout, chkComunicacao, chkContingencia,
                chkBackup, txtBackupPath, txtSaftExport
        );

        for (Control control : controls) {
            control.setDisable(!editable);
        }
    }

    private void merge(
            String chave,
            String valor,
            String tipo,
            String desc,
            String grupo
    ) {
        ParametroSistema p = parametroRepository
                .findByChaveAndEmpresaIdIsNull(chave)
                .orElseGet(() -> ParametroSistema.builder()
                        .chave(chave)
                        .tipoValor(tipo)
                        .descricao(desc)
                        .grupo(grupo)
                        .editavel(true)
                        .build()
                );

        p.setValor(valor == null ? "" : valor.trim());
        p.setTipoValor(tipo);
        p.setAtualizadoEm(LocalDateTime.now());

        if (sessionManager.getUser() != null) {
            p.setAtualizadoPor(sessionManager.getUser().getEmail());
        }

        parametroRepository.save(p);
    }

    private void registarAuditoria(String acao, String descricao) {
        try {
            acessoService.registrarAuditoria(
                    sessionManager.getUser(),
                    acao,
                    "FISCAL_AGT",
                    "127.0.0.1",
                    descricao,
                    true
            );
        } catch (Exception ignored) {
            // A auditoria não deve impedir a configuração fiscal principal.
        }
    }

    private void deny(String message) {
        modalManager.alert("Acesso negado", message, "warning", null);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
