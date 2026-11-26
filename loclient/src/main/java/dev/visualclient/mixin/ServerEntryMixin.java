package dev.visualclient.mixin;

import dev.visualclient.Config;
import dev.visualclient.Hud;
import dev.visualclient.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Multiplayer list: each server sits in its own round slot. */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerEntryMixin {
    @Inject(method = "renderContent", at = @At("HEAD"), require = 0)
    private void loclient$slot(GuiGraphics g, int mouseX, int mouseY, boolean hovered,
                               float partialTick, CallbackInfo ci) {
        ObjectSelectionList.Entry<?> self = (ObjectSelectionList.Entry<?>) (Object) this;
        int x = self.getContentX();
        int y = self.getContentY();
        int w = self.getContentWidth();
        int accent = Hud.accent(Config.get()) & 0xFFFFFF;
        int sx = x - 6, sy = y - 1, sw = w + 12, sh = 34;
        UI.roundRect(g, sx, sy, sw, sh, 17, ((hovered ? 0x44 : 0x24) << 24) | accent);
        UI.roundRing(g, sx, sy, sw, sh, 17, ((hovered ? 0xC0 : 0x66) << 24) | accent);
    }
}
