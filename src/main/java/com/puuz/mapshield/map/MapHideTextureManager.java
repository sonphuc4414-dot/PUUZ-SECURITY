package com.puuz.mapshield.map;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-thread-owned cache for the optional Map Shield replacement image. */
public final class MapHideTextureManager {
    private static final Identifier CUSTOM_TEXTURE = Identifier.of(PuuzMapShieldClient.MOD_ID, "custom_map_hidden");
    private static NativeImageBackedTexture customTexture;
    private static Identifier activeTexture = PuuzMapShieldClient.SAFE_MAP_TEXTURE;

    private MapHideTextureManager() {}

    public static Identifier getActiveTexture() { return activeTexture; }

    public static boolean reload() {
        dispose();
        String configured = MapShieldConfig.getHideMapImagePath();
        if (configured == null || configured.isBlank()) return false;
        try {
            Path path = Path.of(configured);
            if (!Files.isRegularFile(path)) return false;
            try (InputStream in = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(in);
                if (image.getWidth() <= 0 || image.getHeight() <= 0 || image.getWidth() > 1024 || image.getHeight() > 1024) {
                    image.close();
                    return false;
                }
                customTexture = new NativeImageBackedTexture(() -> "PUUZ Security Custom Map Shield", image);
                MinecraftClient.getInstance().getTextureManager().registerTexture(CUSTOM_TEXTURE, customTexture);
                activeTexture = CUSTOM_TEXTURE;
                return true;
            }
        } catch (Exception ignored) {
            activeTexture = PuuzMapShieldClient.SAFE_MAP_TEXTURE;
            return false;
        }
    }

    public static void dispose() {
        if (customTexture != null) {
            try { MinecraftClient.getInstance().getTextureManager().destroyTexture(CUSTOM_TEXTURE); }
            catch (Exception ignored) {}
            customTexture = null;
        }
        activeTexture = PuuzMapShieldClient.SAFE_MAP_TEXTURE;
    }
}
