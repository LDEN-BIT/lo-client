package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Public entry points for the mixin. */
public final class SakuraBackgroundAccess {
    private SakuraBackgroundAccess() {}

    public static boolean applies(Screen screen) {
        return SakuraBackground.applies(screen);
    }

    public static void draw(GuiGraphics g, int w, int h) {
        SakuraBackground.draw(g, w, h);
    }
}
