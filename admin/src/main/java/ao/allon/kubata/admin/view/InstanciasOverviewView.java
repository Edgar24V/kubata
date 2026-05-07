package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Visão lógica de instância: perfis Spring e propriedades de arranque relevantes.
 */
@Component
public class InstanciasOverviewView extends VBox {

    public InstanciasOverviewView(Environment environment) {
        setSpacing(12);
        setPadding(new Insets(16));
        getStyleClass().add("application-view");

        Label title = new Label("Instância da aplicação", IconUtils.icon(Feather.CPU, 18));
        title.getStyleClass().add("h3");

        String profiles = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.joining(", "));
        if (profiles.isBlank()) {
            profiles = "(perfis por defeito — nenhum activo explícito)";
        }

        String app = environment.getProperty("spring.application.name", "kubata-admin");

        TextArea ta = new TextArea(
                "Nome da aplicação: " + app + "\n\n" +
                "Perfis activos: " + profiles + "\n\n" +
                "Para múltiplas instâncias em cluster, configure balanceador, sessões partilhadas e " +
                "a mesma base de dados; este painel apenas identifica a instância local."
        );
        ta.setEditable(false);
        ta.setWrapText(true);
        VBox.setVgrow(ta, Priority.ALWAYS);

        getChildren().addAll(title, ta);
    }
}
