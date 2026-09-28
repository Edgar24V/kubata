package ao.allon.kubata.admin.ui.fxribbon;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ribbon principal do Kubata.
 *
 * Visual: linguagem Microsoft 365 / Excel 2024, adaptada à identidade
 * verde do Kubata.
 *
 * Estrutura:
 *   [abas]
 *   [grupos de comandos...........................................][Mais »]
 *
 * O conteúdo continua a ser construído pelo RibbonProgrammaticService.
 * Esta classe trata apenas da apresentação, seleção e overflow responsivo.
 */
public class Ribbon extends VBox {

    private static final double OVERFLOW_BUTTON_WIDTH = 34.0;
    private static final double CONTENT_HORIZONTAL_MARGIN = 6.0;

    private final ObservableList<RibbonTab> tabs = FXCollections.observableArrayList();

    private final HBox tabStrip;
    private final StackPane contentHost;
    private final Button overflowButton;

    private final Map<RibbonTab, Label> tabLabelMap = new HashMap<>();
    private final Map<RibbonTab, ScrollPane> scrollMap = new HashMap<>();
    private final Map<RibbonTab, List<RibbonGroup>> hiddenGroupsMap = new HashMap<>();

    private final RibbonOverflowPopup overflowPopup = new RibbonOverflowPopup();

    private RibbonTab selectedTab;
    private RibbonTab overflowTab;
    private boolean updatingOverflow;

    public Ribbon() {
        getStyleClass().add("ribbon-bar");
        setFillWidth(true);
        setSpacing(0);

        String cssPath = getClass().getResource("/css/fxribbon.css").toExternalForm();
        getStylesheets().add(cssPath);

        tabStrip = new HBox(0);
        tabStrip.getStyleClass().add("ribbon-tab-strip");
        tabStrip.setAlignment(Pos.BOTTOM_LEFT);
        tabStrip.setFillHeight(true);

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);
        tabStrip.getChildren().add(tabSpacer);

        contentHost = new StackPane();
        contentHost.getStyleClass().add("ribbon-content-host");
        contentHost.setAlignment(Pos.CENTER_LEFT);

        overflowButton = new Button("»");
        overflowButton.getStyleClass().add("ribbon-overflow-button");
        overflowButton.setTooltip(new javafx.scene.control.Tooltip("Mostrar mais comandos"));
        overflowButton.setFocusTraversable(true);
        overflowButton.setMnemonicParsing(false);
        overflowButton.setVisible(false);
        overflowButton.setManaged(false);
        overflowButton.setOnAction(e -> showOverflow());

        StackPane.setAlignment(overflowButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(overflowButton, new Insets(0, 2, 0, 0));
        contentHost.getChildren().add(overflowButton);

        getChildren().addAll(tabStrip, contentHost);

        widthProperty().addListener((obs, oldWidth, newWidth) -> requestOverflowUpdate());
        contentHost.widthProperty().addListener((obs, oldWidth, newWidth) -> requestOverflowUpdate());
        contentHost.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                requestOverflowUpdate();
            }
        });

        overflowPopup.setOnHidden(event -> {
            if (overflowTab != null) {
                overflowPopup.setGroups(List.of());
                restoreAllGroups(overflowTab);
                overflowTab = null;
                requestOverflowUpdate();
            }
        });
    }

    /** Adiciona uma tab; a primeira torna-se automaticamente ativa. */
    public void addTab(RibbonTab tab) {
        if (tab == null || tabs.contains(tab)) {
            return;
        }

        tabs.add(tab);
        hiddenGroupsMap.put(tab, new ArrayList<>());

        Label tabLabel = buildTabLabel(tab);
        tabLabelMap.put(tab, tabLabel);

        int spacerIdx = Math.max(0, tabStrip.getChildren().size() - 1);
        tabStrip.getChildren().add(spacerIdx, tabLabel);

        ScrollPane scroll = buildContentScroll(tab);
        scrollMap.put(tab, scroll);
        contentHost.getChildren().add(0, scroll);

        scroll.setVisible(false);
        scroll.setManaged(false);

        if (tabs.size() == 1) {
            selectTab(tab);
        } else {
            requestOverflowUpdate();
        }
    }

    public void selectTabByIndex(int index) {
        if (index >= 0 && index < tabs.size()) {
            selectTab(tabs.get(index));
        }
    }

    public void selectTabByTitle(String title) {
        tabs.stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .ifPresent(this::selectTab);
    }

    public ObservableList<RibbonTab> getTabs() {
        return tabs;
    }

    public RibbonTab getSelectedTab() {
        return selectedTab;
    }

    private Label buildTabLabel(RibbonTab tab) {
        Label lbl = new Label(tab.getTitle());
        lbl.getStyleClass().add("ribbon-tab-label");
        lbl.setFocusTraversable(true);
        lbl.setOnMouseClicked(e -> selectTab(tab));
        lbl.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> selectTab(tab);
                default -> { }
            }
        });
        return lbl;
    }

    private ScrollPane buildContentScroll(RibbonTab tab) {
        ScrollPane scroll = new ScrollPane(tab.getContentPane());
        scroll.getStyleClass().add("ribbon-content-scroll");
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setFocusTraversable(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void selectTab(RibbonTab tab) {
        if (tab == null || !tabs.contains(tab)) {
            return;
        }

        closeOverflowPopup();

        tabLabelMap.forEach((t, lbl) -> {
            if (t == tab) {
                if (!lbl.getStyleClass().contains("ribbon-tab-label-selected")) {
                    lbl.getStyleClass().add("ribbon-tab-label-selected");
                }
            } else {
                lbl.getStyleClass().remove("ribbon-tab-label-selected");
            }
        });

        scrollMap.forEach((t, scroll) -> {
            boolean active = t == tab;
            scroll.setVisible(active);
            scroll.setManaged(active);
        });

        ScrollPane activeScroll = scrollMap.get(tab);
        if (activeScroll != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(110), activeScroll);
            ft.setFromValue(0.72);
            ft.setToValue(1.0);
            ft.play();
        }

        selectedTab = tab;
        requestOverflowUpdate();
    }

    /**
     * Calcula quais grupos conseguem permanecer visíveis.
     * Os últimos grupos são deslocados para o popup para preservar os
     * primeiros comandos da tab.
     */
    private void updateOverflow() {
        if (updatingOverflow || selectedTab == null) {
            return;
        }

        updatingOverflow = true;
        try {
            closeOverflowPopup();
            restoreAllGroups(selectedTab);

            double available = contentHost.getWidth()
                    - OVERFLOW_BUTTON_WIDTH
                    - CONTENT_HORIZONTAL_MARGIN;

            if (available < 80) {
                hideAllButFirstGroup(selectedTab);
                return;
            }

            List<RibbonGroup> groups = selectedTab.getGroups();
            List<RibbonGroup> hidden = hiddenGroupsMap.get(selectedTab);

            hidden.clear();

            double required = 8;
            int visibleCount = groups.size();

            for (RibbonGroup group : groups) {
                required += preferredGroupWidth(group);
            }

            while (visibleCount > 1 && required > available) {
                RibbonGroup group = groups.get(visibleCount - 1);
                hidden.add(0, group);
                required -= preferredGroupWidth(group);
                visibleCount--;
            }

            if (!hidden.isEmpty()) {
                selectedTab.getContentPane().getChildren().removeAll(hidden);
                NodeOrder.ensureSpacerAtEnd(selectedTab.getContentPane());
            }

            boolean showOverflow = !hidden.isEmpty();
            overflowButton.setVisible(showOverflow);
            overflowButton.setManaged(showOverflow);

        } finally {
            updatingOverflow = false;
        }
    }

    private void hideAllButFirstGroup(RibbonTab tab) {
        restoreAllGroups(tab);

        List<RibbonGroup> groups = tab.getGroups();
        List<RibbonGroup> hidden = hiddenGroupsMap.get(tab);
        hidden.clear();

        if (groups.size() <= 1) {
            overflowButton.setVisible(false);
            overflowButton.setManaged(false);
            return;
        }

        hidden.addAll(groups.subList(1, groups.size()));
        tab.getContentPane().getChildren().removeAll(hidden);
        NodeOrder.ensureSpacerAtEnd(tab.getContentPane());

        overflowButton.setVisible(true);
        overflowButton.setManaged(true);
    }

    private double preferredGroupWidth(RibbonGroup group) {
        double width = group.prefWidth(-1);

        if (width <= 0) {
            width = group.getLayoutBounds().getWidth();
        }

        if (width <= 0) {
            width = 120;
        }

        return Math.max(88, width);
    }

    private void restoreAllGroups(RibbonTab tab) {
        if (tab == null) {
            return;
        }

        HBox pane = tab.getContentPane();
        pane.getChildren().removeIf(node -> node instanceof RibbonGroup);

        int insertIndex = Math.max(0, pane.getChildren().size());
        for (RibbonGroup group : tab.getGroups()) {
            pane.getChildren().add(insertIndex++, group);
        }

        NodeOrder.ensureSpacerAtEnd(pane);

        List<RibbonGroup> hidden = hiddenGroupsMap.get(tab);
        if (hidden != null) {
            hidden.clear();
        }
    }

    private void requestOverflowUpdate() {
        Platform.runLater(this::updateOverflow);
    }

    private void showOverflow() {
        if (selectedTab == null) {
            return;
        }

        List<RibbonGroup> hidden = hiddenGroupsMap.get(selectedTab);
        if (hidden == null || hidden.isEmpty()) {
            return;
        }

        closeOverflowPopup();

        overflowTab = selectedTab;
        List<RibbonGroup> popupGroups = new ArrayList<>(hidden);
        selectedTab.getContentPane().getChildren().removeAll(popupGroups);
        overflowPopup.setGroups(popupGroups);

        Point2D point = overflowButton.localToScreen(
                Math.max(0, overflowButton.getWidth() - 12),
                overflowButton.getHeight()
        );

        if (point != null) {
            overflowPopup.show(overflowButton, point.getX(), point.getY());
        }
    }

    private void closeOverflowPopup() {
        if (overflowPopup.isShowing()) {
            overflowPopup.hide();
        }

        if (overflowTab != null) {
            overflowPopup.setGroups(List.of());
            restoreAllGroups(overflowTab);
            overflowTab = null;
        }
    }

    /**
     * Destaca o botão cujo ID coincide com o fornecido.
     * Mantém o Ribbon sincronizado com a view ativa.
     */
    public void highlightButton(String buttonId) {
        tabs.forEach(tab -> {
            tab.getGroups().forEach(group -> {
                if (!group.getChildren().isEmpty()
                        && group.getChildren().get(0) instanceof HBox contentBox) {

                    contentBox.getChildren().forEach(colNode -> {
                        if (colNode instanceof VBox column) {
                            column.getChildren().forEach(btnNode -> {
                                if (btnNode instanceof RibbonButton btn) {
                                    btn.setSelected(buttonId != null
                                            && buttonId.equals(btn.getId()));
                                }
                            });
                        }
                    });
                }
            });
        });
    }

    private static final class NodeOrder {
        private NodeOrder() { }

        static void ensureSpacerAtEnd(HBox pane) {
            for (int i = 0; i < pane.getChildren().size(); i++) {
                if (pane.getChildren().get(i) instanceof Region
                        && !(pane.getChildren().get(i) instanceof RibbonGroup)) {
                    Node spacer = pane.getChildren().remove(i);
                    pane.getChildren().add(spacer);
                    break;
                }
            }
        }
    }
}