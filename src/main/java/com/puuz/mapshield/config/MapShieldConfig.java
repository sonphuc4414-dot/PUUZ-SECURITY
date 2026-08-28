package com.puuz.mapshield.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny local JSON config with a safe default. */
public final class MapShieldConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("puuz-map-shield.json");

    private static boolean enabled = true;

    public static void load() {
        enabled = true;
        if (!Files.isRegularFile(PATH)) {
            return;
        }

        try {
            JsonObject object = GSON.fromJson(
                    Files.readString(PATH, StandardCharsets.UTF_8),
                    JsonObject.class
            );
            if (object != null && object.has("enabled") && object.get("enabled").isJsonPrimitive()) {
                enabled = object.get("enabled").getAsBoolean();
            }
        } catch (Exception ignored) {
            // Safe default: protection stays enabled if config is invalid.
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject object = new JsonObject();
            object.addProperty("enabled", enabled);
            Files.writeString(PATH, GSON.toJson(object), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Config failure must never crash Minecraft.
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private MapShieldConfig() {
    }
}
