package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Small drawing helpers. Menus draw their own visuals over invisible vanilla buttons. */
public final class UI {
    private UI() {}

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Filled rectangle with clipped corners (looks rounded). */
    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private static double cornerInsetF(int row, int h, int r) {
        int k = Math.min(row, h - 1 - row);
        if (k >= r) return 0;
        double dy = r - (k + 0.5);
        return r - Math.sqrt(Math.max(0, r * r - dy * dy));
    }

    private static int cornerInset(int row, int h, int r) {
        return (int) Math.round(cornerInsetF(row, h, r));
    }

    /** Screen pixels per GUI pixel. Round shapes are drawn at this finer resolution so they are smooth. */
    private static int sub() {
        try {
            return Math.max(1, (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
        } catch (Throwable t) {
            return 1;
        }
    }

    private static long lastHoverSound;

    /** A soft tick when the mouse moves onto a button or module. */
    public static void hoverSound() {
        if (!Config.get().hoverSounds) return;
        long now = System.currentTimeMillis();
        if (now - lastHoverSound < 60) return;
        lastHoverSound = now;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.9f));
    }

    public static void roundRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int s = sub();
        if (s <= 1) {
            roundRectRaw(g, x, y, w, h, r, color);
            return;
        }
        g.pose().pushMatrix();
        g.pose().scale(1f / s, 1f / s);
        roundRectRaw(g, x * s, y * s, w * s, h * s, r * s, color);
        g.pose().popMatrix();
    }

    public static void roundRing(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int s = sub();
        if (s <= 1) {
            roundRingRaw(g, x, y, w, h, r, color);
            return;
        }
        g.pose().pushMatrix();
        g.pose().scale(1f / s, 1f / s);
        roundRingRaw(g, x * s, y * s, w * s, h * s, r * s, color);
        g.pose().popMatrix();
    }

    /** Filled rounded rectangle with smoothed edges; r = h / 2 gives a fully round (pill) shape. */
    private static void roundRectRaw(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        int alpha = color >>> 24;
        int rgb = color & 0xFFFFFF;
        for (int row = 0; row < h; row++) {
            double inset = cornerInsetF(row, h, rr);
            int full = (int) Math.ceil(inset);
            g.fill(x + full, y + row, x + w - full, y + row + 1, color);
            if (full > 0) {
                int a = Math.round(alpha * (float) (full - inset)); // partly covered edge pixel
                if (a > 0) {
                    int edge = (a << 24) | rgb;
                    g.fill(x + full - 1, y + row, x + full, y + row + 1, edge);
                    g.fill(x + w - full, y + row, x + w - full + 1, y + row + 1, edge);
                }
            }
        }
    }

    /** 1px outline following the same rounded shape. */
    private static void roundRingRaw(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int o = cornerInset(row, h, rr);
            int ir = row - 1;
            if (ir < 0 || ir >= h - 2) {
                g.fill(x + o, y + row, x + w - o, y + row + 1, color);
            } else {
                int in = 1 + cornerInset(ir, h - 2, Math.max(0, rr - 1));
                if (in <= o) in = o + 1;
                g.fill(x + o, y + row, x + in, y + row + 1, color);
                g.fill(x + w - in, y + row, x + w - o, y + row + 1, color);
            }
        }
    }

    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int lerpRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** A fully transparent button: it handles clicks, we draw the looks. */
    public static Button ghost(int x, int y, int w, int h, Runnable onPress) {
        Button b = Button.builder(Component.empty(), btn -> onPress.run())
                .bounds(x, y, w, h).build();
        b.setAlpha(0f);
        return b;
    }
}
