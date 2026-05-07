package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.ui.modal.CustomModal;
import ao.allon.kubata.faturacao.ui.modal.ModalBuilder;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.components.VirtualKeyboardPopup;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class PaymentDialog extends BorderPane {

    private final BigDecimal totalAmount;
    private final Consumer<Map<MetodoPagamento, BigDecimal>> onConfirm;
    private final NumberFormat currencyFormat = Money.getCurrencyFormat();
    private final ModalService modalService;
    private final Runnable onCancel;
    
    private ToggleGroup methodGroup;
    private MetodoPagamento selectedMethod = MetodoPagamento.CARTAO_POS; // Default to Multicaixa (Common in Angola)

    // Input Fields
    private TextField txtReceived; // For Cash
    private TextField txtReference; // For others
    private Label lblChangeValue;
    private Label lblChangeText;
    private VBox dynamicContentArea;

    public PaymentDialog(BigDecimal totalAmount, Consumer<Map<MetodoPagamento, BigDecimal>> onConfirm, ModalService modalService, Runnable onCancel) {
        this.totalAmount = totalAmount;
        this.onConfirm = onConfirm;
        this.modalService = modalService;
        this.onCancel = onCancel;
        
        getStyleClass().add("payment-dialog");
        setPrefSize(900, 600);
        setStyle("-fx-background-color: -color-bg-default;");

        // Init dynamic content area BEFORE creating toggles to avoid NPE
        dynamicContentArea = new VBox(20);
        dynamicContentArea.setAlignment(Pos.TOP_LEFT);
        
        // --- Left Pane: Method Selection ---
        VBox leftPane = createLeftPane();
        setLeft(leftPane);

        // --- Center Pane: Payment Details & Summary ---
        VBox centerPane = createCenterPane();
        setCenter(centerPane);

        // --- Bottom: Actions (Moved to Center Pane bottom for better flow or kept separate?)
        // Let's keep actions in the center pane bottom.

        // Keyboard shortcuts
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) onCancel.run();
        });
        
        // Initial State
        updateDynamicContent();
    }

    private VBox createLeftPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(30));
        pane.setPrefWidth(320);
        pane.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 0 1 0 0;");
        pane.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Método de Pagamento");
        title.getStyleClass().addAll(Styles.TITLE_4);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        methodGroup = new ToggleGroup();

        // 1. Multicaixa (Default)
        ToggleButton btnMulticaixa = createMethodCard("Multicaixa", Feather.CREDIT_CARD, MetodoPagamento.CARTAO_POS);
        btnMulticaixa.setSelected(true);
        
        // 2. Numerário
        ToggleButton btnCash = createMethodCard("Numerário", Feather.DOLLAR_SIGN, MetodoPagamento.DINHEIRO);
        
        // 3. Transferência
        ToggleButton btnTransfer = createMethodCard("Transferência", Feather.REPEAT, MetodoPagamento.TRANSFERENCIA_BANCARIA);
        
        // 4. Mobile Money
        ToggleButton btnMobile = createMethodCard("Mobile Money", Feather.SMARTPHONE, MetodoPagamento.MOBILE_MONEY);
        
        // 5. Pagamento Misto (Special Action)
        Button btnSplit = new Button("Pagamento Misto");
        btnSplit.setGraphic(new FontIcon(Feather.LAYERS));
        btnSplit.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "method-card");
        btnSplit.setPrefSize(130, 100);
        btnSplit.setContentDisplay(ContentDisplay.TOP);
        btnSplit.setWrapText(true);
        btnSplit.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        btnSplit.setOnAction(e -> openSplitPayment());

        // Add to Grid
        grid.add(btnMulticaixa, 0, 0);
        grid.add(btnCash, 1, 0);
        grid.add(btnTransfer, 0, 1);
        grid.add(btnMobile, 1, 1);
        grid.add(btnSplit, 0, 2, 2, 1); // Span 2 columns
        btnSplit.setMaxWidth(Double.MAX_VALUE); // Full width for Split

        // Info Section
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(20, 0, 0, 0));
        Label infoTitle = new Label("Informação");
        infoTitle.getStyleClass().add(Styles.TEXT_BOLD);
        TextFlow infoText = new TextFlow(
            new Text("Para pagamentos via "),
            boldText("Multicaixa"),
            new Text(", certifique-se de que o TPA emitiu o recibo antes de confirmar.\n\n"),
            new Text("Para "),
            boldText("Pagamento Misto"),
            new Text(", utilize a opção dedicada para dividir entre Numerário e TPA.")
        );
        infoText.setStyle("-fx-fill: -color-text-subtle; -fx-font-size: 0.9em;");
        infoBox.getChildren().addAll(infoTitle, infoText);

        pane.getChildren().addAll(title, grid, new Region(), infoBox);
        VBox.setVgrow(infoBox, Priority.ALWAYS); // Push info to bottom if needed, or just below grid
        
        return pane;
    }

    private Text boldText(String content) {
        Text t = new Text(content);
        t.setStyle("-fx-font-weight: bold;");
        return t;
    }

    private ToggleButton createMethodCard(String title, Feather icon, MetodoPagamento method) {
        ToggleButton btn = new ToggleButton(title);
        btn.setGraphic(new FontIcon(icon));
        btn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "method-card");
        btn.setPrefSize(130, 100);
        btn.setContentDisplay(ContentDisplay.TOP);
        btn.setToggleGroup(methodGroup);
        btn.setUserData(method);
        btn.setWrapText(true);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        btn.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                selectedMethod = method;
                updateDynamicContent();
            }
        });
        
        return btn;
    }

    private VBox createCenterPane() {
        VBox pane = new VBox(25);
        pane.setPadding(new Insets(40));
        pane.setAlignment(Pos.TOP_CENTER);

        // --- Total Card ---
        StackPane totalCard = new StackPane();
        totalCard.getStyleClass().addAll(Styles.ELEVATED_2);
        totalCard.setStyle("-fx-background-color: -color-accent-emphasis; -fx-background-radius: 10; -fx-padding: 20;");
        totalCard.setMaxWidth(Double.MAX_VALUE);
        
        VBox totalContent = new VBox(5);
        totalContent.setAlignment(Pos.CENTER);
        
        Label lblTotalLabel = new Label("TOTAL A PAGAR");
        lblTotalLabel.setGraphic(new FontIcon(Feather.LOCK));
        lblTotalLabel.setStyle("-fx-text-fill: -color-fg-emphasis; -fx-font-size: 0.9em; -fx-font-weight: bold; -fx-opacity: 0.8;");
        
        Label lblTotalValue = new Label(currencyFormat.format(totalAmount));
        lblTotalValue.setStyle("-fx-text-fill: -color-fg-emphasis; -fx-font-size: 2.5em; -fx-font-weight: 900;");
        
        Label lblTaxInfo = new Label("Inclui IVA à taxa legal em vigor");
        lblTaxInfo.setStyle("-fx-text-fill: -color-fg-emphasis; -fx-font-size: 0.8em; -fx-opacity: 0.7;");

        totalContent.getChildren().addAll(lblTotalLabel, lblTotalValue, lblTaxInfo);
        totalCard.getChildren().add(totalContent);

        // Dynamic area already initialized in constructor
        VBox.setVgrow(dynamicContentArea, Priority.ALWAYS);

        // --- Action Buttons ---
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnCancel.setPrefHeight(45);
        btnCancel.setPrefWidth(120);
        btnCancel.setOnAction(e -> onCancel.run());
        
        Button btnConfirm = new Button("Confirmar Pagamento");
        btnConfirm.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnConfirm.setPrefHeight(45);
        btnConfirm.setPrefWidth(220);
        btnConfirm.setDefaultButton(true); // Enter triggers this
        btnConfirm.setGraphic(new FontIcon(Feather.CHECK));
        btnConfirm.setOnAction(e -> confirm());

        actions.getChildren().addAll(btnCancel, btnConfirm);

        pane.getChildren().addAll(totalCard, dynamicContentArea, actions);
        return pane;
    }

    private void updateDynamicContent() {
        dynamicContentArea.getChildren().clear();
        
        Label lblSection = new Label();
        lblSection.getStyleClass().add(Styles.TITLE_4);
        
        if (selectedMethod == MetodoPagamento.DINHEIRO) {
            lblSection.setText("Detalhes do Numerário");
            
            // Received Amount Field
            VBox boxReceived = new VBox(8);
            Label lblRec = new Label("Valor Recebido (KZ)");
            lblRec.getStyleClass().add(Styles.TEXT_BOLD);
            
            txtReceived = new TextField();
            txtReceived.setPromptText("0,00");
            txtReceived.setStyle("-fx-font-size: 1.5em;");
            txtReceived.setText(formatDecimal(totalAmount)); // Pre-fill
            txtReceived.setTextFormatter(new TextFormatter<>(change -> {
                String nt = change.getControlNewText();
                return nt.matches("\\d{0,12}([\\.,]\\d{0,2})?") ? change : null;
            }));
            txtReceived.textProperty().addListener((obs, old, newVal) -> calculateChange());
            Button btnPad = new Button("Teclado");
            btnPad.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
            btnPad.setOnAction(e -> VirtualKeyboardPopup.show(txtReceived, VirtualKeyboardPopup.KeyboardType.NUMERIC, this::confirm));
            HBox row = new HBox(8, txtReceived, btnPad);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(txtReceived, Priority.ALWAYS);
            boxReceived.getChildren().addAll(lblRec, row);
            
            // Change Display
            HBox boxChange = new HBox(20);
            boxChange.setAlignment(Pos.CENTER_LEFT);
            boxChange.setPadding(new Insets(15));
            boxChange.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");
            
            lblChangeText = new Label("Troco:");
            lblChangeText.setStyle("-fx-font-size: 1.2em;");
            
            lblChangeValue = new Label("KZ 0,00");
            lblChangeValue.setStyle("-fx-font-size: 1.8em; -fx-font-weight: bold; -fx-text-fill: -color-success-fg;");
            
            boxChange.getChildren().addAll(lblChangeText, lblChangeValue);
            
            // Overlay flutuante é acionado por foco e botão
            txtReceived.focusedProperty().addListener((o, ov, nv) -> {
                if (nv) VirtualKeyboardPopup.show(txtReceived, VirtualKeyboardPopup.KeyboardType.NUMERIC, this::confirm);
            });
            dynamicContentArea.getChildren().addAll(lblSection, boxReceived, boxChange);
            
            Platform.runLater(() -> {
                txtReceived.requestFocus();
                txtReceived.selectAll();
            });
            
        } else {
            // Generic fields for other methods
            String methodTitle = switch (selectedMethod) {
                case CARTAO_POS -> "Pagamento via Multicaixa";
                case TRANSFERENCIA_BANCARIA -> "Transferência Bancária";
                case MOBILE_MONEY -> "Mobile Money (Unitel/Africell)";
                default -> "Detalhes do Pagamento";
            };
            lblSection.setText(methodTitle);
            
            VBox boxRef = new VBox(8);
            Label lblRef = new Label("Nº de Referência / Transação (Opcional)");
            lblRef.getStyleClass().add(Styles.TEXT_BOLD);
            
            txtReference = new TextField();
            txtReference.setPromptText("Insira o número do talão ou referência...");

            txtReference.focusedProperty().addListener((o, ov, nv) -> {
                if (nv) VirtualKeyboardPopup.show(txtReference, VirtualKeyboardPopup.KeyboardType.ALPHANUMERIC);
            });

            Button btnPadRef = new Button("Teclado");
            btnPadRef.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
            btnPadRef.setOnAction(e -> VirtualKeyboardPopup.show(txtReference, VirtualKeyboardPopup.KeyboardType.ALPHANUMERIC));
            HBox refRow = new HBox(8, txtReference, btnPadRef);
            refRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(txtReference, Priority.ALWAYS);
            
            Label lblInfo = new Label("A confirmação deste pagamento registará a fatura como liquidada.");
            lblInfo.getStyleClass().add(Styles.TEXT_MUTED);
            lblInfo.setWrapText(true);
            
            boxRef.getChildren().addAll(lblRef, refRow, lblInfo);
            dynamicContentArea.getChildren().addAll(lblSection, boxRef);
        }
    }
    
    private String formatDecimal(BigDecimal val) {
        return val.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private void calculateChange() {
        if (txtReceived == null) return;
        try {
            String text = txtReceived.getText().replace(",", ".");
            if (text.isEmpty()) text = "0";
            BigDecimal received = new BigDecimal(text);
            
            BigDecimal change = received.subtract(totalAmount);
            if (change.compareTo(BigDecimal.ZERO) < 0) {
                lblChangeText.setText("Falta:");
                lblChangeValue.setText(currencyFormat.format(change.abs()));
                lblChangeValue.setStyle("-fx-font-size: 1.8em; -fx-font-weight: bold; -fx-text-fill: -color-danger-fg;");
            } else {
                lblChangeText.setText("Troco:");
                lblChangeValue.setText(currencyFormat.format(change));
                lblChangeValue.setStyle("-fx-font-size: 1.8em; -fx-font-weight: bold; -fx-text-fill: -color-success-fg;");
            }
        } catch (NumberFormatException e) {
            lblChangeValue.setText("Valor Inválido");
            lblChangeValue.setStyle("-fx-font-size: 1.5em; -fx-text-fill: -color-danger-fg;");
        }
    }

    private void confirm() {
        if (selectedMethod == MetodoPagamento.DINHEIRO) {
            try {
                String text = txtReceived.getText().replace(",", ".");
                if (text.isEmpty()) text = "0";
                BigDecimal received = new BigDecimal(text);
                
                if (received.compareTo(totalAmount) < 0) {
                    AlertUtils.showErrorAlert("Pagamento Insuficiente", "O valor recebido é menor que o total a pagar.");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtils.showErrorAlert("Valor Inválido", "Por favor verifique o valor recebido.");
                return;
            }
        }
        
        Map<MetodoPagamento, BigDecimal> payments = new HashMap<>();
        payments.put(selectedMethod, totalAmount);
        
        // Optionally capture reference if needed, but the current consumer only takes map.
        // If we want to save reference, we might need to update the consumer or handle it separately.
        // For now, we stick to the interface.
        
        onConfirm.accept(payments);
        onCancel.run();
    }
    
    private void openSplitPayment() {
        final CustomModal[] splitModalRef = new CustomModal[1];
        
        Runnable closeSplitAction = () -> {
            if (splitModalRef[0] != null) {
                splitModalRef[0].hide();
            }
        };

        SplitPaymentDialog dialog = new SplitPaymentDialog(totalAmount, (payments) -> {
            this.onConfirm.accept(payments);
            closeSplitAction.run();
            this.onCancel.run();
        }, closeSplitAction);

        ModalBuilder builder = modalService.create()
            .title("Pagamento Misto / Dividir Conta")
            .content(dialog)
            .autoSize();
            
        splitModalRef[0] = builder.getModal();
        builder.buildAndShow();
    }
}
