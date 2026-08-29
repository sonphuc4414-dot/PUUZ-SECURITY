package com.puuz.mapshield.mixin;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.access.MapRenderStateAccess;
import com.puuz.mapshield.config.MapShieldConfig;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla Sponge Mixin hooks for Minecraft 1.21.11.
 * No MixinExtras features are required.
 */
@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Unique
    private Identifier puuz$originalTexture;

    @Inject(method = "update", at = @At("RETURN"), require = 1)
    private void puuz$rememberMapId(
            MapIdComponent mapId,
            MapState mapState,
            MapRenderState renderState,
            CallbackInfo ci
    ) {
        ((MapRenderStateAccess) (Object) renderState).puuz$setMapId(mapId.id());
    }

    @Inject(method = "draw", at = @At("HEAD"), require = 1)
    private void puuz$beginProtection(
            MapRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            boolean renderDecorations,
            int light,
            CallbackInfo ci
    ) {
        if (!MapShieldConfig.isEnabled()) {
            return;
        }

        int mapId = ((MapRenderStateAccess) (Object) state).puuz$getMapId();
        if (mapId < 0) {
            return;
        }

        if (!MapShieldConfig.isMapAllowed(PuuzMapShieldClient.currentServerKey(), mapId)) {
            puuz$originalTexture = state.texture;
            state.texture = PuuzMapShieldClient.SAFE_MAP_TEXTURE;
        }
    }

    @Inject(method = "draw", at = @At("RETURN"), require = 1)
    private void puuz$restoreTexture(
            MapRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            boolean renderDecorations,
            int light,
            CallbackInfo ci
    ) {
        if (puuz$originalTexture != null) {
            state.texture = puuz$originalTexture;
            puuz$originalTexture = null;
        }
    }
}
