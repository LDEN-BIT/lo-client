package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Add or edit a server: a name and an address. The callback gets true on Done, false on Cancel. */
public class ServerEditScreen extends Screen {
    private final ServerData data;
    private final Consumer<Boolean> callback;
    private EditBox nameBox, addressBox;
    private int px, py, pw, ph;

    public ServerEditScreen(Screen parent, Consumer<Boolean> callback, ServerData data) {
        super(Component.literal("Server"));
        this.callback = callback;
        this.data = data;
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 16, 300);
        ph = 140;
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;

        nameBox = new EditBox(this.font, px + 14, py + 40, pw - 28, 18, Component.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(data.name == null ? "" : data.name);
        addRenderableWidget(nameBox);

        addressBox = new EditBox(this.font, px + 14, py + 78, pw - 28, 18, Component.literal("Address"));
        addressBox.setMaxLength(128);
        addressBox.setHint(Component.literal("example.com"));
        addressBox.setValue(data.ip == null ? "" : data.ip);
        addRenderableWidget(addressBox);

        int bw = (pw - 28 - 8) / 2;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            String ip = addressBox.getValue().trim();
            if (ip.isEmpty()) return;
            String name = nameBox.getValue().trim();
            data.name = name.isEmpty() ? "Minecraft Server" : name;
            data.ip = ip;
            callback.accept(true);
        }).bounds(px + 14, py + ph - 32, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> callback.accept(false))
                .bounds(px + 14 + bw + 8, py + ph - 32, bw, 20).build());
        setInitialFocus(nameBox);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int accent = Hud.accent(Config.get());
        g.fill(0, 0, this.width, this.height, 0x99000000);
        UI.roundRect(g, px, py, pw, ph, 16, 0xF2101018);
        UI.roundRing(g, px, py, pw, ph, 16, 0x80000000 | (accent & 0xFFFFFF));
        g.drawString(this.font, "Server", px + 14, py + 12, accent, true);
        g.drawString(this.font, "Name", px + 14, py + 29, 0xFF8888A0, false);
        g.drawString(this.font, "Address", px + 14, py + 67, 0xFF8888A0, false);
    }

    @Override
    public void onClose() {
        callback.accept(false);
    }
}
