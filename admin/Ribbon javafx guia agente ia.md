# GUIA COMPLETO — RIBBON ESTILO OFFICE 2020 EM JAVAFX + JFXTRAS
## Documento para Agente de IA — Implementação Passo a Passo

---

## CONTEXTO E OBJECTIVOS

Implementar um componente `RibbonBar` reutilizável em JavaFX que reproduz o comportamento do Ribbon do Microsoft Office 2020/365, com as seguintes características obrigatórias:

- **Separadores (Tabs)** clicáveis no topo
- **Grupos** de botões com título em baixo e separador vertical entre grupos
- **Agrupamento vertical** de botões dentro de cada grupo (até 3 linhas)
- **Overflow automático**: quando os grupos não cabem na largura disponível, oculta os que não cabem e mostra um botão `»` que abre um popup com os grupos ocultos
- Estilo visual via **JFXtras** (CSS customizável)

---

## STACK TÉCNICA

```xml
<!-- pom.xml — dependências Maven -->
<dependency>
    <groupId>org.jfxtras</groupId>
    <artifactId>jfxtras-controls</artifactId>
    <version>17-r1</version>
</dependency>
<dependency>
    <groupId>org.jfxtras</groupId>
    <artifactId>jfxtras-styles</artifactId>
    <version>17-r1</version>
</dependency>
<!-- JavaFX via OpenJFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21</version>
</dependency>
```

**Java**: 17 ou superior  
**JavaFX**: 21  
**JFXtras**: 17-r1  

---

## PASSO 1 — ESTRUTURA DE CLASSES (HIERARQUIA)

Criar os seguintes ficheiros Java no pacote `com.empresa.ribbon`:

```
com.empresa.ribbon/
├── RibbonBar.java          ← Componente raiz (VBox)
├── RibbonTab.java          ← Um separador com os seus grupos
├── RibbonGroup.java        ← Um grupo com título e botões
├── RibbonButton.java       ← Botão individual (LARGE ou SMALL)
├── RibbonButtonSize.java   ← Enum: LARGE, SMALL
└── RibbonOverflowPopup.java ← Popup que mostra grupos ocultos
```

---

## PASSO 2 — ENUM `RibbonButtonSize`

```java
// RibbonButtonSize.java
package com.empresa.ribbon;

public enum RibbonButtonSize {
    /** Botão grande: ícone 32×32 + rótulo abaixo. Ocupa coluna inteira. */
    LARGE,
    /** Botão pequeno: ícone 16×16 + rótulo à direita. Partilha coluna (até 3). */
    SMALL
}
```

---

## PASSO 3 — `RibbonButton`

```java
// RibbonButton.java
package com.empresa.ribbon;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class RibbonButton extends Button {

    private final RibbonButtonSize size;

    /**
     * @param label   Texto do botão
     * @param icon    Node gráfico (SVGPath, ImageView, etc.)
     * @param size    LARGE ou SMALL
     * @param tooltip Texto do tooltip (pode ser null)
     * @param action  Runnable executado ao clicar
     */
    public RibbonButton(String label, Node icon, RibbonButtonSize size,
                        String tooltip, Runnable action) {
        this.size = size;

        getStyleClass().add("ribbon-button");
        getStyleClass().add(size == RibbonButtonSize.LARGE
                ? "ribbon-button-large"
                : "ribbon-button-small");

        if (size == RibbonButtonSize.LARGE) {
            // Ícone grande em cima, rótulo em baixo
            VBox content = new VBox(2);
            content.setAlignment(Pos.CENTER);
            if (icon != null) {
                icon.getStyleClass().add("ribbon-icon-large"); // 32×32 via CSS
                content.getChildren().add(icon);
            }
            Text text = new Text(label);
            text.getStyleClass().add("ribbon-button-text");
            content.getChildren().add(text);
            setGraphic(content);
        } else {
            // Ícone pequeno à esquerda, rótulo à direita
            HBox content = new HBox(4);
            content.setAlignment(Pos.CENTER_LEFT);
            if (icon != null) {
                icon.getStyleClass().add("ribbon-icon-small"); // 16×16 via CSS
                content.getChildren().add(icon);
            }
            Text text = new Text(label);
            text.getStyleClass().add("ribbon-button-text");
            content.getChildren().add(text);
            setGraphic(content);
        }

        // Sem texto nativo do Button (usamos gráfico customizado)
        setText("");

        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(new Tooltip(tooltip));
        }

        if (action != null) {
            setOnAction(e -> action.run());
        }
    }

    public RibbonButtonSize getRibbonSize() {
        return size;
    }
}
```

---

## PASSO 4 — `RibbonGroup`

```java
// RibbonGroup.java
package com.empresa.ribbon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Um grupo do Ribbon. Estrutura visual:
 *
 *  ┌─────────────────────────────┐
 *  │  [Col1]  [Col2]  [Col3] ... │  ← conteúdo (HBox de VBox)
 *  ├─────────────────────────────┤
 *  │        Título do Grupo      │  ← label em baixo
 *  └─────────────────────────────┘
 */
public class RibbonGroup extends VBox {

    private final String groupTitle;
    private final HBox contentBox;          // Colunas de botões
    private final List<VBox> columns = new ArrayList<>();

    public RibbonGroup(String title) {
        this.groupTitle = title;

        getStyleClass().add("ribbon-group");
        setFillWidth(false);
        setAlignment(Pos.TOP_CENTER);

        // Área de conteúdo (botões)
        contentBox = new HBox(2);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(4, 6, 2, 6));
        HBox.setHgrow(contentBox, Priority.NEVER);

        // Separador horizontal entre conteúdo e título
        Region separator = new Region();
        separator.getStyleClass().add("ribbon-group-separator");
        separator.setMinHeight(1);
        separator.setPrefHeight(1);

        // Título do grupo em baixo
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ribbon-group-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPadding(new Insets(1, 4, 2, 4));

        getChildren().addAll(contentBox, separator, titleLabel);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
    }

    /**
     * Adiciona botões numa nova coluna vertical.
     * Para botões LARGE: cada um ocupa a coluna inteira.
     * Para botões SMALL: até 3 botões por coluna.
     *
     * @param buttons Lista de RibbonButton a colocar na mesma coluna
     */
    public void addColumn(List<RibbonButton> buttons) {
        VBox column = new VBox(1);
        column.setAlignment(Pos.TOP_LEFT);
        column.getStyleClass().add("ribbon-button-column");

        for (RibbonButton btn : buttons) {
            column.getChildren().add(btn);
            if (btn.getRibbonSize() == RibbonButtonSize.LARGE) {
                // Botão LARGE ocupa altura máxima da coluna
                VBox.setVgrow(btn, Priority.ALWAYS);
            }
        }

        columns.add(column);
        contentBox.getChildren().add(column);

        // Separador vertical entre colunas (opcional, estético)
        if (contentBox.getChildren().size() > 1) {
            Region vSep = new Region();
            vSep.getStyleClass().add("ribbon-column-separator");
            vSep.setPrefWidth(1);
            // Inserir antes da coluna recém-adicionada
            int idx = contentBox.getChildren().indexOf(column);
            contentBox.getChildren().add(idx, vSep);
        }
    }

    /**
     * Atalho: adiciona um único botão LARGE numa coluna própria.
     */
    public void addLargeButton(RibbonButton button) {
        addColumn(List.of(button));
    }

    /**
     * Atalho: adiciona até 3 botões SMALL numa mesma coluna.
     */
    public void addSmallButtons(RibbonButton... buttons) {
        if (buttons.length > 3) {
            throw new IllegalArgumentException(
                "Máximo de 3 botões SMALL por coluna. Recebido: " + buttons.length);
        }
        addColumn(List.of(buttons));
    }

    public String getGroupTitle() {
        return groupTitle;
    }
}
```

---

## PASSO 5 — `RibbonOverflowPopup`

```java
// RibbonOverflowPopup.java
package com.empresa.ribbon;

import javafx.geometry.Insets;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Popup flutuante que aparece ao clicar no botão "»" de overflow.
 * Mostra os grupos que não cabem na largura visível do RibbonTab.
 */
public class RibbonOverflowPopup extends PopupControl {

    private final VBox container;

    public RibbonOverflowPopup() {
        container = new VBox(4);
        container.setPadding(new Insets(8));
        container.getStyleClass().add("ribbon-overflow-popup");

        // Configurar PopupControl
        setAutoHide(true);       // fecha ao clicar fora
        setAutoFix(true);        // reposiciona se sair do ecrã
        setHideOnEscape(true);

        // Skin mínimo obrigatório para PopupControl
        setSkin(new javafx.scene.control.skin.PopupControlSkin<>(this) {
            @Override
            public javafx.scene.Node getNode() {
                return container;
            }
        });
    }

    /**
     * Substitui o conteúdo do popup com os grupos ocultos.
     * Os grupos são exibidos verticalmente no popup.
     *
     * @param hiddenGroups Lista de RibbonGroup que não caberam no layout
     */
    public void setGroups(List<RibbonGroup> hiddenGroups) {
        container.getChildren().clear();
        // Cada grupo é apresentado na horizontal dentro do popup
        for (RibbonGroup group : hiddenGroups) {
            // Clonar visualmente o grupo para o popup
            // (o grupo original fica no Tab, mas oculto)
            container.getChildren().add(group);
        }
    }
}
```

---

## PASSO 6 — `RibbonTab`

```java
// RibbonTab.java
package com.empresa.ribbon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Conteúdo de um separador do Ribbon.
 * Contém uma fila horizontal de RibbonGroup.
 * Quando a largura não chega, oculta grupos da direita
 * e exibe o botão de overflow "»".
 */
public class RibbonTab extends HBox {

    private final List<RibbonGroup> groups = new ArrayList<>();
    private final Button overflowButton;
    private final RibbonOverflowPopup overflowPopup;

    public RibbonTab() {
        getStyleClass().add("ribbon-tab-content");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(new Insets(0));

        // Botão de overflow — aparece apenas quando necessário
        overflowButton = new Button("»");
        overflowButton.getStyleClass().add("ribbon-overflow-button");
        overflowButton.setVisible(false);
        overflowButton.setManaged(false);

        overflowPopup = new RibbonOverflowPopup();

        overflowButton.setOnAction(e -> {
            if (overflowPopup.isShowing()) {
                overflowPopup.hide();
            } else {
                // Mostrar popup abaixo do botão de overflow
                overflowPopup.show(overflowButton,
                    overflowButton.localToScreen(0, overflowButton.getHeight()).getX(),
                    overflowButton.localToScreen(0, overflowButton.getHeight()).getY());
            }
        });

        // Listener de largura: recalcular quais grupos cabem
        widthProperty().addListener((obs, oldW, newW) ->
            recalculateOverflow(newW.doubleValue()));
    }

    /**
     * Adiciona um grupo ao separador.
     */
    public void addGroup(RibbonGroup group) {
        groups.add(group);
        // Inserir antes do botão de overflow (que está no fim)
        getChildren().add(group);
        if (!getChildren().contains(overflowButton)) {
            getChildren().add(overflowButton);
        }
    }

    /**
     * Recalcula quais grupos são visíveis com base na largura disponível.
     * Grupos que não cabem são ocultados e passam para o popup.
     *
     * @param availableWidth Largura actual do RibbonTab em pixels
     */
    private void recalculateOverflow(double availableWidth) {
        if (groups.isEmpty()) return;

        // Largura reservada para o botão de overflow
        double overflowBtnWidth = 28.0;
        double usedWidth = 0;
        List<RibbonGroup> hidden = new ArrayList<>();
        boolean anyHidden = false;

        for (RibbonGroup group : groups) {
            // Obter largura preferida real do grupo
            double groupWidth = group.prefWidth(-1);

            if (!anyHidden && (usedWidth + groupWidth + overflowBtnWidth) <= availableWidth) {
                // Grupo cabe → mostrar
                group.setVisible(true);
                group.setManaged(true);
                usedWidth += groupWidth;
            } else {
                // Grupo não cabe → ocultar e adicionar ao popup
                group.setVisible(false);
                group.setManaged(false);
                hidden.add(group);
                anyHidden = true;
            }
        }

        // Mostrar ou ocultar botão de overflow
        overflowButton.setVisible(anyHidden);
        overflowButton.setManaged(anyHidden);

        if (anyHidden) {
            overflowPopup.setGroups(hidden);
        }
    }

    public List<RibbonGroup> getGroups() {
        return groups;
    }
}
```

---

## PASSO 7 — `RibbonBar` (COMPONENTE RAIZ)

```java
// RibbonBar.java
package com.empresa.ribbon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente raiz do Ribbon.
 *
 * Estrutura visual:
 *
 *  ┌──────────────────────────────────────────────────────┐
 *  │  [Início]  [Inserir]  [Esquema]  [Ver]               │ ← tabBar (HBox)
 *  ├──────────────────────────────────────────────────────┤
 *  │  [Grupo1│]  [Grupo2│]  [Grupo3│]  ...  [»]          │ ← tabContent (StackPane)
 *  └──────────────────────────────────────────────────────┘
 *
 * Uso:
 *   RibbonBar ribbon = new RibbonBar();
 *   ribbon.addTab("Início", tabInicio);
 *   ribbon.addTab("Inserir", tabInserir);
 *   // Adicionar ao layout principal no topo
 */
public class RibbonBar extends VBox {

    private final HBox tabBar;
    private final StackPane tabContent;
    private final ToggleGroup tabToggleGroup;

    private final List<String> tabNames = new ArrayList<>();
    private final List<RibbonTab> tabPanes = new ArrayList<>();

    public RibbonBar() {
        getStyleClass().add("ribbon-bar");
        setFillWidth(true);

        // Barra de separadores (topo)
        tabBar = new HBox(0);
        tabBar.getStyleClass().add("ribbon-tab-bar");
        tabBar.setAlignment(Pos.BOTTOM_LEFT);
        tabBar.setPadding(new Insets(0, 0, 0, 4));

        tabToggleGroup = new ToggleGroup();

        // Área de conteúdo do separador activo
        tabContent = new StackPane();
        tabContent.getStyleClass().add("ribbon-tab-content-area");
        tabContent.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(tabBar, tabContent);
        VBox.setVgrow(tabContent, Priority.NEVER);
    }

    /**
     * Adiciona um separador ao Ribbon.
     *
     * @param title Texto do separador (ex: "Início")
     * @param tab   RibbonTab com os grupos e botões
     */
    public void addTab(String title, RibbonTab tab) {
        tabNames.add(title);
        tabPanes.add(tab);

        // Botão do separador
        ToggleButton tabButton = new ToggleButton(title);
        tabButton.getStyleClass().add("ribbon-tab-button");
        tabButton.setToggleGroup(tabToggleGroup);
        tabButton.setFocusTraversable(false);

        // Ao seleccionar, mostrar o conteúdo correspondente
        tabButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                tabContent.getChildren().setAll(tab);
            }
        });

        tabBar.getChildren().add(tabButton);

        // Seleccionar o primeiro separador automaticamente
        if (tabPanes.size() == 1) {
            tabButton.setSelected(true);
            tabContent.getChildren().setAll(tab);
        }
    }

    /**
     * Selecciona programaticamente um separador pelo índice (0-based).
     */
    public void selectTab(int index) {
        if (index >= 0 && index < tabBar.getChildren().size()) {
            ToggleButton btn = (ToggleButton) tabBar.getChildren().get(index);
            btn.setSelected(true);
        }
    }

    /**
     * Selecciona programaticamente um separador pelo título.
     */
    public void selectTab(String title) {
        int idx = tabNames.indexOf(title);
        if (idx >= 0) selectTab(idx);
    }
}
```

---

## PASSO 8 — CSS (`ribbon.css`)

Colocar em `src/main/resources/com/empresa/ribbon/ribbon.css`:

```css
/* ═══════════════════════════════════════════════
   RIBBON — Estilos base (tema claro Office 2020)
   ═══════════════════════════════════════════════ */

/* Raiz do Ribbon */
.ribbon-bar {
    -fx-background-color: #f3f3f3;
    -fx-border-color: transparent transparent #d1d1d1 transparent;
    -fx-border-width: 0 0 1 0;
}

/* Barra de separadores */
.ribbon-tab-bar {
    -fx-background-color: #f3f3f3;
    -fx-min-height: 32px;
    -fx-pref-height: 32px;
}

/* Botões de separador */
.ribbon-tab-button {
    -fx-background-color: transparent;
    -fx-text-fill: #444444;
    -fx-font-size: 12px;
    -fx-font-family: "Segoe UI", "Arial", sans-serif;
    -fx-padding: 6 12 6 12;
    -fx-background-radius: 0;
    -fx-border-radius: 0;
    -fx-cursor: hand;
}

.ribbon-tab-button:hover {
    -fx-background-color: #e5e5e5;
    -fx-text-fill: #222222;
}

.ribbon-tab-button:selected {
    -fx-background-color: white;
    -fx-text-fill: #185abd;    /* Azul Office */
    -fx-font-weight: bold;
    -fx-border-color: #d1d1d1 #d1d1d1 white #d1d1d1;
    -fx-border-width: 1 1 2 1;
}

/* Área de conteúdo do separador */
.ribbon-tab-content-area {
    -fx-background-color: white;
    -fx-min-height: 90px;
    -fx-pref-height: 90px;
    -fx-max-height: 90px;
    -fx-border-color: transparent transparent #d1d1d1 transparent;
    -fx-border-width: 0 0 1 0;
}

/* Conteúdo do tab (HBox de grupos) */
.ribbon-tab-content {
    -fx-background-color: white;
    -fx-min-height: 90px;
}

/* Grupo */
.ribbon-group {
    -fx-background-color: transparent;
    -fx-min-height: 88px;
    -fx-pref-height: 88px;
    -fx-max-height: 88px;
    -fx-border-color: transparent #e0e0e0 transparent transparent;
    -fx-border-width: 0 1 0 0;
    -fx-padding: 0 0 0 0;
}

/* Título do grupo */
.ribbon-group-title {
    -fx-font-size: 10px;
    -fx-text-fill: #888888;
    -fx-font-family: "Segoe UI", "Arial", sans-serif;
    -fx-alignment: center;
    -fx-max-width: infinity;
    -fx-padding: 1 4 2 4;
}

/* Separador horizontal entre conteúdo e título do grupo */
.ribbon-group-separator {
    -fx-background-color: #e0e0e0;
    -fx-pref-height: 1;
    -fx-min-height: 1;
    -fx-max-height: 1;
}

/* Separador vertical entre colunas de botões */
.ribbon-column-separator {
    -fx-background-color: transparent;
    -fx-pref-width: 1;
    -fx-min-width: 1;
}

/* Coluna de botões */
.ribbon-button-column {
    -fx-alignment: top-left;
    -fx-spacing: 1;
}

/* Botão do Ribbon (base) */
.ribbon-button {
    -fx-background-color: transparent;
    -fx-background-radius: 3;
    -fx-border-color: transparent;
    -fx-border-radius: 3;
    -fx-border-width: 1;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.ribbon-button:hover {
    -fx-background-color: #e5e5e5;
    -fx-border-color: #c8c8c8;
}

.ribbon-button:pressed {
    -fx-background-color: #d0d0d0;
    -fx-border-color: #b0b0b0;
}

/* Botão LARGE */
.ribbon-button-large {
    -fx-min-width: 48px;
    -fx-pref-width: 56px;
    -fx-min-height: 62px;
    -fx-pref-height: 66px;
    -fx-padding: 2 4 2 4;
}

/* Botão SMALL */
.ribbon-button-small {
    -fx-min-width: 80px;
    -fx-pref-width: 100px;
    -fx-min-height: 20px;
    -fx-pref-height: 22px;
    -fx-padding: 1 4 1 4;
}

/* Texto dos botões */
.ribbon-button-text {
    -fx-font-size: 11px;
    -fx-fill: #333333;
    -fx-font-family: "Segoe UI", "Arial", sans-serif;
}

/* Ícone LARGE (32×32) */
.ribbon-icon-large {
    -fx-pref-width: 32px;
    -fx-pref-height: 32px;
    -fx-min-width: 32px;
    -fx-min-height: 32px;
}

/* Ícone SMALL (16×16) */
.ribbon-icon-small {
    -fx-pref-width: 16px;
    -fx-pref-height: 16px;
    -fx-min-width: 16px;
    -fx-min-height: 16px;
}

/* Botão de overflow "»" */
.ribbon-overflow-button {
    -fx-background-color: #f0f0f0;
    -fx-text-fill: #444444;
    -fx-font-size: 14px;
    -fx-min-width: 24px;
    -fx-pref-width: 24px;
    -fx-min-height: 88px;
    -fx-pref-height: 88px;
    -fx-background-radius: 0;
    -fx-border-color: #d1d1d1;
    -fx-border-width: 0 0 0 1;
    -fx-cursor: hand;
    -fx-padding: 0;
}

.ribbon-overflow-button:hover {
    -fx-background-color: #e0e0e0;
}

/* Popup de overflow */
.ribbon-overflow-popup {
    -fx-background-color: white;
    -fx-border-color: #d1d1d1;
    -fx-border-width: 1;
    -fx-background-radius: 4;
    -fx-padding: 8;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);
}
```

---

## PASSO 9 — APLICAR O CSS NO ARRANQUE DA APLICAÇÃO

```java
// No método start() da Application principal
@Override
public void start(Stage primaryStage) {
    // ... criar o layout ...
    
    // Aplicar CSS do Ribbon
    scene.getStylesheets().add(
        getClass().getResource("/com/empresa/ribbon/ribbon.css").toExternalForm()
    );

    // Opcional: estilo global JFXtras (tema claro)
    // scene.getStylesheets().add(
    //     JMetro.class.getResource("JMetroLightTheme.css").toExternalForm()
    // );
}
```

---

## PASSO 10 — EXEMPLO DE USO COMPLETO

```java
// MainApp.java — exemplo de montagem do Ribbon
package com.empresa.app;

import com.empresa.ribbon.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        // ── Criar o Ribbon ──────────────────────────────────────────
        RibbonBar ribbon = new RibbonBar();

        // ── Separador "Início" ───────────────────────────────────────
        RibbonTab tabInicio = new RibbonTab();

        // Grupo "Área de Transferência"
        RibbonGroup grpClipboard = new RibbonGroup("Área de Transferência");

        SVGPath iconColar = new SVGPath();
        iconColar.setContent("M19 2h-4.18C14.4.84 13.3 0 12 0 ..."); // SVG real
        RibbonButton btnColar = new RibbonButton(
            "Colar", iconColar, RibbonButtonSize.LARGE,
            "Colar (Ctrl+V)", () -> System.out.println("Colar clicado")
        );
        grpClipboard.addLargeButton(btnColar);

        SVGPath iconCortar = new SVGPath();
        iconCortar.setContent("M ..."); // SVG real
        SVGPath iconCopiar = new SVGPath();
        iconCopiar.setContent("M ..."); // SVG real
        SVGPath iconFormat = new SVGPath();
        iconFormat.setContent("M ..."); // SVG real

        RibbonButton btnCortar = new RibbonButton(
            "Cortar", iconCortar, RibbonButtonSize.SMALL,
            "Cortar (Ctrl+X)", () -> System.out.println("Cortar")
        );
        RibbonButton btnCopiar = new RibbonButton(
            "Copiar", iconCopiar, RibbonButtonSize.SMALL,
            "Copiar (Ctrl+C)", () -> System.out.println("Copiar")
        );
        RibbonButton btnFormat = new RibbonButton(
            "Formatar", iconFormat, RibbonButtonSize.SMALL,
            "Copiar Formatação", () -> System.out.println("Formatar")
        );
        // 3 botões SMALL na mesma coluna (agrupamento vertical)
        grpClipboard.addSmallButtons(btnCortar, btnCopiar, btnFormat);
        tabInicio.addGroup(grpClipboard);

        // Grupo "Fonte"
        RibbonGroup grpFonte = new RibbonGroup("Fonte");
        // ... adicionar botões de fonte ...
        tabInicio.addGroup(grpFonte);

        // Grupo "Parágrafo"
        RibbonGroup grpParagrafo = new RibbonGroup("Parágrafo");
        // ... adicionar botões de parágrafo ...
        tabInicio.addGroup(grpParagrafo);

        // Grupo "Estilos"
        RibbonGroup grpEstilos = new RibbonGroup("Estilos");
        tabInicio.addGroup(grpEstilos);

        // Grupo "Editar"
        RibbonGroup grpEditar = new RibbonGroup("Editar");
        tabInicio.addGroup(grpEditar);

        ribbon.addTab("Início", tabInicio);

        // ── Separador "Inserir" ──────────────────────────────────────
        RibbonTab tabInserir = new RibbonTab();
        RibbonGroup grpTabelas = new RibbonGroup("Tabelas");
        // ... adicionar botões ...
        tabInserir.addGroup(grpTabelas);
        ribbon.addTab("Inserir", tabInserir);

        // ── Layout principal ─────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(ribbon);
        // root.setCenter(editorPane);

        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(
            getClass().getResource("/com/empresa/ribbon/ribbon.css").toExternalForm()
        );

        stage.setTitle("Minha Aplicação");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## PASSO 11 — COMPORTAMENTO DE OVERFLOW DETALHADO

O mecanismo de overflow funciona assim:

### Detecção
O `widthProperty` do `RibbonTab` dispara o método `recalculateOverflow(double availableWidth)` sempre que a janela é redimensionada.

### Algoritmo
```
largura_usada = 0
grupos_ocultos = []
para cada grupo (da esquerda para a direita):
    largura_grupo = grupo.prefWidth(-1)
    se (largura_usada + largura_grupo + 28px_overflow) <= largura_disponivel:
        mostrar grupo
        largura_usada += largura_grupo
    senão:
        ocultar grupo (setVisible(false), setManaged(false))
        adicionar a grupos_ocultos

se grupos_ocultos não está vazio:
    mostrar botão "»"
    overflowPopup.setGroups(grupos_ocultos)
senão:
    ocultar botão "»"
```

### Nota sobre `setManaged(false)`
Quando `setManaged(false)`, o nó é excluído do cálculo de layout (não ocupa espaço). `setVisible(false)` apenas o torna invisível mas continua a ocupar espaço. **Usar sempre ambos juntos** para grupos ocultos.

---

## PASSO 12 — INTEGRAÇÃO COM JFXTRAS (OPCIONAL)

Para usar tooltips avançados do JFXtras:

```java
import jfxtras.scene.control.CalendarPicker; // Exemplo de controlo JFXtras

// Substituir Tooltip padrão por jfxtras se necessário
// O CSS principal já é suficiente; JFXtras é usado para componentes extras
// como CalendarPicker, ListSpinner, etc. dentro dos grupos do Ribbon
```

Para aplicar tema JMetro (estilo Office) via JFXtras:

```java
// No start() da Application
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

JMetro jMetro = new JMetro(Style.LIGHT);
jMetro.setScene(scene);
// O ribbon.css sobrepõe estilos específicos do Ribbon
```

---

## RESUMO DOS FICHEIROS A CRIAR

| Ficheiro | Localização | Descrição |
|---|---|---|
| `RibbonButtonSize.java` | `com/empresa/ribbon/` | Enum LARGE / SMALL |
| `RibbonButton.java` | `com/empresa/ribbon/` | Botão do Ribbon |
| `RibbonGroup.java` | `com/empresa/ribbon/` | Grupo com título |
| `RibbonTab.java` | `com/empresa/ribbon/` | Conteúdo de um separador + overflow |
| `RibbonOverflowPopup.java` | `com/empresa/ribbon/` | Popup dos grupos ocultos |
| `RibbonBar.java` | `com/empresa/ribbon/` | Componente raiz |
| `ribbon.css` | `resources/com/empresa/ribbon/` | Estilos visuais |
| `MainApp.java` | `com/empresa/app/` | Exemplo de uso |

---

## NOTAS FINAIS PARA O AGENTE DE IA

1. **Ícones SVG**: Substituir os `SVGPath.setContent("M ...")` por paths SVG reais (Material Icons, Fluent Icons, ou ícones próprios).

2. **Thread-safety**: Todas as actualizações visuais devem ocorrer na JavaFX Application Thread. Usar `Platform.runLater()` se vier de outra thread.

3. **Teste de overflow**: Reduzir a janela para menos de 600px de largura para ver o botão `»` aparecer e o popup com os grupos ocultos.

4. **Grupos dinâmicos**: É possível adicionar/remover grupos em runtime chamando `tabInicio.addGroup()` e depois chamar `recalculateOverflow()` manualmente.

5. **Acessibilidade**: Adicionar `setAccessibleText()` em cada `RibbonButton` para suporte a leitores de ecrã.

6. **FXML**: Este componente pode ser integrado em FXML usando `<fx:define>` e injectado via `@FXML`.