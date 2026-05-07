package ao.allon.kubata.core.ui.table;

import javafx.scene.control.TextField;
import javafx.util.converter.DefaultStringConverter;

/**
 * Célula com máscara de entrada customizada para campos como NIF, BI, Telefone.
 * Suporta '#' para dígitos.
 */
public class MaskedTextTableCell<S> extends EditableTableCell<S, String> {

    private final String mask;
    private TextField maskedField;

    public MaskedTextTableCell(String mask) {
        super(new DefaultStringConverter());
        this.mask = mask;
    }

    @Override
    protected void createEditor() {
        maskedField = new TextField();
        maskedField.getStyleClass().add("cell-editor");
        
        // Implementação simples de máscara
        maskedField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            
            StringBuilder sb = new StringBuilder();
            int maskIdx = 0;
            int textIdx = 0;
            
            while (maskIdx < mask.length() && textIdx < newValue.length()) {
                char m = mask.charAt(maskIdx);
                char t = newValue.charAt(textIdx);
                
                if (m == '#') {
                    if (Character.isDigit(t)) {
                        sb.append(t);
                        maskIdx++;
                        textIdx++;
                    } else {
                        textIdx++; // Ignora não-dígito
                    }
                } else {
                    sb.append(m);
                    maskIdx++;
                    if (t == m) textIdx++;
                }
            }
            
            if (!sb.toString().equals(newValue)) {
                maskedField.setText(sb.toString());
            }
        });

        maskedField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && isEditing()) {
                commitValue(maskedField.getText());
            }
        });

        maskedField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                commitValue(maskedField.getText());
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (maskedField == null) {
            createEditor();
        }
        maskedField.setText(getItem());
        setGraphic(maskedField);
        maskedField.requestFocus();
        maskedField.selectAll();
    }
}
