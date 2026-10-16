package ao.allon.kubata.faturacao.ui.util;

import ao.allon.kubata.core.exception.AppBusinessException;
import javafx.application.Platform;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handler global para exceções não tratadas na UI JavaFX.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static void setup() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> handleException(t, e));
    }

    private static void handleException(Thread thread, Throwable throwable) {
        log.error("Exceção não tratada na thread {}: ", thread.getName(), throwable);

        if (Platform.isFxApplicationThread()) {
            showErrorDialog(throwable);
        } else {
            Platform.runLater(() -> showErrorDialog(throwable));
        }
    }

    private static void showErrorDialog(Throwable throwable) {
        if (throwable instanceof AppBusinessException
                || throwable.getCause() instanceof AppBusinessException) {
            Throwable business = throwable instanceof AppBusinessException
                    ? throwable
                    : throwable.getCause();
            AlertUtils.showError("Erro de Negócio", business.getMessage());
            return;
        }

        AlertUtils.showExceptionAlert(
                "Erro Inesperado",
                "Ocorreu um erro inesperado no sistema. Contacte o suporte se o problema persistir.",
                throwable
        );
    }
}
