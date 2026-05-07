package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.service.SaftAoExportService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.time.LocalDate;

@Controller
public class SaftExportController {

    private final SaftAoExportService saftService;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private Button btnExportar;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea txtLog;
    @FXML private Label lblStatus;

    public SaftExportController(SaftAoExportService saftService) {
        this.saftService = saftService;
    }

    @FXML
    public void initialize() {
        dpInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFim.setValue(LocalDate.now());
        progressBar.setVisible(false);
        lblStatus.setText("Pronto para exportar");
    }

    @FXML
    public void exportar() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        if (inicio == null || fim == null) {
            AlertUtils.showWarning("Datas Inválidas", "Selecione o período para exportação.");
            return;
        }

        if (inicio.isAfter(fim)) {
            AlertUtils.showWarning("Datas Inválidas", "A data inicial não pode ser posterior à data final.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Arquivo SAFT-AO");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        fileChooser.setInitialFileName("SAFT_AO_" + LocalDate.now() + ".xml");
        
        File file = fileChooser.showSaveDialog(btnExportar.getScene().getWindow());
        
        if (file != null) {
            runExportTask(file, inicio, fim);
        }
    }

    private void runExportTask(File file, LocalDate inicio, LocalDate fim) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Iniciando exportação...");
                updateProgress(0.1, 1.0);
                
                // Simulação de progresso ou chamada real
                // O serviço poderia reportar progresso se modificado, 
                // por enquanto chamamos a exportação direta
                updateMessage("Gerando XML...");
                saftService.exportFullSaft(file, inicio, fim);
                
                updateProgress(1.0, 1.0);
                updateMessage("Exportação concluída com sucesso!");
                return null;
            }
        };

        progressBar.visibleProperty().bind(task.runningProperty());
        progressBar.progressProperty().bind(task.progressProperty());
        
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            txtLog.appendText(newMsg + "\n");
            lblStatus.setText(newMsg);
        });

        task.setOnSucceeded(e -> {
            AlertUtils.showSuccess("Exportação Concluída", "O arquivo SAFT-AO foi gerado com sucesso em:\n" + file.getAbsolutePath());
            progressBar.visibleProperty().unbind();
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            AlertUtils.showExceptionAlert("Erro na Exportação", "Falha ao gerar arquivo SAFT-AO.", (Exception) ex);
            txtLog.appendText("ERRO: " + ex.getMessage() + "\n");
            progressBar.visibleProperty().unbind();
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });

        new Thread(task).start();
    }
}
