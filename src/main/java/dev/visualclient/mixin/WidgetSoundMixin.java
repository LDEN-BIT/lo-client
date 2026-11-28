package dev.visualclient.mixin;

import dev.visualclient.Config;
import dev.visualclient.UI;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class WidgetSoundMixin {
    // Every button click uses the Lo Client click instead of the vanilla one.
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$click(SoundManager soundManager, CallbackInfo ci) {
        if (Config.get().customClick) {
            UI.clickSound();
            ci.cancel();
        }
    }
}
