package com.puuz.mapshield.mixin;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.21.11 map render hook.
 *
 * Vanilla creates the commands for the map while MapRenderer#draw executes.
 * We swap the render-state texture before draw starts and restore it after the
 * method returns. No MixinExtras features are required.
 */
@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private Identifier puuz$originalTexture;
    @Inject(
            method = "draw(Lnet/minecraft/client/render/MapRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ZI)V",
            at = @At("HEAD"),
            require = 1
    )
    private void puuz$beginProtection(
            MapRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            boolean renderDecorations,
            int light,
            CallbackInfo ci
    ) {
        if (MapShieldConfig.isEnabled()) {
            puuz$originalTexture = state.texture;
            state.texture = PuuzMapShieldClient.SAFE_MAP_TEXTURE;
        }
    }

    @Inject(
            method = "draw(Lnet/minecraft/client/render/MapRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ZI)V",
            at = @At("RETURN"),
            require = 1
    )
    private void puuz$restoreTexture(
            MapRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            boolean renderDecorations,
            int light,
            CallbackInfo ci
    ) {
        // The render command queue already captured its texture during draw.
        // The state itself is restored so vanilla keeps its original map state.
        if (MapShieldConfig.isEnabled() && puuz$originalTexture != null) {
            state.texture = puuz$originalTexture;
            puuz$originalTexture = null;
        }
    }
}
