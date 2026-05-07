package ao.allon.kubata.faturacao.ui.util;

import ao.allon.kubata.core.exception.AppBusinessException;
import javafx.application.Platform;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

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
        // Se for uma exceção de negócio, mostra apenas a mensagem amigável
        if (throwable instanceof AppBusinessException || throwable.getCause() instanceof AppBusinessException) {
            Throwable business = (throwable instanceof AppBusinessException) ? throwable : throwable.getCause();
            AlertUtils.showError("Erro de Negócio", business.getMessage());
            return;
        }

        // Caso contrário, mostra o diálogo de erro fatal com detalhes técnicos ocultos por padrão
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Inesperado");
        alert.setHeaderText("Ocorreu um erro inesperado no sistema.");
        alert.setContentText("Por favor, contate o suporte técnico se o problema persistir.");

        // Criar seção expansível para o stacktrace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Detalhes do erro:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
}
