package dev.visualclient;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Lo Client fonts. The font files and definitions live inside the mod jar (no resource pack needed);
 * FontMixin swaps the game's default font for the one chosen here.
 */
public final class LoFonts {
    public static final String DEFAULT = "Default";

    private record F(String name, String id) {}

    private static final F[] FONTS = {
            new F("Xuong", "xuong"),
            new F("Outfit", "outfit"),
            new F("Jura", "jura"),
            new F("Tektur", "tektur"),
            new F("Pixelify Sans", "pixelify"),
            new F("Instrument Sans", "instrument"),
    };

    private static String cachedName = DEFAULT;
    private static FontDescription cached;

    private LoFonts() {}

    public static String[] names() {
        String[] out = new String[FONTS.length + 1];
        out[0] = DEFAULT;
        for (int i = 0; i < FONTS.length; i++) out[i + 1] = FONTS[i].name;
        return out;
    }

    /** The font to use instead of the vanilla one, or null for vanilla. */
    public static FontDescription override() {
        String name = Config.get().font;
        if (name == null || DEFAULT.equals(name)) return null;
        if (!name.equals(cachedName)) {
            cachedName = name;
            cached = null;
            for (F f : FONTS) {
                if (f.name.equals(name)) {
                    cached = new FontDescription.Resource(Identifier.fromNamespaceAndPath(VisualClient.MOD_ID, f.id));
                }
            }
        }
        return cached;
    }
}
