package com.puuz.mapshield.config;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public final class MapShieldConfigPathHelper {
    private MapShieldConfigPathHelper() {}
    public static Path moneyHistoryPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("puuz-security")
                .resolve("money-history.json");
    }
}
