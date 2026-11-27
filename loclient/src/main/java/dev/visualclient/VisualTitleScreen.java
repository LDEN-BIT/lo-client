package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Lo Client main menu. Opens the real Minecraft screens. Its text uses smooth fonts of its
 * own (not the pixel font), so it stays sharp at every GUI scale.
 */
public class VisualTitleScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int LABEL_CAP = 7; // cap height of the label font, in GUI pixels

    // icon ids: 0 person, 1 globe, 2 gear, 3 sliders, 4 x
    private record Btn(String label, int icon, int x, int y, int w, int h) {}

    private String titleFont = "menu_title_m";
    private int titleSize = 28;
    private int logoY, welcomeY;

    private final List<Btn> btns = new ArrayList<>();
    private float[] hover = new float[0];
    private boolean[] wasOver = new boolean[0];
    private long openTime = 0L;
    private long lastFrame = 0L;

    public VisualTitleScreen() {
        super(Component.literal("Lo Client"));
    }

    private static Component styled(String text, String fontId) {
        Style style = Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath(VisualClient.MOD_ID, fontId)));
        return Component.literal(text).withStyle(style);
    }

    @Override
    protected void init() {
        btns.clear();
        int w = 170, h = 20, gap = 6;
        int x = this.width / 2 - w / 2;

        if (this.height >= 300) {
            titleSize = 36;
            titleFont = "menu_title_l";
        } else if (this.height >= 220) {
            titleSize = 28;
            titleFont = "menu_title_m";
        } else {
            titleSize = 20;
            titleFont = "menu_title_s";
        }
        int blockH = logoBlockHeight();
        int total = blockH + 14 + 9 + 18 + 5 * h + 4 * gap;
        logoY = Math.max(8, (this.height - total) / 2 - 4);
        welcomeY = logoY + blockH + 12;
        int y = welcomeY + 9 + 18;

        add("Singleplayer", 0, x, y, w, h, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
        add("Multiplayer", 1, x, y + (h + gap), w, h, () -> this.minecraft.setScreen(new LoMultiplayerScreen(this)));
        add("Lo Client Settings", 2, x, y + (h + gap) * 2, w, h,
                () -> this.minecraft.setScreen(new SettingsScreen(this)));
        add("Options", 3, x, y + (h + gap) * 3, w, h,
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));
        add("Quit", 4, x, y + (h + gap) * 4, w, h, () -> this.minecraft.stop());

        if (hover.length != btns.size()) {
            hover = new float[btns.size()];
            wasOver = new boolean[btns.size()];
        }
        if (openTime == 0L) openTime = System.currentTimeMillis();
    }

    private int capHeight() {
        return Math.round(titleSize * 0.72f);
    }

    private int lineStep() {
        return Math.round(titleSize * 0.84f);
    }

    private int logoBlockHeight() {
        return lineStep() + capHeight();
    }

    private void add(String label, int icon, int x, int y, int w, int h, Runnable action) {
        btns.add(new Btn(label, icon, x, y, w, h));
        addRenderableWidget(UI.ghost(x, y, w, h, action));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static float hash(float s) {
        float v = (float) Math.sin(s) * 43758.5453f;
        return v - (float) Math.floor(v);
    }

    private static float easeOut(float p) {
        p = Mth.clamp(p, 0f, 1f);
        return 1f - (1f - p) * (1f - p) * (1f - p);
    }

    /** One line of the big logo: soft white glow behind crisp white letters. */
    private void logoLine(GuiGraphics g, String text, int capTop, float alpha) {
        int a = (int) (255 * alpha);
        if (a <= 8) return;
        Component comp = styled(text, titleFont);
        int x = this.width / 2 - this.font.width(comp) / 2;
        int y = capTop - 7 + capHeight(); // glyph baseline sits 7 GUI pixels below the draw position
        int glow = (int) (0x1C * alpha);
        if (glow > 3) {
            int[][] offs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
            for (int[] o : offs) {
                g.drawString(this.font, comp, x + o[0], y + o[1], (glow << 24) | 0xFFFFFF, false);
            }
        }
        g.drawString(this.font, comp, x, y, (a << 24) | 0xFFFFFF, false);
    }

    private static void icon(GuiGraphics g, int id, int x, int y, int color) {
        switch (id) {
            case 0 -> { // person
                UI.roundRect(g, x + 2, y, 4, 4, 2, color);
                UI.roundRect(g, x, y + 5, 8, 3, 1, color);
            }
            case 1 -> { // globe
                UI.roundRing(g, x, y, 8, 8, 4, color);
                g.fill(x + 4, y, x + 5, y + 8, color);
                g.fill(x, y + 4, x + 8, y + 5, color);
            }
            case 2 -> { // gear
                UI.roundRing(g, x + 1, y + 1, 6, 6, 3, color);
                g.fill(x + 3, y, x + 5, y + 1, color);
                g.fill(x + 3, y + 7, x + 5, y + 8, color);
                g.fill(x, y + 3, x + 1, y + 5, color);
                g.fill(x + 7, y + 3, x + 8, y + 5, color);
            }
            case 3 -> { // sliders
                g.fill(x, y + 1, x + 8, y + 2, color);
                g.fill(x, y + 4, x + 8, y + 5, color);
                g.fill(x, y + 7, x + 8, y + 8, color);
                g.fill(x + 5, y, x + 7, y + 3, color);
                g.fill(x + 1, y + 3, x + 3, y + 6, color);
                g.fill(x + 4, y + 6, x + 6, y + 9, color);
            }
            default -> { // x
                for (int i = 0; i < 8; i++) {
                    g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
                    g.fill(x + 7 - i, y + i, x + 8 - i, y + i + 1, color);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // vanilla background + invisible buttons

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        long t = now;

        float mxN = (mouseX - this.width / 2f) / (this.width / 2f);
        float myN = (mouseY - this.height / 2f) / (this.height / 2f);

        // ---- Background: near black, a soft glow behind the logo, faint drifting stars ----
        g.fillGradient(0, 0, this.width, this.height, 0xFF0C0C0F, 0xFF060608);
        int gw = Math.min(this.width - 20, 340), gh = logoBlockHeight() + 50;
        for (int k = 0; k < 5; k++) {
            UI.roundRect(g, this.width / 2 - gw / 2 - k * 8, logoY - 25 - k * 8,
                    gw + k * 16, gh + k * 16, gh / 2 + k * 8, 0x05FFFFFF);
        }
        for (int i = 0; i < 70; i++) {
            int layer = i % 3;
            float fx = hash(i * 12.9898f);
            float fy = hash(i * 78.233f + 3.1f);
            float speed = 0.003f + layer * 0.003f;
            float par = 3f + layer * 5f;
            int x = (int) ((((fx * this.width + t * speed - mxN * par) % this.width) + this.width) % this.width);
            int y = (int) (fy * this.height - myN * par);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 0.0015 + i);
            int alpha = (int) (30 + 90 * tw);
            g.fill(x, y, x + 1 + (layer == 2 ? 1 : 0), y + 1 + (layer == 2 ? 1 : 0), (alpha << 24) | 0xFFFFFF);
        }

        // ---- Logo and welcome line ----
        float introLogo = easeOut((t - openTime) / 700f);
        int bob = (int) Math.round(Math.sin(t / 1100.0) * 1.5);
        logoLine(g, "LO", logoY + bob, introLogo);
        logoLine(g, "CLIENT", logoY + lineStep() + bob, introLogo);

        if (introLogo > 0.5f) {
            Component welcome = styled("Welcome back, " + this.minecraft.getUser().getName(), "menu_text");
            int wa = (int) (255 * Mth.clamp((introLogo - 0.5f) * 2f, 0f, 1f));
            if (wa > 8) {
                g.drawString(this.font, welcome, this.width / 2 - this.font.width(welcome) / 2,
                        welcomeY, (wa << 24) | 0x9A9AA2, false);
            }
        }

        // ---- Buttons: dark pills with a light outline; they fade to white while hovered ----
        float ease = 1f - (float) Math.exp(-dt * 12f);
        for (int i = 0; i < btns.size(); i++) {
            Btn b = btns.get(i);
            boolean over = UI.in(mouseX, mouseY, b.x, b.y, b.w, b.h);
            if (over && !wasOver[i]) UI.hoverSound();
            wasOver[i] = over;
            hover[i] += ((over ? 1f : 0f) - hover[i]) * ease;
            float hv = hover[i];

            float p = easeOut((t - openTime - 200 - i * 80L) / 450f);
            if (p <= 0.04f) continue;

            int growW = (int) (hv * 6f), growH = (int) (hv * 2f);
            int bw = b.w + growW, bh = b.h + growH;
            int bx = b.x - growW / 2 + (int) ((1f - p) * -50f);
            int by = b.y - growH / 2;
            int rad = bh / 2;

            int fill = UI.lerpRgb(0x141417, 0xF2F2F5, hv) & 0xFFFFFF;
            int ring = UI.lerpRgb(0x7A7A80, 0xFFFFFF, hv) & 0xFFFFFF;
            int text = UI.lerpRgb(0xD8D8DC, 0x111114, hv) & 0xFFFFFF;

            if (hv > 0.05f) {
                for (int k = 1; k <= 3; k++) {
                    int ga = (int) (0x16 * hv * p * (4 - k) / 3f);
                    if (ga > 0) UI.roundRect(g, bx - k, by - k, bw + k * 2, bh + k * 2, rad + k,
                            (ga << 24) | 0xFFFFFF);
                }
            }
            UI.roundRect(g, bx, by, bw, bh, rad, ((int) (0xE6 * p) << 24) | fill);
            UI.roundRing(g, bx, by, bw, bh, rad, ((int) (0xFF * p) << 24) | ring);
            UI.roundRing(g, bx + 1, by + 1, bw - 2, bh - 2, Math.max(0, rad - 1), ((int) (0x70 * p) << 24) | ring);

            int ca = (int) (255 * Mth.clamp((p - 0.3f) / 0.7f, 0f, 1f));
            if (ca > 8) {
                Component label = styled(b.label, "menu_text");
                int cw = 8 + 6 + this.font.width(label);
                int cx = bx + (bw - cw) / 2;
                icon(g, b.icon, cx, by + (bh - 8) / 2, (ca << 24) | text);
                g.drawString(this.font, label, cx + 14, by + (bh - LABEL_CAP) / 2, (ca << 24) | text, false);
            }
        }

        // ---- Footer: version on the left, clock on the right ----
        Component version = styled("Lo Client - Minecraft 1.21.11", "menu_text");
        g.drawString(this.font, version, 8, this.height - 14, 0xFF6C6C74, false);
        Component clock = styled(LocalTime.now().format(CLOCK), "menu_text");
        g.drawString(this.font, clock, this.width - 8 - this.font.width(clock), this.height - 14, 0xFF6C6C74, false);

        // ---- Fade in from black ----
        float f = Mth.clamp((t - openTime) / 500f, 0f, 1f);
        int fadeA = (int) ((1f - f) * 255);
        if (fadeA >= 4) g.fill(0, 0, this.width, this.height, fadeA << 24);
    }
}
