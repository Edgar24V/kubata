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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Parametrização de API REST e webhooks.
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
    private final Label validationLabel = new Label();

    public IntegracoesView(ParametroSistemaRepository parametroRepository,
                           PersistenceService persistenceService,
                           SessionManager sessionManager) {
        this.parametroRepository = parametroRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;

        setSpacing(0);
        getStyleClass().add("application-view");
        buildUi();
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
        HBox head = new HBox(12);
        head.setPadding(new Insets(12, 16, 12, 16));
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("header-box");

        Label title = new Label("API e Webhooks", IconUtils.icon(Feather.SHARE_2, 18));
        title.getStyleClass().add("h3");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button("Guardar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSave.getStyleClass().add("button-primary");
        btnSave.setOnAction(e -> saveAll());

        head.getChildren().addAll(title, spacer, btnSave);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(14);
        grid.setVgap(12);
        grid.getStyleClass().add("card");

        int r = 0;

        grid.add(chkApi, 0, r++, 3, 1);

        Label apiPortLabel = new Label("Porta API");
        grid.add(apiPortLabel, 0, r);

        HBox portBox = new HBox(8, txtPort);
        HBox.setHgrow(txtPort, Priority.ALWAYS);
        txtPort.setPromptText("8080");
        txtPort.setPrefWidth(220);
        grid.add(portBox, 1, r++, 2, 1);

        Label apiKeyLabel = new Label("Chave API");
        grid.add(apiKeyLabel, 0, r);

        HBox apiKeyBox = new HBox(8);
        HBox.setHgrow(txtApiKey, Priority.ALWAYS);
        txtApiKey.setPromptText("KBT-...");
        Button btnGenerateKey = new Button("Gerar", IconUtils.icon(Feather.KEY, 12));
        btnGenerateKey.getStyleClass().add("button-outlined");
        btnGenerateKey.setOnAction(e -> txtApiKey.setText(generateToken("KBT-", 32)));
        apiKeyBox.getChildren().addAll(txtApiKey, btnGenerateKey);
        grid.add(apiKeyBox, 1, r++, 2, 1);

        Label apiBase = new Label("API v1: http://127.0.0.1:<porta>/api/v1");
        apiBase.getStyleClass().add("text-muted");
        grid.add(apiBase, 1, r++, 2, 1);

        Label webhookUrlLabel = new Label("URL Webhook");
        grid.add(webhookUrlLabel, 0, r);

        txtWebhookUrl.setPromptText("https://exemplo.tld/webhooks/kubata");
        grid.add(txtWebhookUrl, 1, r++, 2, 1);
        HBox.setHgrow(txtWebhookUrl, Priority.ALWAYS);

        Label webhookSecretLabel = new Label("Segredo Webhook");
        grid.add(webhookSecretLabel, 0, r);

        HBox secretBox = new HBox(8);
        HBox.setHgrow(txtWebhookSecret, Priority.ALWAYS);
        Button btnGenerateSecret = new Button("Gerar", IconUtils.icon(Feather.LOCK, 12));
        btnGenerateSecret.getStyleClass().add("button-outlined");
        btnGenerateSecret.setOnAction(e -> txtWebhookSecret.setText(generateToken("", 64)));
        secretBox.getChildren().addAll(txtWebhookSecret, btnGenerateSecret);
        grid.add(secretBox, 1, r++, 2, 1);

        validationLabel.getStyleClass().add("text-muted");
        validationLabel.setWrapText(true);
        validationLabel.setText(
                "A API usa X-API-Key ou Authorization: ApiKey <chave>. "
                        + "Os Webhooks são assinados com HMAC-SHA256."
        );

        getChildren().addAll(head, grid, validationLabel);
    }

    private void load() {
        chkApi.setSelected("true".equalsIgnoreCase(val("INTEGRACAO_API_ENABLED")));
        txtPort.setText(val("INTEGRACAO_API_PORT"));
        txtApiKey.setText(val("INTEGRACAO_API_KEY"));
        txtWebhookUrl.setText(val("INTEGRACAO_WEBHOOK_URL"));
        txtWebhookSecret.setText(val("INTEGRACAO_WEBHOOK_SECRET"));
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
            validationLabel.setText(validationError);
            validationLabel.getStyleClass().remove("text-muted");
            if (!validationLabel.getStyleClass().contains("text-danger")) {
                validationLabel.getStyleClass().add("text-danger");
            }
            return;
        }

        validationLabel.getStyleClass().remove("text-danger");
        if (!validationLabel.getStyleClass().contains("text-muted")) {
            validationLabel.getStyleClass().add("text-muted");
        }
        validationLabel.setText("A guardar configurações de API e Webhooks...");

        persistenceService.executeAsync(() -> {
            mergeParam("INTEGRACAO_API_ENABLED",
                    apiEnabled ? "true" : "false",
                    "BOOLEAN",
                    "Activar API REST de integracao");

            mergeParam("INTEGRACAO_API_PORT",
                    port,
                    "STRING",
                    "Porta do servico API");

            mergeParam("INTEGRACAO_API_KEY",
                    apiKey,
                    "STRING",
                    "Chave de API");

            mergeParam("INTEGRACAO_WEBHOOK_URL",
                    webhookUrl,
                    "STRING",
                    "URL webhooks");

            mergeParam("INTEGRACAO_WEBHOOK_SECRET",
                    webhookSecret,
                    "STRING",
                    "Segredo webhooks");
        }, "UPDATE", "PARAMETRO_SISTEMA",
                "Gravacao integracao API/Webhooks",
                () -> Platform.runLater(() -> {
                    load();
                    validationLabel.getStyleClass().remove("text-danger");
                    if (!validationLabel.getStyleClass().contains("text-muted")) {
                        validationLabel.getStyleClass().add("text-muted");
                    }
                    validationLabel.setText(
                            "Configurações guardadas. A API usa /api/v1 e os Webhooks ficaram prontos para entrega."
                    );
                }));
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

    private String generateToken(String prefix, int hexLength) {
        byte[] bytes = new byte[(hexLength + 1) / 2];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        return prefix + token.substring(0, hexLength).toUpperCase();
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
}
