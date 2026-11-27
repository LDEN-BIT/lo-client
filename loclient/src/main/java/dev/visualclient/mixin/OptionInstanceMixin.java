package dev.visualclient.mixin;

import dev.visualclient.Fullbright;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class OptionInstanceMixin {
    // Fullbright: the Brightness option reports a huge value while Lo Client fullbright is on.
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$gamma(CallbackInfoReturnable<Object> cir) {
        if (Fullbright.on()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null && (Object) this == mc.options.gamma()) {
                cir.setReturnValue(Fullbright.GAMMA);
            }
        }
    }
}
