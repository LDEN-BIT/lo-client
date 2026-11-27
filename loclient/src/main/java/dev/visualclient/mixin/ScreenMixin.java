package dev.visualclient.mixin;

import dev.visualclient.SakuraBackgroundAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    // Loading screens get the cherry blossom background instead of the vanilla one.
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$sakura(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (SakuraBackgroundAccess.applies(self)) {
            SakuraBackgroundAccess.draw(g, self.width, self.height);
            ci.cancel();
        }
    }
}
