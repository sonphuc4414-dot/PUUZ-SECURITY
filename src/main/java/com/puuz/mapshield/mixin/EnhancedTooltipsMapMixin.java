package com.puuz.mapshield.mixin;

import com.puuz.mapshield.config.MapShieldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional compatibility hook for EnhancedTooltips.
 *
 * EnhancedTooltips creates a fresh MapRenderState itself and submits it
 * directly through GuiGraphicsExtractor#map(), so protecting MapRenderer.draw()
 * alone is too late. This hook cancels only the image part of its map tooltip,
 * leaving the normal tooltip text (Map, ID, scaling, level, etc.) untouched.
 *
 * @Pseudo keeps PUUZ compatible when EnhancedTooltips is not installed.
 */
@Pseudo
@Mixin(targets = "dev.ultimatchamp.enhancedtooltips.component.MapTooltipComponent", remap = false)
public abstract class EnhancedTooltipsMapMixin {

    @Inject(method = "drawImage", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void puuz$blockEnhancedMapPreview(CallbackInfo ci) {
        if (MapShieldConfig.isEnabled() && MapShieldConfig.isMapTooltipPreviewBlocked()) {
            ci.cancel();
        }
    }
}
