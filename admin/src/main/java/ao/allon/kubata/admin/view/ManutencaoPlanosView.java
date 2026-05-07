package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/**
 * Orientação para planos de manutenção (backup, Flyway, revisão de logs).
 */
@Component
public class ManutencaoPlanosView extends VBox {

    public ManutencaoPlanosView() {
        setSpacing(12);
        setPadding(new Insets(16));
        getStyleClass().add("application-view");

        Label title = new Label("Planos de manutenção", IconUtils.icon(Feather.CALENDAR, 18));
        title.getStyleClass().add("h3");

        String doc = """
                Recomendações mínimas para ambientes de produção:

                1. Backup completo da base de dados: diário (fora de horário de pico) e antes de cada actualização.
                2. Teste de restauro: mensal em ambiente de homologação.
                3. Migrações Flyway: aplicar apenas após backup verificado; rever scripts em ambiente de teste.
                4. Revisão de auditoria: semanal — separador "Auditoria" no Kubata Administrator.
                5. Rotação de chaves API / webhooks: trimestral ou após saída de pessoal com acesso.

                Utilize o separador Backup para operações de cópia de segurança integradas na aplicação.
                """;

        TextArea ta = new TextArea(doc);
        ta.setEditable(false);
        ta.setWrapText(true);
        VBox.setVgrow(ta, Priority.ALWAYS);

        getChildren().addAll(title, ta);
    }
}
