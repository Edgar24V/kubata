package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Informação não sensível sobre a ligação JDBC principal (máscara de credenciais).
 */
@Component
public class DataServidorView extends VBox {

    public DataServidorView(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.driver-class-name:}") String driver) {
        setSpacing(12);
        setPadding(new Insets(16));
        getStyleClass().add("application-view");

        Label title = new Label("Servidor de dados (ligação principal)", IconUtils.icon(Feather.SERVER, 18));
        title.getStyleClass().add("h3");

        TextArea area = new TextArea(buildText(jdbcUrl, username, driver));
        area.setEditable(false);
        area.setWrapText(true);
        VBox.setVgrow(area, Priority.ALWAYS);

        Label foot = new Label("Password não é mostrada. Para alterar, use application.properties ou variáveis de ambiente.");
        foot.getStyleClass().add("text-muted");
        foot.setWrapText(true);

        getChildren().addAll(title, area, foot);
    }

    private static String buildText(String url, String user, String driver) {
        StringBuilder sb = new StringBuilder();
        sb.append("Driver: ").append(driver.isBlank() ? "(não definido)" : driver).append("\n\n");
        sb.append("JDBC URL:\n").append(url.isBlank() ? "(não definido)" : url).append("\n\n");
        sb.append("Utilizador BD: ").append(user.isBlank() ? "(vazio / integrado)" : user).append("\n");
        return sb.toString();
    }
}
