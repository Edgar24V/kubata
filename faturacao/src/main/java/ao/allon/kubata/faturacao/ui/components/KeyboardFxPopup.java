package ao.allon.kubata.faturacao.ui.components;

import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Popup;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class KeyboardFxPopup {

    private KeyboardFxPopup() {
    }

    private static final Set<TextInputControl> INSTALLED = new HashSet<>();
    private static volatile boolean REPORTED_ERROR;

    private static Popup popup;
    private static StackPane popupContainer;
    private static TextInputControl currentTarget;

    public static void install(Parent root) {
        if (root == null) return;
        installRecursive(root);
    }

    private static void installRecursive(Parent parent) {
        for (Node n : parent.getChildrenUnmodifiable()) {
            if (n instanceof TextInputControl tic) {
                installOnControl(tic);
            }
            if (n instanceof Parent p) {
                installRecursive(p);
            }
        }
    }

    private static void installOnControl(TextInputControl control) {
        if (control == null) return;
        if (INSTALLED.contains(control)) return;
        INSTALLED.add(control);

        control.focusedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                showForPopup(control, null);
            } else {
                Platform.runLater(() -> {
                    if (control.isFocused()) return;
                    if (!isFocusInsideKeyboard(control)) hideKeyboard();
                });
            }
        });
    }

    private static void keepFocusOnTarget(Node keyboard, TextInputControl target) {
        if (keyboard == null || target == null) return;

        // KeyboardFX will typically send key events to the focused node.
        // When the user clicks on the popup keyboard, focus can move away from the TextField.
        // Force focus back to the target before the click is processed.
        javafx.event.EventHandler<MouseEvent> refocus = e -> {
            if (!target.isFocused()) {
                target.requestFocus();
                Platform.runLater(target::requestFocus);
            }
        };
        keyboard.addEventFilter(MouseEvent.MOUSE_PRESSED, refocus);
        keyboard.addEventFilter(MouseEvent.MOUSE_CLICKED, refocus);
    }

    private static void disableFocusTraversal(Node node) {
        if (node == null) return;
        node.setFocusTraversable(false);
        if (node instanceof Parent p) {
            for (Node ch : p.getChildrenUnmodifiable()) {
                disableFocusTraversal(ch);
            }
        }
    }

    public static void showFor(TextInputControl field) {
        showFor(field, null, null);
    }

    public static void showFor(TextInputControl field, Runnable onConfirm) {
        showFor(field, onConfirm, null);
    }

    public static void showFor(TextInputControl field, Runnable onConfirm, Object ignoredPositionHint) {
        showForPopup(field, onConfirm);
    }

    private static void ensurePopup() {
        if (popup != null) return;
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        popupContainer = new StackPane();
        popupContainer.setPadding(new Insets(8));
        popupContainer.setOnMouseClicked(e -> e.consume());

        popup.getContent().add(popupContainer);

        popup.setOnHidden(e -> {
            currentTarget = null;
            if (popupContainer != null) popupContainer.getChildren().clear();
        });
    }

    private static void showForPopup(TextInputControl field, Runnable onConfirm) {
        if (field == null || field.getScene() == null || field.getScene().getWindow() == null) return;
        ensurePopup();

        currentTarget = field;

        if (onConfirm != null) {
            popupContainer.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> {
                        onConfirm.run();
                        hideKeyboard();
                        e.consume();
                    }
                    case ESCAPE -> {
                        hideKeyboard();
                        e.consume();
                    }
                }
            });
        } else {
            popupContainer.setOnKeyPressed(null);
        }

        Platform.runLater(() -> {
            try {
                Bounds fieldScreen = field.localToScreen(field.getBoundsInLocal());
                if (fieldScreen == null) return;

                Rectangle2D screenBounds = Screen.getScreensForRectangle(
                                fieldScreen.getMinX(), fieldScreen.getMinY(), fieldScreen.getWidth(), fieldScreen.getHeight())
                        .stream().findFirst().map(Screen::getVisualBounds).orElse(Screen.getPrimary().getVisualBounds());

                if (!popup.isShowing()) {
                    popup.show(field.getScene().getWindow());
                }

                Node keyboard = buildKeyboardNode(field);
                if (keyboard != null) {
                    disableFocusTraversal(keyboard);
                    keepFocusOnTarget(keyboard, field);
                    popupContainer.getChildren().setAll(keyboard);
                } else {
                    popupContainer.getChildren().clear();
                }

                // Position: try below field, otherwise above.
                popupContainer.applyCss();
                popupContainer.layout();
                double pw = popupContainer.prefWidth(-1);
                double ph = popupContainer.prefHeight(-1);

                double x = clamp(fieldScreen.getMinX(), screenBounds.getMinX(), screenBounds.getMaxX() - pw);
                double yBelow = fieldScreen.getMaxY() + 6;
                double yAbove = fieldScreen.getMinY() - ph - 6;
                double y = (yBelow + ph <= screenBounds.getMaxY()) ? yBelow : Math.max(screenBounds.getMinY(), yAbove);

                popup.setX(x);
                popup.setY(y);

            } catch (Throwable t) {
                reportKeyboardErrorOnce(t);
            }
        });
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void hideKeyboard() {
        if (popup != null) popup.hide();
    }

    private static boolean isFocusInsideKeyboard(TextInputControl field) {
        if (popup == null || !popup.isShowing()) return false;
        if (field != null && field.isFocused()) return true;
        Node focusOwner = field != null && field.getScene() != null ? field.getScene().getFocusOwner() : null;
        return focusOwner != null && popupContainer != null && isDescendantOf(focusOwner, popupContainer);
    }

    private static Node buildKeyboardNode(TextInputControl target) {
        try {
            // Prefer an embeddable view if available. KeyboardPane manages its own overlay
            // and can throw NPEs when embedded inside a PopupControl / Popover.
            Node view = tryBuildKeyboard("com.dlsc.keyboardfx.KeyboardView", target);
            if (view != null) return view;

            // Fallback to KeyboardPane.
            Node pane = tryBuildKeyboard("com.dlsc.keyboardfx.KeyboardPane", target);
            if (pane != null) return pane;

            return null;
        } catch (Exception ex) {
            reportKeyboardErrorOnce(ex);
            return null;
        }
    }

    private static Node tryBuildKeyboard(String className, TextInputControl target) {
        try {
            Class<?> clazz = Class.forName(className);
            Object keyboard = clazz.getDeclaredConstructor().newInstance();

            // Try to connect target node using reflection.
            // KeyboardFX APIs can differ between versions (KeyboardView/KeyboardPane).
            // We first try common explicit methods and then fall back to an automatic scan.
            invokeIfExists(clazz, keyboard, "setTarget", Node.class, target);
            invokeIfExists(clazz, keyboard, "setTargetNode", Node.class, target);
            invokeIfExists(clazz, keyboard, "setTargetControl", TextInputControl.class, target);
            invokeIfExists(clazz, keyboard, "setFocusOwner", Node.class, target);
            invokeIfExists(clazz, keyboard, "setFocusOwnerNode", Node.class, target);
            invokeIfExists(clazz, keyboard, "setFocusOwnerControl", TextInputControl.class, target);
            connectByHeuristics(clazz, keyboard, target);

            return keyboard instanceof Node n ? n : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void connectByHeuristics(Class<?> clazz, Object instance, TextInputControl target) {
        try {
            // 1) Setter-like methods
            for (Method m : clazz.getMethods()) {
                if (m.getParameterCount() != 1) continue;
                String name = m.getName().toLowerCase();
                boolean isTarget = name.contains("target");
                boolean isFocusOwner = name.contains("focus") && name.contains("owner");
                if (!(isTarget || isFocusOwner)) continue;

                Class<?> p = m.getParameterTypes()[0];
                Object arg = null;

                if (p.isAssignableFrom(target.getClass())) {
                    arg = target;
                } else if (p.isAssignableFrom(Node.class)) {
                    arg = target;
                } else if (p.isAssignableFrom(TextInputControl.class)) {
                    arg = target;
                }

                if (arg != null) {
                    try {
                        m.invoke(instance, arg);
                    } catch (Exception ignored) {
                    }
                }
            }

            // 2) *Property() accessors (e.g., targetNodeProperty)
            for (Method m : clazz.getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("property")) continue;
                boolean isTarget = name.contains("target");
                boolean isFocusOwner = name.contains("focus") && name.contains("owner");
                if (!(isTarget || isFocusOwner)) continue;

                if (!Property.class.isAssignableFrom(m.getReturnType())) continue;

                try {
                    Object propObj = m.invoke(instance);
                    if (propObj instanceof Property<?> p) {
                        // We intentionally use raw setValue via reflection-friendly API.
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        Property raw = (Property) p;
                        raw.setValue(target);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isDescendantOf(Node node, Node ancestor) {
        Node cur = node;
        while (cur != null) {
            if (cur == ancestor) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private static void reportKeyboardErrorOnce(Throwable ex) {
        if (REPORTED_ERROR) return;
        REPORTED_ERROR = true;
        AlertUtils.showExceptionAlert(
                "Teclado Virtual",
                "Falha ao carregar o teclado virtual (KeyboardFX). Verifique se a dependência está no classpath.",
                ex
        );
    }

    private static void invokeIfExists(Class<?> clazz, Object instance, String methodName, Class<?> paramType, Object arg) {
        try {
            Method m = clazz.getMethod(methodName, paramType);
            m.invoke(instance, arg);
        } catch (Exception ignored) {
        }
    }
}
