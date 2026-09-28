package ao.allon.kubata.admin.ui.fxribbon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Aba do Ribbon.
 */
public class RibbonTab {

    private final String title;
    private final ObservableList<RibbonGroup> groups = FXCollections.observableArrayList();
    private final HBox contentPane;

    public RibbonTab(String title) {
        this.title = title == null ? "" : title;

        contentPane = new HBox(1);
        contentPane.getStyleClass().add("ribbon-tab-content");
        contentPane.setAlignment(Pos.TOP_LEFT);
        contentPane.setFillHeight(true);
        contentPane.setPadding(new Insets(3, 8, 2, 8));
        contentPane.setMinWidth(0);

        Region spacer = new Region();
        spacer.getStyleClass().add("ribbon-content-spacer");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        contentPane.getChildren().add(spacer);
    }

    public String getTitle() {
        return title;
    }

    public ObservableList<RibbonGroup> getGroups() {
        return groups;
    }

    public void addGroup(RibbonGroup group) {
        if (group == null) {
            return;
        }

        groups.add(group);
        int spacerIdx = Math.max(0, contentPane.getChildren().size() - 1);
        contentPane.getChildren().add(spacerIdx, group);
        HBox.setMargin(group, new Insets(0, 2, 0, 2));
    }

    public HBox getContentPane() {
        return contentPane;
    }
}
