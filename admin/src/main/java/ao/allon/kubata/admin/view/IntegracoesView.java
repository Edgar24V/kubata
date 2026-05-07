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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
/**
 * Parametrização de API REST e webhooks (valores em {@code adm_parametro_sistema}).
 */
@Component
public class IntegracoesView extends VBox {

    private final ParametroSistemaRepository parametroRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;

    private final CheckBox chkApi = new CheckBox("Activar Web API (REST)");
    private final TextField txtPort = new TextField();
    private final TextField txtApiKey = new TextField();
    private final TextField txtWebhookUrl = new TextField();
    private final TextField txtWebhookSecret = new TextField();

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
        javafx.scene.layout.Pane sp = new javafx.scene.layout.Pane();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnSave = new Button("Guardar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSave.getStyleClass().add("button-primary");
        btnSave.setOnAction(e -> saveAll());
        head.getChildren().addAll(title, sp, btnSave);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(14);
        grid.setVgap(12);
        grid.getStyleClass().add("card");

        int r = 0;
        grid.add(chkApi, 0, r++, 2, 1);
        grid.add(new Label("Porta API"), 0, r);
        grid.add(txtPort, 1, r++);
        grid.add(new Label("Chave API"), 0, r);
        grid.add(txtApiKey, 1, r++);
        grid.add(new Label("URL Webhook"), 0, r);
        grid.add(txtWebhookUrl, 1, r++);
        grid.add(new Label("Segredo Webhook"), 0, r);
        grid.add(txtWebhookSecret, 1, r++);

        Label hint = new Label("Os valores são persistidos como parâmetros de sistema (grupo INTEGRACAO) e auditados ao guardar.");
        hint.getStyleClass().add("text-muted");
        hint.setWrapText(true);

        getChildren().addAll(head, grid, hint);
    }

    private void load() {
        chkApi.setSelected("true".equalsIgnoreCase(val("INTEGRACAO_API_ENABLED")));
        txtPort.setText(val("INTEGRACAO_API_PORT"));
        txtApiKey.setText(val("INTEGRACAO_API_KEY"));
        txtWebhookUrl.setText(val("INTEGRACAO_WEBHOOK_URL"));
        txtWebhookSecret.setText(val("INTEGRACAO_WEBHOOK_SECRET"));
    }

    private String val(String chave) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(chave).map(ParametroSistema::getValor).orElse("");
    }

    private void saveAll() {
        persistenceService.executeAsync(() -> {
            mergeParam("INTEGRACAO_API_ENABLED", chkApi.isSelected() ? "true" : "false", "BOOLEAN", "Activar API REST de integracao");
            mergeParam("INTEGRACAO_API_PORT", txtPort.getText(), "STRING", "Porta do servico API");
            mergeParam("INTEGRACAO_API_KEY", txtApiKey.getText(), "STRING", "Chave de API");
            mergeParam("INTEGRACAO_WEBHOOK_URL", txtWebhookUrl.getText(), "STRING", "URL webhooks");
            mergeParam("INTEGRACAO_WEBHOOK_SECRET", txtWebhookSecret.getText(), "STRING", "Segredo webhooks");
        }, "UPDATE", "PARAMETRO_SISTEMA", "Gravacao integracao API/Webhooks", () -> Platform.runLater(this::load));
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
