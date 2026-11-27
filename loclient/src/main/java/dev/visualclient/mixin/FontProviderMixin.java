package dev.visualclient.mixin;

import dev.visualclient.LoFonts;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Second hook for the same thing, at the place every font lookup goes through. */
@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
public abstract class FontProviderMixin {
    @ModifyVariable(method = "glyphs", at = @At("HEAD"), argsOnly = true, require = 0)
    private FontDescription loclient$font(FontDescription description) {
        FontDescription override = LoFonts.override();
        if (override != null && FontDescription.DEFAULT.equals(description)) return override;
        return description;
    }
}
