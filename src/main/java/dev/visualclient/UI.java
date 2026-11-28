package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    // Round shapes are drawn from small pre-made sprites (smooth, high resolution, and cheap to draw),
    // tinted with the color you pass in.
    private static final int[] RADII = {2, 3, 4, 6, 8, 10, 14, 24};
    private static final Identifier[] FILL = new Identifier[RADII.length];
    private static final Identifier[] RING = new Identifier[RADII.length];

    private static int radiusIndex(int wanted) {
        int best = -1;
        for (int i = 0; i < RADII.length; i++) {
            if (RADII[i] <= wanted) best = i;
        }
        return best;
    }

    private static Identifier sprite(Identifier[] cache, int index, String prefix) {
        if (cache[index] == null) {
            cache[index] = Identifier.fromNamespaceAndPath(VisualClient.MOD_ID, "round/" + prefix + RADII[index]);
        }
        return cache[index];
    }

    private static long lastHoverSound;
    private static net.minecraft.sounds.SoundEvent clickEvent;

    /** A soft tick when the mouse moves onto a button or module. */
    public static void hoverSound() {
        if (!Config.get().hoverSounds) return;
        long now = System.currentTimeMillis();
        if (now - lastHoverSound < 60) return;
        lastHoverSound = now;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.9f));
    }

    /** Lo Client's own click sound (replaces the vanilla button click). */
    public static void clickSound() {
        if (clickEvent == null) {
            clickEvent = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(VisualClient.MOD_ID, "ui.click"));
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Holder.direct(clickEvent), 1.0f));
    }

    /** Filled rounded rectangle; r = h / 2 gives a fully round (pill) shape. */
    public static void roundRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int i = radiusIndex(Math.min(r, Math.min(w, h) / 2));
        if (i < 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(FILL, i, "f"), x, y, w, h, color);
    }

    /** 1px outline following the same rounded shape. */
    public static void roundRing(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int i = radiusIndex(Math.min(r, Math.min(w, h) / 2));
        if (i < 0) {
            border(g, x, y, w, h, color);
            return;
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(RING, i, "r"), x, y, w, h, color);
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
