package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.SerieDocumentoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SerieDocumentoWizardView extends VBox {

    private final SerieDocumentoService serieService;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;

    public SerieDocumentoWizardView(SerieDocumentoService serieService, EmpresaRepository empresaRepository,
                                  AcessoService acessoService, SessionManager sessionManager, ModalManager modalManager) {
        this.serieService = serieService;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
    }

    public void start() {
        if (empresaRepository.findFirstByAtivaTrue().isEmpty()) {
            modalManager.alert("Empresa Inativa", "Nenhuma empresa ativa selecionada. Por favor, ative uma empresa no menu 'Empresas' antes de gerir séries.", "warning", null);
            return;
        }
        showWizard();
    }

    private void showWizard() {
        ValidationSupport validationSupport = new ValidationSupport();
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(600);

        Label title = new Label("Assistente de Criação de Séries");
        title.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;");

        Label desc = new Label("Este assistente permite criar séries para múltiplos tipos de documentos de uma só vez.");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);

        // Seleção de Tipos
        ListView<TipoDocumentoSAFT> listTipos = new ListView<>(FXCollections.observableArrayList(TipoDocumentoSAFT.values()));
        listTipos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listTipos.getSelectionModel().selectAll();
        listTipos.setPrefHeight(150);

        // Parâmetros da Série
        TextField txtNomeSerie = new TextField(String.valueOf(LocalDate.now().getYear() + 1));
        txtNomeSerie.setPromptText("Ex: 2025");
        validationSupport.registerValidator(txtNomeSerie, Validator.createEmptyValidator("O nome da série é obrigatório"));

        TextField txtAno = new TextField(String.valueOf(LocalDate.now().getYear() + 1));
        validationSupport.registerValidator(txtAno, Validator.createRegexValidator("Ano inválido", "^[0-9]{4}$", org.controlsfx.validation.Severity.ERROR));

        // Série Base
        ComboBox<SerieDocumento> cmbBase = new ComboBox<>();
        cmbBase.setPromptText("Copiar atributos de uma série existente (opcional)");
        cmbBase.setMaxWidth(Double.MAX_VALUE);
        
        empresaRepository.findFirstByAtivaTrue().ifPresent(empresa -> {
            cmbBase.setItems(FXCollections.observableArrayList(serieService.listarPorEmpresa(empresa.getId())));
        });

        grid.add(new Label("Tipos de Documento:"), 0, 0);
        grid.add(listTipos, 1, 0);
        grid.add(new Label("Nome da Nova Série:"), 0, 1);
        grid.add(txtNomeSerie, 1, 1);
        grid.add(new Label("Ano Fiscal:"), 0, 2);
        grid.add(txtAno, 1, 2);
        grid.add(new Label("Série Base:"), 0, 3);
        grid.add(cmbBase, 1, 3);

        modalManager.showConfirmModal(grid, "Criação de Séries por Lote", () -> {
            if (validationSupport.isInvalid()) {
                modalManager.alert("Erro de Validação", "Por favor, corrija os erros nos campos destacados.", "error", null);
                return;
            }

            try {
                List<TipoDocumentoSAFT> selecionados = listTipos.getSelectionModel().getSelectedItems();
                if (selecionados.isEmpty()) {
                    modalManager.alert("Aviso", "Selecione pelo menos um tipo de documento.", "warning", null);
                    return;
                }

                String nomeSerie = txtNomeSerie.getText().trim().toUpperCase();
                int ano = Integer.parseInt(txtAno.getText().trim());
                Long baseId = cmbBase.getValue() != null ? cmbBase.getValue().getId() : null;

                Empresa empresa = empresaRepository.findFirstByAtivaTrue()
                        .orElseThrow(() -> new IllegalStateException("Nenhuma empresa ativa selecionada. Por favor, ative uma empresa antes de criar séries."));

                List<SerieDocumento> criadas = serieService.criarNovasSeries(empresa, selecionados, nomeSerie, ano, baseId);

                acessoService.registrarAuditoria(sessionManager.getUser(), "BATCH_CREATE", "SERIE",
                        "127.0.0.1", "Criadas " + criadas.size() + " séries documentais para o exercício " + ano, true);

                javafx.application.Platform.runLater(() -> {
                    modalManager.alert("Sucesso", "Foram criadas " + criadas.size() + " séries com sucesso para o exercício " + ano + ".", "info", null);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                modalManager.alert("Erro", "Erro ao processar séries: " + ex.getMessage(), "error", ex);
            }
        }, null);
    }
}
