package ao.allon.kubata.rh.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DashboardView extends VBox {

    public DashboardView() {
        setSpacing(20);
        setPadding(new Insets(20));
    }

    @PostConstruct
    public void init() {
        Platform.runLater(this::buildUI);
    }

    private void buildUI() {
        Label welcomeLabel = new Label("Bem-vindo ao Kubata RH");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label subtitleLabel = new Label("Sistema de Gestão de Recursos Humanos");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            grid.getColumnConstraints().add(col);
        }

        grid.add(createCard("Colaboradores Ativos", "0", "#e3f2fd", "#1976d2"), 0, 0);
        grid.add(createCard("Departamentos", "0", "#e8f5e8", "#388e3c"), 1, 0);
        grid.add(createCard("Contratos Ativos", "0", "#fff3e0", "#f57c00"), 2, 0);
        grid.add(createCard("Pedidos Pendentes", "0", "#fce4ec", "#c2185b"), 3, 0);
        
        Label footerLabel = new Label("Use o menu superior para acessar as funcionalidades do sistema.");
        footerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        
        getChildren().addAll(welcomeLabel, subtitleLabel, grid, footerLabel);
    }

    private VBox createCard(String title, String value, String bgColor, String textColor) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8;", bgColor));
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label lblValue = new Label(value);
        lblValue.setStyle(String.format("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s;", textColor));
        
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }
}
