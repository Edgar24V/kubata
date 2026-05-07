package ao.allon.kubata.faturacao.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoadingScreen {

    private Stage loadingStage;
    private ProgressBar progressBar;
    private Label statusLabel;

    public LoadingScreen() {
        loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Loading...");

        progressBar = new ProgressBar();
        statusLabel = new Label("Processing...");

        VBox vbox = new VBox(10, progressBar, statusLabel);
        vbox.setStyle("-fx-padding: 10; -fx-alignment: center;");

        Scene scene = new Scene(vbox);
        loadingStage.setScene(scene);
    }

    public void show() {
        Platform.runLater(() -> loadingStage.show());
    }

    public void hide() {
        Platform.runLater(() -> loadingStage.hide());
    }

    public void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    public void setProgress(double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

    public void execute(Task<?> task) {
        task.setOnRunning(e -> show());
        task.setOnSucceeded(e -> hide());
        task.setOnFailed(e -> {
            hide();
            // Captura e trata a exceção na thread correta
            Platform.runLater(() -> showErrorDialog(task.getException()));
        });

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


    public Task<Void> createTask(Runnable process) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    process.run();
                } catch (Exception e) {
                    updateMessage("Error occurred");
                   throw e;
                }
                return null;
            }
        };
    }

    private void showErrorDialog(Throwable exception) {
        AlertUtils.showExceptionAlert("Erro durante o processamento", "Ocorreu um erro inesperado.", exception);
    }
}
