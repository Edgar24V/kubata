package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Administração da API REST e dos Webhooks do Kubata.
 *
 * A lógica de persistência e validação é mantida na própria classe.
 * A alteração concentra-se na organização visual e feedback da interface.
 */
@Component
public class IntegracoesView extends VBox {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParametroSistemaRepository parametroRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;

    private final CheckBox chkApi = new CheckBox("Activar Web API (REST)");
    private final TextField txtPort = new TextField();
    private final TextField txtApiKey = new TextField();
    private final TextField txtWebhookUrl = new TextField();
    private final PasswordField txtWebhookSecret = new PasswordField();

    private final Label apiStatus = new Label();
    private final Label webhookStatus = new Label();
    private final Label validationLabel = new Label();

    public IntegracoesView(
            ParametroSistemaRepository parametroRepository,
            PersistenceService persistenceService,
            SessionManager sessionManager) {

        this.parametroRepository = parametroRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;

        setSpacing(0);
        getStyleClass().add("application-view");

        buildUi();
        installUiListeners();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            load();
        }
    }

    private void buildUi() {
        HBox header = buildHeader();

        VBox content = new VBox(18);
        content.setPadding(new Insets(18, 20, 24, 20));
        content.setFillWidth(true);

        content.getChildren().addAll(
                buildOverview(),
                buildApiCard(),
                buildWebhookCard(),
                buildSecurityCard(),
                buildHelpCard(),
                buildValidationArea()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setMaxHeight(Double.MAX_VALUE);
        scroll.getStyleClass().add("application-scroll");

        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-box");

        VBox titleBox = new VBox(2);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.SHARE_2, 18));
        Label title = new Label("API e Webhooks");
        title.getStyleClass().add("h3");

        titleLine.getChildren().addAll(icon, title);

        Label subtitle = new Label("Integrações, credenciais e entrega de eventos do Kubata");
        subtitle.getStyleClass().add("text-muted");

        titleBox.getChildren().addAll(titleLine, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button(
                "Guardar alterações",
                IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL)
        );
        btnSave.getStyleClass().add("button-primary");
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> saveAll());

        header.getChildren().addAll(titleBox, spacer, btnSave);

        return header;
    }

    private HBox buildOverview() {
        HBox overview = new HBox(12);
        overview.setFillHeight(true);

        VBox api = buildStatusCard(
                "API REST",
                "Disponibiliza a API v1 para integrações externas.",
                apiStatus,
                Feather.SERVER
        );

        VBox webhook = buildStatusCard(
                "Webhooks",
                "Entrega eventos do Kubata para um sistema externo.",
                webhookStatus,
                Feather.RADIO
        );

        HBox.setHgrow(api, Priority.ALWAYS);
        HBox.setHgrow(webhook, Priority.ALWAYS);

        overview.getChildren().addAll(api, webhook);
        return overview;
    }

    private VBox buildStatusCard(
            String title,
            String description,
            Label statusLabel,
            Feather iconType) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(34, 34);
        iconBox.setPrefSize(34, 34);
        iconBox.setMaxSize(34, 34);
        iconBox.setStyle(
                "-fx-background-color: rgba(15,118,110,0.08);" +
                "-fx-background-radius: 9px;"
        );

        iconBox.getChildren().add(IconUtils.icon(iconType, 16));

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);

        textBox.getChildren().addAll(titleLabel, descLabel);

        top.getChildren().addAll(iconBox, textBox);

        statusLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 20px;" +
                "-fx-padding: 5px 9px;"
        );

        card.getChildren().addAll(top, statusLabel);
        return card;
    }

    private VBox buildApiCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("card");

        VBox heading = buildSectionHeading(
                Feather.SERVER,
                "Configuração da API REST",
                "Defina como o Kubata disponibiliza a API v1."
        );

        GridPane grid = createGrid();

        grid.add(createFieldLabel("Estado"), 0, 0);
        HBox apiToggle = new HBox(10, chkApi);
        apiToggle.setAlignment(Pos.CENTER_LEFT);
        grid.add(apiToggle, 1, 0);
        grid.add(createHint("Quando desactivada, os endpoints protegidos recusam chamadas externas."), 1, 1);

        grid.add(createFieldLabel("Porta"), 0, 2);
        txtPort.setPromptText("8080");
        txtPort.setPrefWidth(150);
        grid.add(txtPort, 1, 2);
        grid.add(createHint("Intervalo válido: 1–65535. Por defeito, 8080."), 1, 3);

        grid.add(createFieldLabel("API Key"), 0, 4);
        HBox keyBox = new HBox(8);
        keyBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtApiKey, Priority.ALWAYS);
        txtApiKey.setPromptText("KBT-...");
        Button generateKey = smallActionButton(
                "Gerar chave",
                Feather.KEY,
                () -> txtApiKey.setText(generateToken("KBT-", 32))
        );
        keyBox.getChildren().addAll(txtApiKey, generateKey);
        grid.add(keyBox, 1, 4);
        grid.add(createHint("Use X-API-Key ou Authorization: ApiKey &lt;chave&gt;."), 1, 5);

        HBox endpoint = new HBox(8);
        endpoint.setAlignment(Pos.CENTER_LEFT);

        Label endpointValue = new Label("http://127.0.0.1:<porta>/api/v1");
        endpointValue.getStyleClass().add("text-muted");

        Button copyLabel = smallActionButton(
                "Copiar",
                Feather.COPY,
                () -> copyToClipboard(endpointValue.getText())
        );

        endpoint.getChildren().addAll(endpointValue, copyLabel);

        grid.add(createFieldLabel("Base URL"), 0, 6);
        grid.add(endpoint, 1, 6);

        card.getChildren().addAll(heading, grid);
        return card;
    }

    private VBox buildWebhookCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("card");

        VBox heading = buildSectionHeading(
                Feather.RADIO,
                "Configuração dos Webhooks",
                "Receba automaticamente eventos do Kubata noutro sistema."
        );

        GridPane grid = createGrid();

        grid.add(createFieldLabel("URL de destino"), 0, 0);

        txtWebhookUrl.setPromptText("https://exemplo.tld/webhooks/kubata");
        HBox.setHgrow(txtWebhookUrl, Priority.ALWAYS);

        Button testButton = smallActionButton(
                "Testar",
                Feather.SEND,
                this::showWebhookTestInfo
        );

        HBox urlBox = new HBox(8);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtWebhookUrl, Priority.ALWAYS);
        urlBox.getChildren().addAll(txtWebhookUrl, testButton);

        grid.add(urlBox, 1, 0);
        grid.add(createHint("Aceites HTTP/HTTPS. HTTPS é recomendado para ambientes de produção."), 1, 1);

        grid.add(createFieldLabel("Segredo"), 0, 2);

        HBox secretBox = new HBox(8);
        secretBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtWebhookSecret, Priority.ALWAYS);

        txtWebhookSecret.setPromptText("Segredo HMAC");

        Button generateSecret = smallActionButton(
                "Gerar segredo",
                Feather.LOCK,
                () -> txtWebhookSecret.setText(generateToken("", 64))
        );

        secretBox.getChildren().addAll(txtWebhookSecret, generateSecret);

        grid.add(secretBox, 1, 2);
        grid.add(createHint("O Kubata assina o payload com HMAC-SHA256 antes de o enviar."), 1, 3);

        HBox events = new HBox(8);
        events.setAlignment(Pos.CENTER_LEFT);

        events.getChildren().addAll(
                eventChip("webhook.test"),
                eventChip("document.copy.created"),
                eventChip("document.third_party_issuance.registered")
        );

        grid.add(createFieldLabel("Eventos"), 0, 4);
        grid.add(events, 1, 4);

        card.getChildren().addAll(heading, grid);
        return card;
    }

    private VBox buildSecurityCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("card");

        VBox heading = buildSectionHeading(
                Feather.SHIELD,
                "Segurança",
                "Informações importantes para a utilização da integração."
        );

        HBox info1 = infoRow(
                Feather.LOCK,
                "API protegida",
                "A API v1 exige API Key quando está activa."
        );

        HBox info2 = infoRow(
                Feather.CODE,
                "Assinatura dos Webhooks",
                "Cada entrega inclui ID, timestamp e assinatura HMAC-SHA256."
        );

        HBox info3 = infoRow(
                Feather.REFRESH_CW,
                "Entrega resiliente",
                "Falhas temporárias entram em reenvio automático até ao limite configurado."
        );

        card.getChildren().addAll(heading, info1, info2, info3);
        return card;
    }

    private VBox buildHelpCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("card");

        VBox heading = buildSectionHeading(
                Feather.BOOK_OPEN,
                "Ajuda rápida",
                "Dados úteis para o administrador."
        );

        VBox help = new VBox(7);

        Label line1 = new Label("API:  /api/v1");
        Label line2 = new Label("OpenAPI:  /api/v1/openapi");
        Label line3 = new Label("Swagger UI:  /swagger-ui.html");
        Label line4 = new Label("Header:  X-API-Key");
        Label line5 = new Label("Webhook:  X-Kubata-Webhook-Signature");

        line1.getStyleClass().add("text-muted");
        line2.getStyleClass().add("text-muted");
        line3.getStyleClass().add("text-muted");
        line4.getStyleClass().add("text-muted");
        line5.getStyleClass().add("text-muted");

        help.getChildren().addAll(line1, line2, line3, line4, line5);
        card.getChildren().addAll(heading, help);

        return card;
    }

    private HBox buildValidationArea() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(11, 13, 11, 13));
        box.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4.5, Color.web("#64748b"));

        validationLabel.setWrapText(true);
        validationLabel.getStyleClass().add("text-muted");
        validationLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(validationLabel, Priority.ALWAYS);

        validationLabel.setText(
                "A API usa X-API-Key ou Authorization: ApiKey <chave>. "
                        + "Os Webhooks são assinados com HMAC-SHA256."
        );

        box.getStyleClass().add("card");
        box.getChildren().addAll(dot, validationLabel);

        return box;
    }

    private VBox buildSectionHeading(Feather iconType, String title, String description) {
        VBox box = new VBox(3);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(iconType, 16));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");

        titleLine.getChildren().addAll(icon, titleLabel);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("text-muted");
        descriptionLabel.setWrapText(true);

        box.getChildren().addAll(titleLine, descriptionLabel);
        return box;
    }

    private HBox infoRow(Feather iconType, String title, String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label icon = new Label("", IconUtils.icon(iconType, 14));

        VBox body = new VBox(2);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 12px;");

        Label description = new Label(text);
        description.getStyleClass().add("text-muted");
        description.setWrapText(true);

        body.getChildren().addAll(titleLabel, description);
        HBox.setHgrow(body, Priority.ALWAYS);

        row.getChildren().addAll(icon, body);
        return row;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(7);

        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(120);
        left.setPrefWidth(135);

        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");
        return label;
    }

    private Label createHint(String text) {
        Label hint = new Label(text);
        hint.getStyleClass().add("text-muted");
        hint.setStyle("-fx-font-size: 11px;");
        hint.setWrapText(true);
        return hint;
    }

    private Label eventChip(String value) {
        Label chip = new Label(value);
        chip.setStyle(
                "-fx-background-color: rgba(15,118,110,0.08);" +
                "-fx-text-fill: #0f766e;" +
                "-fx-background-radius: 18px;" +
                "-fx-padding: 5px 8px;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: 700;"
        );
        return chip;
    }

    private Button smallActionButton(String text, Feather iconType, Runnable action) {
        Button button = new Button(text, IconUtils.icon(iconType, 12));
        button.getStyleClass().add("button-outlined");
        button.setOnAction(e -> action.run());
        return button;
    }

    private void installUiListeners() {
        chkApi.selectedProperty().addListener((obs, oldValue, newValue) -> updateStatusLabels());

        txtWebhookUrl.textProperty().addListener((obs, oldValue, newValue) -> updateStatusLabels());
        txtWebhookSecret.textProperty().addListener((obs, oldValue, newValue) -> updateStatusLabels());
    }

    private void updateStatusLabels() {
        boolean apiEnabled = chkApi.isSelected();

        apiStatus.setText(apiEnabled ? "● API activa" : "● API desactivada");
        apiStatus.setStyle(
                apiEnabled
                        ? "-fx-text-fill: #087443; -fx-background-color: #ecfdf3;" +
                          "-fx-background-radius: 20px; -fx-padding: 5px 9px;" +
                          "-fx-font-size: 11px; -fx-font-weight: 800;"
                        : "-fx-text-fill: #64748b; -fx-background-color: #f1f5f9;" +
                          "-fx-background-radius: 20px; -fx-padding: 5px 9px;" +
                          "-fx-font-size: 11px; -fx-font-weight: 800;"
        );

        boolean webhookConfigured =
                txtWebhookUrl.getText() != null
                        && !txtWebhookUrl.getText().isBlank()
                        && txtWebhookSecret.getText() != null
                        && !txtWebhookSecret.getText().isBlank();

        webhookStatus.setText(webhookConfigured
                ? "● Webhook configurado"
                : "● Webhook não configurado");

        webhookStatus.setStyle(
                webhookConfigured
                        ? "-fx-text-fill: #087443; -fx-background-color: #ecfdf3;" +
                          "-fx-background-radius: 20px; -fx-padding: 5px 9px;" +
                          "-fx-font-size: 11px; -fx-font-weight: 800;"
                        : "-fx-text-fill: #64748b; -fx-background-color: #f1f5f9;" +
                          "-fx-background-radius: 20px; -fx-padding: 5px 9px;" +
                          "-fx-font-size: 11px; -fx-font-weight: 800;"
        );
    }

    private void load() {
        chkApi.setSelected("true".equalsIgnoreCase(val("INTEGRACAO_API_ENABLED")));
        txtPort.setText(val("INTEGRACAO_API_PORT"));
        txtApiKey.setText(val("INTEGRACAO_API_KEY"));
        txtWebhookUrl.setText(val("INTEGRACAO_WEBHOOK_URL"));
        txtWebhookSecret.setText(val("INTEGRACAO_WEBHOOK_SECRET"));

        updateStatusLabels();

        validationLabel.setText(
                "Configurações carregadas. Reveja os dados e utilize «Guardar alterações» para aplicar mudanças."
        );
    }

    private String val(String chave) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(chave)
                .map(ParametroSistema::getValor)
                .orElse("");
    }

    private void saveAll() {
        String port = txtPort.getText() == null ? "" : txtPort.getText().trim();
        String apiKey = txtApiKey.getText() == null ? "" : txtApiKey.getText().trim();
        String webhookUrl = txtWebhookUrl.getText() == null ? "" : txtWebhookUrl.getText().trim();
        String webhookSecret = txtWebhookSecret.getText() == null ? "" : txtWebhookSecret.getText().trim();
        boolean apiEnabled = chkApi.isSelected();

        String validationError = validate(port, apiKey, webhookUrl, webhookSecret, apiEnabled);

        if (validationError != null) {
            showValidation(validationError, true);
            return;
        }

        showValidation("A guardar configurações de API e Webhooks...", false);

        persistenceService.executeAsync(
                () -> {
                    mergeParam(
                            "INTEGRACAO_API_ENABLED",
                            apiEnabled ? "true" : "false",
                            "BOOLEAN",
                            "Activar API REST de integracao"
                    );

                    mergeParam(
                            "INTEGRACAO_API_PORT",
                            port,
                            "STRING",
                            "Porta do servico API"
                    );

                    mergeParam(
                            "INTEGRACAO_API_KEY",
                            apiKey,
                            "STRING",
                            "Chave de API"
                    );

                    mergeParam(
                            "INTEGRACAO_WEBHOOK_URL",
                            webhookUrl,
                            "STRING",
                            "URL webhooks"
                    );

                    mergeParam(
                            "INTEGRACAO_WEBHOOK_SECRET",
                            webhookSecret,
                            "STRING",
                            "Segredo webhooks"
                    );
                },
                "UPDATE",
                "PARAMETRO_SISTEMA",
                "Gravacao integracao API/Webhooks",
                () -> Platform.runLater(() -> {
                    load();
                    showValidation(
                            "Configurações guardadas com sucesso. A API usa /api/v1 e os Webhooks estão prontos.",
                            false
                    );
                })
        );
    }

    private String validate(
            String port,
            String apiKey,
            String webhookUrl,
            String webhookSecret,
            boolean apiEnabled) {

        if (port.isBlank()) {
            return "Informe a porta da API.";
        }

        try {
            int value = Integer.parseInt(port);

            if (value < 1 || value > 65535) {
                return "A porta da API deve estar entre 1 e 65535.";
            }
        } catch (NumberFormatException ex) {
            return "A porta da API deve ser numérica.";
        }

        if (apiEnabled && apiKey.isBlank()) {
            return "A API está activa: gere ou informe uma API Key.";
        }

        if (!webhookUrl.isBlank()) {
            try {
                URI uri = URI.create(webhookUrl);

                if (!"http".equalsIgnoreCase(uri.getScheme())
                        && !"https".equalsIgnoreCase(uri.getScheme())) {
                    return "A URL do Webhook deve usar HTTP ou HTTPS.";
                }

                if (uri.getHost() == null) {
                    return "A URL do Webhook não contém um host válido.";
                }
            } catch (IllegalArgumentException ex) {
                return "A URL do Webhook é inválida.";
            }

            if (webhookSecret.isBlank()) {
                return "Existe URL de Webhook: gere ou informe o segredo.";
            }
        } else if (!webhookSecret.isBlank()) {
            return "Existe segredo de Webhook sem URL configurada.";
        }

        return null;
    }

    private void showValidation(String message, boolean error) {
        validationLabel.setText(message);

        if (error) {
            if (!validationLabel.getStyleClass().contains("text-danger")) {
                validationLabel.getStyleClass().add("text-danger");
            }
            validationLabel.getStyleClass().remove("text-muted");
        } else {
            validationLabel.getStyleClass().remove("text-danger");
            if (!validationLabel.getStyleClass().contains("text-muted")) {
                validationLabel.getStyleClass().add("text-muted");
            }
        }
    }

    private void mergeParam(String chave, String valor, String tipo, String desc) {
        ParametroSistema p = parametroRepository.findByChaveAndEmpresaIdIsNull(chave)
                .orElseGet(() -> ParametroSistema.builder()
                        .chave(chave)
                        .tipoValor(tipo)
                        .descricao(desc)
                        .grupo("INTEGRACAO")
                        .editavel(true)
                        .build());

        p.setValor(valor);
        p.setTipoValor(tipo);
        p.setAtualizadoEm(LocalDateTime.now());

        if (sessionManager.getUser() != null) {
            p.setAtualizadoPor(sessionManager.getUser().getEmail());
        }

        parametroRepository.save(p);
    }

    private String generateToken(String prefix, int hexLength) {
        byte[] bytes = new byte[(hexLength + 1) / 2];
        RANDOM.nextBytes(bytes);

        String token = HexFormat.of()
                .formatHex(bytes)
                .toUpperCase();

        return prefix + token.substring(0, hexLength);
    }

    private void copyToClipboard(String value) {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(value);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);

        showValidation("Base URL copiada para a área de transferência.", false);
    }

    private void showWebhookTestInfo() {
        if (txtWebhookUrl.getText() == null || txtWebhookUrl.getText().isBlank()) {
            showValidation("Configure primeiro a URL do Webhook para poder testar a integração.", true);
            return;
        }

        showValidation(
                "O teste de entrega está disponível em POST /api/v1/webhooks/test após guardar as configurações.",
                false
        );
    }
}
