package ao.allon.kubata.faturacao.ui.components;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VirtualKeyboardPopup {

    public enum KeyboardType {
        NUMERIC,
        ALPHANUMERIC
    }

    private VirtualKeyboardPopup() {
    }

    private static final Set<TextInputControl> INSTALLED = new HashSet<>();

    private static Popup popup;
    private static StackPane container;
    private static TextInputControl target;
    private static KeyboardType targetType;
    private static Runnable onConfirm;

    private static boolean shift;
    private static final Map<Button, String> letterBase = new HashMap<>();
    
    // Configuração global de ativação (cache local para evitar lookup repetitivo de preferências)
    private static boolean enabled = true;

    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }

    public static void install(Parent root) {
        if (root == null) return;
        installRecursive(root);
    }

    private static void installRecursive(Parent parent) {
        for (Node n : parent.getChildrenUnmodifiable()) {
            if (n instanceof TextInputControl tic) {
                attach(tic, KeyboardType.ALPHANUMERIC, null, false);
            }
            if (n instanceof Parent p) {
                installRecursive(p);
            }
        }
    }

    public static void attach(TextInputControl field, KeyboardType type) {
        attach(field, type, null, true);
    }

    public static void attach(TextInputControl field, KeyboardType type, Runnable confirm, boolean showOnFocus) {
        if (field == null) return;
        if (INSTALLED.contains(field)) return;
        INSTALLED.add(field);

        if (showOnFocus) {
            field.focusedProperty().addListener((o, ov, nv) -> {
                if (nv) {
                    show(field, type, confirm);
                } else {
                    Platform.runLater(() -> {
                        if (field.isFocused()) return;
                        if (!isFocusInsideKeyboard(field)) hide();
                    });
                }
            });
        }
    }

    public static void show(TextInputControl field, KeyboardType type) {
        show(field, type, null);
    }

    public static void show(TextInputControl field, KeyboardType type, Runnable confirm) {
        if (!enabled) return; // Respeita configuração global
        if (field == null || field.getScene() == null || field.getScene().getWindow() == null) return;
        ensurePopup();

        target = field;
        targetType = type;
        onConfirm = confirm;
        shift = false;

        Platform.runLater(() -> {
            if (!popup.isShowing()) {
                popup.show(field.getScene().getWindow());
            }

            rebuild();
            positionNear(field);

            target.requestFocus();
            Platform.runLater(target::requestFocus);
        });
    }

    public static void hide() {
        if (popup != null) popup.hide();
    }

    private static void ensurePopup() {
        if (popup != null) return;

        popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        container = new StackPane();
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: -color-border-default; -fx-border-width: 1;");
        container.setOnMouseClicked(e -> e.consume());

        popup.getContent().add(container);

        popup.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (target != null && !target.isFocused()) {
                target.requestFocus();
            }
        });

        popup.setOnHidden(e -> {
            target = null;
            targetType = null;
            onConfirm = null;
            container.getChildren().clear();
            letterBase.clear();
            shift = false;
        });
    }

    private static void rebuild() {
        container.getChildren().clear();

        VBox root = new VBox(8);
        root.setFillWidth(true);

        HBox header = new HBox(8);
        header.setFillHeight(true);

        Button btnClose = new Button("Fechar");
        btnClose.setFocusTraversable(false);
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        btnClose.setOnAction(e -> hide());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnOk = new Button("OK");
        btnOk.setFocusTraversable(false);
        btnOk.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-background-radius: 6;");
        btnOk.setOnAction(e -> {
            if (onConfirm != null) onConfirm.run();
            hide();
        });

        header.getChildren().addAll(spacer, btnClose, btnOk);

        Node keyboard = (targetType == KeyboardType.NUMERIC) ? buildNumeric() : buildAlphaNumeric();

        root.getChildren().addAll(keyboard, header);
        container.getChildren().add(root);

        container.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });
    }

    private static Node buildNumeric() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[][] keys = {
                {"7", "8", "9", "⌫"},
                {"4", "5", "6", "C"},
                {"1", "2", "3", ","},
                {"0", ".", "-", "OK"}
        };

        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String k = keys[r][c];
                Button b = keyButton(k);
                b.setPrefWidth(70);
                b.setPrefHeight(55);

                if ("OK".equals(k)) {
                    b.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis; -fx-background-radius: 6;");
                    b.setOnAction(e -> {
                        if (onConfirm != null) onConfirm.run();
                        hide();
                    });
                } else if ("⌫".equals(k)) {
                    b.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
                    b.setOnAction(e -> backspace());
                } else if ("C".equals(k)) {
                    b.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
                    b.setOnAction(e -> clear());
                } else if ("-".equals(k)) {
                    b.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
                    b.setOnAction(e -> insertText("-"));
                } else {
                    b.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
                    b.setOnAction(e -> insertText(k));
                }

                grid.add(b, c, r);
            }
        }

        return grid;
    }

    private static Node buildAlphaNumeric() {
        VBox box = new VBox(6);

        GridPane row1 = rowOf("1 2 3 4 5 6 7 8 9 0".split(" "), true);
        GridPane row2 = rowOf("Q W E R T Y U I O P".split(" "), false);
        GridPane row3 = rowOf("A S D F G H J K L".split(" "), false);

        HBox row4 = new HBox(6);
        Button shiftBtn = keyButton("Shift");
        shiftBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        shiftBtn.setOnAction(e -> toggleShift());
        shiftBtn.setPrefHeight(50);
        shiftBtn.setPrefWidth(90);

        GridPane letters4 = rowOf("Z X C V B N M".split(" "), false);

        Button backBtn = keyButton("⌫");
        backBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        backBtn.setOnAction(e -> backspace());
        backBtn.setPrefHeight(50);
        backBtn.setPrefWidth(90);

        row4.getChildren().addAll(shiftBtn, letters4, backBtn);
        HBox.setHgrow(letters4, Priority.ALWAYS);

        HBox row5 = new HBox(6);
        Button clearBtn = keyButton("C");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> clear());
        clearBtn.setPrefHeight(50);
        clearBtn.setPrefWidth(80);

        Button spaceBtn = keyButton("Espaço");
        spaceBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        spaceBtn.setOnAction(e -> insertText(" "));
        spaceBtn.setPrefHeight(50);

        Button dotBtn = keyButton(".");
        dotBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");
        dotBtn.setOnAction(e -> insertText("."));
        dotBtn.setPrefHeight(50);
        dotBtn.setPrefWidth(60);

        row5.getChildren().addAll(clearBtn, spaceBtn, dotBtn);
        HBox.setHgrow(spaceBtn, Priority.ALWAYS);

        box.getChildren().addAll(row1, row2, row3, row4, row5);
        return box;
    }

    private static GridPane rowOf(String[] keys, boolean numbers) {
        GridPane row = new GridPane();
        row.setHgap(6);

        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            Button b = keyButton(k);
            b.setPrefHeight(50);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6;");

            if (numbers) {
                b.setOnAction(e -> insertText(k));
            } else {
                letterBase.put(b, k);
                b.setOnAction(e -> insertText(shift ? k.toUpperCase() : k.toLowerCase()));
            }

            row.add(b, i, 0);
            GridPane.setHgrow(b, Priority.ALWAYS);
        }

        return row;
    }

    private static void toggleShift() {
        shift = !shift;
        for (Map.Entry<Button, String> e : letterBase.entrySet()) {
            Button b = e.getKey();
            String base = e.getValue();
            if (b == null || base == null) continue;
            b.setText(shift ? base.toUpperCase() : base.toLowerCase());
        }
    }

    private static Button keyButton(String text) {
        Button b = new Button(text);
        b.setFocusTraversable(false);
        b.setOnMousePressed(e -> {
            if (target != null && !target.isFocused()) target.requestFocus();
        });
        return b;
    }

    private static void insertText(String s) {
        if (target == null) return;
        target.requestFocus();

        if (targetType == KeyboardType.NUMERIC) {
            if ("-".equals(s) && target.getCaretPosition() != 0) return;
            if ((",".equals(s) || ".".equals(s))) {
                String t = target.getText();
                if (t != null && (t.contains(",") || t.contains("."))) return;
            }
        }

        int start = Math.min(target.getAnchor(), target.getCaretPosition());
        int end = Math.max(target.getAnchor(), target.getCaretPosition());
        if (start != end) {
            target.replaceText(start, end, s);
        } else {
            target.insertText(target.getCaretPosition(), s);
        }
    }

    private static void backspace() {
        if (target == null) return;
        target.requestFocus();

        int start = Math.min(target.getAnchor(), target.getCaretPosition());
        int end = Math.max(target.getAnchor(), target.getCaretPosition());
        if (start != end) {
            target.deleteText(start, end);
            return;
        }
        int pos = target.getCaretPosition();
        if (pos > 0) {
            target.deleteText(pos - 1, pos);
        }
    }

    private static void clear() {
        if (target == null) return;
        target.requestFocus();
        target.clear();
    }

    private static void positionNear(TextInputControl field) {
        if (popup == null || container == null) return;

        Bounds fieldScreen = field.localToScreen(field.getBoundsInLocal());
        if (fieldScreen == null) return;

        Rectangle2D screenBounds = Screen.getScreensForRectangle(
                        fieldScreen.getMinX(), fieldScreen.getMinY(), fieldScreen.getWidth(), fieldScreen.getHeight())
                .stream().findFirst().map(Screen::getVisualBounds).orElse(Screen.getPrimary().getVisualBounds());

        container.applyCss();
        container.layout();

        double pw = container.prefWidth(-1);
        double ph = container.prefHeight(-1);

        double x = clamp(fieldScreen.getMinX(), screenBounds.getMinX(), screenBounds.getMaxX() - pw);
        double yBelow = fieldScreen.getMaxY() + 8;
        double yAbove = fieldScreen.getMinY() - ph - 8;
        double y = (yBelow + ph <= screenBounds.getMaxY()) ? yBelow : Math.max(screenBounds.getMinY(), yAbove);

        popup.setX(x);
        popup.setY(y);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isFocusInsideKeyboard(TextInputControl field) {
        if (popup == null || !popup.isShowing()) return false;
        if (field != null && field.isFocused()) return true;
        Node focusOwner = field != null && field.getScene() != null ? field.getScene().getFocusOwner() : null;
        return focusOwner != null && isDescendantOf(focusOwner, container);
    }

    private static boolean isDescendantOf(Node node, Node ancestor) {
        Node cur = node;
        while (cur != null) {
            if (cur == ancestor) return true;
            cur = cur.getParent();
        }
        return false;
    }
}
