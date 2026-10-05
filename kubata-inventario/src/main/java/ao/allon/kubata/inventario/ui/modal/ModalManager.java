package ao.allon.kubata.inventario.ui.modal;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ModalManager {

    private Window owner(Node node) {
        return node == null || node.getScene() == null ? null : node.getScene().getWindow();
    }

    public void info(Node ownerNode, String title, String message) {
        show(ownerNode, Alert.AlertType.INFORMATION, title, message);
    }

    public void error(Node ownerNode, String title, String message) {
        show(ownerNode, Alert.AlertType.ERROR, title, message);
    }

    public boolean confirm(Node ownerNode, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = owner(ownerNode);
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    public Optional<ButtonType> show(
            Node ownerNode,
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Window owner = owner(ownerNode);
        if (owner != null) {
            alert.initOwner(owner);
        }

        return alert.showAndWait();
    }

    public Dialog<ButtonType> form(
            Node ownerNode,
            String title,
            GridPane content) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.CANCEL,
                ButtonType.OK
        );

        Window owner = owner(ownerNode);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        return dialog;
    }
}
