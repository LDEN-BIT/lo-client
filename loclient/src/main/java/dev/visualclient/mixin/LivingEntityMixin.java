package dev.visualclient.mixin;

import dev.visualclient.Buddy;
import dev.visualclient.Fullbright;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // Fullbright: the light texture asks the local player for Night Vision; we say yes.
    @Inject(method = "hasEffect", at = @At("RETURN"), cancellable = true, require = 0)
    private void loclient$nvHas(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && Fullbright.fakes((LivingEntity) (Object) this, effect)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getEffect", at = @At("RETURN"), cancellable = true, require = 0)
    private void loclient$nvGet(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (cir.getReturnValue() == null && Fullbright.fakes((LivingEntity) (Object) this, effect)) {
            cir.setReturnValue(Fullbright.instance());
        }
    }

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
