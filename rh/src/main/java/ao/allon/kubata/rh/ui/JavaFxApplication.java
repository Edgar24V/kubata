package ao.allon.kubata.rh.ui;

import ao.allon.kubata.rh.KubataRhApplication;
import ao.allon.kubata.rh.ui.util.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        GlobalExceptionHandler.setup();
        applicationContext = new SpringApplicationBuilder(KubataRhApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        try {
            // O estilo transparente deve ser definido antes de o Stage receber
            // a primeira Scene ou ser apresentado. O Login Central apenas usa
            // o Stage já configurado.
            stage.initStyle(StageStyle.TRANSPARENT);
            applicationContext.publishEvent(new StageReadyEvent(stage));
        } catch (Throwable error) {
            error.printStackTrace(System.err);
            throw error;
        }
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }
}
