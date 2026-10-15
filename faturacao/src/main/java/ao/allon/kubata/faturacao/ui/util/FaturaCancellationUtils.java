package ao.allon.kubata.faturacao.ui.util;

import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;

/** Diálogo único para exigir selecção explícita do motivo fiscal da anulação. */
public final class FaturaCancellationUtils {

    private FaturaCancellationUtils() {}

    public static Optional<DocumentCancelReason> escolherMotivoFiscal() {
        Dialog<DocumentCancelReason> dialog = new Dialog<>();
        dialog.setTitle("Motivo fiscal da anulação");
        dialog.setHeaderText("Seleccione um dos motivos fiscais autorizados.");

        ComboBox<DocumentCancelReason> motivos = new ComboBox<>();
        motivos.getItems().setAll(DocumentCancelReason.values());
        motivos.setPromptText("Seleccione o motivo");
        motivos.setMaxWidth(Double.MAX_VALUE);

        ButtonType confirmar = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(confirmar, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10,
                new Label("A anulação será registada no documento e comunicada à AGT quando configurada."),
                motivos));
        Node botaoConfirmar = dialog.getDialogPane().lookupButton(confirmar);
        botaoConfirmar.setDisable(true);
        motivos.valueProperty().addListener((obs, anterior, actual) ->
                botaoConfirmar.setDisable(actual == null));
        dialog.setResultConverter(botao -> botao == confirmar ? motivos.getValue() : null);
        return dialog.showAndWait();
    }
}
