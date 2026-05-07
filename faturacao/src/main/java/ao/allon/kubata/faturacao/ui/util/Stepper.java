package ao.allon.kubata.faturacao.ui.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.faturacao.ui.util.IconUtils;

public class Stepper {

    private final TabPane tabPane;

    public Stepper(TabPane tabPane) {
        this.tabPane = tabPane;
        configureStepper();
    }

    /**
     * Configurações iniciais do TabPane para otimizar o layout.
     */
    private void configureStepper() {
        tabPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    /**
     * Adiciona um novo passo ao TabPane.
     *
     * @param stepName Nome do passo.
     * @param icon     Ícone do passo.
     * @param form     O conteúdo (formulário) do passo, como um Node.
     */
    public void addStep(String stepName, Feather icon, Node form) {
        // Criação do passo com ícone e título
        Tab step = new Tab(stepName);
        step.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_MEDIUM));

        // Container para o formulário
        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        StackPane formContainer = new StackPane(scrollPane);
        formContainer.setAlignment(Pos.TOP_CENTER);
        formContainer.setStyle("-fx-padding: 20;"); // Adiciona espaçamento interno
        formContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Ajusta o conteúdo do passo
        step.setContent(formContainer);

        // Adiciona o passo ao Stepper
        tabPane.getTabs().add(step);
    }
}
