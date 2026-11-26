package dev.visualclient;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The Lo Client main menu. Opens the real Minecraft screens, with smooth animations. */
public class VisualTitleScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    // icon ids: 0 person, 1 globe, 2 gear, 3 sliders, 4 x
    private record Btn(String label, int icon, int x, int y, int w, int h) {}

    private float logoScale = 3f;
    private int logoY, welcomeY;

    private final List<Btn> btns = new ArrayList<>();
    private float[] hover = new float[0];
    private long openTime = 0L;
    private long lastFrame = 0L;

    public VisualTitleScreen() {
        super(Component.literal("Lo Client"));
    }

    @Override
    protected void init() {
        btns.clear();
        int w = 170, h = 20, gap = 6;
        int x = this.width / 2 - w / 2;

        logoScale = Mth.clamp(this.height / 90f, 2f, 4f);
        int blockH = (int) (this.font.lineHeight * logoScale * 2f);
        int total = blockH + 12 + 9 + 18 + 5 * h + 4 * gap;
        logoY = Math.max(8, (this.height - total) / 2 - 6);
        welcomeY = logoY + blockH + 10;
        int y = welcomeY + 9 + 18;

        add("Singleplayer", 0, x, y, w, h, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
        add("Multiplayer", 1, x, y + (h + gap), w, h, () -> this.minecraft.setScreen(new LoMultiplayerScreen(this)));
        add("Lo Client Settings", 2, x, y + (h + gap) * 2, w, h,
                () -> this.minecraft.setScreen(new SettingsScreen(this)));
        add("Options", 3, x, y + (h + gap) * 3, w, h,
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));
        add("Quit", 4, x, y + (h + gap) * 4, w, h, () -> this.minecraft.stop());

        if (hover.length != btns.size()) hover = new float[btns.size()];
        if (openTime == 0L) openTime = System.currentTimeMillis();
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
        float q = 1f - Mth.clamp(p, 0f, 1f);
        return 1f - q * q * q;
    }

    /** Big logo line: soft white glow behind crisp white text. */
    private void logoLine(GuiGraphics g, String text, int y, float alpha) {
        var comp = Component.literal(text).withStyle(ChatFormatting.BOLD);
        float w = this.font.width(comp) * logoScale;
        float x = this.width / 2f - w / 2f;
        int a = (int) (255 * alpha);
        if (a <= 8) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(logoScale, logoScale);
        int glow = (int) (0x1A * alpha);
        if (glow > 3) {
            for (float r = 0.6f; r <= 1.8f; r += 0.6f) {
                for (int d = 0; d < 8; d++) {
                    double ang = d * Math.PI / 4.0;
                    g.drawString(this.font, comp, (int) Math.round(Math.cos(ang) * r),
                            (int) Math.round(Math.sin(ang) * r), (glow << 24) | 0xFFFFFF, false);
                }
            }
        }
        g.drawString(this.font, comp, 0, 0, (a << 24) | 0xFFFFFF, false);
        g.pose().popMatrix();
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

        // Mouse position in -1..1, used for gentle parallax.
        float mxN = (mouseX - this.width / 2f) / (this.width / 2f);
        float myN = (mouseY - this.height / 2f) / (this.height / 2f);

        // ---- Background: near black with a soft glow behind the logo and faint drifting stars ----
        g.fillGradient(0, 0, this.width, this.height, 0xFF0C0C0F, 0xFF060608);
        int gw = Math.min(this.width - 20, 320), gh = (int) (this.font.lineHeight * logoScale * 2f) + 40;
        for (int k = 0; k < 5; k++) {
            UI.roundRect(g, this.width / 2 - gw / 2 - k * 8, logoY - 20 - k * 8,
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

        // ---- Logo, welcome line ----
        float introLogo = easeOut((t - openTime) / 700f);
        int bob = (int) Math.round(Math.sin(t / 1100.0) * 1.5);
        int lh = (int) (this.font.lineHeight * logoScale);
        logoLine(g, "LO", logoY + bob, introLogo);
        logoLine(g, "CLIENT", logoY + lh - 2 + bob, introLogo);

        if (introLogo > 0.5f) {
            String welcome = "Welcome back, " + this.minecraft.getUser().getName();
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
                int cw = 8 + 5 + this.font.width(b.label);
                int cx = bx + (bw - cw) / 2;
                icon(g, b.icon, cx, by + (bh - 8) / 2, (ca << 24) | text);
                g.drawString(this.font, b.label, cx + 13, by + (bh - this.font.lineHeight) / 2 + 1,
                        (ca << 24) | text, false);
            }
        }

        // ---- Footer: version on the left, clock on the right ----
        g.drawString(this.font, "Lo Client - Minecraft 1.21.11", 8, this.height - 12, 0xFF6C6C74, false);
        String clock = LocalTime.now().format(CLOCK);
        g.drawString(this.font, clock, this.width - 8 - this.font.width(clock), this.height - 12, 0xFF6C6C74, false);

        // ---- Fade in from black ----
        float f = Mth.clamp((t - openTime) / 500f, 0f, 1f);
        int fadeA = (int) ((1f - f) * 255);
        if (fadeA >= 4) g.fill(0, 0, this.width, this.height, fadeA << 24);
    }
}
