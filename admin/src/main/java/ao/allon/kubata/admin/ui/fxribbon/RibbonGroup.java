package ao.allon.kubata.admin.ui.fxribbon;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de comandos do Ribbon.
 *
 * Layout:
 *   [comandos]
 *   [título]
 *
 * Cada grupo recebe uma linha vertical no lado direito através do CSS,
 * reproduzindo a separação visual típica do Office.
 */
public class RibbonGroup extends VBox {

    private final String groupTitle;
    private final HBox contentBox;
    private final List<VBox> columns = new ArrayList<>();

    public RibbonGroup(String title) {
        this.groupTitle = title;

        getStyleClass().add("ribbon-group");
        setFillWidth(true);
        setAlignment(Pos.TOP_CENTER);
        setMinWidth(0);
        setMaxHeight(Double.MAX_VALUE);

        contentBox = new HBox(2);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(4, 7, 3, 7));
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.NEVER);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ribbon-group-title");
        titleLabel.setWrapText(true);
        titleLabel.prefWidthProperty().bind(widthProperty().subtract(10));
        titleLabel.maxWidthProperty().bind(widthProperty().subtract(10));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        titleLabel.setPadding(new Insets(3, 7, 3, 7));

        VBox titleArea = new VBox();
        titleArea.setAlignment(Pos.BOTTOM_CENTER);
        titleArea.setMinWidth(0);
        titleArea.setMaxWidth(Double.MAX_VALUE);
        titleArea.getChildren().add(titleLabel);

        getChildren().addAll(contentBox, titleArea);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        VBox.setVgrow(titleArea, Priority.NEVER);
    }

    /**
     * Adiciona uma coluna de comandos.
     * Separadores internos são inseridos automaticamente entre colunas.
     */
    public void addColumn(List<RibbonButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            return;
        }

        VBox column = new VBox(1);
        column.setAlignment(Pos.TOP_LEFT);
        column.setFillWidth(true);
        column.setMinWidth(0);
        column.setMaxWidth(Double.MAX_VALUE);
        column.getStyleClass().add("ribbon-column-container");

        for (RibbonButton btn : buttons) {
            if (btn == null) {
                continue;
            }

            column.getChildren().add(btn);

            if (btn.getRibbonSize() == RibbonButtonSize.LARGE) {
                VBox.setVgrow(btn, Priority.ALWAYS);
            }
        }

        if (!columns.isEmpty()) {
            Region separator = new Region();
            separator.getStyleClass().add("ribbon-column-separator");
            separator.setPrefWidth(1);
            separator.setMinWidth(1);
            separator.setMaxWidth(1);
            HBox.setMargin(separator, new Insets(0, 3, 0, 3));
            contentBox.getChildren().add(separator);
        }

        columns.add(column);
        contentBox.getChildren().add(column);
    }

    public void addLargeButton(RibbonButton button) {
        addColumn(List.of(button));
    }

    /**
     * Adiciona no máximo três comandos SMALL à mesma coluna.
     */
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
