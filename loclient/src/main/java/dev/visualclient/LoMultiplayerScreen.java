package dev.visualclient;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lo Client multiplayer screen: servers as round cards in a grid, with search and refresh. */
public class LoMultiplayerScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CARD_H = 58, GAP = 8;

    private final Screen parent;
    private final ServerList servers;
    private final Map<ServerData, ServerPinger.Info> infos = new IdentityHashMap<>();
    private final List<ServerData> view = new ArrayList<>();
    private final Map<ServerData, FaviconTexture> icons = new IdentityHashMap<>();
    private final Set<ServerData> badIcons = Collections.newSetFromMap(new IdentityHashMap<>());

    private EditBox search;
    private Button joinBtn, editBtn, deleteBtn;
    private ServerData selected;
    private float scroll, scrollTarget;
    private int gx, gy, gw, gh, cols, cardW;

    public LoMultiplayerScreen(Screen parent) {
        super(Component.literal("Multiplayer"));
        this.parent = parent;
        this.servers = new ServerList(Minecraft.getInstance());
        this.servers.load();
        refreshAll();
    }

    private void refreshAll() {
        for (int i = 0; i < servers.size(); i++) startPing(servers.get(i));
    }

    private void startPing(ServerData sd) {
        FaviconTexture old = icons.remove(sd);
        if (old != null) old.close();
        badIcons.remove(sd);
        ServerPinger.Info info = new ServerPinger.Info();
        infos.put(sd, info);
        ServerPinger.ping(sd.ip, info);
    }

    private void rebuildView() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        view.clear();
        for (int i = 0; i < servers.size(); i++) {
            ServerData sd = servers.get(i);
            if (q.isEmpty() || sd.name.toLowerCase(Locale.ROOT).contains(q)
                    || sd.ip.toLowerCase(Locale.ROOT).contains(q)) {
                view.add(sd);
            }
        }
    }

    @Override
    protected void init() {
        gx = 18;
        gw = this.width - 36;
        gy = 80;
        gh = this.height - gy - 40;
        cols = Mth.clamp((gw + GAP) / (200 + GAP), 1, 4);
        cardW = (gw - GAP * (cols - 1)) / cols;

        search = new EditBox(this.font, this.width - 18 - 60 - 6 - 150, 44, 150, 18, Component.literal("Search"));
        search.setHint(Component.literal("Search servers"));
        search.setMaxLength(40);
        search.setResponder(s -> {
            rebuildView();
            scroll = 0f;
            scrollTarget = 0f;
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            refreshAll();
        }).bounds(this.width - 18 - 60, 44, 60, 18).build());

        int[] w = {120, 24, 44, 52, 100, 50};
        int total = 5 * 6;
        for (int v : w) total += v;
        float k = Math.min(1f, (this.width - 16f) / total);
        int x = (int) ((this.width - total * k) / 2f);
        int y = this.height - 28;
        joinBtn = Button.builder(Component.literal("Join Server"), b -> join(selected))
                .bounds(x, y, (int) (w[0] * k), 20).build();
        x += (int) ((w[0] + 6) * k);
        Button addBtn = Button.builder(Component.literal("+"), b -> addServer())
                .bounds(x, y, (int) (w[1] * k), 20).build();
        x += (int) ((w[1] + 6) * k);
        editBtn = Button.builder(Component.literal("Edit"), b -> edit(selected))
                .bounds(x, y, (int) (w[2] * k), 20).build();
        x += (int) ((w[2] + 6) * k);
        deleteBtn = Button.builder(Component.literal("Delete"), b -> delete(selected))
                .bounds(x, y, (int) (w[3] * k), 20).build();
        x += (int) ((w[3] + 6) * k);
        Button directBtn = Button.builder(Component.literal("Direct Connection"), b -> direct())
                .bounds(x, y, (int) (w[4] * k), 20).build();
        x += (int) ((w[4] + 6) * k);
        Button backBtn = Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(x, y, (int) (w[5] * k), 20).build();
        addRenderableWidget(joinBtn);
        addRenderableWidget(addBtn);
        addRenderableWidget(editBtn);
        addRenderableWidget(deleteBtn);
        addRenderableWidget(directBtn);
        addRenderableWidget(backBtn);

        rebuildView();
    }

    // ------------------------------------------------------------------ actions

    private void join(ServerData sd) {
        if (sd == null) return;
        ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(sd.ip), sd, false, null);
    }

    private void addServer() {
        ServerData sd = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new ServerEditScreen(this, ok -> {
            if (ok) {
                servers.add(sd, false);
                servers.save();
                startPing(sd);
                selected = sd;
                rebuildView();
            }
            this.minecraft.setScreen(this);
        }, sd));
    }

    private void edit(ServerData sd) {
        if (sd == null) return;
        this.minecraft.setScreen(new ServerEditScreen(this, ok -> {
            if (ok) {
                servers.save();
                startPing(sd);
                rebuildView();
            }
            this.minecraft.setScreen(this);
        }, sd));
    }

    private void delete(ServerData sd) {
        if (sd == null) return;
        this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                servers.remove(sd);
                servers.save();
                selected = null;
                rebuildView();
            }
            this.minecraft.setScreen(this);
        }, Component.literal("Remove this server?"), Component.literal(sd.name)));
    }

    private void direct() {
        ServerData sd = new ServerData("Minecraft Server", "", ServerData.Type.OTHER);
        this.minecraft.setScreen(new DirectJoinServerScreen(this, ok -> {
            if (ok) join(sd);
            else this.minecraft.setScreen(this);
        }, sd));
    }

    // -------------------------------------------------------------------- input

    private float maxScroll() {
        int rows = (view.size() + cols - 1) / cols;
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gh);
    }

    private ServerData cardAt(int mx, int my) {
        if (!UI.in(mx, my, gx, gy, gw, gh)) return null;
        int col = (mx - gx) / (cardW + GAP);
        if (col >= cols || (mx - gx) % (cardW + GAP) >= cardW) return null;
        int rel = my - gy + Math.round(scroll);
        int row = rel / (CARD_H + GAP);
        if (rel % (CARD_H + GAP) >= CARD_H) return null;
        int index = row * cols + col;
        return index >= 0 && index < view.size() ? view.get(index) : null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (UI.in((int) mouseX, (int) mouseY, gx, gy, gw, gh)) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 30f, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ServerData hit = cardAt((int) event.x(), (int) event.y());
        if (hit != null) {
            selected = hit;
            if (event.button() == 1) edit(hit);
            else if (doubleClick) join(hit);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN && selected != null && !search.isFocused()) {
            join(selected);
            return true;
        }
        return super.keyPressed(event);
    }

    // ---------------------------------------------------------------- rendering

    private String fit(String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (this.font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && this.font.width(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private void drawSegs(GuiGraphics g, List<ServerPinger.Seg> line, int x, int y, int maxW) {
        int cx = x;
        for (ServerPinger.Seg s : line) {
            int room = x + maxW - cx;
            if (room <= 0) return;
            String t = s.text();
            boolean cut = this.font.width(t) > room;
            if (cut) t = fit(t, room);
            g.drawString(this.font, t, cx, y, 0xFF000000 | s.rgb(), false);
            if (cut) return;
            cx += this.font.width(t);
        }
    }

    /** The server's own icon, uploaded the first time it is needed (on the render thread). */
    private FaviconTexture iconFor(ServerData sd, ServerPinger.Info info) {
        FaviconTexture t = icons.get(sd);
        if (t != null) return t;
        if (info == null || info.favicon == null || badIcons.contains(sd)) return null;
        try (InputStream in = new ByteArrayInputStream(info.favicon)) {
            NativeImage image = NativeImage.read(in);
            t = FaviconTexture.forServer(this.minecraft.getTextureManager(), sd.ip);
            t.upload(image);
            icons.put(sd, t);
            return t;
        } catch (Throwable e) {
            badIcons.add(sd);
            return null;
        }
    }

    private void drawCard(GuiGraphics g, ServerData sd, int x, int y, boolean hover) {
        boolean sel = sd == selected;
        ServerPinger.Info info = infos.get(sd);
        UI.roundRect(g, x, y, cardW, CARD_H, 10, hover ? 0x26FFFFFF : 0x14FFFFFF);
        UI.roundRing(g, x, y, cardW, CARD_H, 10, sel ? 0xFFFFFFFF : 0x30FFFFFF);

        FaviconTexture icon = iconFor(sd, info);
        if (icon != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, icon.textureLocation(), x + 8, y + 8, 0f, 0f, 28, 28, 28, 28);
        } else {
            int hue = Math.abs(sd.name.hashCode()) % 360;
            UI.roundRect(g, x + 8, y + 8, 28, 28, 14, 0xFF000000 | (Mth.hsvToRgb(hue / 360f, 0.45f, 0.7f) & 0xFFFFFF));
            String letter = sd.name.isEmpty() ? "?" : sd.name.substring(0, 1).toUpperCase(Locale.ROOT);
            g.drawString(this.font, letter, x + 22 - this.font.width(letter) / 2, y + 18, 0xFFFFFFFF, false);
        }

        // ping bars + player count, top right
        int right = x + cardW - 8;
        int barsX = right - 14;
        long lat = info != null && info.state == 1 ? info.latency : -1;
        int level = lat < 0 ? 0 : lat < 150 ? 5 : lat < 300 ? 4 : lat < 600 ? 3 : lat < 1000 ? 2 : 1;
        for (int i = 0; i < 5; i++) {
            int h = 3 + i * 2;
            g.fill(barsX + i * 3, y + 19 - h, barsX + i * 3 + 2, y + 19, i < level ? 0xFF55FF55 : 0xFF3A3A44);
        }
        int playersW = 0;
        if (info != null && info.state == 1 && info.online >= 0) {
            String players = info.online + "/" + info.max;
            playersW = this.font.width(players);
            g.drawString(this.font, players, barsX - 6 - playersW, y + 11, 0xFFAAAABB, false);
            playersW += 6;
        }

        int textX = x + 44;
        int nameMax = barsX - 6 - playersW - textX;
        g.drawString(this.font, fit(sd.name, nameMax), textX, y + 10, 0xFFFFFFFF, false);
        g.drawString(this.font, fit(sd.ip, cardW - 52), textX, y + 21, 0xFF7C7C88, false);

        int mx = x + 8, mw = cardW - 16;
        if (info == null || info.state == 0) {
            g.drawString(this.font, "Pinging...", mx, y + 37, 0xFF7C7C88, false);
        } else if (info.state == 2) {
            g.drawString(this.font, fit("Can't connect to server", mw), mx, y + 37, 0xFFFF6666, false);
        } else {
            List<List<ServerPinger.Seg>> lines = info.motd;
            if (lines.size() > 0) drawSegs(g, lines.get(0), mx, y + 35, mw);
            if (lines.size() > 1) drawSegs(g, lines.get(1), mx, y + 46, mw);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fillGradient(0, 0, this.width, this.height, 0xFF0C0C0F, 0xFF060608);
        Config c = Config.get();

        // Top bar
        g.drawString(this.font, Component.literal("LO CLIENT").withStyle(ChatFormatting.BOLD), 18, 14, 0xFFFFFFFF, false);
        String clock = LocalTime.now().format(CLOCK);
        String user = this.minecraft.getUser().getName();
        int uw = this.font.width(user) + 14, cw = this.font.width(clock) + 14;
        int ux = this.width - 18 - uw;
        UI.roundRect(g, ux, 8, uw, 18, 9, 0x22FFFFFF);
        g.drawString(this.font, user, ux + 7, 13, 0xFFFFFFFF, false);
        int cx = ux - 6 - cw;
        UI.roundRect(g, cx, 8, cw, 18, 9, 0x22FFFFFF);
        g.drawString(this.font, clock, cx + 7, 13, 0xFFCCCCD4, false);

        // Title
        g.pose().pushMatrix();
        g.pose().translate(18f, 34f);
        g.pose().scale(2f, 2f);
        g.drawString(this.font, Component.literal("Multiplayer").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
        String sub = servers.size() + (servers.size() == 1 ? " server" : " servers");
        g.drawString(this.font, sub, 18, 56, 0xFF7C7C88, false);

        // Grid
        scroll += (scrollTarget - scroll) * 0.3f;
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;
        g.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < view.size(); i++) {
            int col = i % cols, row = i / cols;
            int x = gx + col * (cardW + GAP);
            int y = gy + row * (CARD_H + GAP) - Math.round(scroll);
            if (y + CARD_H < gy || y > gy + gh) continue;
            boolean hover = UI.in(mouseX, mouseY, x, y, cardW, CARD_H) && mouseY >= gy && mouseY < gy + gh;
            drawCard(g, view.get(i), x, y, hover);
        }
        g.disableScissor();
        if (view.isEmpty()) {
            String none = servers.size() == 0 ? "No servers yet. Press + to add one." : "No servers match your search.";
            g.drawString(this.font, none, this.width / 2 - this.font.width(none) / 2, gy + 30, 0xFF7C7C88, false);
        }

        // Scrollbar
        float max = maxScroll();
        if (max > 0) {
            int thumbH = Math.max(18, (int) (gh * (gh / (gh + max))));
            int thumbY = gy + (int) ((gh - thumbH) * (scroll / max));
            UI.roundRect(g, this.width - 12, gy, 4, gh, 2, 0x30FFFFFF);
            UI.roundRect(g, this.width - 12, thumbY, 4, thumbH, 2, 0xFFCCCCD4);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        joinBtn.active = selected != null;
        editBtn.active = selected != null;
        deleteBtn.active = selected != null;
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        for (FaviconTexture t : icons.values()) t.close();
        icons.clear();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
