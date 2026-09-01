package com.puuz.mapshield.integration;

import com.puuz.mapshield.gui.PuuzSecuritySettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu integration for PUUZ SECURITY settings. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new PuuzSecuritySettingsScreen(parent);
    }
}
