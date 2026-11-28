package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pick your buddy: any living mob, a name, a size. */
public class BuddyScreen extends Screen {
    private static final int ROW = 16;

    private final Screen parent;
    private EditBox search, nameBox;
    private Button toggleButton;
    private List<EntityType<?>> shown = new ArrayList<>();
    private float scroll, scrollTarget;
    private int px, py, pw, ph, listX, listY, listW, listH;

    public BuddyScreen(Screen parent) {
        super(Component.literal("Buddy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Config c = Config.get();
        pw = Math.min(this.width - 16, 400);
        ph = Math.min(this.height - 16, 260);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        listX = px + 12;
        listY = py + 56;
        listW = pw / 2 - 18;
        listH = ph - 56 - 14;
        int rx = px + pw / 2 + 6;
        int rw = pw - (rx - px) - 12;

        search = new EditBox(this.font, listX, py + 32, listW, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search mobs"));
        search.setMaxLength(24);
        search.setResponder(s -> {
            refilter(s);
            scroll = 0f;
            scrollTarget = 0f;
        });
        addRenderableWidget(search);

        nameBox = new EditBox(this.font, rx, py + 56, rw, 18, Component.literal("Name"));
        nameBox.setHint(Component.literal("Name your buddy"));
        nameBox.setMaxLength(32);
        nameBox.setValue(c.buddyName == null ? "" : c.buddyName);
        nameBox.setResponder(s -> c.buddyName = s);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            c.buddyScale = Mth.clamp(Math.round((c.buddyScale - 0.1f) * 10f) / 10f, 0.2f, 1.5f);
        }).bounds(rx, py + 98, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            c.buddyScale = Mth.clamp(Math.round((c.buddyScale + 0.1f) * 10f) / 10f, 0.2f, 1.5f);
        }).bounds(rx + rw - 24, py + 98, 24, 18).build());

        toggleButton = Button.builder(Component.literal(""), b -> {
            c.buddyEnabled = !c.buddyEnabled;
            updateToggleText();
        }).bounds(rx, py + 128, rw, 20).build();
        addRenderableWidget(toggleButton);
        updateToggleText();

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(rx, py + ph - 34, rw, 20).build());

        refilter(search.getValue());
    }

    private void updateToggleText() {
        toggleButton.setMessage(Component.literal(Config.get().buddyEnabled ? "Buddy: ON" : "Buddy: OFF"));
    }

    private void refilter(String q) {
        String needle = q.trim().toLowerCase(Locale.ROOT);
        shown = new ArrayList<>();
        for (EntityType<?> t : Buddy.types()) {
            if (needle.isEmpty() || t.getDescription().getString().toLowerCase(Locale.ROOT).contains(needle)) {
                shown.add(t);
            }
        }
    }

    private float maxScroll() {
        return Math.max(0, shown.size() * ROW - listH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (UI.in((int) mouseX, (int) mouseY, listX, listY, listW, listH)) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * ROW * 2, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (event.button() == 0 && UI.in(mx, my, listX, listY, listW, listH)) {
            int index = (int) ((my - listY + scroll) / ROW);
            if (index >= 0 && index < shown.size()) {
                Config c = Config.get();
                c.buddyType = Buddy.idOf(shown.get(index));
                c.buddyEnabled = true;
                updateToggleText();
                Config.save();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        Config c = Config.get();
        int accent = Hud.accent(c);
        g.fill(0, 0, this.width, this.height, 0x88000000);
        UI.roundRect(g, px, py, pw, ph, 16, 0xF2101018);
        UI.roundRing(g, px, py, pw, ph, 16, 0x80000000 | (accent & 0xFFFFFF));
        g.drawString(this.font, "Buddy", px + 14, py + 12, accent, true);

        // Mob list
        scroll += (scrollTarget - scroll) * 0.3f;
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;
        UI.roundRect(g, listX, listY, listW, listH, 8, 0x30000000);
        g.enableScissor(listX, listY, listX + listW, listY + listH);
        int first = Math.max(0, (int) (scroll / ROW));
        for (int i = first; i < shown.size(); i++) {
            int y = listY + i * ROW - Math.round(scroll);
            if (y > listY + listH) break;
            EntityType<?> t = shown.get(i);
            boolean selected = Buddy.idOf(t).equals(c.buddyType);
            if (selected) {
                UI.roundRect(g, listX + 3, y + 1, listW - 6, ROW - 2, 7, 0x55000000 | (accent & 0xFFFFFF));
            }
            g.drawString(this.font, trim(t.getDescription().getString(), listW - 20), listX + 9, y + 4,
                    selected ? 0xFFFFFFFF : 0xFFBBBBCC, false);
        }
        g.disableScissor();

        // Right column labels
        int rx = px + pw / 2 + 6;
        int rw = pw - (rx - px) - 12;
        g.drawString(this.font, "Name", rx, py + 44, 0xFF8888A0, false);
        g.drawString(this.font, "Size", rx, py + 88, 0xFF8888A0, false);
        String size = String.format(Locale.ROOT, "%.1f", c.buddyScale);
        g.drawString(this.font, size, rx + rw / 2 - this.font.width(size) / 2, py + 103, 0xFFFFFFFF, false);
        String current = "Current: " + Buddy.displayName(c.buddyType);
        g.drawString(this.font, trim(current, rw), rx, py + 158, 0xFFBBBBCC, false);
        g.drawString(this.font, trim("Click a mob to pick it.", rw), rx, py + 172, 0xFF6C6C80, false);
    }

    private String trim(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && this.font.width(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
