package ao.allon.kubata.rh.ui.loading;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.List;
import java.util.function.Consumer;

public class SplashScreen {
    private Stage stage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Consumer<Void> onComplete;

    public void show() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        // Logo placeholder (pode ser substituído por imagem real)
        ImageView logo = new ImageView();
        try {
            // Tenta carregar logo se existir
            Image logoImage = new Image(getClass().getResourceAsStream("/images/kubata-logo.png"));
            if (!logoImage.isError()) {
                logo.setImage(logoImage);
                logo.setFitHeight(80);
                logo.setFitWidth(80);
            }
        } catch (Exception e) {
            // Se não encontrar logo, usa texto
            Label logoText = new Label("KUBATA RH");
            logoText.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            logoText.setTextFill(Color.valueOf("#2E86AB"));
            logo = null; // será tratado abaixo
        }

        Label titleLabel = new Label("Kubata Recursos Humanos");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.valueOf("#2E86AB"));

        statusLabel = new Label("Iniciando...");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setTextFill(Color.GRAY);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setProgress(0);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-border-radius: 10; -fx-background-radius: 10;");
        
        if (logo != null) {
            root.getChildren().add(logo);
        } else {
            // Adiciona texto do logo se imagem não encontrada
            root.getChildren().add(new Label("KUBATA RH"));
        }
        root.getChildren().addAll(titleLabel, statusLabel, progressBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();

        // Animação de fade-in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public void simulateLoading(List<String> steps, Consumer<Void> onComplete) {
        this.onComplete = onComplete;
        
        // Simulação em thread de background para não travar a UI
        new Thread(() -> {
            for (int i = 0; i < steps.size(); i++) {
                final int stepIndex = i;
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(steps.get(stepIndex));
                    progressBar.setProgress((double) (stepIndex + 1) / steps.size());
                });
                
                try {
                    Thread.sleep(300); // Simula tempo de processamento
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Finaliza splash
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                close();
            });
        }).start();
    }

    public void setOnComplete(Consumer<Void> onComplete) {
        this.onComplete = onComplete;
    }

    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    private void close() {
        if (stage != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                if (onComplete != null) {
                    onComplete.accept(null);
                }
                stage.close();
            });
            fadeOut.play();
        }
    }
}
