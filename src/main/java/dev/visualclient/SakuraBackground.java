package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;

/** A cherry blossom background (pink sky, blossom branches, falling petals) for the loading screens. */
final class SakuraBackground {
    private SakuraBackground() {}

    /**
     * Loading screens are the ones you cannot close with Escape (connecting, loading the world...).
     * Our own main menu, the death screen and confirm dialogs are left alone.
     */
    static boolean applies(Screen screen) {
        if (!Config.get().sakura) return false;
        if (screen instanceof ConnectScreen) return true;
        if (screen instanceof VisualTitleScreen || screen instanceof DeathScreen || screen instanceof ConfirmScreen) {
            return false;
        }
        return !screen.shouldCloseOnEsc();
    }

    private static float hash(float s) {
        float v = (float) Math.sin(s) * 43758.5453f;
        return v - (float) Math.floor(v);
    }

    static void draw(GuiGraphics g, int w, int h) {
        long t = System.currentTimeMillis();
        float tt = t / 1000f;

        // Sky
        g.fillGradient(0, 0, w, h, 0xFFF6BBD1, 0xFFA2507C);

        // Soft light blobs
        for (int i = 0; i < 8; i++) {
            int d = 50 + (int) (hash(i * 1.9f) * 80);
            float drift = (float) Math.sin(tt * 0.3f + i) * 10f;
            int x = (int) (hash(i * 7.13f) * w - d / 2 + drift);
            int y = (int) (hash(i * 3.77f) * h - d / 2);
            UI.roundRect(g, x, y, d, d, d / 2, 0x12FFFFFF);
        }

        branch(g, w, h, tt, false);
        branch(g, w, h, tt, true);

        // Falling petals
        for (int i = 0; i < 50; i++) {
            float seed = i * 17.31f;
            float speed = 16f + hash(seed) * 30f;
            float sway = 10f + hash(seed + 1f) * 22f;
            float spanX = w + 80f, spanY = h + 20f;
            float y = ((hash(seed + 3f) * h + tt * speed) % spanY) - 10f;
            float x = hash(seed + 2f) * spanX + tt * 9f + (float) Math.sin(tt * (0.6f + hash(seed + 4f)) + seed) * sway;
            x = ((x % spanX) + spanX) % spanX - 40f;
            int sz = 4 + (int) (hash(seed + 5f) * 3f);
            float rot = tt * (1f + hash(seed + 6f)) + seed;
            int pw = (int) (sz + Math.abs(Math.sin(rot)) * 2f);
            int ph = (int) (sz * 0.7f + 2f);
            int col = i % 3 == 0 ? 0xDDFFFFFF : i % 3 == 1 ? 0xDDFFD1E3 : 0xDDFF9EC3;
            UI.roundRect(g, (int) x, (int) y, pw, ph, ph / 2, col);
        }

        // Darker toward the bottom so the loading text is easy to read
        g.fillGradient(0, h / 2, w, h, 0x00000000, 0x55000000);
    }

    private static void branch(GuiGraphics g, int w, int h, float tt, boolean right) {
        float[][] pts = {{-0.02f, 0.10f}, {0.12f, 0.17f}, {0.25f, 0.12f}, {0.36f, 0.21f}, {0.44f, 0.18f}};
        float prevX = 0, prevY = 0;
        for (int i = 0; i < pts.length; i++) {
            float px = (right ? 1f - pts[i][0] : pts[i][0]) * w;
            float py = pts[i][1] * h;
            if (i > 0) {
                float len = (float) Math.hypot(px - prevX, py - prevY);
                int steps = Math.max(2, (int) (len / 3f));
                int th = Math.max(3, 7 - i);
                for (int s = 0; s <= steps; s++) {
                    float f = s / (float) steps;
                    float x = prevX + (px - prevX) * f;
                    float y = prevY + (py - prevY) * f;
                    UI.roundRect(g, (int) x - th / 2, (int) y - th / 2, th, th, th / 2, 0xFF4A2E3A);
                }
                blossom(g, px, py, tt, i * (right ? 3 : 1));
                blossom(g, (prevX + px) / 2f, (prevY + py) / 2f - 4f, tt, i + 5 + (right ? 3 : 0));
            }
            prevX = px;
            prevY = py;
        }
    }

    private static void blossom(GuiGraphics g, float cx, float cy, float tt, int seed) {
        float sway = (float) Math.sin(tt * 0.8f + seed) * 1.5f;
        for (int p = 0; p < 5; p++) {
            double a = p * Math.PI * 2 / 5 + seed;
            int x = (int) (cx + Math.cos(a) * 5 + sway);
            int y = (int) (cy + Math.sin(a) * 5);
            UI.roundRect(g, x - 3, y - 3, 7, 7, 3, p % 2 == 0 ? 0xEEFFE0EC : 0xEEFFC8DD);
        }
        UI.roundRect(g, (int) (cx + sway) - 2, (int) cy - 2, 4, 4, 2, 0xFFE9B949);
    }
}
