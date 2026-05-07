package ao.allon.kubata.faturacao.ui;

import ao.allon.kubata.faturacao.KubataApplication;
import ao.allon.kubata.faturacao.ui.loading.SplashScreen;
import ao.allon.kubata.faturacao.ui.util.GlobalExceptionHandler;
import ao.allon.kubata.faturacao.ui.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private SplashScreen splashScreen;

    @Override
    public void init() {
        GlobalExceptionHandler.setup();
        // Mostrar splash screen antes de inicializar Spring
        Platform.runLater(() -> {
            splashScreen = new SplashScreen();
            splashScreen.show();
            
            // Simular etapas de carregamento
            List<String> steps = Arrays.asList(
                "Inicializando sistema...",
                "Carregando configurações...",
                "Conectando ao banco de dados...",
                "Verificando migrações...",
                "Inicializando serviços...",
                "Preparando interface...",
                "Pronto!"
            );
            
            splashScreen.simulateLoading(steps, null);
        });
        
        // Inicializar Spring em background
        applicationContext = new SpringApplicationBuilder(KubataApplication.class)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        // Aguardar splash screen terminar antes de mostrar main stage
        if (splashScreen != null && splashScreen.isShowing()) {
            splashScreen.setOnComplete(v -> {
                applicationContext.publishEvent(new StageReadyEvent(stage));
            });
        } else {
            applicationContext.publishEvent(new StageReadyEvent(stage));
        }
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}
