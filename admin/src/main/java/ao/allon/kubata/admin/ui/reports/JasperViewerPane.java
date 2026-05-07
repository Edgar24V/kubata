package ao.allon.kubata.admin.ui.reports;

import ao.allon.kubata.admin.ui.util.IconUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import org.kordamp.ikonli.feather.Feather;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class JasperViewerPane extends BorderPane {

    private final JasperPrint jasperPrint;
    private final ImageView imageView;
    private final ScrollPane scrollPane;
    private final Label lblPageStatus;
    private final Button btnPrev;
    private final Button btnNext;
    private final TextField txtZoom;
    
    private int currentPage = 0;
    private float zoom = 1.0f;
    private final float dpi = 150f;
    private final Map<Integer, Image> pageCache = new HashMap<>();

    public JasperViewerPane(JasperPrint jasperPrint) {
        this.jasperPrint = jasperPrint;
        this.imageView = new ImageView();
        this.scrollPane = new ScrollPane(new StackPane(imageView));
        this.lblPageStatus = new Label();
        this.btnPrev = new Button(null, IconUtils.icon(Feather.CHEVRON_LEFT, 16));
        this.btnNext = new Button(null, IconUtils.icon(Feather.CHEVRON_RIGHT, 16));
        this.txtZoom = new TextField("100%");

        setupUI();
        renderPage();
    }

    private void setupUI() {
        setStyle("-fx-background-color: #f4f4f4;");
        
        // --- Toolbar superior ---
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: white; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        Button btnPrint = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, 16));
        btnPrint.getStyleClass().add("button-outlined");
        btnPrint.setOnAction(e -> printReport());

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);
        
        btnPrev.setOnAction(e -> { if (currentPage > 0) { currentPage--; renderPage(); } });
        btnNext.setOnAction(e -> { if (currentPage < jasperPrint.getPages().size() - 1) { currentPage++; renderPage(); } });
        
        lblPageStatus.setStyle("-fx-font-size: 13px;");
        
        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);
        
        Button btnZoomOut = new Button(null, IconUtils.icon(Feather.MINUS, 14));
        btnZoomOut.setOnAction(e -> { zoom -= 0.1f; renderPage(); });
        
        txtZoom.setPrefWidth(60);
        txtZoom.setEditable(false);
        txtZoom.setAlignment(Pos.CENTER);
        
        Button btnZoomIn = new Button(null, IconUtils.icon(Feather.PLUS, 14));
        btnZoomIn.setOnAction(e -> { zoom += 0.1f; renderPage(); });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(btnPrint, sep1, btnPrev, lblPageStatus, btnNext, sep2, btnZoomOut, txtZoom, btnZoomIn, spacer);
        
        // --- Área central ---
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #525659;"); // Estilo "PDF viewer dark"
        
        ((StackPane)scrollPane.getContent()).setAlignment(Pos.CENTER);
        ((StackPane)scrollPane.getContent()).setPadding(new Insets(20));

        setTop(toolbar);
        setCenter(scrollPane);
    }

    private void renderPage() {
        if (jasperPrint.getPages().isEmpty()) return;

        txtZoom.setText((int)(zoom * 100) + "%");
        lblPageStatus.setText(String.format("Página %d de %d", currentPage + 1, jasperPrint.getPages().size()));
        
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage == jasperPrint.getPages().size() - 1);

        Task<Image> renderTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // JasperPrintManager usa DPI de 72 por padrão. Ajustamos para melhor qualidade.
                BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, currentPage, zoom * (dpi/72f));
                return SwingFXUtils.toFXImage(image, null);
            }
        };

        renderTask.setOnSucceeded(e -> imageView.setImage(renderTask.getValue()));
        renderTask.setOnFailed(e -> System.err.println("Erro ao renderizar página: " + renderTask.getException()));
        
        new Thread(renderTask).start();
    }

    private void printReport() {
        try {
            JasperPrintManager.printReport(jasperPrint, true);
        } catch (JRException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao imprimir: " + ex.getMessage());
            alert.show();
        }
    }
}
