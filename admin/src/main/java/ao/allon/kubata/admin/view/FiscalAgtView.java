package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Parametrização fiscal AGT / SAFT-AO centralizada no Administrator.
 */
@Component
public class FiscalAgtView extends VBox {

    private final ParametroSistemaRepository parametroRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;

    private final TextField txtSaft = new TextField();
    private final TextField txtIva = new TextField();
    private final ComboBox<String> cmbAmbiente = new ComboBox<>(FXCollections.observableArrayList("PRODUCAO", "TESTES"));
    private final TextField txtCert = new TextField();

    public FiscalAgtView(ParametroSistemaRepository parametroRepository,
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
        Label title = new Label("AGT e SAFT", IconUtils.icon(Feather.FILE_TEXT, 18));
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
        grid.add(new Label("Versão SAFT (exportação)"), 0, r);
        grid.add(txtSaft, 1, r++);
        grid.add(new Label("IVA padrão (%)"), 0, r);
        grid.add(txtIva, 1, r++);
        grid.add(new Label("Ambiente AGT"), 0, r);
        grid.add(cmbAmbiente, 1, r++);
        grid.add(new Label("Caminho certificado AGT"), 0, r);
        grid.add(txtCert, 1, r++);

        Label hint = new Label("Estes campos reflectem-se em adm_parametro_sistema (grupos SISTEMA / FISCAL). Alterações ficam sujeitas a auditoria.");
        hint.setWrapText(true);
        hint.getStyleClass().add("text-muted");

        getChildren().addAll(head, grid, hint);
    }

    private void load() {
        txtSaft.setText(val("SAFT_VERSAO"));
        txtIva.setText(val("IVA_PADRAO"));
        String amb = val("AGT_AMBIENTE");
        cmbAmbiente.setValue(amb.isBlank() ? "PRODUCAO" : amb);
        txtCert.setText(val("AGT_CERT_PATH"));
    }

    private String val(String chave) {
        return parametroRepository.findByChaveAndEmpresaIdIsNull(chave).map(ParametroSistema::getValor).orElse("");
    }

    private void saveAll() {
        persistenceService.executeAsync(() -> {
            merge("SAFT_VERSAO", txtSaft.getText(), "STRING", "Versão do esquema SAF-T AO exportado", "SISTEMA");
            merge("IVA_PADRAO", txtIva.getText(), "DECIMAL", "Taxa de IVA padrão (%)", "FISCAL");
            merge("AGT_AMBIENTE", cmbAmbiente.getValue(), "STRING", "Ambiente AGT", "FISCAL");
            merge("AGT_CERT_PATH", txtCert.getText(), "STRING", "Caminho certificado AGT", "FISCAL");
        }, "UPDATE", "PARAMETRO_SISTEMA", "Gravacao parametros AGT/SAFT", () -> Platform.runLater(this::load));
    }

    private void merge(String chave, String valor, String tipo, String desc, String grupo) {
        ParametroSistema p = parametroRepository.findByChaveAndEmpresaIdIsNull(chave)
                .orElseGet(() -> ParametroSistema.builder()
                        .chave(chave)
                        .tipoValor(tipo)
                        .descricao(desc)
                        .grupo(grupo)
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
