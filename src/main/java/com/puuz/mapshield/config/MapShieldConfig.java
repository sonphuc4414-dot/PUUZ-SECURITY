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

import com.puuz.mapshield.money.MoneyHistoryStyle;

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
    private static int backgroundColor = 0xFF15151A;
    private static int passwordCurtainColor = 0xB913151A;
    private static int passwordMaskColor = 0xFFF1EEF5;
    private static int passwordMaskBorderColor = 0xFF7C5CBF;
    private static int menuSurfaceColor = 0xFF1B1B21;
    private static int menuCardColor = 0xFF21212A;
    private static int menuBorderColor = 0xFF30303A;
    private static int menuGradientStartColor = 0xFF15151A;
    private static int menuGradientEndColor = 0xFF20202A;
    private static boolean menuGradientEnabled = true;
    private static int textGradientStartColor = 0xFFB69ADF;
    private static int textGradientEndColor = 0xFF7C5CBF;
    private static boolean textGradientEnabled = true;
    private static String backgroundPath = "";
    private static boolean moneyHistoryEnabled = false;
    private static boolean moneyBalanceVisible = true;
    private static boolean moneySentVisible = true;
    private static boolean moneyReceivedVisible = true;
    private static boolean moneyShowNames = true;
    private static boolean moneyShowAmounts = true;
    private static float moneyX = 0.02f;
    private static float moneyY = 0.02f;
    private static int moneyWidth = 240;
    private static int moneyHeight = 118;
    private static float moneyScale = 1.0f;
    private static int moneyVisibleEntries = 5;
    private static String moneyStyle = MoneyHistoryStyle.CARD.name();
    private static int moneyOpacity = 92;
    private static String hideMapImagePath = "";
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
        backgroundColor = 0xFF15151A;
        passwordCurtainColor = 0xB913151A;
        passwordMaskColor = 0xFFF1EEF5;
        passwordMaskBorderColor = 0xFF7C5CBF;
        menuSurfaceColor = 0xFF1B1B21;
        menuCardColor = 0xFF21212A;
        menuBorderColor = 0xFF30303A;
        menuGradientStartColor = 0xFF15151A;
        menuGradientEndColor = 0xFF20202A;
        menuGradientEnabled = true;
        textGradientStartColor = 0xFFB69ADF;
        textGradientEndColor = 0xFF7C5CBF;
        textGradientEnabled = true;
        backgroundPath = "";
        moneyHistoryEnabled = false;
        moneyBalanceVisible = true;
        moneySentVisible = true;
        moneyReceivedVisible = true;
        moneyShowNames = true;
        moneyShowAmounts = true;
        moneyX = 0.02f;
        moneyY = 0.02f;
        moneyWidth = 240;
        moneyHeight = 118;
        moneyScale = 1.0f;
        moneyVisibleEntries = 5;
        moneyStyle = MoneyHistoryStyle.CARD.name();
        moneyOpacity = 92;
        hideMapImagePath = "";
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
            if (root.has("passwordCurtainColor") && root.get("passwordCurtainColor").isJsonPrimitive()) {
                passwordCurtainColor = root.get("passwordCurtainColor").getAsInt();
            }
            if (root.has("passwordMaskColor") && root.get("passwordMaskColor").isJsonPrimitive()) {
                passwordMaskColor = root.get("passwordMaskColor").getAsInt();
            }
            if (root.has("passwordMaskBorderColor") && root.get("passwordMaskBorderColor").isJsonPrimitive()) {
                passwordMaskBorderColor = root.get("passwordMaskBorderColor").getAsInt();
            }
            if (root.has("backgroundPath") && root.get("backgroundPath").isJsonPrimitive()) {
                backgroundPath = root.get("backgroundPath").getAsString();
            }
            if (root.has("moneyHistoryEnabled")) moneyHistoryEnabled = root.get("moneyHistoryEnabled").getAsBoolean();
            if (root.has("moneyBalanceVisible")) moneyBalanceVisible = root.get("moneyBalanceVisible").getAsBoolean();
            if (root.has("moneySentVisible")) moneySentVisible = root.get("moneySentVisible").getAsBoolean();
            if (root.has("moneyReceivedVisible")) moneyReceivedVisible = root.get("moneyReceivedVisible").getAsBoolean();
            if (root.has("moneyShowNames")) moneyShowNames = root.get("moneyShowNames").getAsBoolean();
            if (root.has("moneyShowAmounts")) moneyShowAmounts = root.get("moneyShowAmounts").getAsBoolean();
            if (root.has("moneyX")) moneyX = root.get("moneyX").getAsFloat();
            if (root.has("moneyY")) moneyY = root.get("moneyY").getAsFloat();
            if (root.has("moneyWidth")) moneyWidth = root.get("moneyWidth").getAsInt();
            if (root.has("moneyHeight")) moneyHeight = root.get("moneyHeight").getAsInt();
            if (root.has("moneyScale")) moneyScale = root.get("moneyScale").getAsFloat();
            if (root.has("moneyVisibleEntries")) moneyVisibleEntries = root.get("moneyVisibleEntries").getAsInt();
            if (root.has("moneyStyle")) moneyStyle = MoneyHistoryStyle.from(root.get("moneyStyle").getAsString()).name();
            if (root.has("moneyOpacity")) moneyOpacity = root.get("moneyOpacity").getAsInt();
            if (root.has("hideMapImagePath")) hideMapImagePath = root.get("hideMapImagePath").getAsString();
            moneyX = Math.max(0.0f, Math.min(moneyX, 1.0f));
            moneyY = Math.max(0.0f, Math.min(moneyY, 1.0f));
            moneyWidth = Math.max(160, Math.min(moneyWidth, 520));
            moneyHeight = Math.max(80, Math.min(moneyHeight, 320));
            moneyScale = Math.max(0.65f, Math.min(moneyScale, 2.0f));
            moneyVisibleEntries = Math.max(1, Math.min(moneyVisibleEntries, 20));
            moneyOpacity = Math.max(20, Math.min(moneyOpacity, 100));

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
            root.addProperty("passwordCurtainColor", passwordCurtainColor);
            root.addProperty("passwordMaskColor", passwordMaskColor);
            root.addProperty("passwordMaskBorderColor", passwordMaskBorderColor);
            root.addProperty("menuSurfaceColor", menuSurfaceColor);
            root.addProperty("menuCardColor", menuCardColor);
            root.addProperty("menuBorderColor", menuBorderColor);
            root.addProperty("menuGradientStartColor", menuGradientStartColor);
            root.addProperty("menuGradientEndColor", menuGradientEndColor);
            root.addProperty("menuGradientEnabled", menuGradientEnabled);
            root.addProperty("textGradientStartColor", textGradientStartColor);
            root.addProperty("textGradientEndColor", textGradientEndColor);
            root.addProperty("textGradientEnabled", textGradientEnabled);
            root.addProperty("backgroundPath", backgroundPath);
            root.addProperty("moneyHistoryEnabled", moneyHistoryEnabled);
            root.addProperty("moneyBalanceVisible", moneyBalanceVisible);
            root.addProperty("moneySentVisible", moneySentVisible);
            root.addProperty("moneyReceivedVisible", moneyReceivedVisible);
            root.addProperty("moneyShowNames", moneyShowNames);
            root.addProperty("moneyShowAmounts", moneyShowAmounts);
            root.addProperty("moneyX", moneyX);
            root.addProperty("moneyY", moneyY);
            root.addProperty("moneyWidth", moneyWidth);
            root.addProperty("moneyHeight", moneyHeight);
            root.addProperty("moneyScale", moneyScale);
            root.addProperty("moneyVisibleEntries", moneyVisibleEntries);
            root.addProperty("moneyStyle", moneyStyle);
            root.addProperty("moneyOpacity", moneyOpacity);
            root.addProperty("hideMapImagePath", hideMapImagePath);

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

    public static synchronized int getPasswordCurtainColor() { return passwordCurtainColor; }
    public static synchronized void setPasswordCurtainColor(int value) { passwordCurtainColor = value; }
    public static synchronized int getPasswordMaskColor() { return passwordMaskColor; }
    public static synchronized void setPasswordMaskColor(int value) { passwordMaskColor = value | 0xFF000000; }
    public static synchronized int getPasswordMaskBorderColor() { return passwordMaskBorderColor; }
    public static synchronized void setPasswordMaskBorderColor(int value) { passwordMaskBorderColor = value | 0xFF000000; }

    public static synchronized int getMenuSurfaceColor() { return menuSurfaceColor; }
    public static synchronized void setMenuSurfaceColor(int value) { menuSurfaceColor = value | 0xFF000000; }
    public static synchronized int getMenuCardColor() { return menuCardColor; }
    public static synchronized void setMenuCardColor(int value) { menuCardColor = value | 0xFF000000; }
    public static synchronized int getMenuBorderColor() { return menuBorderColor; }
    public static synchronized void setMenuBorderColor(int value) { menuBorderColor = value | 0xFF000000; }
    public static synchronized int getMenuGradientStartColor() { return menuGradientStartColor; }
    public static synchronized void setMenuGradientStartColor(int value) { menuGradientStartColor = value | 0xFF000000; }
    public static synchronized int getMenuGradientEndColor() { return menuGradientEndColor; }
    public static synchronized void setMenuGradientEndColor(int value) { menuGradientEndColor = value | 0xFF000000; }
    public static synchronized boolean isMenuGradientEnabled() { return menuGradientEnabled; }
    public static synchronized void setMenuGradientEnabled(boolean value) { menuGradientEnabled = value; }
    public static synchronized int getTextGradientStartColor() { return textGradientStartColor; }
    public static synchronized void setTextGradientStartColor(int value) { textGradientStartColor = value | 0xFF000000; }
    public static synchronized int getTextGradientEndColor() { return textGradientEndColor; }
    public static synchronized void setTextGradientEndColor(int value) { textGradientEndColor = value | 0xFF000000; }
    public static synchronized boolean isTextGradientEnabled() { return textGradientEnabled; }
    public static synchronized void setTextGradientEnabled(boolean value) { textGradientEnabled = value; }

    public static synchronized String getBackgroundPath() {
        return backgroundPath;
    }

    public static synchronized void setBackgroundPath(String value) {
        backgroundPath = value == null ? "" : value.trim();
    }

    public static synchronized boolean isMoneyHistoryEnabled() { return moneyHistoryEnabled; }
    public static synchronized void setMoneyHistoryEnabled(boolean value) { moneyHistoryEnabled = value; }
    public static synchronized boolean isMoneyBalanceVisible() { return moneyBalanceVisible; }
    public static synchronized void setMoneyBalanceVisible(boolean value) { moneyBalanceVisible = value; }
    public static synchronized boolean isMoneySentVisible() { return moneySentVisible; }
    public static synchronized void setMoneySentVisible(boolean value) { moneySentVisible = value; }
    public static synchronized boolean isMoneyReceivedVisible() { return moneyReceivedVisible; }
    public static synchronized void setMoneyReceivedVisible(boolean value) { moneyReceivedVisible = value; }
    public static synchronized boolean isMoneyShowNames() { return moneyShowNames; }
    public static synchronized void setMoneyShowNames(boolean value) { moneyShowNames = value; }
    public static synchronized boolean isMoneyShowAmounts() { return moneyShowAmounts; }
    public static synchronized void setMoneyShowAmounts(boolean value) { moneyShowAmounts = value; }
    public static synchronized float getMoneyX() { return moneyX; }
    public static synchronized void setMoneyX(float value) { moneyX = Math.max(0.0f, Math.min(value, 1.0f)); }
    public static synchronized float getMoneyY() { return moneyY; }
    public static synchronized void setMoneyY(float value) { moneyY = Math.max(0.0f, Math.min(value, 1.0f)); }
    public static synchronized int getMoneyWidth() { return moneyWidth; }
    public static synchronized void setMoneyWidth(int value) { moneyWidth = Math.max(160, Math.min(value, 520)); }
    public static synchronized int getMoneyHeight() { return moneyHeight; }
    public static synchronized void setMoneyHeight(int value) { moneyHeight = Math.max(80, Math.min(value, 320)); }
    public static synchronized float getMoneyScale() { return moneyScale; }
    public static synchronized void setMoneyScale(float value) { moneyScale = Math.max(0.65f, Math.min(value, 2.0f)); }
    public static synchronized int getMoneyVisibleEntries() { return moneyVisibleEntries; }
    public static synchronized void setMoneyVisibleEntries(int value) { moneyVisibleEntries = Math.max(1, Math.min(value, 20)); }
    public static synchronized String getMoneyStyle() { return moneyStyle; }
    public static synchronized void setMoneyStyle(String value) { moneyStyle = MoneyHistoryStyle.from(value).name(); }
    public static synchronized int getMoneyOpacity() { return moneyOpacity; }
    public static synchronized void setMoneyOpacity(int value) { moneyOpacity = Math.max(20, Math.min(value, 100)); }
    public static synchronized String getHideMapImagePath() { return hideMapImagePath; }
    public static synchronized void setHideMapImagePath(String value) { hideMapImagePath = value == null ? "" : value.trim(); }

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
