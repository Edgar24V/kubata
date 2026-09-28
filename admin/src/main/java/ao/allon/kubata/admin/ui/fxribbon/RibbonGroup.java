package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de comandos do Ribbon.
 *
 * Estrutura:
 *   área de comandos
 *   margem vertical
 *   título da categoria
 */
public class RibbonGroup extends VBox {

    private final String groupTitle;
    private final HBox contentBox;
    private final VBox titleArea;
    private final Label titleLabel;
    private final List<VBox> columns = new ArrayList<>();

    public RibbonGroup(String title) {
        this.groupTitle = title == null ? "" : title;

        getStyleClass().add("ribbon-group");
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setMinWidth(0);
        setPrefHeight(108);
        setMinHeight(108);
        setMaxHeight(108);

        contentBox = new HBox(2);
        contentBox.getStyleClass().add("ribbon-group-content");
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setFillHeight(true);
        contentBox.setPadding(new Insets(4, 5, 0, 5));

        titleArea = new VBox();
        titleArea.getStyleClass().add("ribbon-group-title-area");
        titleArea.setAlignment(Pos.CENTER);
        titleArea.setFillWidth(true);
        titleArea.setMinHeight(25);
        titleArea.setPrefHeight(25);
        titleArea.setMaxHeight(25);
        titleArea.setPadding(new Insets(2, 6, 2, 6));

        titleLabel = new Label(this.groupTitle);
        titleLabel.getStyleClass().add("ribbon-group-title");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setMinWidth(0);
        titleLabel.setMinHeight(18);
        titleLabel.setPrefHeight(21);
        titleLabel.setMaxHeight(23);
        titleLabel.setPadding(new Insets(0, 2, 0, 2));

        titleArea.getChildren().add(titleLabel);

        getChildren().addAll(contentBox, titleArea);
    }

    /**
     * Adiciona uma coluna de comandos.
     * Os separadores recebem margem própria para nunca colidir com os botões.
     */
    public void addColumn(List<RibbonButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            return;
        }

        VBox column = new VBox(2);
        column.getStyleClass().add("ribbon-column-container");
        column.setAlignment(Pos.TOP_CENTER);
        column.setFillWidth(true);
        column.setMinWidth(0);

        for (RibbonButton button : buttons) {
            if (button == null) {
                continue;
            }
            column.getChildren().add(button);
        }

        if (!columns.isEmpty()) {
            Region separator = new Region();
            separator.getStyleClass().add("ribbon-column-separator");
            separator.setPrefWidth(1);
            separator.setMinWidth(1);
            separator.setMaxWidth(1);
            HBox.setMargin(separator, new Insets(3, 4, 3, 4));
            contentBox.getChildren().add(separator);
        }

        columns.add(column);
        contentBox.getChildren().add(column);
    }

    public void addLargeButton(RibbonButton button) {
        addColumn(List.of(button));
    }

    public void addSmallButtons(List<RibbonButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            return;
        }

        if (buttons.size() > 3) {
            throw new IllegalArgumentException("Máximo de 3 botões SMALL por coluna.");
        }

        addColumn(buttons);
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public HBox getContentBox() {
        return contentBox;
    }
}
