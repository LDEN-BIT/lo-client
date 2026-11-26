package dev.visualclient.mixin;

import dev.visualclient.LoFonts;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps the default font for the Lo Client font you picked (instantly, no reload). */
@Mixin(Font.class)
public abstract class FontMixin {
    @ModifyVariable(method = "getFontSet", at = @At("HEAD"), argsOnly = true, require = 0)
    private FontDescription loclient$font(FontDescription description) {
        FontDescription override = LoFonts.override();
        if (override != null && FontDescription.DEFAULT.equals(description)) return override;
        return description;
    }
}
