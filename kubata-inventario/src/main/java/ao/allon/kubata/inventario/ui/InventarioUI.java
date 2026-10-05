package ao.allon.kubata.inventario.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class InventarioUI {
    public static final String GREEN_DARK = "#1A5C35";
    public static final String GREEN = "#217346";
    public static final String GREEN_PALE = "#E8F5E9";
    public static final String PAGE_BG = "#F5F5F5";

    private InventarioUI() {}

    public static void installCss(Region root) {
        String css = InventarioUI.class.getResource("/css/inventario.css").toExternalForm();
        if (!root.getStylesheets().contains(css)) root.getStylesheets().add(css);
    }

    public static VBox page(String title, String subtitle) {
        VBox root = new VBox(16);
        root.getStyleClass().add("inventario-page");
        installCss(root);

        VBox header = new VBox(4);
        header.getStyleClass().add("inventario-page-header");
        Label h = new Label(title);
        h.getStyleClass().add("inventario-page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("inventario-page-subtitle");
        s.setWrapText(true);
        header.getChildren().addAll(h, s);

        root.getChildren().add(header);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    public static HBox toolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("inventario-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    public static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-primary");
        return b;
    }

    public static Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-outlined");
        return b;
    }

    public static VBox card(String title, Node content) {
        VBox card = new VBox(10);
        card.getStyleClass().add("inventario-card");
        Label label = new Label(title);
        label.getStyleClass().add("inventario-card-title");
        card.getChildren().addAll(label, content);
        return card;
    }

    public static VBox metric(String label, String value, String detail) {
        VBox box = new VBox(6);
        box.getStyleClass().add("inventario-metric");
        Label l = new Label(label);
        l.getStyleClass().add("inventario-metric-label");
        Label v = new Label(value);
        v.getStyleClass().add("inventario-metric-value");
        Label d = new Label(detail == null ? "" : detail);
        d.getStyleClass().add("inventario-metric-detail");
        d.setWrapText(true);
        box.getChildren().addAll(l, v, d);
        return box;
    }

    public static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("inventario-field");
        return f;
    }

    public static void styleTable(TableView<?> table) {
        table.getStyleClass().addAll("inventario-table", "striped", "bordered");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhum registo encontrado."));
    }

    public static Optional<ButtonType> showDialog(String title, String subtitle,
                                                   Node content, double width, double height) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setMinSize(width, height);
        dialog.getDialogPane().setPrefSize(width, height);
        dialog.getDialogPane().getStyleClass().add("inventario-dialog");

        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        if (subtitle != null && !subtitle.isBlank()) {
            Label s = new Label(subtitle);
            s.getStyleClass().add("inventario-dialog-subtitle");
            wrapper.getChildren().add(s);
        }
        wrapper.getChildren().add(content);

        dialog.getDialogPane().setContent(wrapper);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.getStyleClass().add("button-primary");
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancel.getStyleClass().add("button-outlined");

        Window owner = content.getScene() == null ? null : content.getScene().getWindow();
        if (owner != null) dialog.initOwner(owner);
        return dialog.showAndWait();
    }

    public static String money(BigDecimal value) {
        return String.format("%,.2f Kz", value == null ? BigDecimal.ZERO : value);
    }

    public static String integer(Number value) {
        return String.format("%,d", value == null ? 0 : value.longValue());
    }

    public static String dateTime(java.time.LocalDateTime value) {
        return value == null ? "—" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }

    public static void error(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }
}
