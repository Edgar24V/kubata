package ao.allon.kubata.rh.ui.util;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public final class IconUtils {

    public static final int SIZE_SMALL = 14;
    public static final int SIZE_MEDIUM = 16;
    public static final int SIZE_LARGE = 22;
    public static final int SIZE_XLARGE = 32;

    private IconUtils() { }

    public static FontIcon icon(Ikon code, int size) {
        FontIcon fi = new FontIcon(code);
        fi.setIconSize(size);
        return fi;
    }
}
