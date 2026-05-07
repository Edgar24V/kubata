package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.service.SaftAoExportService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.time.LocalDate;

@Controller
public class SaftController {

    private final SaftAoExportService saftService;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private TextField txtVersao;
    @FXML private TextField txtTipo;
    @FXML private Button btnExportar;

    public SaftController(SaftAoExportService saftService) {
        this.saftService = saftService;
    }

    @FXML
    public void initialize() {
        dpInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFim.setValue(LocalDate.now());
        txtVersao.setText("1.01_01");
        txtTipo.setText("F - Faturação");
    }

    @FXML
    public void exportar() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        if (inicio == null || fim == null) {
            AlertUtils.showWarning("Validação", "Selecione as datas de início e fim.");
            return;
        }

        if (fim.isBefore(inicio)) {
            AlertUtils.showWarning("Validação", "A data final não pode ser anterior à data inicial.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Arquivo SAF-T (AO)");
        fileChooser.setInitialFileName("SAFT_AO_" + LocalDate.now() + ".xml");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showSaveDialog(dpInicio.getScene().getWindow());

        if (file != null) {
            try {
                saftService.exportFullSaft(file, inicio, fim);
                AlertUtils.showSuccess("Sucesso", "Arquivo SAF-T exportado com sucesso para:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro na Exportação", "Falha ao gerar o arquivo SAF-T.", e);
            }
        }
    }
}
