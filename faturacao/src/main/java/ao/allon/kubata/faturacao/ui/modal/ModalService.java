package ao.allon.kubata.faturacao.ui.modal;

import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ModalService {

    private StackPane rootPane;

    public void initialize(StackPane rootPane) {
        this.rootPane = rootPane;
    }

    public ModalBuilder create() {
        if (rootPane == null) {
            throw new IllegalStateException("ModalService não foi inicializado. Chame initialize(rootPane) primeiro.");
        }
        return new ModalBuilder(this, rootPane);
    }

    void show(CustomModal modal) {
        if (!rootPane.getChildren().contains(modal)) {
            String css = Objects.requireNonNull(getClass().getResource("custom-modal.css")).toExternalForm();
            modal.getStylesheets().add(css);
            rootPane.getChildren().add(modal);
        }
        modal.show();
    }
}
