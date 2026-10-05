package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.inventario.KubataInventarioApplication;
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
        applicationContext = new SpringApplicationBuilder(KubataInventarioApplication.class)
                .headless(false)
                .properties("spring.profiles.active=inventario")
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
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
