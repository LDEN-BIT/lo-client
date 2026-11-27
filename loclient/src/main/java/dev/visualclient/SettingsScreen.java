package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Lo Client settings. Draws its own panel, tabs, switches, sliders, color swatches and a
 * smooth scrollbar over invisible vanilla widgets that do the clicking.
 */
public class SettingsScreen extends Screen {
    private static final String[] TABS = {"HUD", "Style", "World", "Camera", "Waypoints", "Profiles", "Modules", "Buddy"};
    private static final String[] CROSSHAIRS = {"Vanilla", "Dot", "Cross", "Circle"};
    private static final String[] MENU_STYLES = {"Classic", "Dropdown"};

    private static int tab = 0;
    private static String status = "";
    private static String query = "";            // search text (lower case)
    private static boolean searchFocus = false;
    private static final Set<Integer> openTabs = new HashSet<>();   // expanded sections in dropdown style

    private final Screen parent;
    private int px, py, pw, ph;
    private int cx, cy, colW, viewH;
    private int slot;
    private boolean rebuild;
    private String waitingId; // module whose key is being rebound
    private EditBox searchBox;
    private boolean dropdown;
    private boolean filtering;
    private int buildingTab;

    // Smooth scrolling
    private float scroll = 0f, scrollTarget = 0f;
    private boolean draggingBar = false;

    private long openTime = 0L, lastFrame = 0L;

    private enum Kind { TOGGLE, SLIDER, CYCLE, ACTION, COLOR, HEADER }

    private static final class Item {
        Kind kind;
        String label;
        int x, y, w, h;
        Supplier<Boolean> on;
        Supplier<String> text;
        IntSupplier color;
        GhostSlider slider;
        AbstractWidget widget;
        boolean skip;
        float min, max;
        float hover;
        boolean over;
    }

    private final List<Item> items = new ArrayList<>();
    private final boolean[] tabOver = new boolean[16];

    public SettingsScreen(Screen parent) {
        super(Component.literal("Lo Client"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------- layout

    @Override
    protected void init() {
        items.clear();
        slot = 0;
        filtering = false;
        if (openTime == 0L) openTime = System.currentTimeMillis();
        Config c = Config.get();
        dropdown = c.menuStyle == 1;

        pw = Math.min(this.width - 16, 470);
        ph = Math.min(this.height - 16, 280);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        cy = py + 40;
        if (dropdown) {
            cx = px + 14;
            colW = (pw - 28 - 14 - 8) / 2;
        } else {
            cx = px + 100;
            colW = (pw - 110 - 14) / 2;
        }
        viewH = ph - 40 - 24;

        // Search box: finds any setting or module in every section
        if (searchBox == null) {
            searchBox = new EditBox(this.font, 0, 0, 120, 18, Component.literal("Search"));
            searchBox.setMaxLength(32);
            searchBox.setHint(Component.literal("Search modules"));
        }
        searchBox.setResponder(null);
        searchBox.setWidth(Math.min(150, pw - 130));
        searchBox.setX(px + pw - 30 - searchBox.getWidth());
        searchBox.setY(py + 6);
        if (!searchBox.getValue().equalsIgnoreCase(query)) searchBox.setValue(query);
        searchBox.setResponder(s -> {
            String q = s.trim().toLowerCase(Locale.ROOT);
            if (!q.equals(query)) {
                query = q;
                scroll = 0f;
                scrollTarget = 0f;
                rebuild = true;
            }
        });
        addRenderableWidget(searchBox);
        if (searchFocus) {
            setFocused(searchBox);
            searchBox.setFocused(true);
        }

        if (!dropdown) {
            for (int i = 0; i < TABS.length; i++) {
                final int index = i;
                addRenderableWidget(UI.ghost(px + 8, py + 36 + i * 24, 76, 20, () -> {
                    tab = index;
                    scroll = 0f;
                    scrollTarget = 0f;
                    if (!query.isEmpty()) {
                        query = "";
                        searchBox.setValue("");
                    }
                    rebuild = true;
                }));
            }
        }
        addRenderableWidget(UI.ghost(px + pw - 24, py + 6, 18, 18, this::onClose));

        if (dropdown) {
            buildDropdown(c);
        } else if (!query.isEmpty()) {
            filtering = true;
            for (int t = 0; t < TABS.length; t++) {
                buildingTab = t;
                buildTab(t, c);
            }
            filtering = false;
        } else {
            buildTab(tab, c);
        }

        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());
        scroll = Mth.clamp(scroll, 0f, maxScroll());
    }

    /** Dropdown style: every section is a collapsible header; searching opens the matching ones. */
    private void buildDropdown(Config c) {
        boolean searching = !query.isEmpty();
        for (int t = 0; t < TABS.length; t++) {
            int slotBefore = slot;
            Item h = header(t);
            int afterHeader = items.size();
            if (searching || openTabs.contains(t)) {
                filtering = searching;
                buildingTab = t;
                buildTab(t, c);
                filtering = false;
            }
            if (searching && items.size() == afterHeader) {
                items.remove(h);
                removeWidget(h.widget);
                slot = slotBefore;
            }
        }
    }

    private Item header(int t) {
        if (slot % 2 == 1) slot++;
        Item it = new Item();
        it.kind = Kind.HEADER;
        it.label = TABS[t];
        it.x = cx;
        it.y = cy + (slot / 2) * 25;
        it.w = colW * 2 + 8;
        it.h = 21;
        it.on = () -> openTabs.contains(t) || !query.isEmpty();
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> {
            if (!openTabs.remove(t)) openTabs.add(t);
            rebuild = true;
        });
        addRenderableWidget(it.widget);
        slot += 2;
        items.add(it);
        return it;
    }

    private void buildTab(int t, Config c) {
        switch (t) {
            case 0 -> {
                action("Move HUD elements", "Edit layout", () -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().setScreen(new HudEditorScreen(this));
                    } else {
                        status = "Join a world first to edit the layout.";
                    }
                });
                action("Reset positions", "Reset", () -> {
                    c.resetPositions();
                    Config.save();
                });
                toggle("HUD", () -> c.hud, v -> c.hud = v);
                toggle("Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
                toggle("FPS", () -> c.fps, v -> c.fps = v);
                toggle("Coordinates", () -> c.coords, v -> c.coords = v);
                toggle("Facing", () -> c.facing, v -> c.facing = v);
                toggle("Ping", () -> c.ping, v -> c.ping = v);
                toggle("Clock", () -> c.clock, v -> c.clock = v);
                toggle("Biome", () -> c.biome, v -> c.biome = v);
                toggle("Light level", () -> c.light, v -> c.light = v);
                toggle("CPS", () -> c.cps, v -> c.cps = v);
                toggle("Armor durability", () -> c.armor, v -> c.armor = v);
                toggle("Potion effects", () -> c.effects, v -> c.effects = v);
                toggle("Compass", () -> c.compass, v -> c.compass = v);
                toggle("Sprint / sneak", () -> c.sprintIndicator, v -> c.sprintIndicator = v);
            }
            case 1 -> {
                toggle("Rainbow accent", () -> c.rainbow, v -> c.rainbow = v);
                color("Theme color", () -> c.accentColor, v -> { c.accentColor = v; c.rainbow = false; }, null);
                color("HUD text color", () -> c.hudTextColor, v -> c.hudTextColor = v, null);
                color("Panel color", () -> c.panelColor, v -> c.panelColor = v, null);
                slider("Panel opacity", 0f, 1f, () -> c.bgOpacity, v -> c.bgOpacity = v);
                cycle("Crosshair", CROSSHAIRS, () -> c.crosshair, v -> c.crosshair = v);
                color("Crosshair color", () -> c.crosshairColor, v -> c.crosshairColor = v, null);
                color("Crosshair on target", () -> c.crosshairTargetColor, v -> c.crosshairTargetColor = v, null);
                toggle("Sakura loading screen", () -> c.sakura, v -> c.sakura = v);
                toggle("Hover sounds", () -> c.hoverSounds, v -> c.hoverSounds = v);
                String[] fonts = LoFonts.names();
                cycle("Font", fonts, () -> Math.max(0, java.util.Arrays.asList(fonts).indexOf(c.font)), v -> {
                    c.font = fonts[v];
                    Config.save();
                    status = "Font: " + fonts[v];
                });
                cycle("Menu style", MENU_STYLES, () -> c.menuStyle, v -> {
                    c.menuStyle = v;
                    Config.save();
                    scroll = 0f;
                    scrollTarget = 0f;
                    rebuild = true;
                });
            }
            case 2 -> {
                toggle("Fullbright", () -> c.fullbright, v -> c.fullbright = v);
                toggle("Sky tint", () -> c.skyTint, v -> c.skyTint = v);
                color("Sky color", () -> c.skyColor, v -> c.skyColor = v, null);
                slider("Sky strength", 0f, 1f, () -> c.skyStrength, v -> c.skyStrength = v);
                toggle("Cloud tint", () -> c.cloudTint, v -> c.cloudTint = v);
                color("Cloud color", () -> c.cloudColor, v -> c.cloudColor = v, null);
                slider("Star field boost", 0f, 1f, () -> c.starBoost, v -> c.starBoost = v);
                toggle("Block outline color", () -> c.outlineEnabled, v -> c.outlineEnabled = v);
                color("Outline color", () -> c.outlineColor, v -> c.outlineColor = v, null);
                slider("Particles", 0f, 1f, () -> c.particles, v -> c.particles = v);
            }
            case 3 -> {
                toggle("Freelook", () -> c.freelook, v -> c.freelook = v);
                toggle("Freelook third person", () -> c.freelookThirdPerson, v -> c.freelookThirdPerson = v);
                toggle("Freecam", () -> c.freecam, v -> c.freecam = v);
                slider("Freecam speed", 0.1f, 3f, () -> c.freecamSpeed, v -> c.freecamSpeed = v);
                slider("Zoom strength", 0.05f, 0.8f, () -> c.zoom, v -> c.zoom = v);
                slider("Third-person distance", 0.5f, 3f, () -> c.thirdPersonDistance, v -> c.thirdPersonDistance = v);
            }
            case 4 -> {
                toggle("Show waypoints", () -> c.showWaypoints, v -> c.showWaypoints = v);
                action("New waypoint here", "Add", () -> {
                    Waypoints.addHere(null);
                    rebuild = true;
                });
                List<Config.Waypoint> list = Waypoints.list();
                for (int i = 0; i < Math.min(list.size(), 40); i++) {
                    final Config.Waypoint w = list.get(i);
                    action(w.name + " (" + Math.round(w.x) + ", " + Math.round(w.z) + ")", "Remove", () -> {
                        Waypoints.remove(w.name);
                        rebuild = true;
                    });
                }
            }
            case 5 -> {
                for (int n = 1; n <= 3; n++) {
                    final int s = n;
                    action("Profile " + n, "Save", () -> {
                        Config.saveProfile(s);
                        rebuild = true;
                    });
                    action("Profile " + n + (Config.hasProfile(n) ? "" : " (empty)"), "Load", () -> {
                        if (Config.loadProfile(s)) rebuild = true;
                    });
                }
            }
            case 6 -> {
                moduleRow("HUD", "hud", () -> c.hud, v -> c.hud = v);
                moduleRow("Keystrokes", "keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
                moduleRow("Freelook", "freelook", () -> c.freelook, v -> c.freelook = v);
                moduleRow("Zoom", "zoom", () -> c.zoomModule, v -> c.zoomModule = v);
                moduleRow("Fullbright", "fullbright", () -> c.fullbright, v -> c.fullbright = v);
                moduleRow("Waypoints", "waypoint", () -> c.showWaypoints, v -> c.showWaypoints = v);
                moduleRow("Buddy", "buddy", () -> c.buddyEnabled, v -> c.buddyEnabled = v);
                moduleRow("Freecam", "freecam", () -> c.freecam, v -> c.freecam = v);
                cycle("Menu style", MENU_STYLES, () -> c.menuStyle, v -> {
                    c.menuStyle = v;
                    Config.save();
                    scroll = 0f;
                    scrollTarget = 0f;
                    rebuild = true;
                });
                keyItem("HUD layout editor key", "editor");
                keyItem("Next HUD corner key", "corner");
                action("Clear all module keys", "Clear", () -> {
                    Modules.clearAll();
                    status = "All module keys cleared.";
                });
            }
            case 7 -> {
                toggle("Buddy", () -> c.buddyEnabled, v -> c.buddyEnabled = v);
                action("Mob: " + Buddy.displayName(c.buddyType), "Choose", () ->
                        Minecraft.getInstance().setScreen(new BuddyScreen(this)));
                action("Name: " + (c.buddyName == null || c.buddyName.isBlank() ? "(none)" : c.buddyName), "Rename", () ->
                        Minecraft.getInstance().setScreen(new BuddyScreen(this)));
                slider("Buddy size", 0.2f, 1.5f, () -> c.buddyScale, v -> c.buddyScale = v);
            }
            default -> {}
        }
    }

    private int contentHeight() {
        return ((slot + 1) / 2) * 25;
    }

    private float maxScroll() {
        return Math.max(0, contentHeight() - viewH);
    }

    private Item next(Kind kind, String label) {
        Item it = new Item();
        it.kind = kind;
        it.label = label;
        if (filtering && !matches(label)) {
            it.skip = true;
            return it;
        }
        it.x = cx + (slot % 2) * (colW + 8);
        it.y = cy + (slot / 2) * 25;
        it.w = colW;
        it.h = 21;
        slot++;
        items.add(it);
        return it;
    }

    private boolean matches(String label) {
        return label.toLowerCase(Locale.ROOT).contains(query)
                || TABS[buildingTab].toLowerCase(Locale.ROOT).contains(query);
    }

    private void toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        Item it = next(Kind.TOGGLE, label);
        if (it.skip) return;
        it.on = get;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> set.accept(!get.get()));
        addRenderableWidget(it.widget);
    }

    private void cycle(String label, String[] names, Supplier<Integer> get, Consumer<Integer> set) {
        Item it = next(Kind.CYCLE, label);
        if (it.skip) return;
        it.text = () -> names[get.get()];
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> set.accept((get.get() + 1) % names.length));
        addRenderableWidget(it.widget);
    }

    private void keyItem(String label, String id) {
        Item it = next(Kind.CYCLE, label);
        if (it.skip) return;
        it.text = () -> id.equals(waitingId) ? "Press a key..." : Modules.name(id);
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> waitingId = id);
        addRenderableWidget(it.widget);
    }

    private void moduleRow(String label, String id, Supplier<Boolean> get, Consumer<Boolean> set) {
        toggle(label, get, set);
        keyItem(label + " key", id);
    }

    private void slider(String label, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        Item it = next(Kind.SLIDER, label);
        if (it.skip) return;
        it.min = min;
        it.max = max;
        it.slider = new GhostSlider(it.x, it.y, it.w, it.h, min, max, get.get(), set);
        it.widget = it.slider;
        addRenderableWidget(it.widget);
    }

    private void action(String label, String chip, Runnable run) {
        Item it = next(Kind.ACTION, label);
        if (it.skip) return;
        it.text = () -> chip;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, run);
        addRenderableWidget(it.widget);
    }

    private void color(String label, IntSupplier get, IntConsumer set, Runnable onDone) {
        Item it = next(Kind.COLOR, label);
        if (it.skip) return;
        it.color = get;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () ->
                Minecraft.getInstance().setScreen(new ColorPickerScreen(this, label, get, set, onDone)));
        addRenderableWidget(it.widget);
    }

    // ------------------------------------------------------------------- input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 28f, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int trackX() {
        return px + pw - 10;
    }

    private int thumbH() {
        return maxScroll() > 0
                ? Math.max(18, (int) (viewH * (viewH / (float) contentHeight())))
                : viewH;
    }

    private void dragBarTo(double my) {
        float max = maxScroll();
        float frac = ((float) my - cy - thumbH() / 2f) / Math.max(1f, (viewH - thumbH()));
        scrollTarget = Mth.clamp(frac * max, 0f, max);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && maxScroll() > 0
                && UI.in((int) event.x(), (int) event.y(), trackX() - 3, cy, 12, viewH)) {
            draggingBar = true;
            dragBarTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingBar) {
            dragBarTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (searchBox != null) searchFocus = searchBox.isFocused();
        if (rebuild) {
            rebuild = false;
            rebuildWidgets();
        }

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float ease = 1f - (float) Math.exp(-dt * 14f);

        float max = maxScroll();
        scrollTarget = Mth.clamp(scrollTarget, 0f, max);
        scroll += (scrollTarget - scroll) * (draggingBar ? 0.6f : ease);
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;

        // Move the invisible widgets with the content; hide the ones outside the view
        int sOff = Math.round(scroll);
        for (Item it : items) {
            int y = it.y - sOff;
            it.widget.setY(y);
            it.widget.visible = y >= cy && y + it.h <= cy + viewH;
        }

        super.render(g, mx, my, partialTick); // vanilla background + invisible widgets

        Config c = Config.get();
        int accent = Hud.accent(c);
        float open = Mth.clamp((now - openTime) / 260f, 0f, 1f);
        open = 1f - (1f - open) * (1f - open);

        g.fill(0, 0, this.width, this.height, ((int) (0x88 * open)) << 24);
        UI.roundRect(g, px, py, pw, ph, 16, 0xF2101018);
        UI.roundRing(g, px, py, pw, ph, 16, 0x80000000 | (accent & 0xFFFFFF));
        if (!dropdown) g.fill(px + 92, py + 14, px + 93, py + ph - 14, 0xFF2A2A3C);

        g.drawString(this.font, "Lo Client", px + 10, py + 12, accent, true);
        g.drawString(this.font, "x", px + pw - 19, py + 11,
                UI.in(mx, my, px + pw - 24, py + 6, 18, 18) ? 0xFFFFFFFF : 0xFF888899, false);

        // Tabs (classic style)
        for (int i = 0; !dropdown && i < TABS.length; i++) {
            int x = px + 8, y = py + 36 + i * 24;
            boolean sel = i == tab && query.isEmpty();
            boolean hover = UI.in(mx, my, x, y, 76, 20);
            if (hover && !tabOver[i]) UI.hoverSound();
            tabOver[i] = hover;
            if (sel) {
                UI.roundRect(g, x, y, 76, 20, 10, 0x55000000 | (accent & 0xFFFFFF));
            } else if (hover) {
                UI.roundRect(g, x, y, 76, 20, 10, 0x18FFFFFF);
            }
            g.drawString(this.font, trim(TABS[i], 62), x + 9, y + 6, sel ? 0xFFFFFFFF : 0xFFAAAABB, false);
        }

        // Items (clipped to the scroll view)
        g.enableScissor(cx - 2, cy, px + pw - 14, cy + viewH);
        for (Item it : items) {
            int iy = it.y - sOff;
            if (iy + it.h < cy || iy > cy + viewH) continue;
            boolean hoverNow = UI.in(mx, my, it.x, iy, it.w, it.h) && my >= cy && my < cy + viewH;
            if (hoverNow && !it.over) UI.hoverSound();
            it.over = hoverNow;
            it.hover += ((hoverNow ? 1f : 0f) - it.hover) * ease;
            int bgA = (int) (0x18 + 0x18 * it.hover);
            UI.roundRect(g, it.x, iy, it.w, it.h, 10, (bgA << 24) | 0xFFFFFF);

            switch (it.kind) {
                case HEADER -> {
                    boolean openNow = it.on.get();
                    UI.roundRect(g, it.x, iy, it.w, it.h, 10, 0x40000000 | (accent & 0xFFFFFF));
                    g.drawString(this.font, trim(it.label, it.w - 40), it.x + 10, iy + 7, 0xFFFFFFFF, false);
                    int ax = it.x + it.w - 16, ay = iy + 8;
                    if (openNow) {
                        for (int r = 0; r < 4; r++) g.fill(ax + r, ay + r, ax + 9 - r, ay + r + 1, 0xFFFFFFFF);
                    } else {
                        for (int r = 0; r < 4; r++) g.fill(ax + r, ay - 1 + r, ax + r + 1, ay + 8 - r, 0xFFFFFFFF);
                    }
                }
                case TOGGLE -> {
                    g.drawString(this.font, trim(it.label, it.w - 8 - 46), it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    boolean on = it.on.get();
                    int sx = it.x + it.w - 36, sy = iy + 4; // 28 x 13 switch
                    int track = on ? (0xFF000000 | (accent & 0xFFFFFF)) : 0xFF454555;
                    UI.roundRect(g, sx, sy, 28, 13, 6, track);
                    UI.roundRing(g, sx, sy, 28, 13, 6, on ? 0xFFFFFFFF : 0xFF8A8A9C);
                    // the knob is dark on a light track and white on a dark one, so it is always visible
                    int lum = (((track >> 16) & 255) * 299 + ((track >> 8) & 255) * 587 + (track & 255) * 114) / 1000;
                    int knob = lum > 140 ? 0xFF16161E : 0xFFFFFFFF;
                    int kx = on ? sx + 16 : sx + 3;
                    UI.roundRect(g, kx, sy + 2, 9, 9, 4, knob);
                }
                case SLIDER -> {
                    float frac = it.slider.frac();
                    float val = it.min + frac * (it.max - it.min);
                    String vs = String.format("%.2f", val);
                    int vw = this.font.width(vs);
                    g.drawString(this.font, trim(it.label, it.w - 16 - vw - 6), it.x + 8, iy + 3, 0xFFFFFFFF, false);
                    g.drawString(this.font, vs, it.x + it.w - 8 - vw, iy + 3, 0xFFAAAABB, false);
                    int tx = it.x + 8, tw = it.w - 16, ty = iy + it.h - 6;
                    UI.roundRect(g, tx, ty, tw, 2, 1, 0xFF3A3A4A);
                    UI.roundRect(g, tx, ty, Math.max(2, (int) (tw * frac)), 2, 1, accent);
                    int kx = tx + (int) (tw * frac);
                    UI.roundRect(g, kx - 3, ty - 3, 7, 7, 3, 0xFFFFFFFF);
                }
                case COLOR -> {
                    int rgb = it.color.getAsInt() & 0xFFFFFF;
                    int swX = it.x + it.w - 26;
                    String hx = String.format("#%06X", rgb);
                    int hw = this.font.width(hx);
                    boolean showHex = it.w - 8 - 26 - 4 - hw - 40 > 0;
                    int labelRoom = (showHex ? swX - 4 - hw : swX) - 6 - (it.x + 8);
                    g.drawString(this.font, trim(it.label, labelRoom), it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    UI.roundRect(g, swX, iy + 4, 20, 13, 6, 0xFF000000 | rgb);
                    UI.roundRing(g, swX, iy + 4, 20, 13, 6, hoverNow ? 0xFFFFFFFF : 0xFF4A4A5C);
                    if (showHex) g.drawString(this.font, hx, swX - 4 - hw, iy + 7, 0xFFAAAABB, false);
                }
                case CYCLE, ACTION -> {
                    String chip = trim(it.text.get(), (int) (it.w * 0.55f) - 12);
                    int cw = this.font.width(chip) + 12;
                    int chx = it.x + it.w - cw - 4;
                    g.drawString(this.font, trim(it.label, chx - 6 - (it.x + 8)), it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    int chA = (int) (0x30 + 0x25 * it.hover);
                    UI.roundRect(g, chx, iy + 3, cw, it.h - 6, (it.h - 6) / 2, (chA << 24) | 0xFFFFFF);
                    g.drawString(this.font, chip, chx + 6, iy + 7, accent, false);
                }
            }
        }
        g.disableScissor();

        // Scrollbar
        if (max > 0) {
            int trackX = trackX(), thumbH = thumbH();
            UI.roundRect(g, trackX, cy, 4, viewH, 2, 0x30FFFFFF);
            int thumbY = cy + (int) ((viewH - thumbH) * (scroll / max));
            boolean thumbHover = draggingBar || UI.in(mx, my, trackX - 3, cy, 12, viewH);
            UI.roundRect(g, trackX, thumbY, 4, thumbH, 2, thumbHover ? 0xFFFFFFFF : accent);
        }

        // Hints
        String hint = !query.isEmpty() ? (items.isEmpty() ? "Nothing matches your search." : "Showing results for your search.")
                : switch (tab) {
            case 0 -> "Edit layout lets you drag HUD elements (or bind a key in the Modules tab).";
            case 1 -> "Fonts change instantly. Menu style switches between Classic and Dropdown.";
            case 2 -> "Fullbright has its own switch and key in the Modules tab.";
            case 3 -> "Freelook and Freecam keys are set in the Modules tab. Freecam: movement keys fly the camera.";
            case 4 -> "Bind a waypoint key in the Modules tab. Commands: /wp add, /wp remove, /wp list";
            case 5 -> "Profiles store all your look and camera settings. Waypoints are kept separate.";
            case 6 -> "Click a key box, then press the new key. Backspace clears it, Esc cancels.";
            case 7 -> "Your buddy is a small mob that follows you. Only you can see it.";
            default -> "";
        };
        if (dropdown && query.isEmpty()) hint = "Click a section to open it. Use the search box to find any setting.";
        if (!status.isEmpty()) hint = status;
        if (!hint.isEmpty()) {
            g.drawString(this.font, trim(hint, pw - (dropdown ? 30 : 116)), cx, py + ph - 16, 0xFF8888A0, false);
        }

        searchBox.render(g, mx, my, partialTick); // drawn last so the panel does not cover it
    }

    private String trim(String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (this.font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && this.font.width(s + "...") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (waitingId != null) {
            String id = waitingId;
            waitingId = null;
            int key = event.key();
            if (key == InputConstants.KEY_ESCAPE) {
                return true; // cancel
            }
            boolean clear = key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE;
            Modules.set(id, clear ? -1 : key);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void onClose() {
        status = "";
        waitingId = null;
        query = "";
        searchFocus = false;
        Minecraft.getInstance().setScreen(parent);
    }
}
