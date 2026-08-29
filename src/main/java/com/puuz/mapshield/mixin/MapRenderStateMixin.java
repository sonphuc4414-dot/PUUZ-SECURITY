package com.puuz.mapshield.mixin;

import com.puuz.mapshield.access.MapRenderStateAccess;
import net.minecraft.client.render.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adds a client-only map-id tag to vanilla's render state. */
@Mixin(MapRenderState.class)
public abstract class MapRenderStateMixin implements MapRenderStateAccess {
    @Unique
    private int puuz$mapId = -1;

    @Override
    @Unique
    public int puuz$getMapId() {
        return puuz$mapId;
    }

    @Override
    @Unique
    public void puuz$setMapId(int mapId) {
        puuz$mapId = mapId;
    }
}
