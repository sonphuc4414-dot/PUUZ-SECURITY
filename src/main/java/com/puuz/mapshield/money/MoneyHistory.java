package com.puuz.mapshield.money;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-only payment history. Records are stored locally and are intentionally
 * never capped or uploaded anywhere. The visible HUD entry count is separate
 * from storage, so old transactions remain available forever until the user
 * explicitly clears them.
 */
public final class MoneyHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = com.puuz.mapshield.config.MapShieldConfigPathHelper.moneyHistoryPath();

    private static final Pattern PAY_COMMAND = Pattern.compile(
            "^\\s*/?pay\\s+([^\\s]+)\\s+([+\\-]?[$€£]?\\d[\\d,]*(?:\\.\\d+)?(?:[kKmMbB])?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INCOMING_1 = Pattern.compile(
            "^.*?([A-Za-z0-9_]{2,32})\\s+(?:paid|sent|gave)\\s+you\\s+([$€£]?\\d[\\d,]*(?:\\.\\d+)?(?:[kKmMbB])?).*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INCOMING_2 = Pattern.compile(
            "^.*?(?:received|got)\\s+([$€£]?\\d[\\d,]*(?:\\.\\d+)?(?:[kKmMbB])?)\\s+(?:from|by)\\s+([A-Za-z0-9_]{2,32}).*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BALANCE = Pattern.compile(
            "^.*?(?:balance|bal(?:ance)?|money)\\s*[:=]?\\s*([$€£]?\\d[\\d,]*(?:\\.\\d+)?(?:[kKmMbB])?).*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<Transaction> HISTORY = new ArrayList<>();
    private static String currentServer = "singleplayer";
    private static String balance = "";
    private static boolean loaded;

    private MoneyHistory() {
    }

    public static synchronized void init() {
        if (loaded) return;
        loaded = true;
        load();

        ClientSendMessageEvents.COMMAND.register(MoneyHistory::onSendCommand);
        ClientReceiveMessageEvents.GAME.register(MoneyHistory::onReceiveGameMessage);
    }

    public static synchronized void onServerContextChanged() {
        currentServer = PuuzMapShieldClient.currentServerKey();
    }

    private static void onSendCommand(String command) {
        if (!MapShieldConfig.isMoneyHistoryEnabled()) return;
        String value = "/" + (command == null ? "" : command.trim());
        Matcher matcher = PAY_COMMAND.matcher(value);
        if (!matcher.matches()) return;

        String player = matcher.group(1);
        String amount = matcher.group(2);
        record(TransactionDirection.SENT, player, amount, currentServer);
    }

    private static void onReceiveGameMessage(Text message, boolean overlay) {
        if (message == null) return;
        String text = message.getString();
        parseIncoming(text);
        parseBalance(text);
    }

    private static synchronized void parseIncoming(String text) {
        Matcher m = INCOMING_1.matcher(text);
        if (m.matches()) {
            record(TransactionDirection.RECEIVED, m.group(1), m.group(2), currentServer);
            return;
        }
        m = INCOMING_2.matcher(text);
        if (m.matches()) {
            record(TransactionDirection.RECEIVED, m.group(2), m.group(1), currentServer);
        }
    }

    private static synchronized void parseBalance(String text) {
        Matcher m = BALANCE.matcher(text);
        if (m.matches()) {
            balance = normalizeAmount(m.group(1));
        }
    }

    public static synchronized void record(TransactionDirection direction, String player, String amount, String server) {
        if (player == null || player.isBlank() || amount == null || amount.isBlank()) return;
        Transaction tx = new Transaction(
                System.currentTimeMillis(),
                server == null || server.isBlank() ? "singleplayer" : server,
                player.trim(),
                normalizeAmount(amount),
                direction.name()
        );
        HISTORY.add(tx);
        save();
    }

    public static synchronized List<Transaction> getForCurrentServer() {
        String server = currentServer == null ? "singleplayer" : currentServer;
        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : HISTORY) {
            if (server.equalsIgnoreCase(tx.server())) result.add(tx);
        }
        return Collections.unmodifiableList(result);
    }

    public static synchronized String getBalance() {
        return balance;
    }

    public static synchronized long getTotalStoredTransactions() {
        return HISTORY.size();
    }

    public static synchronized void clearAllHistory() {
        HISTORY.clear();
        save();
    }

    private static String normalizeAmount(String value) {
        String s = value.trim().replace(",", "");
        if (s.startsWith("$") || s.startsWith("€") || s.startsWith("£")) s = s.substring(1);
        if (s.isBlank()) return value.trim();
        char suffix = Character.toLowerCase(s.charAt(s.length() - 1));
        if (suffix == 'k' || suffix == 'm' || suffix == 'b') {
            String numeric = s.substring(0, s.length() - 1);
            try {
                BigDecimal base = new BigDecimal(numeric);
                BigDecimal multiplier = suffix == 'k' ? BigDecimal.valueOf(1000) : suffix == 'm' ? BigDecimal.valueOf(1_000_000) : BigDecimal.valueOf(1_000_000_000);
                return "$" + base.multiply(multiplier).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return "$" + s;
            }
        }
        return "$" + s;
    }

    private static synchronized void load() {
        try {
            if (!Files.isRegularFile(FILE)) return;
            JsonObject root = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            JsonElement b = root.get("lastKnownBalance");
            if (b != null && b.isJsonPrimitive()) balance = b.getAsString();
            JsonElement array = root.get("transactions");
            if (array == null || !array.isJsonArray()) return;
            for (JsonElement element : array.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                try {
                    HISTORY.add(new Transaction(
                            o.get("timestamp").getAsLong(),
                            o.get("server").getAsString(),
                            o.get("player").getAsString(),
                            o.get("amount").getAsString(),
                            o.get("direction").getAsString()
                    ));
                } catch (RuntimeException ignored) {
                }
            }
        } catch (Exception ignored) {
            HISTORY.clear();
            balance = "";
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("lastKnownBalance", balance);
            root.addProperty("storagePolicy", "unlimited-local");
            JsonArray array = new JsonArray();
            for (Transaction tx : HISTORY) {
                JsonObject o = new JsonObject();
                o.addProperty("timestamp", tx.timestamp());
                o.addProperty("server", tx.server());
                o.addProperty("player", tx.player());
                o.addProperty("amount", tx.amount());
                o.addProperty("direction", tx.direction());
                array.add(o);
            }
            root.add("transactions", array);
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temp, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }

    public enum TransactionDirection { SENT, RECEIVED }

    public record Transaction(long timestamp, String server, String player, String amount, String direction) {
    }
}
