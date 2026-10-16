package ao.allon.kubata.faturacao.ui.modal;

import ao.allon.kubata.core.ui.modal.ModalManager;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Service;

@Service
public class ModalService {

    private final ModalManager modalManager;
    private StackPane rootPane;

    public ModalService(ModalManager modalManager) {
        this.modalManager = modalManager;
    }

    public void initialize(StackPane rootPane) {
        if (rootPane == null) {
            throw new IllegalArgumentException("A raiz da aplicação não pode ser nula.");
        }
        this.rootPane = rootPane;
        modalManager.setRoot(rootPane);
    }

    public ModalBuilder create() {
        if (rootPane == null) {
            throw new IllegalStateException(
                    "ModalService não foi inicializado. Chame initialize(rootPane) primeiro."
            );
        }
        return new ModalBuilder(this, rootPane);
    }

    void show(Node content, ModalManager.ModalConfig config) {
        modalManager.showModal(content, config);
    }

    void hide() {
        modalManager.hideModal();
    }
}
