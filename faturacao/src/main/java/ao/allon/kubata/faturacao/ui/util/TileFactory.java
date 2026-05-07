package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Factory for creating modern AtlantaFX Tiles for forms, settings, and lists.
 * Provides consistent styling and behavior across the application.
 */
public final class TileFactory {

    private TileFactory() {
        // Utility class
    }

    /**
     * Creates a standard information tile.
     */
    public static Tile createInfo(String title, String description) {
        return createInfo(title, description, null);
    }

    /**
     * Creates an information tile with an icon.
     */
    public static Tile createInfo(String title, String description, Feather icon) {
        var tile = new Tile(title, description);
        if (icon != null) {
            tile.setGraphic(new FontIcon(icon));
        }
        return tile;
    }

    /**
     * Creates a tile specifically for form inputs (TextField, ComboBox, etc.).
     * The input control is placed in the action slot.
     */
    public static Tile createFormField(String title, String description, Node inputNode) {
        var tile = new Tile(title, description);
        if (inputNode != null) {
            tile.setAction(inputNode);
            // Form fields usually handle their own interactions, so we don't set a global handler
        }
        return tile;
    }

    /**
     * Creates a settings tile with a ToggleSwitch.
     * Clicking the tile body also toggles the switch.
     */
    public static Tile createSwitch(String title, String description, Feather icon, ToggleSwitch toggle) {
        var tile = createInfo(title, description, icon);
        if (toggle != null) {
            tile.setAction(toggle);
            tile.setActionHandler(toggle::fire);
        }
        return tile;
    }

    /**
     * Creates a tile representing an actionable item (button-like).
     */
    public static Tile createAction(String title, String description, Feather icon, Node actionNode) {
        var tile = createInfo(title, description, icon);
        if (actionNode != null) {
            tile.setAction(actionNode);
        }
        return tile;
    }
    
    /**
     * Creates a navigation tile that implies movement to another view.
     * Automatically adds a chevron icon and hover styling.
     */
    public static Tile createNavigation(String title, String description, Feather icon, Runnable action) {
        var tile = createInfo(title, description, icon);
        tile.setAction(new FontIcon(Feather.CHEVRON_RIGHT));
        if (action != null) {
            tile.setActionHandler(action);
        }
        return tile;
    }

    /**
     * Creates a compact tile useful for dense lists.
     */
    public static Tile createCompact(String title, String description, Feather icon) {
        var tile = createInfo(title, description, icon);
        tile.getStyleClass().add(Tweaks.ALT_ICON);
        return tile;
    }
    
    /**
     * Creates a section header tile.
     * Visually distinct to separate form groups.
     */
    public static Tile createSectionHeader(String title, String description, Feather icon) {
        var tile = createInfo(title, description, icon);
        tile.getStyleClass().addAll(Styles.TITLE_3, "form-section-header");
        tile.getStyleClass().add(Tweaks.ALT_ICON);
        return tile;
    }
    
    /**
     * Creates a tile that displays a Key-Value pair (e.g., Version: 1.0.0).
     * Useful for 'About' screens or read-only details.
     */
    public static Tile createKeyValue(String key, String value, Feather icon) {
        var tile = createInfo(key, null, icon);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(Styles.TEXT_MUTED);
        tile.setAction(valueLabel);
        return tile;
    }
}
