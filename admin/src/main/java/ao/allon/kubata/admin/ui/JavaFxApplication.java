package ao.allon.kubata.admin.ui;

import ao.allon.kubata.admin.KubataAdminApplication;
import ao.allon.kubata.admin.ui.util.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        GlobalExceptionHandler.setup(); // Configura o handler de exceções global
        applicationContext = new SpringApplicationBuilder(KubataAdminApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        applicationContext.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }
}
