package com.puuz.mapshield.access;

/**
 * Runtime access bridge implemented by MapRenderStateMixin.
 *
 * This interface intentionally lives outside the mixin package. Mixin classes
 * and helper types inside a configured mixin package may not be referenced as
 * ordinary runtime classes by transformed classes.
 */
public interface MapRenderStateAccess {
    int puuz$getMapId();
    void puuz$setMapId(int mapId);
}
