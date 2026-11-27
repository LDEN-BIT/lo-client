package dev.visualclient.mixin;

import dev.visualclient.Buddy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // Buddy: never the target of your cursor and never pushes you around.
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$buddyNoPick(CallbackInfoReturnable<Boolean> cir) {
        if (Buddy.isBuddy((Entity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true, require = 0)
    private void loclient$buddyNoPush(CallbackInfoReturnable<Boolean> cir) {
        if (Buddy.isBuddy((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
