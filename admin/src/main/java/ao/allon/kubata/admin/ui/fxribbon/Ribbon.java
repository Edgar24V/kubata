package ao.allon.kubata.admin.ui.fxribbon;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Container principal do Ribbon.
 *
 * Estrutura visual:
 * ┌─────────────────────────────────────────────────────┐
 * │  [Tab: Plataforma] [Tab: Organização] [Tab: ...]    │  ← tabStrip (HBox)
 * ├─────────────────────────────────────────────────────┤
 * │  [Grupo: Início] [Grupo: Operações] ...             │  ← contentArea (ScrollPane)
 * └─────────────────────────────────────────────────────┘
 */
public class Ribbon extends VBox {

    private final ObservableList<RibbonTab> tabs = FXCollections.observableArrayList();

    private final HBox tabStrip;
    private final StackPane contentHost;
    private final Map<RibbonTab, Label> tabLabelMap = new HashMap<>();

    private RibbonTab selectedTab;

    public Ribbon() {
        getStyleClass().add("ribbon-bar");
        setFillWidth(true);

        // Carregar Estilos
        String cssPath = getClass().getResource("/css/fxribbon.css").toExternalForm();
        getStylesheets().add(cssPath);

        // ── Faixa de separadores (Tab Strip) ─────────────────────────────────
        tabStrip = new HBox(0);
        tabStrip.getStyleClass().add("ribbon-tab-strip");
        tabStrip.setAlignment(Pos.BOTTOM_LEFT);

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);
        tabStrip.getChildren().add(tabSpacer);

        // ── Área de conteúdo (troca conforme tab selecionada) ─────────────────
        contentHost = new StackPane();
        contentHost.getStyleClass().add("ribbon-content-host");

        getChildren().addAll(tabStrip, contentHost);
    }

    // ── API pública ────────────────────────────────────────────────────────────

    /**
     * Adiciona uma tab ao Ribbon. A primeira tab adicionada é automaticamente selecionada.
     */
    public void addTab(RibbonTab tab) {
        tabs.add(tab);

        Label tabLabel = buildTabLabel(tab);
        tabLabelMap.put(tab, tabLabel);

        // Inserir antes do spacer (último elemento)
        int spacerIdx = tabStrip.getChildren().size() - 1;
        tabStrip.getChildren().add(spacerIdx, tabLabel);

        // Envolver conteúdo num ScrollPane horizontal para suporte a overflow
        ScrollPane scroll = buildContentScroll(tab);
        scroll.setVisible(false);
        scroll.setManaged(false);
        contentHost.getChildren().add(scroll);

        if (tabs.size() == 1) {
            selectTab(tab);
        }
    }

    /** Seleciona programaticamente uma tab pelo seu índice (0-based). */
    public void selectTabByIndex(int index) {
        if (index >= 0 && index < tabs.size()) {
            selectTab(tabs.get(index));
        }
    }

    /** Seleciona programaticamente uma tab pelo título. */
    public void selectTabByTitle(String title) {
        tabs.stream()
            .filter(t -> t.getTitle().equals(title))
            .findFirst()
            .ifPresent(this::selectTab);
    }

    public ObservableList<RibbonTab> getTabs() {
        return tabs;
    }

    // ── Interno ────────────────────────────────────────────────────────────────

    private Label buildTabLabel(RibbonTab tab) {
        Label lbl = new Label(tab.getTitle());
        lbl.getStyleClass().add("ribbon-tab-label");
        lbl.setOnMouseClicked(e -> selectTab(tab));
        return lbl;
    }

    private ScrollPane buildContentScroll(RibbonTab tab) {
        ScrollPane scroll = new ScrollPane(tab.getContentPane());
        scroll.getStyleClass().add("ribbon-content-scroll");
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFocusTraversable(false);
        // Transparência total para o scroll
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void selectTab(RibbonTab tab) {
        // Actualiza estilos dos labels
        tabLabelMap.forEach((t, lbl) -> {
            if (t == tab) {
                if (!lbl.getStyleClass().contains("ribbon-tab-label-selected")) {
                    lbl.getStyleClass().add("ribbon-tab-label-selected");
                }
            } else {
                lbl.getStyleClass().remove("ribbon-tab-label-selected");
            }
        });

        // Oculta conteúdo anterior, mostra o novo
        contentHost.getChildren().forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });

        int idx = tabs.indexOf(tab);
        if (idx >= 0 && idx < contentHost.getChildren().size()) {
            var node = contentHost.getChildren().get(idx);
            node.setVisible(true);
            node.setManaged(true);

            // Animação suave de fade
            FadeTransition ft = new FadeTransition(Duration.millis(120), node);
            ft.setFromValue(0.6);
            ft.setToValue(1.0);
            ft.play();
        }

        selectedTab = tab;
    }

    /**
     * Destaca o botão cujo ID coincide com o fornecido (e desmarca os outros).
     * Útil para manter o Ribbon sincronizado com a view ativa.
     */
    public void highlightButton(String buttonId) {
        tabs.forEach(tab -> {
            tab.getGroups().forEach(group -> {
                // O RibbonGroup tem um HBox chamado contentBox que é o primeiro filho
                if (!group.getChildren().isEmpty() && group.getChildren().get(0) instanceof HBox contentBox) {
                    contentBox.getChildren().forEach(colNode -> {
                        if (colNode instanceof VBox column) {
                            column.getChildren().forEach(btnNode -> {
                                if (btnNode instanceof RibbonButton btn) {
                                    btn.setSelected(buttonId != null && buttonId.equals(btn.getId()));
                                }
                            });
                        }
                    });
                }
            });
        });
    }
}
