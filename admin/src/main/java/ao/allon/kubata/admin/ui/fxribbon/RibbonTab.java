package ao.allon.kubata.admin.ui.fxribbon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Representa uma aba (separador) do Ribbon.
 * Contém uma lista de {@link RibbonGroup} exibidos horizontalmente.
 */
public class RibbonTab {

    private final String title;
    private final ObservableList<RibbonGroup> groups = FXCollections.observableArrayList();
    private final HBox contentPane;

    public RibbonTab(String title) {
        this.title = title;

        contentPane = new HBox(0);
        contentPane.getStyleClass().add("ribbon-tab-content");
        contentPane.setAlignment(Pos.CENTER_LEFT);
        contentPane.setFillHeight(true);

        // Espaçador final para empurrar grupos à esquerda
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        contentPane.getChildren().add(spacer);
    }

    // ── API pública ────────────────────────────────────────────────────────────

    public String getTitle() {
        return title;
    }

    public ObservableList<RibbonGroup> getGroups() {
        return groups;
    }

    /**
     * Adiciona um grupo ao tab. O grupo é inserido antes do espaçador final.
     */
    public void addGroup(RibbonGroup group) {
        groups.add(group);
        int spacerIdx = contentPane.getChildren().size() - 1;
        contentPane.getChildren().add(spacerIdx, group);
    }

    /**
     * O painel HBox que contém todos os grupos desta tab.
     * É este painel que é colocado na área de conteúdo do Ribbon.
     */
    public HBox getContentPane() {
        return contentPane;
    }
}
