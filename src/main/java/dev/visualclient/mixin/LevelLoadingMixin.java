package dev.visualclient.mixin;

import dev.visualclient.SakuraBackgroundAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The "loading the world" screen draws its own background, so it needs its own hook. */
@Mixin(targets = "net.minecraft.client.gui.screens.LevelLoadingScreen")
public abstract class LevelLoadingMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$sakura(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (SakuraBackgroundAccess.applies(self)) {
            SakuraBackgroundAccess.draw(g, self.width, self.height);
            ci.cancel();
        }
    }
}
