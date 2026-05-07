package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de controlos do Ribbon.
 *
 * Estrutura visual:
 * <pre>
 * ┌──────────────────────────┐
 * │  [btn] [col] [col] ...   │  ← contentBox (HBox)
 * │  ─────────────────────── │  ← separador horizontal subtil
 * │         TÍTULO           │  ← titleLabel (Label centrado)
 * └──────────────────────────┘
 * O bordo direito do grupo actua como separador vertical entre grupos.
 * </pre>
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
        setMaxHeight(Double.MAX_VALUE); // Permite crescer até ao limite do contentHost

        contentBox = new HBox(2);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(4, 6, 2, 6));
        HBox.setHgrow(contentBox, Priority.NEVER);

        // Separador horizontal subtil entre conteúdo e título
        Region separator = new Region();
        separator.getStyleClass().add("ribbon-group-separator");
        separator.setMinHeight(1);
        separator.setPrefHeight(1);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ribbon-group-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPadding(new Insets(1, 4, 3, 4));

        VBox titleArea = new VBox(0);
        titleArea.setAlignment(Pos.BOTTOM_CENTER);
        titleArea.getChildren().addAll(separator, titleLabel);

        getChildren().addAll(contentBox, titleArea);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        VBox.setVgrow(titleArea, Priority.NEVER);
    }

    // ── API pública ────────────────────────────────────────────────────────────

    /**
     * Adiciona uma coluna de botões. Insere um separador vertical fino
     * entre colunas quando já existe pelo menos uma.
     */
    public void addColumn(List<RibbonButton> buttons) {
        VBox column = new VBox(1);
        column.setAlignment(Pos.TOP_LEFT);
        column.getStyleClass().add("ribbon-column-container");

        for (RibbonButton btn : buttons) {
            column.getChildren().add(btn);
            if (btn.getRibbonSize() == RibbonButtonSize.LARGE) {
                VBox.setVgrow(btn, Priority.ALWAYS);
            }
        }

        // Separador vertical fino entre colunas (exceto antes da primeira)
        if (!columns.isEmpty()) {
            Region vSep = new Region();
            vSep.getStyleClass().add("ribbon-column-separator");
            vSep.setPrefWidth(1);
            vSep.setMinWidth(1);
            contentBox.getChildren().add(vSep);
        }

        columns.add(column);
        contentBox.getChildren().add(column);
    }

    /** Adiciona um único botão LARGE como coluna própria. */
    public void addLargeButton(RibbonButton button) {
        addColumn(List.of(button));
    }

    /**
     * Adiciona até 3 botões SMALL numa mesma coluna.
     *
     * @throws IllegalArgumentException se forem passados mais de 3 botões
     */
    public void addSmallButtons(List<RibbonButton> buttons) {
        if (buttons.size() > 3) {
            throw new IllegalArgumentException("Máximo de 3 botões SMALL por coluna.");
        }
        addColumn(buttons);
    }

    public String getGroupTitle() {
        return groupTitle;
    }
}
