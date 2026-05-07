package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.ui.components.VirtualKeyboardPopup;
import atlantafx.base.theme.Styles;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class SplitPaymentDialog extends VBox {


    private final BigDecimal totalAmount;
    private final Consumer<Map<MetodoPagamento, BigDecimal>> onConfirm;
    private final Runnable onCancel;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));
    
    private final Map<MetodoPagamento, TextField> methodInputs = new HashMap<>();
    private Label lblRemaining;
    private Button btnConfirm;

    private static final MetodoPagamento[] SUPPORTED = new MetodoPagamento[] {
            MetodoPagamento.DINHEIRO,
            MetodoPagamento.CARTAO_POS,
            MetodoPagamento.TRANSFERENCIA_BANCARIA,
            MetodoPagamento.MOBILE_MONEY,
            MetodoPagamento.CHEQUE
    };

    public SplitPaymentDialog(BigDecimal totalAmount, Consumer<Map<MetodoPagamento, BigDecimal>> onConfirm, Runnable onCancel) {
        this.totalAmount = totalAmount;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        
        getStyleClass().add("payment-dialog");
        setSpacing(14);
        setPadding(new Insets(18));
        setAlignment(Pos.CENTER);
        setPrefWidth(520);
        setMaxWidth(560);

        // Header
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label lblTitle = new Label("Pagamento Misto / Dividido");
        lblTitle.getStyleClass().add(Styles.TITLE_4);
        
        Label lblAmount = new Label(currencyFormat.format(totalAmount));
        lblAmount.setStyle("-fx-font-size: 2.1em; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        
        header.getChildren().addAll(lblTitle, lblAmount);

        // Payment Inputs
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.NEVER);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.NEVER);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().setAll(c0, c1, c2, c3);

        Label hMetodo = new Label("Método");
        hMetodo.getStyleClass().addAll(Styles.TEXT_MUTED);
        Label hValor = new Label("Valor");
        hValor.getStyleClass().addAll(Styles.TEXT_MUTED);
        grid.addRow(0, hMetodo, hValor);
        
        int row = 1;
        for (MetodoPagamento metodo : SUPPORTED) {
            if (metodo == MetodoPagamento.MISTO) continue;

            Label lbl = new Label(formatEnum(metodo));
            lbl.getStyleClass().add(Styles.TEXT_BOLD);
            
            TextField txt = new TextField();
            txt.setPromptText("0,00");
            txt.setPrefWidth(170);
            txt.setTextFormatter(new TextFormatter<>(change -> {
                String nt = change.getControlNewText();
                return nt.matches("\\d{0,12}([\\.,]\\d{0,2})?") ? change : null;
            }));
            
            txt.textProperty().addListener((obs, old, newVal) -> updateRemaining());

            txt.focusedProperty().addListener((o, ov, nv) -> {
                if (nv) VirtualKeyboardPopup.show(txt, VirtualKeyboardPopup.KeyboardType.NUMERIC);
            });

            methodInputs.put(metodo, txt);
            
            grid.add(lbl, 0, row);
            grid.add(txt, 1, row);
            GridPane.setHgrow(txt, Priority.ALWAYS);
            
            // Quick fill button (fills remaining amount)
            Button btnFill = new Button(null, new FontIcon(Feather.ARROW_LEFT));
            btnFill.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
            btnFill.setTooltip(new Tooltip("Preencher com restante"));
            btnFill.setOnAction(e -> fillRemaining(txt));

            Button btnPad = new Button(null, new FontIcon(Feather.TYPE));
            btnPad.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
            btnPad.setTooltip(new Tooltip("Teclado"));
            btnPad.setOnAction(e -> VirtualKeyboardPopup.show(txt, VirtualKeyboardPopup.KeyboardType.NUMERIC));

            grid.add(btnFill, 2, row);
            grid.add(btnPad, 3, row);
            
            row++;
        }

        // Remaining Amount Display
        HBox remainingBox = new HBox(10);
        remainingBox.setAlignment(Pos.CENTER);
        Label lblRest = new Label("Restante:");
        lblRest.getStyleClass().add(Styles.TITLE_4);
        lblRemaining = new Label(currencyFormat.format(totalAmount));
        lblRemaining.getStyleClass().addAll(Styles.TITLE_4, Styles.DANGER);
        remainingBox.getChildren().addAll(lblRest, lblRemaining);

        // Buttons
        btnConfirm = new Button("CONFIRMAR PAGAMENTO");
        btnConfirm.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnConfirm.setPrefWidth(260);
        btnConfirm.setDisable(true); // Initially disabled until exact match
        btnConfirm.setOnAction(e -> confirm());
        
        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        btnCancel.setOnAction(e -> onCancel.run());

        HBox actions = new HBox(10, btnCancel, btnConfirm);
        actions.setAlignment(Pos.CENTER);

        getChildren().addAll(header, new Separator(), grid, new Separator(), remainingBox, actions);
        
        // Initial focus
        if (!methodInputs.isEmpty()) {
            javafx.application.Platform.runLater(() -> methodInputs.get(MetodoPagamento.DINHEIRO).requestFocus());
        }
    }

    private String formatEnum(MetodoPagamento m) {
        return switch (m) {
            case DINHEIRO -> "Numerário (Dinheiro)";
            case CARTAO_POS -> "TPA (Multicaixa)";
            case TRANSFERENCIA_BANCARIA -> "Transferência";
            case MOBILE_MONEY -> "Mobile Money";
            case CHEQUE -> "Cheque";
            default -> m.name();
        };
    }

    private void fillRemaining(TextField target) {
        BigDecimal currentSum = calculateSumExcluding(target);
        BigDecimal remaining = totalAmount.subtract(currentSum);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        
        target.setText(String.format("%.2f", remaining).replace(".", ",")); // Simple format
    }

    private BigDecimal calculateSumExcluding(TextField excluded) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TextField txt : methodInputs.values()) {
            if (txt == excluded) continue;
            sum = sum.add(parseValue(txt.getText()));
        }
        return sum;
    }

    private BigDecimal parseValue(String text) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void updateRemaining() {
        BigDecimal sum = BigDecimal.ZERO;
        for (TextField txt : methodInputs.values()) {
            sum = sum.add(parseValue(txt.getText()));
        }
        BigDecimal remaining = totalAmount.subtract(sum);
        lblRemaining.setText(currencyFormat.format(remaining));
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            lblRemaining.getStyleClass().remove(Styles.DANGER);
            lblRemaining.getStyleClass().add(Styles.SUCCESS);
            btnConfirm.setDisable(false);
        } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal change = remaining.abs();
            BigDecimal cash = parseValue(methodInputs.getOrDefault(MetodoPagamento.DINHEIRO, new TextField()).getText());
            lblRemaining.setText("Troco: " + currencyFormat.format(change));
            lblRemaining.getStyleClass().remove(Styles.DANGER);
            if (cash.compareTo(change) >= 0) {
                lblRemaining.getStyleClass().add(Styles.WARNING);
                btnConfirm.setDisable(false);
            } else {
                lblRemaining.getStyleClass().add(Styles.DANGER);
                btnConfirm.setDisable(true);
            }
        } else {
            lblRemaining.getStyleClass().remove(Styles.SUCCESS);
            lblRemaining.getStyleClass().add(Styles.DANGER);
            btnConfirm.setDisable(true);
        }
    }

    private void confirm() {
        Map<MetodoPagamento, BigDecimal> payments = new HashMap<>();
        for (Map.Entry<MetodoPagamento, TextField> entry : methodInputs.entrySet()) {
            BigDecimal val = parseValue(entry.getValue().getText());
            if (val.compareTo(BigDecimal.ZERO) > 0) {
                payments.put(entry.getKey(), val);
            }
        }
        
        onConfirm.accept(payments);
        onCancel.run();
    }
}
