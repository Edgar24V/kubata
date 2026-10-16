package ao.allon.kubata.rh.ui.util;

import ao.allon.kubata.core.ui.modal.ModalManager;
import javafx.application.Platform;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    public static void setup() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);
            
            Platform.runLater(() -> {
                try {
                    ModalManager manager = ModalManager.getActiveInstance();
                    if (manager != null && manager.hasRoot()) {
                        manager.showErrorModal(
                                "Erro inesperado",
                                "Ocorreu um erro inesperado. Os detalhes técnicos foram registados.",
                                throwable
                        );
                    }
                } catch (Exception e) {
                    // Se a interface não estiver disponível, o registo técnico mantém-se no log.
                    LOGGER.log(Level.SEVERE, "Failed to show error modal", e);
                }
            });
        });
    }
}
