package ao.allon.kubata.core.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Ponte entre componentes partilhados (módulo core) e o sistema de modais da
 * aplicação hospedeira.
 *
 * <p>O módulo core não conhece o {@code ModalManager} de cada aplicação. Cada
 * aplicação regista aqui o seu {@link Handler}; assim, todos os diálogos
 * (confirmações, informações e erros) abrem como modais internos e nunca como
 * janelas nativas do sistema operativo. Se nenhum handler estiver registado
 * (testes, módulos sem modais), usa-se um {@link Alert} nativo como último
 * recurso.</p>
 *
 * <p>Deve ser chamada na thread JavaFX.</p>
 */
public final class DialogBridge {

    public interface Handler {
        /** @param type "info", "warning", "error" ou "success" */
        void message(String title, String message, String type);

        void confirm(String title, String message, Runnable onConfirm);
    }

    private static volatile Handler handler;

    private DialogBridge() {
    }

    public static void register(Handler newHandler) {
        handler = newHandler;
    }

    public static void info(String title, String message) {
        Handler h = handler;
        if (h != null) {
            h.message(title, message, "info");
            return;
        }
        nativeAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void error(String title, String message) {
        Handler h = handler;
        if (h != null) {
            h.message(title, message, "error");
            return;
        }
        nativeAlert(Alert.AlertType.ERROR, title, message);
    }

    public static void confirm(String title, String message, Runnable onConfirm) {
        Handler h = handler;
        if (h != null) {
            h.confirm(title, message, onConfirm);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait()
                .filter(button -> button == ButtonType.YES)
                .ifPresent(button -> onConfirm.run());
    }

    private static void nativeAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
