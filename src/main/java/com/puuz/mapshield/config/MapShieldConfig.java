package com.puuz.mapshield.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Persistent client config for PUUZ SECURITY. */
public final class MapShieldConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("puuz-map-shield.json");

    private static boolean enabled = true;
    private static boolean passwordMaskEnabled = true;
    private static boolean updateCheckerEnabled = true;
    private static boolean mapTooltipPreviewBlocked = true;
    private static int accentColor = 0xFF8E78B8;
    private static int textColor = 0xFF56515D;
    private static int backgroundColor = 0xFFF1EEF5;
    private static String backgroundPath = "";
    private static final Map<String, Set<Integer>> ALLOWED_MAPS = new HashMap<>();
    private static final List<String> PASSWORD_COMMANDS = new ArrayList<>();

    private static final List<String> DEFAULT_PASSWORD_COMMANDS = List.of(
            "/l", "/login", "/reg", "/register", "/dn", "/dk"
    );

    public static synchronized void load() {
        enabled = true;
        passwordMaskEnabled = true;
        updateCheckerEnabled = true;
        mapTooltipPreviewBlocked = true;
        accentColor = 0xFF8E78B8;
        textColor = 0xFF56515D;
        backgroundColor = 0xFFF1EEF5;
        backgroundPath = "";
        ALLOWED_MAPS.clear();
        PASSWORD_COMMANDS.clear();
        PASSWORD_COMMANDS.addAll(DEFAULT_PASSWORD_COMMANDS);

        if (!Files.isRegularFile(PATH)) {
            return;
        }

        try {
            JsonObject root = GSON.fromJson(
                    Files.readString(PATH, StandardCharsets.UTF_8),
                    JsonObject.class
            );
            if (root == null) {
                return;
            }

            if (root.has("enabled") && root.get("enabled").isJsonPrimitive()) {
                enabled = root.get("enabled").getAsBoolean();
            }
            if (root.has("passwordMaskEnabled") && root.get("passwordMaskEnabled").isJsonPrimitive()) {
                passwordMaskEnabled = root.get("passwordMaskEnabled").getAsBoolean();
            }
            if (root.has("updateCheckerEnabled") && root.get("updateCheckerEnabled").isJsonPrimitive()) {
                updateCheckerEnabled = root.get("updateCheckerEnabled").getAsBoolean();
            }
            if (root.has("mapTooltipPreviewBlocked") && root.get("mapTooltipPreviewBlocked").isJsonPrimitive()) {
                mapTooltipPreviewBlocked = root.get("mapTooltipPreviewBlocked").getAsBoolean();
            }
            if (root.has("accentColor") && root.get("accentColor").isJsonPrimitive()) {
                accentColor = root.get("accentColor").getAsInt();
            }
            if (root.has("textColor") && root.get("textColor").isJsonPrimitive()) {
                textColor = root.get("textColor").getAsInt();
            }
            if (root.has("backgroundColor") && root.get("backgroundColor").isJsonPrimitive()) {
                backgroundColor = root.get("backgroundColor").getAsInt();
            }
            if (root.has("backgroundPath") && root.get("backgroundPath").isJsonPrimitive()) {
                backgroundPath = root.get("backgroundPath").getAsString();
            }

            JsonElement commandsElement = root.get("passwordCommands");
            if (commandsElement != null && commandsElement.isJsonArray()) {
                List<String> loaded = new ArrayList<>();
                for (JsonElement element : commandsElement.getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        String normalized = normalizeCommand(element.getAsString());
                        if (normalized != null && !loaded.contains(normalized)) {
                            loaded.add(normalized);
                        }
                    }
                }
                if (!loaded.isEmpty()) {
                    PASSWORD_COMMANDS.clear();
                    PASSWORD_COMMANDS.addAll(loaded);
                }
            }

            JsonElement serversElement = root.get("allowedMaps");
            if (serversElement != null && serversElement.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : serversElement.getAsJsonObject().entrySet()) {
                    if (!entry.getValue().isJsonArray()) {
                        continue;
                    }
                    Set<Integer> ids = new HashSet<>();
                    for (JsonElement id : entry.getValue().getAsJsonArray()) {
                        if (id.isJsonPrimitive()) {
                            try {
                                ids.add(id.getAsInt());
                            } catch (RuntimeException ignored) {
                            }
                        }
                    }
                    if (!ids.isEmpty()) {
                        ALLOWED_MAPS.put(entry.getKey(), ids);
                    }
                }
            }
        } catch (Exception ignored) {
            PASSWORD_COMMANDS.clear();
            PASSWORD_COMMANDS.addAll(DEFAULT_PASSWORD_COMMANDS);
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("passwordMaskEnabled", passwordMaskEnabled);
            root.addProperty("updateCheckerEnabled", updateCheckerEnabled);
            root.addProperty("mapTooltipPreviewBlocked", mapTooltipPreviewBlocked);
            root.addProperty("accentColor", accentColor);
            root.addProperty("textColor", textColor);
            root.addProperty("backgroundColor", backgroundColor);
            root.addProperty("backgroundPath", backgroundPath);

            JsonArray commands = new JsonArray();
            for (String command : PASSWORD_COMMANDS) {
                commands.add(command);
            }
            root.add("passwordCommands", commands);

            JsonObject servers = new JsonObject();
            for (Map.Entry<String, Set<Integer>> entry : ALLOWED_MAPS.entrySet()) {
                JsonArray ids = new JsonArray();
                for (Integer id : entry.getValue()) {
                    ids.add(id);
                }
                servers.add(entry.getKey(), ids);
            }
            root.add("allowedMaps", servers);

            Files.writeString(PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }

    public static synchronized void setEnabled(boolean value) {
        enabled = value;
    }

    public static synchronized boolean isPasswordMaskEnabled() {
        return passwordMaskEnabled;
    }

    public static synchronized void setPasswordMaskEnabled(boolean value) {
        passwordMaskEnabled = value;
    }

    public static synchronized boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public static synchronized void setUpdateCheckerEnabled(boolean value) {
        updateCheckerEnabled = value;
    }

    public static synchronized boolean isMapTooltipPreviewBlocked() {
        return mapTooltipPreviewBlocked;
    }

    public static synchronized void setMapTooltipPreviewBlocked(boolean value) {
        mapTooltipPreviewBlocked = value;
    }

    public static synchronized int getAccentColor() {
        return accentColor;
    }

    public static synchronized void setAccentColor(int value) {
        accentColor = value | 0xFF000000;
    }

    public static synchronized int getTextColor() {
        return textColor;
    }

    public static synchronized void setTextColor(int value) {
        textColor = value | 0xFF000000;
    }

    public static synchronized int getBackgroundColor() {
        return backgroundColor;
    }

    public static synchronized void setBackgroundColor(int value) {
        backgroundColor = value | 0xFF000000;
    }

    public static synchronized String getBackgroundPath() {
        return backgroundPath;
    }

    public static synchronized void setBackgroundPath(String value) {
        backgroundPath = value == null ? "" : value.trim();
    }

    public static synchronized void resetPasswordCommands() {
        PASSWORD_COMMANDS.clear();
        PASSWORD_COMMANDS.addAll(DEFAULT_PASSWORD_COMMANDS);
    }

    public static synchronized List<String> getPasswordCommands() {
        return List.copyOf(PASSWORD_COMMANDS);
    }

    public static synchronized boolean addPasswordCommand(String command) {
        String normalized = normalizeCommand(command);
        if (normalized == null || PASSWORD_COMMANDS.contains(normalized)) {
            return false;
        }
        PASSWORD_COMMANDS.add(normalized);
        return true;
    }

    public static synchronized boolean removePasswordCommand(String command) {
        String normalized = normalizeCommand(command);
        if (normalized == null) {
            return false;
        }
        return PASSWORD_COMMANDS.remove(normalized);
    }

    public static synchronized boolean removeLastPasswordCommand() {
        if (PASSWORD_COMMANDS.isEmpty()) {
            return false;
        }
        PASSWORD_COMMANDS.remove(PASSWORD_COMMANDS.size() - 1);
        return true;
    }

    /** Returns true when the visible chat input should hide arguments after a configured command. */
    public static synchronized boolean shouldMaskChatInput(String text) {
        if (!passwordMaskEnabled || text == null) {
            return false;
        }
        String value = text.stripLeading();
        if (!value.startsWith("/")) {
            return false;
        }

        int space = value.indexOf(' ');
        String command = space < 0 ? value : value.substring(0, space);
        String normalized = normalizeCommand(command);
        return normalized != null && PASSWORD_COMMANDS.contains(normalized) && value.length() > command.length();
    }

    /** Keeps the command visible while replacing all arguments with a same-length mask. */
    public static synchronized String maskChatInput(String text) {
        if (!shouldMaskChatInput(text)) {
            return text;
        }

        String value = text.stripLeading();
        int leading = text.length() - value.length();
        int space = value.indexOf(' ');
        if (space < 0) {
            return text;
        }

        String command = value.substring(0, space);
        String arguments = value.substring(space);
        StringBuilder masked = new StringBuilder(text.length());
        masked.append(" ".repeat(leading));
        masked.append(command);
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (Character.isWhitespace(c)) {
                masked.append(c);
            } else {
                masked.append('*');
            }
        }
        return masked.toString();
    }

    public static synchronized boolean addAllowedMap(String serverKey, int mapId) {
        String key = normalizeServerKey(serverKey);
        return ALLOWED_MAPS.computeIfAbsent(key, ignored -> new HashSet<>()).add(mapId);
    }

    public static synchronized boolean removeAllowedMap(String serverKey, int mapId) {
        String key = normalizeServerKey(serverKey);
        Set<Integer> ids = ALLOWED_MAPS.get(key);
        if (ids == null) {
            return false;
        }
        boolean changed = ids.remove(mapId);
        if (ids.isEmpty()) {
            ALLOWED_MAPS.remove(key);
        }
        return changed;
    }

    public static synchronized boolean isMapAllowed(String serverKey, int mapId) {
        Set<Integer> ids = ALLOWED_MAPS.get(normalizeServerKey(serverKey));
        return ids != null && ids.contains(mapId);
    }

    private static String normalizeServerKey(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            return "singleplayer";
        }
        return serverKey.toLowerCase(Locale.ROOT);
    }

    private static String normalizeCommand(String command) {
        if (command == null) {
            return null;
        }
        String value = command.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        int space = value.indexOf(' ');
        if (space >= 0) {
            value = value.substring(0, space);
        }
        return value.length() > 1 ? value : null;
    }

    private MapShieldConfig() {
    }
}
