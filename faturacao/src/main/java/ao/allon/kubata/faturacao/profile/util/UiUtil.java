package ao.allon.kubata.faturacao.profile.util;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.faturacao.ui.util.IconUtils;

public class UiUtil {
    public static FontIcon icon(String code, int size) {
        FontIcon fi = new FontIcon(code);
        fi.setIconSize(size);
        return fi;
    }

    public static FontIcon okIcon() {
        return IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL);
    }

    public static void showToast(StackPane root, String message) {
        Label toast = new Label(message);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        root.getChildren().add(toast);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.3), toast);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        ft.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.seconds(2), toast);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> root.getChildren().remove(toast));
            out.play();
        });
    }

    public static Label statusBadge(boolean ativo) {
        Label badge = new Label(ativo ? "Ativo" : "Inativo");
        return badge;
    }
}
