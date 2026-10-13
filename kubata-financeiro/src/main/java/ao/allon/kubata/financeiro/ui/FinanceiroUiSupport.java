package ao.allon.kubata.financeiro.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Componentes visuais partilhados pelas páginas do Financeiro. */
public final class FinanceiroUiSupport {
    public static final String GREEN = "#107C41";
    public static final String DARK = "#17352B";
    public static final String MUTED = "#64748B";
    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00 'Kz'", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-AO")));

    private FinanceiroUiSupport() { }

    public static VBox page(String title, String subtitle, Node actions) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 23px; -fx-font-weight: 700; -fx-text-fill: " + DARK + ";");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        subtitleLabel.setWrapText(true);

        VBox text = new VBox(4, titleLabel, subtitleLabel);
        HBox heading = new HBox(16, text);
        heading.setAlignment(Pos.CENTER_LEFT);
        if (actions != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            heading.getChildren().addAll(spacer, actions);
        }
        heading.setPadding(new Insets(20, 22, 16, 22));
        heading.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #E4EAE6 transparent;");

        VBox page = new VBox(0, heading);
        page.setStyle("-fx-background-color: #F5F7F6;");
        return page;
    }

    public static VBox metric(String caption, String value, String note, String accent) {
        Label label = new Label(caption);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + MUTED + ";");
        Label amount = new Label(value);
        amount.setWrapText(true);
        amount.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: " + DARK + ";");
        Label description = new Label(note == null ? " " : note);
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");
        VBox card = new VBox(9, label, amount, description);
        card.setPadding(new Insets(16));
        card.setMinHeight(116);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: #E3E9E5; -fx-border-width: 0 0 0 4; -fx-border-left: 4px solid " + accent + ";");
        return card;
    }

    public static Button primary(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + GREEN + "; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-background-radius: 7; -fx-padding: 9 14 9 14; -fx-cursor: hand;");
        button.setMinHeight(34);
        return button;
    }

    public static Button secondary(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: white; -fx-text-fill: #24352D; -fx-font-weight: 600;"
                + "-fx-border-color: #D5DED8; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 8 12 8 12; -fx-cursor: hand;");
        button.setMinHeight(34);
        return button;
    }

    public static Button danger(String text) {
        Button button = secondary(text);
        button.setStyle("-fx-background-color: white; -fx-text-fill: #B42318; -fx-font-weight: 600;"
                + "-fx-border-color: #F2C4C0; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 8 12 8 12; -fx-cursor: hand;");
        return button;
    }

    public static HBox actions(Button... buttons) {
        HBox bar = new HBox(8, buttons);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    public static String money(BigDecimal amount) {
        return MONEY.format(amount == null ? BigDecimal.ZERO : amount);
    }

    public static void styleTable(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Não existem registos para apresentar."));
        table.setStyle("-fx-background-color: white; -fx-border-color: #E3E9E5; -fx-border-radius: 8;"
                + "-fx-background-radius: 8; -fx-selection-bar: #DDEFE4; -fx-selection-bar-non-focused: #E9F3ED;");
    }

    public static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + DARK + ";");
        return label;
    }

    public static VBox whitePanel(String title, Node content) {
        VBox panel = new VBox(12, sectionTitle(title), content);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E3E9E5;");
        return panel;
    }

    public static void error(String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(rootMessage(error));
        alert.showAndWait();
    }

    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).isPresent();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank()
                ? "Ocorreu um erro inesperado. Verifique os dados e tente novamente."
                : message;
    }
}
