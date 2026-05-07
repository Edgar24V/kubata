package ao.allon.kubata.faturacao.ui.util;


import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoadingSpinner {

    private StackPane stackPane;
    private ProgressIndicator progressIndicator;
    private Rectangle overlay;
    private Label loadingLabel;

    public LoadingSpinner(StackPane stackPane) {
        this.stackPane = stackPane;
        this.progressIndicator = new ProgressIndicator();
        this.progressIndicator.setStyle(
                "-fx-progress-color: #415bff; " +
                        "-fx-pref-width: 100px; " +
                        "-fx-pref-height: 100px;"
        );

        // Criar um retângulo semitransparente para servir como overlay
        overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.5)); // Cor preta com 50% de opacidade
        overlay.widthProperty().bind(stackPane.widthProperty());
        overlay.heightProperty().bind(stackPane.heightProperty());

        // Criar label para texto de carregamento
        loadingLabel = new Label("Carregando, aguarde...");
        loadingLabel.setTextFill(Color.WHITE);
        loadingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
    }

    // Método start apenas com callback de sucesso
    public void start(Runnable action, Runnable onSuccess) {
        start(action, onSuccess, null);
    }

    // Método start com callbacks de sucesso e falha
    public void start(Runnable action, Runnable onSuccess, Runnable onFailure) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Adiciona o spinner de loading na thread de UI
                Platform.runLater(() -> {
                    stackPane.getChildren().addAll(overlay, progressIndicator, loadingLabel);
                    StackPane.setAlignment(progressIndicator, Pos.CENTER);
                    StackPane.setAlignment(loadingLabel, Pos.CENTER);
                    StackPane.setMargin(loadingLabel, new javafx.geometry.Insets(80, 0, 0, 0));
                });

                // Executa a ação passada em background
                try {
                    // Isso será executado na thread de background, já que não está envolvido por Platform.runLater
                    action.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    // Lança uma exceção para ser capturada pelo método failed
                    throw new RuntimeException(e);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                // Remove o spinner de loading e executa o callback de sucesso na thread de UI
                Platform.runLater(() -> {
                    stackPane.getChildren().removeAll(overlay, progressIndicator, loadingLabel);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            }

            @Override
            protected void failed() {
                super.failed();
                // Remove o spinner de loading e mostra o alerta de erro na thread de UI
                Platform.runLater(() -> {
                    stackPane.getChildren().removeAll(overlay, progressIndicator, loadingLabel);
                    AlertUtils.showExceptionAlert("Erro durante a operação", "Ocorreu um erro inesperado.", getException());
                    if (onFailure != null) {
                        onFailure.run();
                    }
                });
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                // Remove o spinner de loading e mostra alerta de cancelamento na thread de UI
                Platform.runLater(() -> {
                    stackPane.getChildren().removeAll(overlay, progressIndicator, loadingLabel);
                    AlertUtils.showWarningAlert("Operação cancelada", "A operação foi cancelada.");
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


}
